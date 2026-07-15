package app.vidown.data.storage

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.io.File

class StorageHelper(private val context: Context) {

    fun getDownloadFolder(): File {
        val root = Environment.getExternalStorageDirectory()
        val folder = File(root, "Vidown")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true
        } else {
            val readPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            val writePermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            readPermission && writePermission
        }
    }

    fun requestStoragePermissionIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            } catch (e: Exception) {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            }
        } else {
            null
        }
    }

    fun scanFile(file: File, mimeType: String? = null, onScanComplete: ((Uri?) -> Unit)? = null) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            mimeType?.let { arrayOf(it) },
            { _, uri ->
                onScanComplete?.invoke(uri)
            }
        )
    }
}
