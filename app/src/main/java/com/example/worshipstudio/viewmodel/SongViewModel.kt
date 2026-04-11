package com.example.worshipstudio.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worshipstudio.data.model.Song
import com.example.worshipstudio.engine.ChordEngine
import com.example.worshipstudio.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Stable
data class SongListState(
    val songs:       List<Song> = emptyList(), // displayed (filtered)
    val allSongs:    List<Song> = emptyList(), // full church library
    val activeTagId: String?    = null,
    val isLoading:   Boolean    = false,
    val error:       String?    = null
)

@Stable
data class SongDetailState(
    val song: Song? = null,
    val currentKey: String = "C",
    val currentQuality: String = "Major",
    val isLoading: Boolean = false,
    val error: String? = null
)

class SongViewModel : ViewModel() {
    private val repo = SongRepository()

    private val _listState = MutableStateFlow(SongListState())
    val listState: StateFlow<SongListState> = _listState

    private val _detailState = MutableStateFlow(SongDetailState())
    val detailState: StateFlow<SongDetailState> = _detailState

    fun loadSongs(churchId: String) {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isLoading = true)
            val songs = repo.getSongs(churchId)
            _listState.value = _listState.value.copy(
                allSongs  = songs,
                songs     = applyTagFilter(songs, _listState.value.activeTagId),
                isLoading = false
            )
        }
    }

    fun searchSongs(churchId: String, query: String) {
        viewModelScope.launch {
            // Ensure full song list is loaded before filtering
            val base = if (_listState.value.allSongs.isEmpty()) {
                _listState.value = _listState.value.copy(isLoading = true)
                val loaded = repo.getSongs(churchId)
                _listState.value = _listState.value.copy(allSongs = loaded, isLoading = false)
                loaded
            } else {
                _listState.value.allSongs
            }
            // Client-side filtering: try regex first, fall back to plain contains
            val filtered = if (query.isBlank()) {
                base
            } else {
                val q = query.trim()
                try {
                    val regex = Regex(q, RegexOption.IGNORE_CASE)
                    base.filter { regex.containsMatchIn(it.name) }
                } catch (_: Exception) {
                    // Not valid regex — use case-insensitive contains on full name
                    base.filter { it.name.contains(q, ignoreCase = true) }
                }
            }
            _listState.value = _listState.value.copy(
                songs = applyTagFilter(filtered, _listState.value.activeTagId)
            )
        }
    }

    /** Toggle a tag filter. Passing the same tagId again clears it. */
    fun filterByTag(tagId: String?) {
        val s = _listState.value
        val next = if (tagId == s.activeTagId) null else tagId
        _listState.value = s.copy(
            activeTagId = next,
            songs       = applyTagFilter(s.allSongs, next)
        )
    }

    private fun applyTagFilter(songs: List<Song>, tagId: String?) =
        if (tagId == null) songs else songs.filter { tagId in it.tags }

    fun loadSong(songId: String) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true)
            val song = repo.getSong(songId)
            _detailState.value = _detailState.value.copy(
                song           = song,
                currentKey     = song?.rootKey    ?: "C",
                currentQuality = song?.keyQuality ?: "Major",
                isLoading      = false
            )
        }
    }

    fun transposeUp() {
        val newKey = ChordEngine.transposeKey(_detailState.value.currentKey, 1)
        _detailState.value = _detailState.value.copy(currentKey = newKey)
    }

    fun transposeDown() {
        val newKey = ChordEngine.transposeKey(_detailState.value.currentKey, -1)
        _detailState.value = _detailState.value.copy(currentKey = newKey)
    }

    fun setKey(key: String) {
        _detailState.value = _detailState.value.copy(currentKey = key)
    }

    fun addSong(song: Song, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val result = repo.addSong(song)
            result.onSuccess { id ->
                // Immediately insert into list so UI updates without a round-trip
                val updated = _listState.value.songs + song.copy(id = id)
                _listState.value = _listState.value.copy(songs = updated)
                onSuccess(id)
            }
            result.onFailure { _listState.value = _listState.value.copy(error = it.message) }
        }
    }

    fun updateSong(song: Song, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repo.updateSong(song)
            result.onSuccess {
                // Replace updated song in the list
                val updated = _listState.value.songs.map { if (it.id == song.id) song else it }
                _listState.value = _listState.value.copy(songs = updated)
                onSuccess()
            }
            result.onFailure { _listState.value = _listState.value.copy(error = it.message) }
        }
    }

    fun deleteSong(songId: String, churchId: String) {
        viewModelScope.launch {
            repo.deleteSong(songId)
            loadSongs(churchId)
        }
    }
}
