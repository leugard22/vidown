package app.vidown.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vidown.data.model.VideoFormat
import app.vidown.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onUrlChange(newUrl: String) {
        _url.value = newUrl
        if (newUrl.isBlank()) {
            _uiState.value = HomeUiState.Idle
        }
    }

    fun analyzeUrl() {
        val targetUrl = _url.value.trim()
        if (targetUrl.isBlank()) return

        _uiState.value = HomeUiState.Analyzing

        viewModelScope.launch {
            val defaultQualityPreset = dataStore.data.map { preferences ->
                preferences[stringPreferencesKey("default_quality")] ?: "best"
            }.first()

            downloadRepository.fetchVideoInfo(targetUrl)
                .onSuccess { info ->
                    val defaultFormat = info.formats.firstOrNull { it.formatId == defaultQualityPreset }
                        ?: info.formats.firstOrNull()
                        ?: VideoFormat("best", "Best", "mp4", false)
                    _uiState.value = HomeUiState.Success(info, defaultFormat)
                }
                .onFailure { error ->
                    _uiState.value = HomeUiState.Error(error.message ?: "Failed to retrieve video details")
                }
        }
    }

    fun selectFormat(format: VideoFormat) {
        val currentState = _uiState.value
        if (currentState is HomeUiState.Success) {
            _uiState.value = currentState.copy(selectedFormat = format)
        }
    }

    fun startDownload() {
        val currentState = _uiState.value
        if (currentState is HomeUiState.Success) {
            viewModelScope.launch {
                downloadRepository.enqueueDownload(
                    url = currentState.info.url,
                    format = currentState.selectedFormat,
                    info = currentState.info
                )
                _url.value = ""
                _uiState.value = HomeUiState.Idle
            }
        }
    }
}
