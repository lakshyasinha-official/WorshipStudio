package com.example.worshipstudio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worshipstudio.data.model.Song
import com.example.worshipstudio.data.model.WorshipSet
import com.example.worshipstudio.viewmodel.AuthViewModel
import com.example.worshipstudio.viewmodel.SetViewModel
import com.example.worshipstudio.viewmodel.SongViewModel
import kotlinx.coroutines.launch

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
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val authState    by authViewModel.state.collectAsState()
    val songListState by songViewModel.listState.collectAsState()
    val setListState  by setViewModel.listState.collectAsState()
    var selectedTab  by remember { mutableIntStateOf(0) }
    var searchQuery  by remember { mutableStateOf("") }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()

    LaunchedEffect(authState.churchId) {
        if (authState.churchId.isNotEmpty()) {
            songViewModel.loadSongs(authState.churchId)
            setViewModel.loadSets(authState.churchId)
        }
    }

    ModalNavigationDrawer(
        drawerState   = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            ) {
                ProfileDrawerContent(
                    displayName = authState.displayName,
                    churchId    = authState.churchId,
                    role        = authState.role,
                    onSettings  = {
                        scope.launch { drawerState.close() }
                        onSettings()
                    },
                    onLogout    = {
                        scope.launch { drawerState.close() }
                        onLogout()
                    }
                )
            }
        }
    ) {
        Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("WorshipSync") },
                    actions = {
                        // Profile icon — opens drawer
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = if (selectedTab == 0) onAddSong else onCreateSet
                ) {
                    Icon(Icons.Default.Add, "Add")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
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
                                    song     = song,
                                    onClick  = { onSongClick(song.id) },
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
                                    set      = set,
                                    onClick  = { onSetClick(set.id) },
                                    onDelete = { setViewModel.deleteSet(set.id, authState.churchId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Profile drawer content ────────────────────────────────────────────────────
@Composable
private fun ProfileDrawerContent(
    displayName: String,
    churchId: String,
    role: String,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(0.dp)
    ) {
        // ── Header band ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
        ) {
            Column {
                // Avatar circle with first letter
                val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = initial,
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text       = displayName.ifBlank { "User" },
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Church row ────────────────────────────────────────────────────────
        ProfileInfoRow(label = "Church", value = churchId.ifBlank { "—" })
        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

        // ── Role badge row ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                "Role",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (role == "admin")
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text       = role.replaceFirstChar { it.uppercase() },
                    modifier   = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = if (role == "admin")
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

        // ── Settings button ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSettings() }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Settings",
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

        Spacer(Modifier.weight(1f))

        // ── Logout ────────────────────────────────────────────────────────────
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLogout() }
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.ExitToApp,
                contentDescription = "Logout",
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                "Sign Out",
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── List items ────────────────────────────────────────────────────────────────
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
        headlineContent    = { Text(set.name) },
        supportingContent  = { Text("${set.songs.size} songs  •  ${set.date}") },
        trailingContent    = {
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
        },
        modifier = Modifier.clickable { onClick() }
    )
    HorizontalDivider()
}
