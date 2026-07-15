package app.vidown

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp
class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        try {
            val updateFile = File(filesDir, "yt-dlp.zip")
            if (updateFile.exists()) {
                val py = Python.getInstance()
                val module = py.getModule("bridge")
                module.callAttr("init_update_path", updateFile.absolutePath)
            }
        } catch (e: Exception) {
        }
    }
}
