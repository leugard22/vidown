package app.vidown.ui.home

import app.vidown.data.model.VideoFormat
import app.vidown.data.model.VideoInfo

sealed interface HomeUiState {
    data object Idle : HomeUiState
    data object Analyzing : HomeUiState
    data class Success(val info: VideoInfo, val selectedFormat: VideoFormat) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
