package app.vidown.data.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

object CustomStorageHelper {

    fun isUriAccessible(context: Context, treeUriStr: String): Boolean {
        return try {
            val uri = Uri.parse(treeUriStr)
            val docFile = DocumentFile.fromTreeUri(context, uri)
            docFile != null && docFile.exists() && docFile.canWrite()
        } catch (e: Exception) {
            false
        }
    }

    fun saveFileToUri(context: Context, treeUriStr: String, tempFile: File, mimeType: String): Uri? {
        return try {
            val uri = Uri.parse(treeUriStr)
            val directory = DocumentFile.fromTreeUri(context, uri) ?: return null
            if (!directory.exists() || !directory.canWrite()) return null

            val displayName = tempFile.name
            val docFile = directory.findFile(displayName) ?: directory.createFile(mimeType, displayName) ?: return null

            context.contentResolver.openOutputStream(docFile.uri)?.use { outputStream ->
                tempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            docFile.uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getDirectoryName(context: Context, treeUriStr: String): String? {
        return try {
            val uri = Uri.parse(treeUriStr)
            val docFile = DocumentFile.fromTreeUri(context, uri)
            docFile?.name
        } catch (e: Exception) {
            null
        }
    }
}
