package app.vidown.data.ffmpeg

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FfmpegExtractor(private val context: Context) {

    suspend fun getFfmpegPath(): String? = withContext(Dispatchers.IO) {
        val binDir = File(context.filesDir, "bin")
        if (!binDir.exists()) {
            binDir.mkdirs()
        }
        val ffmpegFile = File(binDir, "ffmpeg")

        if (ffmpegFile.exists() && ffmpegFile.canExecute()) {
            return@withContext binDir.absolutePath
        }

        val abi = getSupportedAbi() ?: return@withContext null
        val assetPath = "bin/$abi/ffmpeg"

        try {
            context.assets.open(assetPath).use { inputStream ->
                FileOutputStream(ffmpegFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            ffmpegFile.setExecutable(true, false)
            if (ffmpegFile.canExecute()) {
                binDir.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getSupportedAbi(): String? {
        for (abi in Build.SUPPORTED_ABIS) {
            if (abi == "arm64-v8a") return "arm64-v8a"
            if (abi == "x86_64") return "x86_64"
        }
        return null
    }
}
