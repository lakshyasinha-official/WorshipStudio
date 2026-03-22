package com.example.worshipstudio.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worshipstudio.data.model.Tag
import com.example.worshipstudio.repository.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Stable
data class TagState(
    val tags:      List<Tag> = emptyList(),
    val isLoading: Boolean   = false,
    val error:     String?   = null
)

class TagViewModel : ViewModel() {
    private val repo = TagRepository()

    private val _state = MutableStateFlow(TagState())
    val state: StateFlow<TagState> = _state

    fun loadTags(churchId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val tags = repo.getTags(churchId)
            _state.value = _state.value.copy(tags = tags, isLoading = false)
        }
    }

    fun addTag(name: String, churchId: String, onSuccess: (() -> Unit)? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = repo.addTag(name, churchId)
            result.onSuccess { tag ->
                val updated = (_state.value.tags + tag).sortedBy { it.name.lowercase() }
                _state.value = _state.value.copy(tags = updated, error = null)
                onSuccess?.invoke()
            }
            result.onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun deleteTag(tagId: String, churchId: String) {
        viewModelScope.launch {
            repo.deleteTag(tagId)
            loadTags(churchId)
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
