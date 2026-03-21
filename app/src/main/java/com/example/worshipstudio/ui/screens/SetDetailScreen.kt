package com.example.worshipstudio.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
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
                // ── Session panel ──────────────────────────────────────────────
                val sessionActive = sessionState.sessionId.isNotEmpty()
                if (sessionActive) {
                    ActiveSessionBanner(
                        sessionId        = sessionState.sessionId,
                        participantCount = sessionState.participantCount,
                        isAdmin          = authState.role == "admin",
                        onOpen           = { onStartSession(sessionState.sessionId, sessionState.isAdmin) },
                        onEnd            = { sessionViewModel.endSession() }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (authState.role == "admin") {
                                Button(
                                    onClick  = { sessionViewModel.createSession(set.id, authState.userId) },
                                    modifier = Modifier.weight(1f),
                                    enabled  = !sessionState.isLoading
                                ) {
                                    if (sessionState.isLoading) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = androidx.compose.ui.graphics.Color.White
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Starting…")
                                    } else {
                                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Start Session")
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick  = { showJoinDialog = true },
                                modifier = Modifier.weight(1f)
                            ) { Text("Join Session") }
                        }

                        // Show error if session creation failed
                        sessionState.error?.let { error ->
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text  = "Failed: $error",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
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

// ── Active session banner ─────────────────────────────────────────────────────
@Composable
private fun ActiveSessionBanner(
    sessionId: String,
    participantCount: Int,
    isAdmin: Boolean,
    onOpen: () -> Unit,
    onEnd: () -> Unit
) {
    val clipboard = LocalClipboardManager.current

    // Pulsing glow animation for the live dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpen() },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1B5E20).copy(alpha = 0.12f),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {

            // ── Top row: live indicator + device count ─────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Pulsing dot + "LIVE" label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(color = Color(0xFF4CAF50).copy(alpha = pulse))
                        drawCircle(color = Color(0xFF4CAF50), radius = size.minDimension * 0.35f)
                    }
                    Text(
                        "SESSION LIVE",
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color(0xFF2E7D32),
                        letterSpacing = 1.sp
                    )
                }

                // Device count chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF2E7D32).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Small device icon using circles
                        Canvas(modifier = Modifier.size(8.dp)) {
                            drawCircle(color = Color(0xFF2E7D32))
                        }
                        Text(
                            text  = "$participantCount ${if (participantCount == 1) "device" else "devices"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Session ID row (tap to copy) ───────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2E7D32).copy(alpha = 0.08f))
                    .clickable {
                        clipboard.setText(AnnotatedString(sessionId))
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Session Code",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text       = sessionId,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp,
                        color      = Color(0xFF1B5E20),
                        letterSpacing = 4.sp
                    )
                }
                Text(
                    "tap to copy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Action buttons ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick  = onOpen,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Open Session")
                }
                if (isAdmin) {
                    OutlinedButton(
                        onClick  = onEnd,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("End")
                    }
                }
            }
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
