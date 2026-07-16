package app.vidown.data.python

import app.vidown.data.model.VideoFormat
import app.vidown.data.model.VideoInfo
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python

class PythonBridge {

    fun fetchInfo(url: String): VideoInfo {
        val py = Python.getInstance()
        val module = py.getModule("bridge")
        try {
            val result = module.callAttr("fetch_info", url)
            val title = result.asStringOrNull("title") ?: "Unknown Title"
            val thumbnail = result.asStringOrNull("thumbnail") ?: ""
            val uploader = result.asStringOrNull("uploader") ?: "Unknown Uploader"
            val durationSeconds = result.asLongOrDefault("duration", 0L)
            val extractor = result.asStringOrNull("extractor") ?: "Unknown"

            val platform = when {
                extractor.contains("youtube", ignoreCase = true) -> "YouTube"
                extractor.contains("twitter", ignoreCase = true) || extractor.contains("x", ignoreCase = true) -> "X / Twitter"
                extractor.contains("instagram", ignoreCase = true) -> "Instagram"
                extractor.contains("tiktok", ignoreCase = true) -> "TikTok"
                else -> extractor.lowercase().replaceFirstChar { it.uppercase() }
            }

            val formatsListObj = result.callAttr("get", "formats")
            val formats = if (formatsListObj != null && formatsListObj.toString() != "None") {
                formatsListObj.asList().map { formatObj ->
                    val formatId = formatObj.asStringOrNull("id") ?: "best"
                    val note = formatObj.asStringOrNull("note") ?: "Best"
                    val ext = formatObj.asStringOrNull("ext") ?: "mp4"
                    val isAudio = formatObj.asBooleanOrDefault("is_audio", false)
                    VideoFormat(formatId, note, ext, isAudio)
                }
            } else {
                listOf(VideoFormat("best", "Best", "mp4", false))
            }

            return VideoInfo(
                url = url,
                title = title,
                thumbnailUrl = thumbnail,
                platform = platform,
                durationSeconds = durationSeconds,
                formats = formats,
                uploader = uploader
            )
        } catch (e: PyException) {
            throw Exception(e.message ?: "Failed to extract video information")
        }
    }

    private fun PyObject.asStringOrNull(key: String): String? {
        val valObj = this.callAttr("get", key)
        return if (valObj == null || valObj.toString() == "None") null else valObj.toString()
    }

    private fun PyObject.asLongOrDefault(key: String, default: Long): Long {
        val valObj = this.callAttr("get", key)
        return if (valObj == null || valObj.toString() == "None") default else valObj.toLong()
    }

    private fun PyObject.asBooleanOrDefault(key: String, default: Boolean): Boolean {
        val valObj = this.callAttr("get", key)
        return if (valObj == null || valObj.toString() == "None") default else valObj.toBoolean()
    }

    fun startDownload(
        url: String,
        formatPreset: String,
        outputPath: String,
        ffmpegDir: String,
        callback: ProgressCallback,
        cookiesPath: String?,
        embedSubtitles: Boolean,
        subLangs: String,
        ratelimit: Long?
    ) {
        val py = Python.getInstance()
        val module = py.getModule("bridge")
        try {
            module.callAttr("download", url, formatPreset, outputPath, ffmpegDir, callback, cookiesPath, embedSubtitles, subLangs, ratelimit)
        } catch (e: PyException) {
            throw Exception(e.message ?: "Python execution error during download")
        }
    }
}
