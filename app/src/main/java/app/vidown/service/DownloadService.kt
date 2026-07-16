package app.vidown.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.vidown.data.db.DownloadDao
import app.vidown.data.db.DownloadEntity
import app.vidown.data.ffmpeg.FfmpegExtractor
import app.vidown.data.model.DownloadStatus
import app.vidown.data.python.ProgressCallback
import app.vidown.data.python.PythonBridge
import app.vidown.data.storage.MediaStoreManager
import app.vidown.data.storage.StorageHelper
import app.vidown.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import app.vidown.data.python.CookieHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.vidown.data.storage.CustomStorageHelper
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var downloadDao: DownloadDao

    @Inject
    lateinit var pythonBridge: PythonBridge

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var isRunning = false
    private lateinit var storageHelper: StorageHelper
    private lateinit var ffmpegExtractor: FfmpegExtractor

    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val activeProgress = ConcurrentHashMap<Long, Triple<String, Int, DownloadStatus>>()

    override fun onCreate() {
        super.onCreate()
        storageHelper = StorageHelper(this)
        ffmpegExtractor = FfmpegExtractor(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Starting downloader...", 0))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        processQueue()
        return START_STICKY
    }

    private fun processQueue() {
        synchronized(this) {
            if (isRunning) return
            isRunning = true
        }

        serviceScope.launch {
            try {
                while (true) {
                    val maxConcurrent = dataStore.data.map { preferences ->
                        preferences[intPreferencesKey("max_concurrent_downloads")] ?: 3
                    }.first()

                    if (activeJobs.size < maxConcurrent) {
                        val task = downloadDao.getNextQueuedDownload()
                        if (task != null) {
                            val taskId = task.id
                            if (!activeJobs.containsKey(taskId)) {
                                val job = launch {
                                    runDownload(task)
                                }
                                activeJobs[taskId] = job
                            }
                            continue
                        }
                    }

                    if (activeJobs.isEmpty()) {
                        val nextQueued = downloadDao.getNextQueuedDownload()
                        if (nextQueued == null) {
                            break
                        }
                    }

                    delay(500)
                }
            } finally {
                synchronized(this@DownloadService) {
                    isRunning = false
                }
                stopSelf()
            }
        }
    }

    private suspend fun runDownload(task: DownloadEntity) {
        val id = task.id
        try {
            val currentTask = downloadDao.getDownloadById(id) ?: return
            if (currentTask.status == DownloadStatus.CANCELLED) return
            downloadDao.updateDownload(currentTask.copy(status = DownloadStatus.DOWNLOADING, progressPercent = 0))

            updateActiveProgress(id, currentTask.title, 0, DownloadStatus.DOWNLOADING)

            val ffmpegDir = ffmpegExtractor.getFfmpegPath()
            if (ffmpegDir == null) {
                updateTaskError(id, "FFmpeg binary unavailable for this device architecture")
                return
            }

            val outFolder = File(cacheDir, "vidown_temp_$id").apply { if (!exists()) mkdirs() }
            val templatePath = File(outFolder, "%(title)s.%(ext)s").absolutePath

            val callback = object : ProgressCallback {
                override fun onProgress(percent: Int, status: String) {
                    val dbTask = kotlinx.coroutines.runBlocking {
                        downloadDao.getDownloadById(id)
                    }
                    if (dbTask == null || dbTask.status == DownloadStatus.CANCELLED) {
                        throw RuntimeException("CANCELLED")
                    }

                    val resolvedStatus = when (status) {
                        "DOWNLOADING" -> DownloadStatus.DOWNLOADING
                        "MERGING" -> DownloadStatus.MERGING
                        else -> dbTask.status
                    }

                    kotlinx.coroutines.runBlocking {
                        downloadDao.updateDownload(
                            dbTask.copy(
                                status = resolvedStatus,
                                progressPercent = percent
                            )
                        )
                    }

                    updateActiveProgress(id, dbTask.title, percent, resolvedStatus)
                }
            }

            val embedSubtitles = dataStore.data.map { preferences ->
                preferences[booleanPreferencesKey("embed_subtitles")] ?: false
            }.first()
            val cookiesPath = CookieHelper.getCombinedCookiesPath(this)

            try {
                withContext(Dispatchers.IO) {
                    pythonBridge.startDownload(
                        url = currentTask.url,
                        formatPreset = currentTask.selectedQuality,
                        outputPath = templatePath,
                        ffmpegDir = ffmpegDir,
                        callback = callback,
                        cookiesPath = cookiesPath,
                        embedSubtitles = embedSubtitles
                    )
                }

                val updatedTask = downloadDao.getDownloadById(id)
                if (updatedTask != null && updatedTask.status != DownloadStatus.CANCELLED) {
                    val matchFile = findDownloadedFile(outFolder)
                    if (matchFile != null) {
                        val ext = matchFile.extension.lowercase()
                        val isVideo = ext in listOf("mp4", "mkv", "webm")
                        val mimeType = when (ext) {
                            "mp3" -> "audio/mpeg"
                            "m4a" -> "audio/mp4"
                            "webm" -> if (isVideo) "video/webm" else "audio/webm"
                            "mkv" -> "video/x-matroska"
                            else -> "video/mp4"
                        }

                        val fileLength = matchFile.length()
                        val customDir = dataStore.data.map { preferences ->
                            preferences[stringPreferencesKey("custom_download_dir")]
                        }.first()

                        var savedPath: String? = null
                        if (!customDir.isNullOrEmpty()) {
                            val savedUri = CustomStorageHelper.saveFileToUri(
                                context = this@DownloadService,
                                treeUriStr = customDir,
                                tempFile = matchFile,
                                mimeType = mimeType
                            )
                            if (savedUri != null) {
                                savedPath = savedUri.toString()
                            } else {
                                showStatusWarningNotification("Folder unavailable. Saving to default library folder.")
                            }
                        }

                        if (savedPath == null) {
                            savedPath = MediaStoreManager.saveFile(
                                context = this@DownloadService,
                                tempFile = matchFile,
                                title = updatedTask.title,
                                mimeType = mimeType,
                                isVideo = isVideo
                            )
                        }

                        if (matchFile.exists()) {
                            matchFile.delete()
                        }
                        try {
                            outFolder.delete()
                        } catch (ignored: Exception) {}

                        if (savedPath != null) {
                            downloadDao.updateDownload(
                                updatedTask.copy(
                                    status = DownloadStatus.DONE,
                                    progressPercent = 100,
                                    filePath = savedPath,
                                    fileSizeBytes = fileLength
                                )
                            )
                            showCompletionNotification(updatedTask.title, savedPath)
                        } else {
                            updateTaskError(id, "Failed to save downloaded file")
                        }
                    } else {
                        updateTaskError(id, "Finished download but failed to locate output file")
                    }
                }
            } catch (e: Exception) {
                val causeMessage = e.cause?.message ?: e.message ?: ""
                if (causeMessage.contains("CANCELLED") || e.message?.contains("CANCELLED") == true) {
                    val cancelledTask = downloadDao.getDownloadById(id)
                    if (cancelledTask != null) {
                        downloadDao.updateDownload(cancelledTask.copy(status = DownloadStatus.CANCELLED))
                    }
                } else {
                    updateTaskError(id, e.message ?: "Unknown download error")
                }
            }
        } finally {
            activeProgress.remove(id)
            activeJobs.remove(id)
        }
    }

    private fun updateActiveProgress(id: Long, title: String, percent: Int, status: DownloadStatus) {
        activeProgress[id] = Triple(title, percent, status)

        val activeCount = activeJobs.size
        if (activeCount <= 1) {
            val singleTask = activeProgress.values.firstOrNull()
            if (singleTask != null) {
                val label = when (singleTask.third) {
                    DownloadStatus.MERGING -> "Merging video and audio..."
                    else -> "Downloading: ${singleTask.second}%"
                }
                updateNotification("${singleTask.first}\n$label", singleTask.second)
            } else {
                updateNotification("Downloading...", 0)
            }
        } else {
            val totalPercent = activeProgress.values.sumOf { it.second }
            val avgPercent = if (activeProgress.isNotEmpty()) totalPercent / activeProgress.size else 0
            val summary = activeProgress.values.joinToString(" | ") {
                val label = if (it.third == DownloadStatus.MERGING) "Merging" else "${it.second}%"
                "${it.first.take(12)}...: $label"
            }
            updateNotification("Downloading $activeCount items ($avgPercent%)\n$summary", avgPercent)
        }
    }

    private fun findDownloadedFile(directory: File): File? {
        val files = directory.listFiles() ?: return null
        return files.firstOrNull { file ->
            val ext = file.extension.lowercase()
            file.isFile && ext in listOf("mp4", "mkv", "webm", "mp3", "m4a", "ogg", "flac")
        }
    }

    private suspend fun updateTaskError(id: Long, error: String) {
        val dbTask = downloadDao.getDownloadById(id)
        if (dbTask != null && dbTask.status != DownloadStatus.CANCELLED) {
            downloadDao.updateDownload(
                dbTask.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = error
                )
            )
            showFailureNotification(id, dbTask.title)
        }
    }

    private fun updateNotification(content: String, progress: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(content, progress))
    }

    private fun createNotification(content: String, progress: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vidown Downloader")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, progress, progress == 0 && isRunning)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Download queue notifications",
                NotificationManager.IMPORTANCE_LOW
            )
            val statusChannel = NotificationChannel(
                STATUS_CHANNEL_ID, "Download completion alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows notifications when a download completes or fails"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(statusChannel)
        }
    }

    private fun showCompletionNotification(title: String, filePath: String) {
        val playIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "app.vidown.action.PLAY"
            putExtra("file_path", filePath)
        }
        val playPendingIntent = PendingIntent.getBroadcast(
            this, filePath.hashCode(), playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val shareIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "app.vidown.action.SHARE"
            putExtra("file_path", filePath)
        }
        val sharePendingIntent = PendingIntent.getBroadcast(
            this, filePath.hashCode() + 1, shareIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, STATUS_CHANNEL_ID)
            .setContentTitle("Download Completed")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(appIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_play, "Play", playPendingIntent)
            .addAction(android.R.drawable.ic_menu_share, "Share", sharePendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(filePath.hashCode(), notification)
    }

    private fun showFailureNotification(id: Long, title: String) {
        val retryIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "app.vidown.action.RETRY"
            putExtra("task_id", id)
        }
        val retryPendingIntent = PendingIntent.getBroadcast(
            this, id.toInt(), retryIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, STATUS_CHANNEL_ID)
            .setContentTitle("Download Failed")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentIntent(appIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_rotate, "Retry", retryPendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id.toInt(), notification)
    }

    private fun showStatusWarningNotification(message: String) {
        val notification = NotificationCompat.Builder(this, STATUS_CHANNEL_ID)
            .setContentTitle("Vidown Storage Warning")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(202, notification)
    }

    override fun onDestroy() {
        serviceJob.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "download_channel"
        private const val STATUS_CHANNEL_ID = "download_status"
        private const val NOTIFICATION_ID = 101
    }
}
