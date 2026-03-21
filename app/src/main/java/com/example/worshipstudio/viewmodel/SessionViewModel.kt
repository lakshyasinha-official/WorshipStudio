package com.example.worshipstudio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worshipstudio.data.model.Session
import com.example.worshipstudio.data.model.Song
import com.example.worshipstudio.data.model.WorshipSet
import com.example.worshipstudio.repository.SessionRepository
import com.example.worshipstudio.repository.SetRepository
import com.example.worshipstudio.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SessionState(
    val session: Session? = null,
    val set: WorshipSet? = null,
    val currentSong: Song? = null,
    val sessionId: String = "",
    val isAdmin: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class SessionViewModel : ViewModel() {
    private val sessionRepo = SessionRepository()
    private val setRepo = SetRepository()
    private val songRepo = SongRepository()

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state

    fun createSession(setId: String, adminId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = sessionRepo.createSession(setId, adminId)
            result.onSuccess { sessionId ->
                _state.value = _state.value.copy(
                    sessionId = sessionId,
                    isAdmin = true,
                    isLoading = false
                )
                observeSession(sessionId, setId)
            }
            result.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun joinSession(sessionId: String, userId: String) {
        _state.value = _state.value.copy(
            sessionId = sessionId,
            isAdmin = false,
            isLoading = true
        )
        observeSession(sessionId, null)
    }

    private fun observeSession(sessionId: String, knownSetId: String?) {
        viewModelScope.launch {
            sessionRepo.observeSession(sessionId).collect { session ->
                session ?: return@collect
                val currentSetId = knownSetId ?: session.setId
                val set = _state.value.set ?: setRepo.getSet(currentSetId)
                val songId = set?.songs?.getOrNull(session.currentSongIndex)
                val song = if (songId != null) songRepo.getSong(songId) else null
                _state.value = _state.value.copy(
                    session = session,
                    set = set,
                    currentSong = song,
                    isLoading = false
                )
            }
        }
    }

    fun nextSong() {
        val s = _state.value
        if (!s.isAdmin) return
        val session = s.session ?: return
        val maxIndex = (s.set?.songs?.size ?: 1) - 1
        val newIndex = (session.currentSongIndex + 1).coerceAtMost(maxIndex)
        viewModelScope.launch { sessionRepo.updateSongIndex(session.sessionId, newIndex) }
    }

    fun previousSong() {
        val s = _state.value
        if (!s.isAdmin) return
        val session = s.session ?: return
        val newIndex = (session.currentSongIndex - 1).coerceAtLeast(0)
        viewModelScope.launch { sessionRepo.updateSongIndex(session.sessionId, newIndex) }
    }

    fun endSession() {
        viewModelScope.launch {
            val sessionId = _state.value.sessionId
            if (sessionId.isNotEmpty()) sessionRepo.endSession(sessionId)
            _state.value = SessionState()
        }
    }
}
