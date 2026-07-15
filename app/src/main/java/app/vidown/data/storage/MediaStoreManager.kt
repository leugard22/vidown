package app.vidown.data.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object MediaStoreManager {

    fun saveFile(
        context: Context,
        tempFile: File,
        title: String,
        mimeType: String,
        isVideo: Boolean
    ): String? {
        val resolver = context.contentResolver

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (isVideo) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
        } else {
            if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
        }

        val relativePath = if (isVideo) {
            "${Environment.DIRECTORY_MOVIES}/Vidown"
        } else {
            "${Environment.DIRECTORY_MUSIC}/Vidown"
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${title}.${tempFile.extension}")
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                val directory = if (isVideo) {
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                } else {
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                }
                val vidownDir = File(directory, "Vidown")
                if (!vidownDir.exists()) {
                    vidownDir.mkdirs()
                }
                put(MediaStore.MediaColumns.DATA, File(vidownDir, "${title}.${tempFile.extension}").absolutePath)
            }
        }

        var uri: Uri? = null
        try {
            uri = resolver.insert(collection, contentValues) ?: return null

            resolver.openOutputStream(uri)?.use { outputStream ->
                tempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            val realPath = getRealPathFromURI(context, uri)
            return realPath ?: uri.toString()

        } catch (e: Exception) {
            e.printStackTrace()
            uri?.let {
                try {
                    resolver.delete(it, null, null)
                } catch (ignored: Exception) {}
            }
            return null
        }
    }

    private fun getRealPathFromURI(context: Context, contentUri: Uri): String? {
        var cursor: android.database.Cursor? = null
        try {
            val proj = arrayOf(MediaStore.MediaColumns.DATA)
            cursor = context.contentResolver.query(contentUri, proj, null, null, null)
            val columnIndex = cursor?.getColumnIndex(MediaStore.MediaColumns.DATA)
            if (columnIndex != null && columnIndex != -1 && cursor.moveToFirst()) {
                return cursor.getString(columnIndex)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return null
    }
}
