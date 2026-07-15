package app.vidown.data.model

data class VideoInfo(
    val url: String,
    val title: String,
    val thumbnailUrl: String,
    val platform: String,
    val durationSeconds: Long,
    val formats: List<VideoFormat>,
    val uploader: String
)
