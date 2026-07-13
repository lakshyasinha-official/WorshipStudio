package com.example.worshipstudio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Church
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worshipstudio.data.model.Song
import com.example.worshipstudio.data.model.WorshipSet
import com.example.worshipstudio.ui.theme.Mint
import com.example.worshipstudio.utils.AppTheme
import com.example.worshipstudio.utils.PdfExporter
import com.example.worshipstudio.viewmodel.AuthViewModel
import com.example.worshipstudio.viewmodel.SessionViewModel
import com.example.worshipstudio.viewmodel.SetViewModel
import com.example.worshipstudio.viewmodel.SongViewModel
import com.example.worshipstudio.viewmodel.TagViewModel

enum class SortOrder(val label: String) {
    NAME_ASC("A → Z"),
    NAME_DESC("Z → A"),
    BY_KEY("By Key"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListScreen(
    authViewModel:    AuthViewModel,
    songViewModel:    SongViewModel,
    setViewModel:     SetViewModel,
    tagViewModel:     TagViewModel,
    sessionViewModel: SessionViewModel? = null,
    currentTheme:     AppTheme = AppTheme.NIGHTFALL,
    onSongClick:      (String) -> Unit,
    onAddSong:        () -> Unit,
    onSetClick:       (String) -> Unit,
    onCreateSet:      () -> Unit,
    onSettings:       () -> Unit,
    onJoinPushSession: ((String) -> Unit)? = null,
    onLogout:         () -> Unit
) {
    val context       = LocalContext.current
    val authState     by authViewModel.state.collectAsState()
    val songListState by songViewModel.listState.collectAsState()
    val setListState  by setViewModel.listState.collectAsState()
    val tagState      by tagViewModel.state.collectAsState()
    val pushFlow      = remember(sessionViewModel) {
        sessionViewModel?.state
            ?: kotlinx.coroutines.flow.MutableStateFlow(com.example.worshipstudio.viewmodel.SessionState())
    }
    val pushState     by pushFlow.collectAsState()
    val churchPush    = pushState.churchPush
    var selectedTab   by remember { mutableIntStateOf(0) }
    var searchQuery   by remember { mutableStateOf("") }
    var drawerOpen    by remember { mutableStateOf(false) }
    var sortOrder     by remember { mutableStateOf(SortOrder.NAME_ASC) }
    var sortMenuOpen  by remember { mutableStateOf(false) }

    // ── Multi-select state ─────────────────────────────────────────────────────
    var selectionMode   by remember { mutableStateOf(false) }
    var selectedIds     by remember { mutableStateOf(setOf<String>()) }
    var overflowOpen    by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var songDelete by remember { mutableStateOf<Song?>(null) }
    fun exitSelection() { selectionMode = false; selectedIds = emptySet() }
    fun toggleSong(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    val isAdmin      by remember { derivedStateOf { authState.role == "admin" } }
    val filterActive by remember { derivedStateOf { songListState.activeTagId != null } }

    LaunchedEffect(authState.churchId) {
        if (authState.churchId.isNotEmpty()) {
            songViewModel.loadSongs(authState.churchId)
            setViewModel.loadSets(authState.churchId)
            tagViewModel.loadTags(authState.churchId)
        }
    }

    // ── Admin alert dialog (password change notification) ─────────────────────
    if (authState.adminAlert != null && isAdmin) {
        AlertDialog(
            onDismissRequest = { authViewModel.dismissAdminAlert() },
            containerColor   = Mint.Card,
            titleContentColor = Mint.TextPrimary,
            textContentColor  = Mint.TextSecondary,
            title = { Text("🔑  Password Update") },
            text  = {
                Text(
                    authState.adminAlert!!,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { authViewModel.dismissAdminAlert() }) {
                    Text("Got it", color = Mint.Accent)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Mint.BgTop, Mint.BgBottom)))
    ) {

        // ── Main scaffold ──────────────────────────────────────────────────────
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (selectionMode) {
                    // ── Selection mode top bar ──────────────────────────────
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor             = Color.Transparent,
                            navigationIconContentColor = Mint.TextPrimary,
                            titleContentColor          = Mint.TextPrimary,
                            actionIconContentColor     = Mint.TextPrimary
                        ),
                        navigationIcon = {
                            IconButton(onClick = { exitSelection() }) {
                                Icon(Icons.Default.Close, "Cancel selection")
                            }
                        },
                        title = {
                            Text(
                                if (selectedIds.isEmpty()) "Select songs"
                                else "${selectedIds.size} selected"
                            )
                        },
                        actions = {
                            // Select All
                            IconButton(onClick = {
                                val all = songListState.songs.map { it.id }.toSet()
                                selectedIds = if (selectedIds == all) emptySet() else all
                            }) {
                                Icon(Icons.Default.SelectAll, "Select all")
                            }
                            // Three-dots overflow
                            Box {
                                IconButton(onClick = { overflowOpen = true }) {
                                    Icon(Icons.Default.MoreVert, "More options")
                                }
                                MintMenuTheme {
                                    DropdownMenu(
                                        expanded         = overflowOpen,
                                        onDismissRequest = { overflowOpen = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Export as PDF") },
                                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) },
                                            enabled = selectedIds.isNotEmpty(),
                                            onClick = {
                                                overflowOpen = false
                                                val toExport = songListState.songs
                                                    .filter { it.id in selectedIds }
                                                    .sortedBy { it.nameLowercase }
                                                PdfExporter.exportAndShare(context, toExport)
                                                exitSelection()
                                            }
                                        )
                                    }
                                }
                            }
                            // Keep clear of the fixed chat button
                            Spacer(Modifier.width(56.dp))
                        }
                    )
                }
            },
            bottomBar = {
                if (!selectionMode) {
                    MintBottomNav(
                        selectedTab = selectedTab,
                        profileOpen = drawerOpen,
                        onHome      = { selectedTab = 0 },
                        onSets      = { selectedTab = 1 },
                        onProfile   = { drawerOpen = true }
                    )
                }
            },
            floatingActionButton = {
                // Songs tab → admin only (database write)
                // Sets tab  → everyone (members can create their own set lists)
                val showFab = if (selectedTab == 0) isAdmin else true
                if (showFab) {
                    FloatingActionButton(
                        onClick        = if (selectedTab == 0) onAddSong else onCreateSet,
                        containerColor = Mint.Accent,
                        contentColor   = Mint.OnAccent,
                        shape          = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Add, "Add")
                    }
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {

                // ── Header row: search + sort + profile ──────────────────────
                if (!selectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedTab == 0) {
                            OutlinedTextField(
                                value         = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    songViewModel.searchSongs(authState.churchId, it)
                                },
                                placeholder   = {
                                    Text("Search song library…", color = Mint.TextSecondary, fontSize = 14.sp)
                                },
                                leadingIcon   = {
                                    Icon(Icons.Default.Search, null, tint = Mint.TextSecondary)
                                },
                                trailingIcon  = if (searchQuery.isNotEmpty()) ({
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        songViewModel.searchSongs(authState.churchId, "")
                                    }) { Icon(Icons.Default.Close, "Clear", tint = Mint.TextSecondary) }
                                }) else null,
                                singleLine    = true,
                                shape         = RoundedCornerShape(16.dp),
                                colors        = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor        = Mint.TextPrimary,
                                    unfocusedTextColor      = Mint.TextPrimary,
                                    focusedBorderColor      = Mint.Accent.copy(alpha = 0.6f),
                                    unfocusedBorderColor    = Mint.BorderField,
                                    cursorColor             = Mint.Accent,
                                    focusedContainerColor   = Mint.Field,
                                    unfocusedContainerColor = Mint.Field
                                ),
                                modifier      = Modifier.weight(1f).height(54.dp)
                            )

                            // Sort / filter button
                            Box {
                                HeaderIconButton(
                                    icon        = Icons.Default.Sort,
                                    description = "Sort & Filter",
                                    tint        = if (sortOrder != SortOrder.NAME_ASC || filterActive)
                                                      Mint.Accent else Mint.TextSecondary
                                ) { sortMenuOpen = true }
                                MintMenuTheme {
                                    DropdownMenu(
                                        expanded         = sortMenuOpen,
                                        onDismissRequest = { sortMenuOpen = false }
                                    ) {
                                        // Sort options
                                        Text(
                                            "Sort by",
                                            style    = MaterialTheme.typography.labelSmall,
                                            color    = Mint.Accent,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                        )
                                        SortOrder.entries.forEach { order ->
                                            DropdownMenuItem(
                                                text = { Text(order.label) },
                                                leadingIcon = {
                                                    if (sortOrder == order)
                                                        Icon(Icons.Default.CheckCircle, null, tint = Mint.Accent)
                                                    else
                                                        Icon(Icons.Default.Sort, null, tint = Mint.TextSecondary)
                                                },
                                                onClick = { sortOrder = order; sortMenuOpen = false }
                                            )
                                        }
                                        // Tag filters (if any exist)
                                        if (tagState.tags.isNotEmpty()) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(vertical = 4.dp),
                                                color    = Mint.BorderSubtle
                                            )
                                            Text(
                                                "Filter by tag",
                                                style    = MaterialTheme.typography.labelSmall,
                                                color    = Mint.Accent,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                            )
                                            DropdownMenuItem(
                                                text = { Text("All tags") },
                                                leadingIcon = {
                                                    if (songListState.activeTagId == null)
                                                        Icon(Icons.Default.CheckCircle, null, tint = Mint.Accent)
                                                    else
                                                        Icon(Icons.Default.FilterList, null, tint = Mint.TextSecondary)
                                                },
                                                onClick = { songViewModel.filterByTag(null); sortMenuOpen = false }
                                            )
                                            tagState.tags.forEach { tag ->
                                                DropdownMenuItem(
                                                    text = { Text(tag.name) },
                                                    leadingIcon = {
                                                        if (songListState.activeTagId == tag.id)
                                                            Icon(Icons.Default.CheckCircle, null, tint = Mint.Accent)
                                                        else
                                                            Icon(Icons.Default.FilterList, null, tint = Mint.TextSecondary)
                                                    },
                                                    onClick = { songViewModel.filterByTag(tag.id); sortMenuOpen = false }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                "Worship Sets",
                                fontSize   = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Mint.TextPrimary,
                                modifier   = Modifier.weight(1f)
                            )
                        }

                        // Space reserved for the fixed chat button overlay
                        Spacer(Modifier.size(54.dp))
                    }

                    Spacer(Modifier.height(14.dp))
                }

                if (selectedTab == 0) {

                    // ── Active filter / sort indicator chips ──────────────────
                    if (filterActive || sortOrder != SortOrder.NAME_ASC) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (sortOrder != SortOrder.NAME_ASC) {
                                IndicatorChip(
                                    label = sortOrder.label,
                                    icon  = Icons.Default.Sort
                                )
                            }
                            if (filterActive) {
                                val activeTag = tagState.tags.find { it.id == songListState.activeTagId }
                                if (activeTag != null) {
                                    IndicatorChip(
                                        label     = activeTag.name,
                                        icon      = Icons.Default.FilterList,
                                        onRemove  = { songViewModel.filterByTag(null) }
                                    )
                                }
                            }
                        }
                    }

                    // ── Song list ──────────────────────────────────────────────
                    if (songListState.isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Mint.Accent)
                        }
                    } else {
                        val sortedSongs = remember(songListState.songs, sortOrder) {
                            when (sortOrder) {
                                SortOrder.NAME_ASC  -> songListState.songs.sortedBy { it.nameLowercase }
                                SortOrder.NAME_DESC -> songListState.songs.sortedByDescending { it.nameLowercase }
                                SortOrder.BY_KEY    -> songListState.songs.sortedWith(
                                    compareBy({ it.rootKey }, { it.nameLowercase })
                                )
                            }
                        }
                        AlphabeticalSongList(
                            songs           = sortedSongs,
                            isAdmin         = isAdmin,
                            sortOrder       = sortOrder,
                            selectionMode   = selectionMode,
                            selectedIds     = selectedIds,
                            onSongClick     = { id ->
                                if (selectionMode) toggleSong(id) else onSongClick(id)
                            },
                            onSongLongClick = { id ->
                                selectionMode = true
                                selectedIds   = setOf(id)
                            },
                            onDelete        = { song ->
                                songDelete = song
                                showDeleteDialog = true
                            }
                        )
                    }
                } else {
                    if (setListState.isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Mint.Accent)
                        }
                    } else if (setListState.sets.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No sets yet.", color = Mint.TextSecondary)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(setListState.sets, key = { it.id }, contentType = { "set" }) { set ->
                                SetCard(
                                    set      = set,
                                    isAdmin  = isAdmin,
                                    onClick  = { onSetClick(set.id) },
                                    onDelete = { setViewModel.deleteSet(set.id, authState.churchId) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Right-side drawer scrim ────────────────────────────────────────────
        AnimatedVisibility(
            visible = drawerOpen,
            enter   = fadeIn(),
            exit    = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { drawerOpen = false }
            )
        }

        // ── Right-side drawer panel ────────────────────────────────────────────
        AnimatedVisibility(
            visible = drawerOpen,
            enter   = slideInHorizontally { it },   // slides in from right
            exit    = slideOutHorizontally { it },  // slides out to right
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                modifier        = Modifier.width(300.dp).fillMaxHeight(),
                color           = Color.Transparent,
                shadowElevation = 0.dp,
                shape           = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
            ) {
                ProfileDrawerContent(
                    displayName = authState.displayName,
                    email       = authState.email,
                    churchId    = authState.churchId,
                    role        = authState.role,
                    songCount   = songListState.songs.size,
                    setCount    = setListState.sets.size,
                    tagCount    = tagState.tags.size,
                    onSettings  = { drawerOpen = false; onSettings() },
                    onLogout    = { drawerOpen = false; onLogout() }
                )
            }
        }

        // ── Push notification banner (members only) ────────────────────────────
        if (churchPush != null && !isAdmin && onJoinPushSession != null) {
            val push = churchPush!!
            var joinValidating by remember(push.sessionId) { mutableStateOf(false) }
            var sessionGone    by remember(push.sessionId) { mutableStateOf(false) }

            // Auto-dismiss after 60 seconds
            LaunchedEffect(push.sessionId) {
                kotlinx.coroutines.delay(60_000)
                sessionViewModel?.dismissChurchPush(authState.churchId)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Surface(
                    shape           = RoundedCornerShape(20.dp),
                    color           = Mint.Card,
                    border          = BorderStroke(
                        1.dp,
                        if (sessionGone) Mint.Error.copy(alpha = 0.5f)
                        else             Mint.Accent.copy(alpha = 0.5f)
                    ),
                    shadowElevation = 8.dp,
                    modifier        = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.foundation.Canvas(Modifier.size(8.dp)) {
                                drawCircle(if (sessionGone) Mint.Error else Mint.Accent)
                            }
                            Text(
                                text  = if (sessionGone) "Session has already ended"
                                        else push.adminName.ifEmpty { "Admin" } + " started a session",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sessionGone) Mint.Error else Mint.TextSecondary
                            )
                        }
                        if (!sessionGone) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text       = push.songName,
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = Mint.TextPrimary
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!sessionGone) {
                                androidx.compose.material3.Button(
                                    onClick  = {
                                        joinValidating = true
                                        sessionViewModel?.validateAndJoinPushSession(
                                            sessionId = push.sessionId,
                                            churchId  = authState.churchId,
                                            onValid   = { id ->
                                                joinValidating = false
                                                onJoinPushSession(id)
                                            },
                                            onStale   = {
                                                joinValidating = false
                                                sessionGone    = true
                                            }
                                        )
                                    },
                                    enabled  = !joinValidating,
                                    colors   = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = Mint.Accent,
                                        contentColor   = Mint.OnAccent
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (joinValidating) CircularProgressIndicator(
                                        Modifier.size(16.dp), color = Mint.OnAccent
                                    )
                                    else Text("Join Now", fontWeight = FontWeight.Bold)
                                }
                            }
                            androidx.compose.material3.OutlinedButton(
                                onClick  = { sessionViewModel?.dismissChurchPush(authState.churchId) },
                                border   = BorderStroke(1.dp, Mint.BorderField),
                                colors   = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    contentColor = Mint.TextSecondary
                                ),
                                modifier = Modifier.weight(1f)
                            ) { Text("Dismiss") }
                        }
                    }
                }
            }
        }
        if (showDeleteDialog && songDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    songDelete = null
                },
                containerColor    = Mint.Card,
                titleContentColor = Mint.TextPrimary,
                textContentColor  = Mint.TextSecondary,
                title = { Text("Delete Song") },
                text  = { Text("Are you sure you want to delete this song?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            songViewModel.deleteSong(
                                songId = songDelete!!.id,
                                churchId = authState.churchId
                            )
                            showDeleteDialog = false
                            songDelete = null
                        }
                    ) {
                        Text("Delete", color = Mint.Error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            songDelete = null
                        }
                    ) {
                        Text("Cancel", color = Mint.TextSecondary)
                    }
                }
            )
        }
    }
}

