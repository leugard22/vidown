package app.vidown.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.vidown.data.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
