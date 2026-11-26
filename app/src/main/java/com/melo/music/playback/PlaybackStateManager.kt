package com.melo.music.playback

import com.melo.music.extractor.TrackItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Неизменяемое состояние воспроизведения для UI.
 */
data class PlaybackUiState(
    val nowPlaying: TrackItem? = null,
    val playingList: List<TrackItem> = emptyList(),
    val playingIndex: Int = -1,
    val resolvingUrl: String? = null,
    val isShuffle: Boolean = false,
    val isRepeatOne: Boolean = false,
    val isSeaActive: Boolean = false,
    val isSeaLoading: Boolean = false,
    val trackSwitchDirection: Int = 1,
)

/**
 * Хранилище состояния воспроизведения, живущее вне жизненного цикла UI Compose.
 */
object PlaybackStateManager {
    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    fun updateNowPlaying(track: TrackItem?) {
        _state.update { it.copy(nowPlaying = track) }
    }

    fun updateResolvingUrl(url: String?) {
        _state.update { it.copy(resolvingUrl = url) }
    }

    fun updateQueue(list: List<TrackItem>, index: Int, switchDirection: Int = 1) {
        _state.update {
            it.copy(
                playingList = list,
                playingIndex = index,
                trackSwitchDirection = switchDirection
            )
        }
    }

    fun updatePlayingIndex(index: Int) {
        _state.update { it.copy(playingIndex = index) }
    }

    fun updatePlayingList(list: List<TrackItem>) {
        _state.update { it.copy(playingList = list) }
    }

    fun updateTrackSwitchDirection(dir: Int) {
        _state.update { it.copy(trackSwitchDirection = dir) }
    }

    fun toggleShuffle(): Boolean {
        val next = !_state.value.isShuffle
        _state.update { it.copy(isShuffle = next) }
        return next
    }

    fun setShuffle(enabled: Boolean) {
        _state.update { it.copy(isShuffle = enabled) }
    }

    fun toggleRepeatOne(): Boolean {
        val next = !_state.value.isRepeatOne
        _state.update { it.copy(isRepeatOne = next) }
        return next
    }

    fun setRepeatOne(enabled: Boolean) {
        _state.update { it.copy(isRepeatOne = enabled) }
    }

    fun setSeaActive(active: Boolean) {
        _state.update { it.copy(isSeaActive = active) }
    }

    fun setSeaLoading(loading: Boolean) {
        _state.update { it.copy(isSeaLoading = loading) }
    }
}
