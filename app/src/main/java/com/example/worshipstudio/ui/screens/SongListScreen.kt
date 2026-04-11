package com.example.worshipstudio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worshipstudio.data.model.Song
import com.example.worshipstudio.data.model.WorshipSet
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

    fun exitSelection() { selectionMode = false; selectedIds = emptySet() }
    fun toggleSong(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    // derivedStateOf: only recomposes dependants when the BOOLEAN value itself changes,
    // not every time an unrelated auth/song field changes
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
            title = { Text("🔑  Password Update") },
            text  = {
                Text(
                    authState.adminAlert!!,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { authViewModel.dismissAdminAlert() }) {
                    Text("Got it")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Main scaffold ──────────────────────────────────────────────────────
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (selectionMode) {
                    // ── Selection mode top bar ──────────────────────────────
                    TopAppBar(
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
                                DropdownMenu(
                                    expanded        = overflowOpen,
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
                    )
                } else {
                    // ── Normal top bar ──────────────────────────────────────
                    TopAppBar(
                        title = { Text(if (selectedTab == 0) "Songs" else "Sets") },
                        actions = {
                            // Sort / filter overflow — Songs tab only
                            if (selectedTab == 0) {
                                Box {
                                    IconButton(onClick = { sortMenuOpen = true }) {
                                        Icon(Icons.Default.Sort, "Sort & Filter")
                                    }
                                    DropdownMenu(
                                        expanded         = sortMenuOpen,
                                        onDismissRequest = { sortMenuOpen = false }
                                    ) {
                                        // Sort options
                                        Text(
                                            "Sort by",
                                            style    = MaterialTheme.typography.labelSmall,
                                            color    = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                        )
                                        SortOrder.entries.forEach { order ->
                                            DropdownMenuItem(
                                                text = { Text(order.label) },
                                                leadingIcon = {
                                                    if (sortOrder == order)
                                                        Icon(Icons.Default.CheckCircle, null,
                                                            tint = MaterialTheme.colorScheme.primary)
                                                    else
                                                        Icon(Icons.Default.Sort, null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                },
                                                onClick = { sortOrder = order; sortMenuOpen = false }
                                            )
                                        }
                                        // Tag filters (if any exist)
                                        if (tagState.tags.isNotEmpty()) {
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                            Text(
                                                "Filter by tag",
                                                style    = MaterialTheme.typography.labelSmall,
                                                color    = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                            )
                                            DropdownMenuItem(
                                                text = { Text("All tags") },
                                                leadingIcon = {
                                                    if (songListState.activeTagId == null)
                                                        Icon(Icons.Default.CheckCircle, null,
                                                            tint = MaterialTheme.colorScheme.primary)
                                                    else
                                                        Icon(Icons.Default.FilterList, null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                },
                                                onClick = { songViewModel.filterByTag(null); sortMenuOpen = false }
                                            )
                                            tagState.tags.forEach { tag ->
                                                DropdownMenuItem(
                                                    text = { Text(tag.name) },
                                                    leadingIcon = {
                                                        if (songListState.activeTagId == tag.id)
                                                            Icon(Icons.Default.CheckCircle, null,
                                                                tint = MaterialTheme.colorScheme.primary)
                                                        else
                                                            Icon(Icons.Default.FilterList, null,
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    },
                                                    onClick = { songViewModel.filterByTag(tag.id); sortMenuOpen = false }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            IconButton(onClick = { drawerOpen = true }) {
                                Icon(Icons.Default.Person, contentDescription = "Profile")
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                // Songs tab → admin only (database write)
                // Sets tab  → everyone (members can create their own set lists)
                val showFab = if (selectedTab == 0) isAdmin else true
                if (showFab) {
                    FloatingActionButton(onClick = if (selectedTab == 0) onAddSong else onCreateSet) {
                        Icon(Icons.Default.Add, "Add")
                    }
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
                    // ── Search bar — contacts-style full width ─────────────────────
                    OutlinedTextField(
                        value         = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            songViewModel.searchSongs(authState.churchId, it)
                        },
                        placeholder   = { Text("Search songs…") },
                        leadingIcon   = { Icon(Icons.Default.Search, null) },
                        trailingIcon  = if (searchQuery.isNotEmpty()) ({
                            IconButton(onClick = {
                                searchQuery = ""
                                songViewModel.searchSongs(authState.churchId, "")
                            }) { Icon(Icons.Default.Close, "Clear") }
                        }) else null,
                        singleLine    = true,
                        shape         = RoundedCornerShape(28.dp),
                        modifier      = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .height(52.dp),
                        textStyle     = MaterialTheme.typography.bodyMedium
                    )

                    // ── Active filter chip ─────────────────────────────────────────
                    if (filterActive) {
                        val activeTag = tagState.tags.find { it.id == songListState.activeTagId }
                        if (activeTag != null) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = true,
                                    onClick  = { songViewModel.filterByTag(null) },
                                    label    = { Text(activeTag.name) },
                                    trailingIcon = { Icon(Icons.Default.Close, "Remove filter", modifier = Modifier.size(14.dp)) },
                                    colors   = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    // ── Sort indicator row ─────────────────────────────────────────
                    if (sortOrder != SortOrder.NAME_ASC) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Sort, null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Text(
                                sortOrder.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // ── Song list ──────────────────────────────────────────────────
                    if (songListState.isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    } else {
                        // Apply sort client-side
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
                            onDelete        = { song -> songViewModel.deleteSong(song.id, authState.churchId) }
                        )
                    }
                } else {
                    if (setListState.isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    } else {
                        LazyColumn {
                            items(setListState.sets, key = { it.id }) { set ->
                                SetItem(
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
                    displayName  = authState.displayName,
                    churchId     = authState.churchId,
                    role         = authState.role,
                    currentTheme = currentTheme,
                    onSettings   = { drawerOpen = false; onSettings() },
                    onLogout     = { drawerOpen = false; onLogout() }
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
                    shape     = RoundedCornerShape(20.dp),
                    color     = if (sessionGone) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 8.dp,
                    modifier  = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.foundation.Canvas(Modifier.size(8.dp)) {
                                drawCircle(
                                    if (sessionGone) androidx.compose.ui.graphics.Color(0xFFEF5350)
                                    else             androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                )
                            }
                            Text(
                                text  = if (sessionGone) "Session has already ended"
                                        else push.adminName.ifEmpty { "Admin" } + " started a session",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sessionGone) MaterialTheme.colorScheme.onErrorContainer
                                        else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (!sessionGone) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text       = push.songName,
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onPrimaryContainer
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
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (joinValidating) CircularProgressIndicator(Modifier.size(16.dp))
                                    else Text("Join Now")
                                }
                            }
                            androidx.compose.material3.OutlinedButton(
                                onClick  = { sessionViewModel?.dismissChurchPush(authState.churchId) },
                                modifier = Modifier.weight(1f)
                            ) { Text(if (sessionGone) "Dismiss" else "Dismiss") }
                        }
                    }
                }
            }
        }
    }
}

// ── Theme-aware colour set for the sidebar ────────────────────────────────────
private data class DrawerColors(
    val bgTop:      Color,
    val bgBottom:   Color,
    val text:       Color,
    val subText:    Color,
    val accent:     Color,
    val cardBg:     Color,
    val divider:    Color,
    val logoutBg:   Color,
    val logoutText: Color
)

private fun drawerColorsForTheme(theme: AppTheme) = when (theme) {
    AppTheme.NIGHTFALL  -> DrawerColors(
        bgTop      = Color(0xFF0A0A18),
        bgBottom   = Color(0xFF2D0060),
        text       = Color.White,
        subText    = Color.White.copy(alpha = 0.50f),
        accent     = Color(0xFFBB86FC),
        cardBg     = Color.White.copy(alpha = 0.07f),
        divider    = Color.White.copy(alpha = 0.10f),
        logoutBg   = Color(0xFFFF6B6B).copy(alpha = 0.13f),
        logoutText = Color(0xFFFF6B6B)
    )
    AppTheme.DAWN_MIST  -> DrawerColors(
        bgTop      = Color(0xFFFDF6EE),
        bgBottom   = Color(0xFFF2D9E8),
        text       = Color(0xFF2D1208),
        subText    = Color(0xFF2D1208).copy(alpha = 0.50f),
        accent     = Color(0xFFC4836C),
        cardBg     = Color(0xFF000000).copy(alpha = 0.05f),
        divider    = Color(0xFF000000).copy(alpha = 0.08f),
        logoutBg   = Color(0xFFB00020).copy(alpha = 0.08f),
        logoutText = Color(0xFFB00020)
    )
    AppTheme.HOLY_LIGHT -> DrawerColors(
        bgTop      = Color(0xFFE8F3FF),
        bgBottom   = Color(0xFFD6EAFF),
        text       = Color(0xFF0D2137),
        subText    = Color(0xFF0D2137).copy(alpha = 0.50f),
        accent     = Color(0xFF3A7FC1),
        cardBg     = Color(0xFF000000).copy(alpha = 0.05f),
        divider    = Color(0xFF000000).copy(alpha = 0.08f),
        logoutBg   = Color(0xFFB00020).copy(alpha = 0.08f),
        logoutText = Color(0xFFB00020)
    )
    AppTheme.SANCTUARY  -> DrawerColors(
        bgTop      = Color(0xFFFDFBF7),
        bgBottom   = Color(0xFFEDE8F6),
        text       = Color(0xFF1C2130),
        subText    = Color(0xFF1C2130).copy(alpha = 0.50f),
        accent     = Color(0xFF7A8CCC),
        cardBg     = Color(0xFF000000).copy(alpha = 0.05f),
        divider    = Color(0xFF000000).copy(alpha = 0.08f),
        logoutBg   = Color(0xFFB00020).copy(alpha = 0.08f),
        logoutText = Color(0xFFB00020)
    )
}

// ── Profile drawer content ────────────────────────────────────────────────────
@Composable
private fun ProfileDrawerContent(
    displayName:  String,
    churchId:     String,
    role:         String,
    currentTheme: AppTheme = AppTheme.NIGHTFALL,
    onSettings:   () -> Unit,
    onLogout:     () -> Unit
) {
    val isAdmin = role == "admin"
    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    // Memoize: drawerColorsForTheme only changes when the theme changes
    val dc      = remember(currentTheme) { drawerColorsForTheme(currentTheme) }
    // Memoize: Brush object creation is non-trivial; stable until theme changes
    val bgBrush = remember(dc.bgTop, dc.bgBottom) {
        Brush.verticalGradient(listOf(dc.bgTop, dc.bgBottom))
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .background(bgBrush)
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {

            // ── Avatar + name block ───────────────────────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(dc.accent.copy(alpha = 0.25f))
                        .border(2.dp, dc.accent.copy(alpha = 0.60f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initial, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = dc.accent)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    displayName.ifBlank { "User" },
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = dc.text
                )
                Spacer(Modifier.height(6.dp))
                // Role pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(dc.accent.copy(alpha = 0.18f))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        role.replaceFirstChar { it.uppercase() },
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color      = dc.accent
                    )
                }
            }

            // ── Info card ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(dc.cardBg)
            ) {
                Column {
                    DrawerInfoRow(icon = Icons.Default.Person,   label = "Name",   value = displayName.ifBlank { "—" }, dc = dc)
                    HorizontalDivider(color = dc.divider)
                    DrawerInfoRow(icon = Icons.Default.Settings, label = "Church", value = churchId.ifBlank { "—" }, dc = dc)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Action buttons ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(dc.cardBg)
            ) {
                Column {
                    DrawerActionRow(
                        icon    = Icons.Default.Settings,
                        label   = "Settings",
                        color   = dc.text,
                        onClick = onSettings
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Sign out ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(dc.logoutBg)
                    .clickable { onLogout() }
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = dc.logoutText, modifier = Modifier.size(20.dp))
                    Text("Sign Out", fontWeight = FontWeight.SemiBold, color = dc.logoutText)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DrawerInfoRow(icon: ImageVector, label: String, value: String, dc: DrawerColors) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = dc.subText, modifier = Modifier.size(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = dc.subText)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = dc.text)
        }
    }
}

@Composable
private fun DrawerActionRow(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = color.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
        Text(label, fontWeight = FontWeight.Medium, color = color)
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
            Text("No songs found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            contentPadding = PaddingValues(0.dp)
        ) {
            groupKeys.forEach { key ->
                stickyHeader(key = "hdr_$key") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.93f))
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text       = key,
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                items(grouped[key] ?: emptyList(), key = { it.id }) { song ->
                    SongItem(
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
    // Group alphabetically; non-letter starts go in '#' at the end.
    // Uses pre-stored nameLowercase to avoid per-comparison string allocations.
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
    // Without this, every recomposition (e.g. bubble show/hide) creates a new Set,
    // causing the gesture coroutine to cancel+restart → visible jank.
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
            contentPadding = PaddingValues(end = 26.dp)  // room for side bar
        ) {
            letters.forEach { letter ->
                stickyHeader(key = "hdr_$letter") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.93f))
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text       = letter.toString(),
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                items(grouped[letter] ?: emptyList(), key = { it.id }) { song ->
                    SongItem(
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
            // Barely-visible tap affordance — matches Android contacts scrollbar style
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 1.dp)
                    .width(2.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.13f))
            )
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
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = bubbleLetter?.toString() ?: "",
                    style      = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
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
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
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
                    color      = MaterialTheme.colorScheme.primary,
                    modifier   = Modifier.padding(horizontal = 5.dp)
                )
            } else {
                Text(
                    text      = "·",
                    fontSize  = 7.sp,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f),
                    modifier  = Modifier.padding(horizontal = 5.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── List items ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongItem(
    song:          Song,
    isAdmin:       Boolean,
    selectionMode: Boolean = false,
    isSelected:    Boolean = false,
    onClick:       () -> Unit,
    onLongClick:   () -> Unit = {},
    onDelete:      () -> Unit
) {
    val bgColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    else
        Color.Transparent

    ListItem(
        headlineContent = { Text(song.name) },
        supportingContent = {
            val subtitle = if (song.parts.isNotEmpty())
                "${song.rootKey} ${song.keyQuality}  •  ${song.parts.size} sections"
            else
                "${song.rootKey} ${song.keyQuality}"
            Text(subtitle)
        },
        leadingContent = if (selectionMode) ({
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckBox
                              else            Icons.Default.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }) else null,
        trailingContent = if (isAdmin && !selectionMode) ({
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
        }) else null,
        modifier = Modifier
            .background(bgColor)
            .combinedClickable(
                onClick     = onClick,
                onLongClick = onLongClick
            )
    )
    HorizontalDivider()
}

@Composable
private fun SetItem(set: WorshipSet, isAdmin: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent    = { Text(set.name) },
        supportingContent  = { Text("${set.songs.size} songs  •  ${set.date}") },
        trailingContent    = if (isAdmin) ({
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
        }) else null,
        modifier = Modifier.clickable { onClick() }
    )
    HorizontalDivider()
}
