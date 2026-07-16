package app.vidown.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import app.vidown.data.db.DownloadDao
import app.vidown.data.model.DownloadStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var downloadDao: DownloadDao

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val taskId = intent.getLongExtra("task_id", -1L)
        val filePath = intent.getStringExtra("file_path") ?: ""

        when (action) {
            "app.vidown.action.PLAY" -> {
                if (filePath.isNotEmpty()) {
                    playFile(context, filePath)
                }
            }
            "app.vidown.action.SHARE" -> {
                if (filePath.isNotEmpty()) {
                    shareFile(context, filePath)
                }
            }
            "app.vidown.action.RETRY" -> {
                if (taskId != -1L) {
                    retryDownload(context, taskId)
                }
            }
        }
    }

    private fun playFile(context: Context, filePath: String) {
        try {
            val uri = if (filePath.startsWith("content://")) {
                Uri.parse(filePath)
            } else {
                val file = File(filePath)
                if (!file.exists()) return
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }

            val mimeType = getMimeType(filePath)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareFile(context: Context, filePath: String) {
        try {
            val uri = if (filePath.startsWith("content://")) {
                Uri.parse(filePath)
            } else {
                val file = File(filePath)
                if (!file.exists()) return
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }

            val mimeType = getMimeType(filePath)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share media file").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun retryDownload(context: Context, taskId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val task = downloadDao.getDownloadById(taskId) ?: return@launch
            downloadDao.updateDownload(
                task.copy(
                    status = DownloadStatus.QUEUED,
                    progressPercent = 0,
                    errorMessage = null
                )
            )
            val serviceIntent = Intent(context, DownloadService::class.java)
            context.startService(serviceIntent)
        }
    }

    private fun getMimeType(url: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "video/*"
    }
}
