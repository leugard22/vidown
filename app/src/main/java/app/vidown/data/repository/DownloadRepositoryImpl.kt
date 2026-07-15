package app.vidown.data.repository

import android.content.Context
import android.content.Intent
import app.vidown.data.db.DownloadDao
import app.vidown.data.db.DownloadEntity
import app.vidown.data.model.DownloadItem
import app.vidown.data.model.DownloadStatus
import app.vidown.data.model.VideoFormat
import app.vidown.data.model.VideoInfo
import app.vidown.data.python.PythonBridge
import app.vidown.service.DownloadService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DownloadRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val pythonBridge: PythonBridge
) : DownloadRepository {

    override fun observeAllDownloads(): Flow<List<DownloadItem>> {
        return downloadDao.getAllDownloads().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeActiveDownloads(): Flow<List<DownloadItem>> {
        return downloadDao.getActiveDownloads().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun fetchVideoInfo(url: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        try {
            val info = pythonBridge.fetchInfo(url)
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun enqueueDownload(url: String, format: VideoFormat, info: VideoInfo): Long {
        val entity = DownloadEntity(
            url = url,
            title = info.title,
            thumbnailUrl = info.thumbnailUrl,
            platform = info.platform,
            durationSeconds = info.durationSeconds,
            selectedQuality = format.formatId,
            status = DownloadStatus.QUEUED,
            progressPercent = 0,
            filePath = null,
            fileSizeBytes = 0L,
            errorMessage = null,
            createdAt = System.currentTimeMillis()
        )
        val id = downloadDao.insertDownload(entity)
        startDownloadService()
        return id
    }

    override suspend fun cancelDownload(id: Long) {
        val entity = downloadDao.getDownloadById(id)
        if (entity != null && (entity.status == DownloadStatus.QUEUED || entity.status == DownloadStatus.DOWNLOADING || entity.status == DownloadStatus.MERGING)) {
            downloadDao.updateDownload(entity.copy(status = DownloadStatus.CANCELLED))
        }
    }

    override suspend fun deleteDownload(id: Long) {
        downloadDao.deleteDownloadById(id)
    }

    override suspend fun retryDownload(id: Long) {
        val entity = downloadDao.getDownloadById(id)
        if (entity != null) {
            downloadDao.updateDownload(
                entity.copy(
                    status = DownloadStatus.QUEUED,
                    progressPercent = 0,
                    errorMessage = null
                )
            )
            startDownloadService()
        }
    }

    override suspend fun clearCompletedDownloads() {
        downloadDao.deleteCompletedDownloads()
    }

    override suspend fun cancelAllActiveDownloads() {
        downloadDao.cancelAllActiveDownloads()
    }

    private fun startDownloadService() {
        val intent = Intent(context, DownloadService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun DownloadEntity.toDomain() = DownloadItem(
        id = id,
        url = url,
        title = title,
        thumbnailUrl = thumbnailUrl,
        platform = platform,
        durationSeconds = durationSeconds,
        selectedQuality = selectedQuality,
        status = status,
        progressPercent = progressPercent,
        filePath = filePath,
        fileSizeBytes = fileSizeBytes,
        errorMessage = errorMessage,
        createdAt = createdAt
    )
}
