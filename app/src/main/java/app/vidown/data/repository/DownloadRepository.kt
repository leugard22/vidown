package app.vidown.data.repository

import app.vidown.data.model.DownloadItem
import app.vidown.data.model.VideoFormat
import app.vidown.data.model.VideoInfo
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun observeAllDownloads(): Flow<List<DownloadItem>>
    fun observeActiveDownloads(): Flow<List<DownloadItem>>
    suspend fun fetchVideoInfo(url: String): Result<VideoInfo>
    suspend fun enqueueDownload(url: String, format: VideoFormat, info: VideoInfo): Long
    suspend fun cancelDownload(id: Long)
    suspend fun deleteDownload(id: Long)
    suspend fun retryDownload(id: Long)
    suspend fun clearCompletedDownloads()
    suspend fun cancelAllActiveDownloads()
}
