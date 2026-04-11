package com.example.worshipstudio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worshipstudio.data.model.ChurchPush
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
    val session:            Session?  = null,
    val set:                WorshipSet? = null,
    val currentSong:        Song?     = null,
    val sessionId:          String    = "",
    val roomCode:           String    = "",   // 4-digit code shown to admin
    val isAdmin:            Boolean   = false,
    val isLoading:          Boolean   = false,
    val error:              String?   = null,
    val participantCount:   Int       = 0,
    val participantKey:     String    = "",
    /** Church-wide active session found via auto-discovery (for banner). */
    val churchActiveSession: Session? = null,
    /** Latest push notification broadcast by admin (null = none active). */
    val churchPush: ChurchPush? = null,
    /** Stored churchId so endSession can clear the church session pointer. */
    val churchId:           String    = ""
)

class SessionViewModel : ViewModel() {
    private val sessionRepo = SessionRepository()
    private val setRepo     = SetRepository()
    private val songRepo    = SongRepository()

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state

    // ── Admin: reconnect when LiveSessionScreen opens ─────────────────────────
    fun connectAsAdmin(sessionId: String) {
        if (_state.value.sessionId == sessionId) return
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

    // ── Admin: create a new session ───────────────────────────────────────────
    fun createSession(setId: String, adminId: String, churchId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = sessionRepo.createSession(setId, adminId, churchId)
            result.onSuccess { (sessionId, roomCode) ->
                val key = sessionRepo.registerPresence(sessionId, "admin")
                _state.value = _state.value.copy(
                    sessionId      = sessionId,
                    roomCode       = roomCode,
                    isAdmin        = true,
                    isLoading      = false,
                    participantKey = key,
                    churchId       = churchId
                )
                observeSession(sessionId, setId)
                observeParticipantCount(sessionId)
            }
            result.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    // ── Member: join by session ID (already resolved) ─────────────────────────
    fun joinSession(sessionId: String, userId: String) {
        if (_state.value.sessionId == sessionId) return
        val key = sessionRepo.registerPresence(sessionId, userId.ifEmpty { "member" })
        _state.value = _state.value.copy(
            sessionId      = sessionId,
            isAdmin        = false,
            isLoading      = true,
            participantKey = key
        )
        observeSession(sessionId, null)
        observeParticipantCount(sessionId)
    }

    // ── Validate push session exists then join (guards "Join Now" banner tap) ──
    fun validateAndJoinPushSession(
        sessionId: String,
        churchId:  String,
        onValid:   (String) -> Unit,
        onStale:   () -> Unit
    ) {
        viewModelScope.launch {
            val alive = sessionRepo.sessionExists(sessionId)
            if (alive) {
                onValid(sessionId)
            } else {
                // Dead session — wipe the stale push so banner disappears
                sessionRepo.clearChurchPush(churchId)
                _state.value = _state.value.copy(churchPush = null)
                onStale()
            }
        }
    }

    // ── Resolve 4-digit room code → sessionId, then join ─────────────────────
    fun joinByCode(
        code:      String,
        onSuccess: (String) -> Unit,
        onError:   (String) -> Unit
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = sessionRepo.findByRoomCode(code.trim())
            result.onSuccess { sessionId ->
                _state.value = _state.value.copy(isLoading = false)
                onSuccess(sessionId)
            }
            result.onFailure { e ->
                val msg = e.message ?: "Session not found"
                _state.value = _state.value.copy(isLoading = false, error = msg)
                onError(msg)
            }
        }
    }

    // ── Admin: create a push session (single song, instant join for members) ──
    fun createPushSession(
        songId:      String,
        songName:    String,
        adminId:     String,
        adminName:   String,
        churchId:    String,
        adminKey:    String = "",
        adminQuality: String = "",
        onSuccess:   (String) -> Unit,
        onError:     (String) -> Unit
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = sessionRepo.createPushSession(
                songId      = songId,
                songName    = songName,
                adminId     = adminId,
                adminName   = adminName,
                churchId    = churchId,
                adminKey    = adminKey,
                adminQuality = adminQuality
            )
            result.onSuccess { sessionId ->
                val key = sessionRepo.registerPresence(sessionId, "admin")
                _state.value = _state.value.copy(
                    sessionId      = sessionId,
                    roomCode       = "",
                    isAdmin        = true,
                    isLoading      = false,
                    participantKey = key,
                    churchId       = churchId
                )
                observeSession(sessionId, null)
                observeParticipantCount(sessionId)
                onSuccess(sessionId)
            }
            result.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
                onError(it.message ?: "Failed to push song")
            }
        }
    }

    // ── Observe church push notifications (members listen for instant join) ───
    fun observeChurchPush(churchId: String) {
        if (churchId.isEmpty()) return
        viewModelScope.launch {
            sessionRepo.observeChurchPush(churchId).collect { push ->
                if (push != null) {
                    // Validate the referenced session still exists before showing banner
                    val alive = sessionRepo.sessionExists(push.sessionId)
                    if (!alive) {
                        // Stale push — clear it from Firebase so nobody ever sees it again
                        sessionRepo.clearChurchPush(churchId)
                        _state.value = _state.value.copy(churchPush = null)
                        return@collect
                    }
                }
                _state.value = _state.value.copy(churchPush = push)
            }
        }
    }

    // ── Dismiss push banner (member tapped Dismiss, or admin ended session) ───
    fun dismissChurchPush(churchId: String) {
        _state.value = _state.value.copy(churchPush = null)
        // Only the admin should clear the node; members just hide locally
    }

    // ── Auto-discovery: watch for any active session in this church ───────────
    fun observeChurchSession(churchId: String) {
        if (churchId.isEmpty()) return
        viewModelScope.launch {
            sessionRepo.observeChurchSession(churchId).collect { active ->
                _state.value = _state.value.copy(churchActiveSession = active)
            }
        }
    }

    // ── Internal: stream session changes ──────────────────────────────────────
    private fun observeSession(sessionId: String, knownSetId: String?) {
        viewModelScope.launch {
            sessionRepo.observeSession(sessionId).collect { session ->
                if (session == null) {
                    // Session was deleted by admin — kick non-admins out
                    if (!_state.value.isAdmin) {
                        _state.value = _state.value.copy(
                            session = null,
                            error   = "Session has ended"
                        )
                    }
                    return@collect
                }
                // Push session: load the single song directly, no set needed
                if (session.pushSongId.isNotEmpty()) {
                    val song = songRepo.getSong(session.pushSongId)
                    _state.value = _state.value.copy(
                        session     = session,
                        roomCode    = session.roomCode,
                        set         = null,
                        currentSong = song,
                        isLoading   = false
                    )
                    return@collect
                }
                // Regular session: load via set
                val currentSetId = knownSetId ?: session.setId
                val set    = _state.value.set ?: setRepo.getSet(currentSetId)
                val songId = set?.songs?.getOrNull(session.currentSongIndex)
                val song   = if (songId != null) songRepo.getSong(songId) else null
                _state.value = _state.value.copy(
                    session     = session,
                    roomCode    = session.roomCode,
                    set         = set,
                    currentSong = song,
                    isLoading   = false
                )
            }
        }
    }

    private fun observeParticipantCount(sessionId: String) {
        viewModelScope.launch {
            sessionRepo.observeParticipantCount(sessionId).collect { count ->
                _state.value = _state.value.copy(participantCount = count)
            }
        }
    }

    // ── Chord callout (admin only) ────────────────────────────────────────────
    /** Toggle: tapping the same degree again clears it; tapping a new one sets it. */
    fun setActiveChord(degree: String) {
        val s = _state.value; if (!s.isAdmin) return
        val next = if (degree == s.session?.activeChordDegree) "" else degree
        sessionRepo.updateActiveChord(s.sessionId, next)
    }

    // ── Admin key change (broadcast to all members) ───────────────────────────
    fun setAdminKey(key: String, quality: String) {
        val s = _state.value; if (!s.isAdmin) return
        sessionRepo.updateAdminKey(s.sessionId, key, quality)
    }

    // ── Song navigation (admin only) ──────────────────────────────────────────
    fun nextSong() {
        val s = _state.value; if (!s.isAdmin) return
        val session  = s.session ?: return
        val maxIndex = (s.set?.songs?.size ?: 1) - 1
        val newIndex = (session.currentSongIndex + 1).coerceAtMost(maxIndex)
        viewModelScope.launch { sessionRepo.updateSongIndex(session.sessionId, newIndex) }
    }

    fun previousSong() {
        val s = _state.value; if (!s.isAdmin) return
        val session  = s.session ?: return
        val newIndex = (session.currentSongIndex - 1).coerceAtLeast(0)
        viewModelScope.launch { sessionRepo.updateSongIndex(session.sessionId, newIndex) }
    }

    // ── End / leave ───────────────────────────────────────────────────────────
    fun endSession(churchId: String = "") {
        viewModelScope.launch {
            val s = _state.value
            val resolvedChurchId = churchId.ifEmpty { s.churchId }
            if (s.participantKey.isNotEmpty() && s.sessionId.isNotEmpty())
                sessionRepo.removePresence(s.sessionId, s.participantKey)
            if (s.isAdmin && s.sessionId.isNotEmpty()) {
                // endSession now always clears churchPush internally
                sessionRepo.endSession(s.sessionId, s.roomCode, resolvedChurchId)
            }
            _state.value = SessionState()
        }
    }

    fun leaveSession() {
        val s = _state.value
        if (s.participantKey.isNotEmpty() && s.sessionId.isNotEmpty())
            sessionRepo.removePresence(s.sessionId, s.participantKey)
        _state.value = SessionState()
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
