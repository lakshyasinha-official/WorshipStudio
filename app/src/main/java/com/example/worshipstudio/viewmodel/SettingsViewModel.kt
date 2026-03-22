package com.example.worshipstudio.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worshipstudio.data.model.Membership
import com.example.worshipstudio.repository.AuthRepository
import com.example.worshipstudio.repository.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Stable
data class SettingsState(
    val members:       List<Membership> = emptyList(),
    val isLoading:     Boolean          = false,
    val isDeleting:    Boolean          = false,
    val deleteSuccess: Boolean          = false,
    val actionError:   String?          = null
)

class SettingsViewModel : ViewModel() {
    private val repo     = MemberRepository()
    private val authRepo = AuthRepository()

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    // ── Load all members for a church ─────────────────────────────────────────
    fun loadMembers(churchId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, actionError = null)
            val members = repo.getMembersForChurch(churchId)
            _state.value = _state.value.copy(members = members, isLoading = false)
        }
    }

    // ── Promote a member to admin ─────────────────────────────────────────────
    fun promoteToAdmin(member: Membership, churchId: String) {
        viewModelScope.launch {
            val result = repo.updateRole(member.userId, churchId, "admin")
            if (result.isSuccess) {
                // Update local list instantly
                _state.value = _state.value.copy(
                    members = _state.value.members.map {
                        if (it.userId == member.userId) it.copy(role = "admin") else it
                    }
                )
            } else {
                _state.value = _state.value.copy(actionError = result.exceptionOrNull()?.message)
            }
        }
    }

    // ── Demote admin to member ────────────────────────────────────────────────
    fun demoteToMember(member: Membership, churchId: String) {
        viewModelScope.launch {
            val result = repo.updateRole(member.userId, churchId, "member")
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    members = _state.value.members.map {
                        if (it.userId == member.userId) it.copy(role = "member") else it
                    }
                )
            } else {
                _state.value = _state.value.copy(actionError = result.exceptionOrNull()?.message)
            }
        }
    }

    // ── Remove a member from the church ──────────────────────────────────────
    fun removeMember(member: Membership, churchId: String) {
        viewModelScope.launch {
            val result = if (member.isPending) {
                val docId = "pending_${churchId}_${member.email.replace(".", "_").replace("@", "_at_")}"
                repo.removePendingMember(docId)
            } else {
                repo.removeMember(member.userId, churchId)
            }
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    members = _state.value.members.filter { it.email != member.email || it.userId != member.userId }
                )
            } else {
                _state.value = _state.value.copy(actionError = result.exceptionOrNull()?.message)
            }
        }
    }

    // ── Pre-add a member by email (pending) ───────────────────────────────────
    fun addMember(email: String, displayName: String, churchId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(actionError = null)
            val result = repo.addPendingMember(email.trim(), displayName.trim(), churchId)
            if (result.isSuccess) {
                // Add placeholder to local list
                val pending = Membership(
                    userId      = "",
                    email       = email.trim(),
                    displayName = displayName.trim(),
                    churchId    = churchId,
                    role        = "member",
                    isPending   = true
                )
                _state.value = _state.value.copy(members = _state.value.members + pending)
            } else {
                _state.value = _state.value.copy(actionError = result.exceptionOrNull()?.message)
            }
        }
    }

    // ── Delete entire church ──────────────────────────────────────────────────
    fun deleteChurch(churchId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isDeleting = true, actionError = null)
            val result = authRepo.deleteChurch(churchId)
            if (result.isSuccess) {
                _state.value = _state.value.copy(isDeleting = false, deleteSuccess = true)
            } else {
                _state.value = _state.value.copy(
                    isDeleting  = false,
                    actionError = result.exceptionOrNull()?.message ?: "Failed to delete church"
                )
            }
        }
    }

    fun clearError() { _state.value = _state.value.copy(actionError = null) }
}
