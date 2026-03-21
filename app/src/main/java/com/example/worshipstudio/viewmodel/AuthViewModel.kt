package com.example.worshipstudio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worshipstudio.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val churchId: String = "",
    val userId: String = "",
    val role: String = "member"
)

class AuthViewModel : ViewModel() {
    private val repo = AuthRepository()

    private val _state = MutableStateFlow(AuthState(isLoggedIn = repo.currentUser != null))
    val state: StateFlow<AuthState> = _state

    init {
        if (repo.currentUser != null) loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val data = repo.getUserData()
            if (data == null) {
                // User doc missing — still mark as logged in, stop spinner
                _state.value = _state.value.copy(isLoggedIn = true, isLoading = false)
                return@launch
            }
            _state.value = _state.value.copy(
                churchId   = data["churchId"] as? String ?: "",
                userId     = data["userId"]   as? String ?: "",
                role       = data["role"]     as? String ?: "member",
                isLoggedIn = true,
                isLoading  = false
            )
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repo.login(email, password)
            if (result.isSuccess) {
                loadUserData()
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun register(email: String, password: String, churchId: String, role: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repo.register(email, password, churchId, role)
            if (result.isSuccess) {
                // Populate state directly from inputs — no need for extra Firestore round-trip
                _state.value = _state.value.copy(
                    churchId   = churchId,
                    role       = role,
                    userId     = result.getOrNull()?.uid ?: "",
                    isLoggedIn = true,
                    isLoading  = false
                )
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun logout() {
        repo.logout()
        _state.value = AuthState(isLoggedIn = false)
    }
}
