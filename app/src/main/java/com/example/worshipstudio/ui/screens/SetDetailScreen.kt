package com.example.worshipstudio.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.worshipstudio.data.model.Song
import com.example.worshipstudio.viewmodel.AuthViewModel
import com.example.worshipstudio.viewmodel.SessionViewModel
import com.example.worshipstudio.viewmodel.SetViewModel
import com.example.worshipstudio.viewmodel.SongViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetDetailScreen(
    setId: String,
    authViewModel: AuthViewModel,
    setViewModel: SetViewModel,
    songViewModel: SongViewModel,
    sessionViewModel: SessionViewModel,
    onBack: () -> Unit,
    onSongClick: (String) -> Unit,
    onStartSession: (String, Boolean) -> Unit
) {
    val authState by authViewModel.state.collectAsState()
    val setDetailState by setViewModel.detailState.collectAsState()
    val songListState by songViewModel.listState.collectAsState()
    val sessionState by sessionViewModel.state.collectAsState()

    var showAddSongDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var sessionIdInput by remember { mutableStateOf("") }
    var navigated by rememberSaveable { mutableStateOf(false) }

    val songMap = remember(songListState.songs) { songListState.songs.associateBy { it.id } }

    LaunchedEffect(setId) { setViewModel.loadSet(setId) }
    LaunchedEffect(authState.churchId) {
        if (authState.churchId.isNotEmpty()) songViewModel.loadSongs(authState.churchId)
    }

    LaunchedEffect(sessionState.sessionId) {
        if (sessionState.sessionId.isNotEmpty() && !navigated) {
            navigated = true
            onStartSession(sessionState.sessionId, sessionState.isAdmin)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(setDetailState.set?.name ?: "Set") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddSongDialog = true }) {
                        Icon(Icons.Default.Add, "Add Song")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            setDetailState.set?.let { set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (authState.role == "admin") {
                        Button(
                            onClick = { sessionViewModel.createSession(set.id, authState.userId) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Start Session") }
                    }
                    OutlinedButton(
                        onClick = { showJoinDialog = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("Join Session") }
                }
                HorizontalDivider()

                if (set.songs.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No songs yet. Tap + to add songs.")
                    }
                } else {
                    LazyColumn {
                        itemsIndexed(set.songs) { index, songId ->
                            SetSongItem(
                                index = index,
                                song = songMap[songId],
                                songId = songId,
                                totalSongs = set.songs.size,
                                onClick = { songMap[songId]?.let { onSongClick(it.id) } },
                                onRemove = { setViewModel.removeSongFromSet(songId) },
                                onMoveUp = { setViewModel.moveSongUp(index) },
                                onMoveDown = { setViewModel.moveSongDown(index) }
                            )
                        }
                    }
                }
            }
        }

        if (showAddSongDialog) {
            val currentSongIds = setDetailState.set?.songs ?: emptyList()
            val available = songListState.songs.filter { it.id !in currentSongIds }
            AlertDialog(
                onDismissRequest = { showAddSongDialog = false },
                title = { Text("Add Song to Set") },
                text = {
                    if (available.isEmpty()) {
                        Text("No more songs to add.")
                    } else {
                        Column(
                            modifier = Modifier
                                .height(300.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            available.forEach { song ->
                                ListItem(
                                    headlineContent = { Text(song.name) },
                                    supportingContent = { Text("Key: ${song.rootKey}") },
                                    modifier = Modifier.clickable {
                                        setViewModel.addSongToSet(song.id)
                                        showAddSongDialog = false
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddSongDialog = false }) { Text("Close") }
                }
            )
        }

        if (showJoinDialog) {
            AlertDialog(
                onDismissRequest = { showJoinDialog = false },
                title = { Text("Join Session") },
                text = {
                    OutlinedTextField(
                        value = sessionIdInput,
                        onValueChange = { sessionIdInput = it },
                        label = { Text("Session ID") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (sessionIdInput.isNotBlank()) {
                            sessionViewModel.joinSession(sessionIdInput.trim().uppercase(), authState.userId)
                            showJoinDialog = false
                        }
                    }) { Text("Join") }
                },
                dismissButton = {
                    TextButton(onClick = { showJoinDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun SetSongItem(
    index: Int,
    song: Song?,
    songId: String,
    totalSongs: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    ListItem(
        headlineContent = { Text("${index + 1}. ${song?.name ?: songId}") },
        supportingContent = { song?.let { Text("Key: ${it.rootKey}") } },
        leadingContent = {
            Column {
                if (index > 0) {
                    TextButton(
                        onClick = onMoveUp,
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("▲") }
                }
                if (index < totalSongs - 1) {
                    TextButton(
                        onClick = onMoveDown,
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("▼") }
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "Remove") }
        },
        modifier = Modifier.clickable { onClick() }
    )
    HorizontalDivider()
}
