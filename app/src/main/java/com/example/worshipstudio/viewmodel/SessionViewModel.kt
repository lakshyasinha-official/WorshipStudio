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
    val session: Session?       = null,
    val set: WorshipSet?        = null,
    val currentSong: Song?      = null,
    val sessionId: String       = "",
    val isAdmin: Boolean        = false,
    val isLoading: Boolean      = false,
    val error: String?          = null,
    val participantCount: Int   = 0,        // live connected device count
    val participantKey: String  = ""        // this device's presence key
)

class SessionViewModel : ViewModel() {
    private val sessionRepo = SessionRepository()
    private val setRepo     = SetRepository()
    private val songRepo    = SongRepository()

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state

    // ── Admin: reconnect to an already-created session (LiveSessionScreen) ───
    // Called when the admin navigates INTO LiveSessionScreen (fresh ViewModel).
    fun connectAsAdmin(sessionId: String) {
        if (_state.value.sessionId == sessionId) return   // already connected
        val key = sessionRepo.registerPresence(sessionId, "admin")
        _state.value = _state.value.copy(
            sessionId      = sessionId,
            isAdmin        = true,
            isLoading      = true,
            participantKey = key
        )
        observeSession(sessionId, null)
        observeParticipantCount(sessionId)
    }

    // ── Admin: create and own the session ─────────────────────────────────────
    fun createSession(setId: String, adminId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = sessionRepo.createSession(setId, adminId)
            result.onSuccess { sessionId ->
                // Register admin presence
                val key = sessionRepo.registerPresence(sessionId, "admin")
                _state.value = _state.value.copy(
                    sessionId      = sessionId,
                    isAdmin        = true,
                    isLoading      = false,
                    participantKey = key
                )
                observeSession(sessionId, setId)
                observeParticipantCount(sessionId)
            }
            result.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    // ── Member: join an existing session ──────────────────────────────────────
    fun joinSession(sessionId: String, userId: String) {
        val key = sessionRepo.registerPresence(sessionId, "member")
        _state.value = _state.value.copy(
            sessionId      = sessionId,
            isAdmin        = false,
            isLoading      = true,
            participantKey = key
        )
        observeSession(sessionId, null)
        observeParticipantCount(sessionId)
    }

    // ── Internal: stream session data changes ─────────────────────────────────
    private fun observeSession(sessionId: String, knownSetId: String?) {
        viewModelScope.launch {
            sessionRepo.observeSession(sessionId).collect { session ->
                session ?: return@collect
                val currentSetId = knownSetId ?: session.setId
                val set  = _state.value.set ?: setRepo.getSet(currentSetId)
                val songId = set?.songs?.getOrNull(session.currentSongIndex)
                val song = if (songId != null) songRepo.getSong(songId) else null
                _state.value = _state.value.copy(
                    session     = session,
                    set         = set,
                    currentSong = song,
                    isLoading   = false
                )
            }
        }
    }

    // ── Internal: stream live participant count ────────────────────────────────
    private fun observeParticipantCount(sessionId: String) {
        viewModelScope.launch {
            sessionRepo.observeParticipantCount(sessionId).collect { count ->
                _state.value = _state.value.copy(participantCount = count)
            }
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    fun nextSong() {
        val s = _state.value
        if (!s.isAdmin) return
        val session  = s.session ?: return
        val maxIndex = (s.set?.songs?.size ?: 1) - 1
        val newIndex = (session.currentSongIndex + 1).coerceAtMost(maxIndex)
        viewModelScope.launch { sessionRepo.updateSongIndex(session.sessionId, newIndex) }
    }

    fun previousSong() {
        val s = _state.value
        if (!s.isAdmin) return
        val session  = s.session ?: return
        val newIndex = (session.currentSongIndex - 1).coerceAtLeast(0)
        viewModelScope.launch { sessionRepo.updateSongIndex(session.sessionId, newIndex) }
    }

    // ── End / leave ───────────────────────────────────────────────────────────
    fun endSession() {
        viewModelScope.launch {
            val s = _state.value
            if (s.participantKey.isNotEmpty() && s.sessionId.isNotEmpty()) {
                sessionRepo.removePresence(s.sessionId, s.participantKey)
            }
            if (s.isAdmin && s.sessionId.isNotEmpty()) {
                sessionRepo.endSession(s.sessionId)
            }
            _state.value = SessionState()
        }
    }

    // Call this when a member leaves (back button) without ending the session
    fun leaveSession() {
        val s = _state.value
        if (s.participantKey.isNotEmpty() && s.sessionId.isNotEmpty()) {
            sessionRepo.removePresence(s.sessionId, s.participantKey)
        }
        _state.value = SessionState()
    }
}
