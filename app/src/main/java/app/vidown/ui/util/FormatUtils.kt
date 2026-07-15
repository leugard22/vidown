package app.vidown.ui.util

import java.util.Locale

fun Long.formatDuration(): String {
    if (this <= 0L) return "0:00"
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val secs = this % 60

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, secs)
    }
}

fun Long.formatFileSize(): String {
    if (this <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = this.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.lastIndex) {
        size /= 1024
        unitIndex++
    }
    return String.format(Locale.getDefault(), "%.1f %s", size, units[unitIndex])
}
