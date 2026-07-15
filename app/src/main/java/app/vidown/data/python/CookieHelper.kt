package app.vidown.data.python

import android.content.Context
import java.io.File

object CookieHelper {
    fun saveNetscapeCookies(context: Context, siteName: String, cookieString: String) {
        val domain = when (siteName.lowercase()) {
            "youtube" -> ".youtube.com"
            "instagram" -> ".instagram.com"
            "twitter", "x" -> ".twitter.com"
            "tiktok" -> ".tiktok.com"
            else -> ""
        }
        if (domain.isEmpty() || cookieString.isBlank()) return

        val cookieFile = File(context.filesDir, "cookies_${siteName.lowercase()}.txt")
        val builder = StringBuilder()
        builder.append("# Netscape HTTP Cookie File\n")
        builder.append("# This file is generated automatically by Vidown\n\n")

        val pairs = cookieString.split(";")
        for (pair in pairs) {
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) {
                val name = parts[0].trim()
                val value = parts[1].trim()
                builder.append("$domain\tTRUE\t/\tTRUE\t2147483647\t$name\t$value\n")
            }
        }

        cookieFile.writeText(builder.toString())
        combineAllCookies(context)
    }

    fun deleteCookies(context: Context, siteName: String) {
        val cookieFile = File(context.filesDir, "cookies_${siteName.lowercase()}.txt")
        if (cookieFile.exists()) {
            cookieFile.delete()
        }
        combineAllCookies(context)
    }

    fun hasCookies(context: Context, siteName: String): Boolean {
        val cookieFile = File(context.filesDir, "cookies_${siteName.lowercase()}.txt")
        return cookieFile.exists() && cookieFile.length() > 50
    }

    fun combineAllCookies(context: Context) {
        val sites = listOf("youtube", "instagram", "twitter", "tiktok")
        val combinedFile = File(context.filesDir, "cookies.txt")
        val builder = StringBuilder()
        builder.append("# Netscape HTTP Cookie File\n\n")

        var hasAny = false
        for (site in sites) {
            val f = File(context.filesDir, "cookies_$site.txt")
            if (f.exists()) {
                val content = f.readText().lines()
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .joinToString("\n")
                if (content.isNotBlank()) {
                    builder.append("# --- Cookies for $site ---\n")
                    builder.append(content)
                    builder.append("\n\n")
                    hasAny = true
                }
            }
        }

        if (hasAny) {
            combinedFile.writeText(builder.toString())
        } else {
            if (combinedFile.exists()) {
                combinedFile.delete()
            }
        }
    }

    fun getCombinedCookiesPath(context: Context): String? {
        val combinedFile = File(context.filesDir, "cookies.txt")
        return if (combinedFile.exists() && combinedFile.length() > 20) {
            combinedFile.absolutePath
        } else {
            null
        }
    }
}
