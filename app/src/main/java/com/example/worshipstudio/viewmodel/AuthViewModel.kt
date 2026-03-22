package com.example.worshipstudio.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worshipstudio.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Stable
data class AuthState(
    val isLoading: Boolean  = false,
    val isLoggedIn: Boolean = false,
    val error: String?      = null,
    val email: String       = "",
    val displayName: String = "",
    val churchId: String    = "",
    val userId: String      = "",
    val role: String        = "member"
)

class AuthViewModel : ViewModel() {
    private val repo = AuthRepository()

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    init {
        // Force re-login on every app start (multi-church: user picks church each time)
        if (repo.currentUser != null) repo.logout()
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    fun login(email: String, password: String, churchId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repo.login(email, password, churchId.trim().lowercase())
            if (result.isSuccess) {
                loadMembership(email, churchId.trim().lowercase())
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error     = result.exceptionOrNull()?.message
                )
            }
        }
    }

    // ── Register as Member (join existing church) ─────────────────────────────
    fun registerAsMember(email: String, password: String, churchId: String, displayName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val churchKey = churchId.trim().lowercase()
            val result = repo.registerAsMember(email, password, churchKey, displayName.trim())
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    email       = email,
                    displayName = displayName.trim(),
                    churchId    = churchKey,
                    role        = "member",
                    userId      = result.getOrNull()?.uid ?: "",
                    isLoggedIn  = true,
                    isLoading   = false
                )
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error     = result.exceptionOrNull()?.message
                )
            }
        }
    }

    // ── Register New Church (caller becomes admin) ────────────────────────────
    fun registerNewChurch(email: String, password: String, churchId: String, displayName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val churchKey = churchId.trim().lowercase()
            val result = repo.registerNewChurch(email, password, churchKey, displayName.trim())
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    email       = email,
                    displayName = displayName.trim(),
                    churchId    = churchKey,
                    role        = "admin",
                    userId      = result.getOrNull()?.uid ?: "",
                    isLoggedIn  = true,
                    isLoading   = false
                )
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error     = result.exceptionOrNull()?.message
                )
            }
        }
    }

    // ── Legacy register (kept for compatibility) ──────────────────────────────
    fun register(email: String, password: String, churchId: String, role: String) =
        if (role == "admin") registerNewChurch(email, password, churchId,
            email.substringBefore("@").replaceFirstChar { it.uppercase() })
        else registerAsMember(email, password, churchId,
            email.substringBefore("@").replaceFirstChar { it.uppercase() })

    // ── Internal: load membership after login ─────────────────────────────────
    private suspend fun loadMembership(email: String, churchId: String) {
        val displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        val data = repo.getMembership(churchId)
        if (data == null) {
            _state.value = _state.value.copy(
                email       = email,
                displayName = displayName,
                churchId    = churchId,
                isLoggedIn  = true,
                isLoading   = false
            )
            return
        }
        _state.value = _state.value.copy(
            email       = email,
            displayName = data["displayName"] as? String ?: displayName,
            churchId    = data["churchId"]    as? String ?: churchId,
            userId      = data["userId"]      as? String ?: "",
            role        = data["role"]        as? String ?: "member",
            isLoggedIn  = true,
            isLoading   = false
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun logout() {
        repo.logout()
        _state.value = AuthState(isLoggedIn = false)
    }
}
