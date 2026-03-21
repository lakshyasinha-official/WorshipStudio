package com.example.worshipstudio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worshipstudio.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

    private val _state = MutableStateFlow(AuthState(isLoggedIn = repo.currentUser != null))
    val state: StateFlow<AuthState> = _state

    // ── Called on app start if user is already signed in.
    // We don't know which church they were using — they must log in again.
    // So we just mark as NOT logged in so the login screen appears.
    init {
        if (repo.currentUser != null) {
            // Force re-login to pick church (multi-church support)
            repo.logout()
            _state.value = AuthState(isLoggedIn = false)
        }
    }

    // ── Login — requires email + password + churchId ──────────────────────────
    fun login(email: String, password: String, churchId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repo.login(email, password, churchId)
            if (result.isSuccess) {
                // Load membership data for this specific church
                loadMembership(email, churchId)
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error     = result.exceptionOrNull()?.message
                )
            }
        }
    }

    // ── Register ──────────────────────────────────────────────────────────────
    fun register(email: String, password: String, churchId: String, role: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repo.register(email, password, churchId, role)
            if (result.isSuccess) {
                val displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                _state.value = _state.value.copy(
                    email       = email,
                    displayName = displayName,
                    churchId    = churchId,
                    role        = role,
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

    // ── Internal: load membership doc after successful auth ───────────────────
    private suspend fun loadMembership(email: String, churchId: String) {
        val displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        val data = repo.getMembership(churchId)
        if (data == null) {
            // Membership was verified to exist in repo; use defaults if fetch fails
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

    fun logout() {
        repo.logout()
        _state.value = AuthState(isLoggedIn = false)
    }
}
