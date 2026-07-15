package app.vidown.data.model

data class DownloadItem(
    val id: Long,
    val url: String,
    val title: String,
    val thumbnailUrl: String,
    val platform: String,
    val durationSeconds: Long,
    val selectedQuality: String,
    val status: DownloadStatus,
    val progressPercent: Int,
    val filePath: String?,
    val fileSizeBytes: Long,
    val errorMessage: String?,
    val createdAt: Long
)
