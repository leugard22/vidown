package app.vidown.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaquo.python.Python
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import androidx.datastore.preferences.core.booleanPreferencesKey
import app.vidown.data.python.CookieHelper

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val defaultQualityKey = stringPreferencesKey("default_quality")
    private val appThemeKey = stringPreferencesKey("app_theme")
    private val embedSubtitlesKey = booleanPreferencesKey("embed_subtitles")
    private val maxConcurrentKey = androidx.datastore.preferences.core.intPreferencesKey("max_concurrent_downloads")
    private val customDownloadDirKey = stringPreferencesKey("custom_download_dir")
    private val subtitleLangsKey = stringPreferencesKey("subtitle_langs")
    private val downloadSpeedLimitKey = stringPreferencesKey("download_speed_limit")

    val defaultQuality: StateFlow<String> = dataStore.data.map { preferences ->
        preferences[defaultQualityKey] ?: "best"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "best"
    )

    val appTheme: StateFlow<String> = dataStore.data.map { preferences ->
        preferences[appThemeKey] ?: "system"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "system"
    )

    val embedSubtitles: StateFlow<Boolean> = dataStore.data.map { preferences ->
        preferences[embedSubtitlesKey] ?: false
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val subtitleLangs: StateFlow<String> = dataStore.data.map { preferences ->
        preferences[subtitleLangsKey] ?: "en"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "en"
    )

    val maxConcurrentDownloads: StateFlow<Int> = dataStore.data.map { preferences ->
        preferences[maxConcurrentKey] ?: 3
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 3
    )

    val downloadSpeedLimit: StateFlow<String> = dataStore.data.map { preferences ->
        preferences[downloadSpeedLimitKey] ?: "none"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "none"
    )

    val customDownloadDir: StateFlow<String?> = dataStore.data.map { preferences ->
        preferences[customDownloadDirKey]
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _youtubeLoggedIn = MutableStateFlow(false)
    val youtubeLoggedIn = _youtubeLoggedIn.asStateFlow()

    private val _instagramLoggedIn = MutableStateFlow(false)
    val instagramLoggedIn = _instagramLoggedIn.asStateFlow()

    private val _twitterLoggedIn = MutableStateFlow(false)
    val twitterLoggedIn = _twitterLoggedIn.asStateFlow()

    private val _tiktokLoggedIn = MutableStateFlow(false)
    val tiktokLoggedIn = _tiktokLoggedIn.asStateFlow()

    private val _pythonVersion = MutableStateFlow("...")
    val pythonVersion = _pythonVersion.asStateFlow()

    private val _ytdlVersion = MutableStateFlow("...")
    val ytdlVersion = _ytdlVersion.asStateFlow()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus = _updateStatus.asStateFlow()

    init {
        checkLoginStatus()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val py = Python.getInstance()
                val sysVersion = py.getModule("sys").get("version")?.toString()?.split(" ")?.firstOrNull() ?: "3.x"
                val ytdlVer = py.getModule("yt_dlp.version").get("__version__")?.toString() ?: "Latest"
                _pythonVersion.value = sysVersion
                _ytdlVersion.value = ytdlVer
            } catch (e: Exception) {
                _pythonVersion.value = "3.x"
                _ytdlVersion.value = "Unknown"
            }
        }
    }

    fun updateDefaultQuality(quality: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[defaultQualityKey] = quality
            }
        }
    }

    fun updateAppTheme(theme: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[appThemeKey] = theme
            }
        }
    }

    fun checkLoginStatus() {
        _youtubeLoggedIn.value = CookieHelper.hasCookies(context, "youtube")
        _instagramLoggedIn.value = CookieHelper.hasCookies(context, "instagram")
        _twitterLoggedIn.value = CookieHelper.hasCookies(context, "twitter")
        _tiktokLoggedIn.value = CookieHelper.hasCookies(context, "tiktok")
    }

    fun logoutSite(siteName: String) {
        CookieHelper.deleteCookies(context, siteName)
        checkLoginStatus()
    }

    fun updateEmbedSubtitles(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[embedSubtitlesKey] = enabled
            }
        }
    }

    fun updateSubtitleLangs(langs: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[subtitleLangsKey] = langs
            }
        }
    }

    fun updateMaxConcurrentDownloads(limit: Int) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[maxConcurrentKey] = limit
            }
        }
    }

    fun updateDownloadSpeedLimit(limit: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[downloadSpeedLimitKey] = limit
            }
        }
    }

    fun updateCustomDownloadDir(uri: String?) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                if (uri != null) {
                    preferences[customDownloadDirKey] = uri
                } else {
                    preferences.remove(customDownloadDirKey)
                }
            }
        }
    }

    fun checkForUpdates() {
        _updateStatus.value = UpdateStatus.Checking
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Vidown-App")
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val latestTag = json.getString("tag_name")
                    val current = _ytdlVersion.value

                    if (latestTag != current) {
                        _updateStatus.value = UpdateStatus.UpdateAvailable(latestTag)
                    } else {
                        _updateStatus.value = UpdateStatus.UpToDate
                    }
                } else {
                    _updateStatus.value = UpdateStatus.Error("HTTP ${connection.responseCode}")
                }
            } catch (e: Exception) {
                _updateStatus.value = UpdateStatus.Error(e.message ?: "Failed to check update")
            }
        }
    }

    fun downloadUpdate() {
        _updateStatus.value = UpdateStatus.Downloading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val downloadUrl = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp"
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                var responseCode = connection.responseCode
                var conn = connection
                var redirectCount = 0
                while ((responseCode == 301 || responseCode == 302 || responseCode == 303 || responseCode == 307 || responseCode == 308) && redirectCount < 5) {
                    val newUrl = conn.getHeaderField("Location")
                    conn = URL(newUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 15000
                    conn.readTimeout = 15000
                    responseCode = conn.responseCode
                    redirectCount++
                }

                if (responseCode == 200) {
                    val tempFile = File(context.filesDir, "yt-dlp.tmp")
                    val destFile = File(context.filesDir, "yt-dlp.zip")

                    BufferedInputStream(conn.inputStream).use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                  output.write(buffer, 0, bytesRead)
                            }
                        }
                    }

                    if (tempFile.exists() && tempFile.length() > 1000) {
                        if (destFile.exists()) destFile.delete()
                        tempFile.renameTo(destFile)

                        withContext(Dispatchers.Main) {
                            try {
                                val py = Python.getInstance()
                                val module = py.getModule("bridge")
                                module.callAttr("init_update_path", destFile.absolutePath)

                                val ytdlVer = py.getModule("yt_dlp.version").get("__version__")?.toString() ?: "Latest"
                                _ytdlVersion.value = ytdlVer
                            } catch (e: Exception) {
                            }
                        }
                        _updateStatus.value = UpdateStatus.UpToDate
                    } else {
                        _updateStatus.value = UpdateStatus.Error("Empty download")
                    }
                } else {
                    _updateStatus.value = UpdateStatus.Error("HTTP $responseCode")
                }
            } catch (e: Exception) {
                _updateStatus.value = UpdateStatus.Error(e.message ?: "Failed to download")
            }
        }
    }
}

sealed interface UpdateStatus {
    object Idle : UpdateStatus
    object Checking : UpdateStatus
    data class UpdateAvailable(val latestVersion: String) : UpdateStatus
    object Downloading : UpdateStatus
    object UpToDate : UpdateStatus
    data class Error(val message: String) : UpdateStatus
}
