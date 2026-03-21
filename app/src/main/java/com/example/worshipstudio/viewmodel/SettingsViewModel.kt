package com.example.worshipstudio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worshipstudio.data.model.Membership
import com.example.worshipstudio.repository.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val members: List<Membership> = emptyList(),
    val isLoading: Boolean        = false,
    val error: String?            = null
)

class SettingsViewModel : ViewModel() {
    private val repo = MemberRepository()

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    fun loadMembers(churchId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val members = repo.getMembersForChurch(churchId)
            _state.value = _state.value.copy(members = members, isLoading = false)
        }
    }
}
