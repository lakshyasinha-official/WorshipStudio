package com.example.worshipstudio.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.worshipstudio.data.model.Song
import com.example.worshipstudio.data.model.WorshipSet
import com.example.worshipstudio.viewmodel.AuthViewModel
import com.example.worshipstudio.viewmodel.SetViewModel
import com.example.worshipstudio.viewmodel.SongViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListScreen(
    authViewModel: AuthViewModel,
    songViewModel: SongViewModel,
    setViewModel: SetViewModel,
    onSongClick: (String) -> Unit,
    onAddSong: () -> Unit,
    onSetClick: (String) -> Unit,
    onCreateSet: () -> Unit,
    onLogout: () -> Unit
) {
    val authState by authViewModel.state.collectAsState()
    val songListState by songViewModel.listState.collectAsState()
    val setListState by setViewModel.listState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(authState.churchId) {
        if (authState.churchId.isNotEmpty()) {
            songViewModel.loadSongs(authState.churchId)
            setViewModel.loadSets(authState.churchId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WorshipSync") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = if (selectedTab == 0) onAddSong else onCreateSet) {
                Icon(Icons.Default.Add, "Add")
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Songs", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Sets", modifier = Modifier.padding(12.dp))
                }
            }

            if (selectedTab == 0) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        songViewModel.searchSongs(authState.churchId, it)
                    },
                    label = { Text("Search songs...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                if (songListState.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn {
                        items(songListState.songs, key = { it.id }) { song ->
                            SongItem(
                                song = song,
                                onClick = { onSongClick(song.id) },
                                onDelete = { songViewModel.deleteSong(song.id, authState.churchId) }
                            )
                        }
                    }
                }
            } else {
                if (setListState.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn {
                        items(setListState.sets, key = { it.id }) { set ->
                            SetItem(
                                set = set,
                                onClick = { onSetClick(set.id) },
                                onDelete = { setViewModel.deleteSet(set.id, authState.churchId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongItem(song: Song, onClick: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(song.name) },
        supportingContent = {
            val parts = song.parts
            val subtitle = if (parts.isNotEmpty())
                "${song.rootKey} ${song.keyQuality}  •  ${parts.size} sections"
            else
                "${song.rootKey} ${song.keyQuality}"
            Text(subtitle)
        },
        trailingContent = {
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
        },
        modifier = Modifier.clickable { onClick() }
    )
    HorizontalDivider()
}

@Composable
private fun SetItem(set: WorshipSet, onClick: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(set.name) },
        supportingContent = { Text("${set.songs.size} songs  •  ${set.date}") },
        trailingContent = {
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
        },
        modifier = Modifier.clickable { onClick() }
    )
    HorizontalDivider()
}