// ── Dark menu container regardless of the active app theme ────────────────────
@Composable
private fun MintMenuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            surface          = Mint.Card,
            surfaceContainer = Mint.Card,
            onSurface        = Mint.TextPrimary,
            onSurfaceVariant = Mint.TextSecondary
        ),
        typography = MaterialTheme.typography,
        content    = content
    )
}

// ── Small square icon button used in the header row ───────────────────────────
@Composable
private fun HeaderIconButton(
    icon:        ImageVector,
    description: String,
    tint:        Color = Mint.TextPrimary,
    onClick:     () -> Unit
) {
    Surface(
        onClick = onClick,
        shape   = RoundedCornerShape(16.dp),
        color   = Mint.Field,
        border  = BorderStroke(1.dp, Mint.BorderSubtle),
        modifier = Modifier.size(54.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, description, tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}

// ── Bottom navigation bar — Home · Sets · Profile ─────────────────────────────
@Composable
private fun MintBottomNav(
    selectedTab: Int,
    profileOpen: Boolean,
    onHome:      () -> Unit,
    onSets:      () -> Unit,
    onProfile:   () -> Unit
) {
    Surface(color = Mint.Card) {
        Column {
            HorizontalDivider(color = Mint.BorderSubtle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 8.dp, bottom = 10.dp)
            ) {
                BottomNavItem(Icons.Outlined.Home, "Home",
                    selected = selectedTab == 0 && !profileOpen,
                    modifier = Modifier.weight(1f), onClick = onHome)
                BottomNavItem(Icons.Outlined.CalendarMonth, "Sets",
                    selected = selectedTab == 1 && !profileOpen,
                    modifier = Modifier.weight(1f), onClick = onSets)
                BottomNavItem(Icons.Outlined.Person, "Profile",
                    selected = profileOpen,
                    modifier = Modifier.weight(1f), onClick = onProfile)
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon:     ImageVector,
    label:    String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick:  () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon, label,
            tint = if (selected) Mint.Accent else Mint.TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            fontSize   = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color      = if (selected) Mint.Accent else Mint.TextSecondary
        )
    }
}

// ── Active sort / filter indicator chip ────────────────────────────────────────
@Composable
private fun IndicatorChip(
    label:    String,
    icon:     ImageVector,
    onRemove: (() -> Unit)? = null
) {
    Surface(
        shape  = RoundedCornerShape(50),
        color  = Mint.Accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Mint.Accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, tint = Mint.Accent, modifier = Modifier.size(13.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Mint.Accent)
            if (onRemove != null) {
                Icon(
                    Icons.Default.Close, "Remove filter",
                    tint = Mint.Accent,
                    modifier = Modifier.size(13.dp).clickable { onRemove() }
                )
            }
        }
    }
}

// ── Profile drawer content — mint profile page ────────────────────────────────
@Composable
private fun ProfileDrawerContent(
    displayName: String,
    email:       String,
    churchId:    String,
    role:        String,
    songCount:   Int,
    setCount:    Int,
    tagCount:    Int,
    onSettings:  () -> Unit,
    onLogout:    () -> Unit
) {
    val isAdmin = role == "admin"
    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .background(Brush.verticalGradient(listOf(Color(0xFF10201A), Mint.BgBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {

            // ── Avatar + name block ───────────────────────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar circle with mint ring + role badge
                Box {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Mint.Accent.copy(alpha = 0.15f))
                            .border(2.5.dp, Mint.Accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initial, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Mint.Accent)
                    }
                    if (isAdmin) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Mint.Accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Verified, "Admin",
                                tint = Mint.OnAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    displayName.ifBlank { "User" },
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Mint.TextPrimary
                )
                if (email.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(email, fontSize = 13.sp, color = Mint.TextSecondary)
                }
                Spacer(Modifier.height(12.dp))
                // Role pill — indigo, like the mockup's "Premium Member"
                Surface(
                    shape  = RoundedCornerShape(50),
                    color  = Mint.IndigoBg,
                    border = BorderStroke(1.dp, Mint.Indigo.copy(alpha = 0.4f))
                ) {
                    Text(
                        if (isAdmin) "Admin" else "Member",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Mint.Indigo,
                        modifier   = Modifier.padding(horizontal = 18.dp, vertical = 7.dp)
                    )
                }
            }

            // ── Stat tiles ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(songCount.toString(), "SONGS", Modifier.weight(1f))
                StatTile(setCount.toString(),  "SETS",  Modifier.weight(1f))
                StatTile(tagCount.toString(),  "TAGS",  Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // ── Preferences ───────────────────────────────────────────────────
            Text(
                "Preferences",
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = Mint.TextPrimary,
                modifier   = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(10.dp))

            Surface(
                shape  = RoundedCornerShape(18.dp),
                color  = Mint.Card,
                border = BorderStroke(1.dp, Mint.BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    DrawerInfoRow(Icons.Default.Person, "Name", displayName.ifBlank { "—" })
                    HorizontalDivider(color = Mint.BorderSubtle)
                    DrawerInfoRow(Icons.Outlined.Church, "Church", churchId.ifBlank { "—" })
                    HorizontalDivider(color = Mint.BorderSubtle)
                    DrawerPrefRow(Icons.Default.Settings, "Settings", onClick = onSettings)
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(24.dp))

            // ── Sign out ──────────────────────────────────────────────────────
            Surface(
                shape  = RoundedCornerShape(18.dp),
                color  = Mint.Error.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Mint.Error.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onLogout() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Mint.Error, modifier = Modifier.size(20.dp))
                    Text("Sign Out", fontWeight = FontWeight.SemiBold, color = Mint.Error)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Stat tile (mockup-style: big mint number, small caps label) ───────────────
@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape  = RoundedCornerShape(16.dp),
        color  = Mint.Card,
        border = BorderStroke(1.dp, Mint.BorderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Mint.Accent)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                color         = Mint.TextSecondary,
                textAlign     = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DrawerInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DrawerIconTile(icon)
        Column {
            Text(label, fontSize = 11.sp, color = Mint.TextSecondary)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Mint.TextPrimary)
        }
    }
}

@Composable
private fun DrawerPrefRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DrawerIconTile(icon)
        Text(
            label,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Medium,
            color      = Mint.TextPrimary,
            modifier   = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.ChevronRight, null,
            tint = Mint.TextSecondary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun DrawerIconTile(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Mint.Field),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Mint.Accent, modifier = Modifier.size(19.dp))
    }
}

