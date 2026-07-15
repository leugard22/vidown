package app.vidown.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vidown.data.model.DownloadItem
import app.vidown.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val downloads: StateFlow<List<DownloadItem>> = downloadRepository.observeAllDownloads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun cancel(id: Long) {
        viewModelScope.launch {
            downloadRepository.cancelDownload(id)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(id)
        }
    }

    fun retry(id: Long) {
        viewModelScope.launch {
            downloadRepository.retryDownload(id)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            downloadRepository.clearCompletedDownloads()
        }
    }

    fun cancelAllActive() {
        viewModelScope.launch {
            downloadRepository.cancelAllActiveDownloads()
        }
    }
}
