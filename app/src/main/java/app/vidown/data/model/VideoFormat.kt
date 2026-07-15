package app.vidown.data.model

data class VideoFormat(
    val formatId: String,
    val note: String,
    val ext: String,
    val isAudioOnly: Boolean
)