// ── Alphabetical song list with side A–Z bar ──────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlphabeticalSongList(
    songs:           List<Song>,
    isAdmin:         Boolean,
    sortOrder:       SortOrder = SortOrder.NAME_ASC,
    selectionMode:   Boolean = false,
    selectedIds:     Set<String> = emptySet(),
    onSongClick:     (String) -> Unit,
    onSongLongClick: (String) -> Unit = {},
    onDelete:        (Song) -> Unit
) {
    if (songs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No songs found.", color = Mint.TextSecondary)
        }
        return
    }

    if (sortOrder == SortOrder.BY_KEY) {
        // ── BY_KEY: group by rootKey, no sidebar ──────────────────────────────
        val keyOrder = listOf("C","C#","D","Eb","E","F","F#","G","Ab","A","Bb","B")
        val grouped = remember(songs) {
            songs.groupBy { it.rootKey.ifEmpty { "?" } }
                .entries
                .sortedWith(compareBy { idx -> keyOrder.indexOf(idx.key).let { if (it < 0) 99 else it } })
                .associate { it.key to it.value }
        }
        val groupKeys = remember(grouped) { grouped.keys.toList() }

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp)
        ) {
            groupKeys.forEach { key ->
                stickyHeader(key = "hdr_$key") { LetterHeader(key) }
                items(grouped[key] ?: emptyList(), key = { it.id }, contentType = { "song" }) { song ->
                    SongCard(
                        song          = song,
                        isAdmin       = isAdmin,
                        selectionMode = selectionMode,
                        isSelected    = song.id in selectedIds,
                        onClick       = { onSongClick(song.id) },
                        onLongClick   = { onSongLongClick(song.id) },
                        onDelete      = { onDelete(song) }
                    )
                }
            }
        }
        return
    }

    // ── NAME_ASC / NAME_DESC: alphabetical grouping with sidebar ─────────────
    val grouped = remember(songs, sortOrder) {
        when (sortOrder) {
            SortOrder.NAME_DESC -> {
                songs.sortedByDescending { it.nameLowercase }
                    .groupBy { s ->
                        val c = s.name.firstOrNull()?.uppercaseChar()
                        if (c != null && c.isLetter()) c else '#'
                    }
                    .entries
                    .sortedByDescending { if (it.key == '#') ' ' else it.key }
                    .associate { it.key to it.value }
            }
            else -> {
                songs.sortedBy { it.nameLowercase }
                    .groupBy { s ->
                        val c = s.name.firstOrNull()?.uppercaseChar()
                        if (c != null && c.isLetter()) c else '#'
                    }
                    .entries
                    .sortedWith(compareBy { if (it.key == '#') Char.MAX_VALUE else it.key })
                    .associate { it.key to it.value }
            }
        }
    }
    val letters = remember(grouped) { grouped.keys.toList() }
    // CRITICAL: memoize the set so pointerInput key is stable across recompositions.
    val availableLettersSet = remember(letters) { letters.toSet() }

    // Map each letter → its start index in the flat LazyColumn item list
    val letterIndex = remember(grouped) {
        val map = mutableMapOf<Char, Int>()
        var pos = 0
        letters.forEach { letter ->
            map[letter] = pos
            pos += 1 + (grouped[letter]?.size ?: 0)  // 1 header + N songs
        }
        map
    }

    val listState  = rememberLazyListState()
    val scope      = rememberCoroutineScope()

    // Letter bubble — shows briefly when the side bar is touched
    var bubbleLetter by remember { mutableStateOf<Char?>(null) }
    var showBubble   by remember { mutableStateOf(false) }
    LaunchedEffect(bubbleLetter) {
        if (bubbleLetter != null) {
            showBubble = true
            delay(700)
            showBubble = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Main list ──────────────────────────────────────────────────────────
        LazyColumn(
            state          = listState,
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp)
        ) {
            letters.forEach { letter ->
                stickyHeader(key = "hdr_$letter") { LetterHeader(letter.toString()) }
                items(grouped[letter] ?: emptyList(), key = { it.id }, contentType = { "song" }) { song ->
                    SongCard(
                        song          = song,
                        isAdmin       = isAdmin,
                        selectionMode = selectionMode,
                        isSelected    = song.id in selectedIds,
                        onClick       = { onSongClick(song.id) },
                        onLongClick   = { onSongLongClick(song.id) },
                        onDelete      = { onDelete(song) }
                    )
                }
            }
        }

        // ── Side A–Z bar (only when 2+ letter groups) ─────────────────────────
        if (letters.size > 1) {
            AlphabetSideBar(
                availableLetters = availableLettersSet,
                modifier         = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = 8.dp, bottom = 8.dp, end = 2.dp),
                onLetterSelect   = { letter ->
                    bubbleLetter = letter
                    scope.launch {
                        letterIndex[letter]?.let { listState.scrollToItem(it) }
                    }
                }
            )
        }

        // ── Letter bubble indicator ────────────────────────────────────────────
        AnimatedVisibility(
            visible  = showBubble && bubbleLetter != null,
            enter    = scaleIn() + fadeIn(),
            exit     = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 40.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Mint.Accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = bubbleLetter?.toString() ?: "",
                    style      = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color      = Mint.OnAccent
                )
            }
        }
    }
}

