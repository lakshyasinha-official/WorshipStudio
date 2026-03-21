package com.example.worshipstudio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worshipstudio.data.model.WorshipSet
import com.example.worshipstudio.repository.SetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SetListState(
    val sets: List<WorshipSet> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class SetDetailState(
    val set: WorshipSet? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class SetViewModel : ViewModel() {
    private val repo = SetRepository()

    private val _listState = MutableStateFlow(SetListState())
    val listState: StateFlow<SetListState> = _listState

    private val _detailState = MutableStateFlow(SetDetailState())
    val detailState: StateFlow<SetDetailState> = _detailState

    fun loadSets(churchId: String) {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isLoading = true)
            val sets = repo.getSets(churchId)
            _listState.value = _listState.value.copy(sets = sets, isLoading = false)
        }
    }

    fun loadSet(setId: String) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true)
            val set = repo.getSet(setId)
            _detailState.value = _detailState.value.copy(set = set, isLoading = false)
        }
    }

    fun createSet(set: WorshipSet, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val result = repo.createSet(set)
            result.onSuccess { onSuccess(it) }
            result.onFailure { _listState.value = _listState.value.copy(error = it.message) }
        }
    }

    fun updateSet(set: WorshipSet, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repo.updateSet(set)
            _detailState.value = _detailState.value.copy(set = set)
            onSuccess()
        }
    }

    fun addSongToSet(songId: String) {
        val current = _detailState.value.set ?: return
        if (songId in current.songs) return
        updateSet(current.copy(songs = current.songs + songId))
    }

    fun removeSongFromSet(songId: String) {
        val current = _detailState.value.set ?: return
        updateSet(current.copy(songs = current.songs - songId))
    }

    fun moveSongUp(index: Int) {
        val current = _detailState.value.set ?: return
        if (index <= 0) return
        val songs = current.songs.toMutableList()
        songs.add(index - 1, songs.removeAt(index))
        updateSet(current.copy(songs = songs))
    }

    fun moveSongDown(index: Int) {
        val current = _detailState.value.set ?: return
        if (index >= current.songs.size - 1) return
        val songs = current.songs.toMutableList()
        songs.add(index + 1, songs.removeAt(index))
        updateSet(current.copy(songs = songs))
    }

    fun deleteSet(setId: String, churchId: String) {
        viewModelScope.launch {
            repo.deleteSet(setId)
            loadSets(churchId)
        }
    }
}
