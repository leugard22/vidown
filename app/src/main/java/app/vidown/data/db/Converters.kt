package app.vidown.data.db

import androidx.room.TypeConverter
import app.vidown.data.model.DownloadStatus

class Converters {
    @TypeConverter
    fun fromStatus(status: DownloadStatus): String {
        return status.name
    }

    @TypeConverter
    fun toStatus(value: String): DownloadStatus {
        return DownloadStatus.valueOf(value)
    }
}