// ── Sticky letter / key header ─────────────────────────────────────────────────
@Composable
private fun LetterHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Mint.BgTop.copy(alpha = 0.95f))
            .padding(horizontal = 20.dp, vertical = 5.dp)
    ) {
        Text(
            text       = label,
            fontSize   = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Mint.Accent
        )
    }
}

// ── Side alphabet scroll bar ───────────────────────────────────────────────────
@Composable
private fun AlphabetSideBar(
    availableLetters: Set<Char>,
    onLetterSelect:   (Char) -> Unit,
    modifier:         Modifier = Modifier
) {
    val allLetters = remember { ('A'..'Z').toList() }
    val scope      = rememberCoroutineScope()

    // Hidden by default; fades in when user presses, fades out after release
    var isActive by remember { mutableStateOf(false) }
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue   = if (isActive) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = if (isActive) 120 else 300
        ),
        label = "sidebarAlpha"
    )

    Column(
        modifier = modifier
            .alpha(alpha)
            .background(
                color = Mint.Card.copy(alpha = 0.92f),
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(availableLetters) {
                val total = allLetters.size
                fun letterAt(y: Float): Char {
                    val idx = ((y / size.height.toFloat()) * total)
                        .toInt().coerceIn(0, total - 1)
                    return allLetters[idx]
                }
                awaitEachGesture {
                    isActive = true
                    val down = awaitFirstDown(requireUnconsumed = false)
                    letterAt(down.position.y)
                        .takeIf { it in availableLetters }
                        ?.let { onLetterSelect(it) }
                    drag(down.id) { change ->
                        letterAt(change.position.y)
                            .takeIf { it in availableLetters }
                            ?.let { onLetterSelect(it) }
                        change.consume()
                    }
                    // Gesture ended — fade out after short delay
                    scope.launch {
                        delay(500)
                        isActive = false
                    }
                }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        allLetters.forEach { letter ->
            val active = letter in availableLetters
            if (active) {
                Text(
                    text       = letter.toString(),
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Mint.Accent,
                    modifier   = Modifier.padding(horizontal = 5.dp)
                )
            } else {
                Text(
                    text      = "·",
                    fontSize  = 7.sp,
                    color     = Mint.TextSecondary.copy(alpha = 0.40f),
                    modifier  = Modifier.padding(horizontal = 5.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Small badge pills used inside cards ────────────────────────────────────────
@Composable
private fun KeyBadge(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = Mint.IndigoBg) {
        Text(
            text       = text,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = Mint.Indigo,
            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun InfoBadge(icon: ImageVector, text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = Mint.Field) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = Mint.TextSecondary, modifier = Modifier.size(12.dp))
            Text(
                text       = text,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium,
                color      = Mint.TextSecondary
            )
        }
    }
}

// ── List items ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongCard(
    song:          Song,
    isAdmin:       Boolean,
    selectionMode: Boolean = false,
    isSelected:    Boolean = false,
    onClick:       () -> Unit,
    onLongClick:   () -> Unit = {},
    onDelete:      () -> Unit
) {
    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = Mint.Card,
        border = BorderStroke(
            1.dp,
            if (isSelected) Mint.Accent.copy(alpha = 0.6f) else Mint.BorderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick     = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Outlined.CheckCircle
                                  else            Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (isSelected) Mint.Accent else Mint.TextSecondary
                )
                Spacer(Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = song.name,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Mint.TextPrimary,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyBadge("${song.rootKey} ${song.keyQuality}".uppercase())
                    if (song.parts.isNotEmpty()) {
                        InfoBadge(Icons.Default.MusicNote, "${song.parts.size} sections")
                    }
                }
            }
            if (isAdmin && !selectionMode) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, "Delete", tint = Mint.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun SetCard(set: WorshipSet, isAdmin: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = Mint.Card,
        border = BorderStroke(1.dp, Mint.BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = set.name,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Mint.TextPrimary,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoBadge(Icons.Default.MusicNote, "${set.songs.size} songs")
                    if (set.date.isNotBlank()) {
                        KeyBadge(set.date)
                    }
                }
            }
            if (isAdmin) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, "Delete", tint = Mint.TextSecondary)
                }
            }
        }
    }
}
