package app.vidown.data.model

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    MERGING,
    DONE,
    FAILED,
    CANCELLED
}
