package com.example.worshipstudio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worshipstudio.data.model.SongPart
import com.example.worshipstudio.engine.ChordEngine
import com.example.worshipstudio.engine.LyricToken
import com.example.worshipstudio.ui.components.ActiveChordColor
import com.example.worshipstudio.ui.components.ChordLyricView
import com.example.worshipstudio.utils.generateQrBitmap
import com.example.worshipstudio.viewmodel.SessionViewModel
import com.example.worshipstudio.viewmodel.SongViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun sessionPartColor(type: String): Color = when (type) {
    "Start"  -> Color(0xFF2E7D32)
    "Verse"  -> Color(0xFF1565C0)
    "Chorus" -> Color(0xFFE65100)
    "Bridge" -> Color(0xFF6A1B9A)
    "End"    -> Color(0xFFC62828)
    else     -> Color(0xFF37474F)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSessionScreen(
    sessionId:        String,
    isAdmin:          Boolean,
    sessionViewModel: SessionViewModel,
    songViewModel:    SongViewModel,
    onBack:           () -> Unit
) {
    val state             by sessionViewModel.state.collectAsState()
    val song              = state.currentSong
    val session           = state.session
    val participantCount  = state.participantCount
    val roomCode          = state.roomCode
    val activeChordDegree = session?.activeChordDegree ?: ""

    // Member chord-change toast state
    var toastVisible   by remember { mutableStateOf(false) }
    var toastChordName by remember { mutableStateOf("") }

    // Admin uses the song's root key (or whatever they set); members pick their own
    val songKey     = song?.rootKey    ?: "C"
    val songQuality = song?.keyQuality ?: "Major"

    // Admin key comes from Firebase session; fallback to song's own key
    val adminKey     = session?.adminKey?.takeIf { it.isNotEmpty() }     ?: songKey
    val adminQuality = session?.adminQuality?.takeIf { it.isNotEmpty() } ?: songQuality

    // Member-local key (resets when song changes)
    var memberKey     by remember(song?.id) { mutableStateOf(songKey) }
    var memberQuality by remember(song?.id) { mutableStateOf(songQuality) }

    // Key actually used for chord rendering
    val displayKey     = if (isAdmin) adminKey     else memberKey
    val displayQuality = if (isAdmin) adminQuality else memberQuality

    // Key picker sheets
    val sheetState          = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val adminSheetState     = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope               = rememberCoroutineScope()
    var showKeyPicker       by remember { mutableStateOf(false) }    // member
    var showAdminKeyPicker  by remember { mutableStateOf(false) }    // admin

    // Member: dialog when admin changes key mid-session
    var showKeyChangeDialog  by remember { mutableStateOf(false) }
    var keyChangeDialogKey   by remember { mutableStateOf("") }
    var keyChangeDialogQuality by remember { mutableStateOf("") }

    // Member: kicked-out dialog
    var showKickedDialog by remember { mutableStateOf(false) }

    val totalSongs = state.set?.songs?.size ?: 0

    LaunchedEffect(sessionId, isAdmin) {
        if (isAdmin) sessionViewModel.connectAsAdmin(sessionId)
        else         sessionViewModel.joinSession(sessionId, "")
    }

    // Member: detect when admin changes the key and show popup
    LaunchedEffect(session?.adminKey, session?.adminQuality) {
        val newKey     = session?.adminKey     ?: ""
        val newQuality = session?.adminQuality ?: ""
        if (!isAdmin && newKey.isNotEmpty()) {
            keyChangeDialogKey     = newKey
            keyChangeDialogQuality = newQuality.ifEmpty { "Major" }
            showKeyChangeDialog    = true
        }
    }

    // Member toast: show briefly when admin calls a chord
    LaunchedEffect(activeChordDegree) {
        if (!isAdmin && activeChordDegree.isNotEmpty()) {
            toastChordName = ChordEngine.resolveChord(activeChordDegree, displayKey, displayQuality)
            toastVisible   = true
            delay(2500L)
            toastVisible   = false
        }
    }

    // Member: handle session ended (kicked out)
    LaunchedEffect(state.error) {
        if (!isAdmin && state.error == "Session has ended") {
            showKickedDialog = true
        }
    }

    // Pulsing live dot
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label         = "liveDot"
    )

    // Chord map: ordered list of (degree, resolvedName) — first appearance wins
    val chordMap: List<Pair<String, String>> = remember(song, displayKey, displayQuality) {
        if (song == null) return@remember emptyList()
        val lines = if (song.parts.isNotEmpty())
            song.parts.flatMap { it.lyrics.lines() }
        else
            song.lyrics.lines()
        val seen   = linkedSetOf<String>()
        val result = mutableListOf<Pair<String, String>>()
        lines.forEach { line ->
            ChordEngine.parseLyrics(line, displayKey, displayQuality).forEach { token ->
                if (token is LyricToken.Chord && token.degree !in seen) {
                    seen.add(token.degree)
                    result.add(token.degree to token.resolved)
                }
            }
        }
        result
    }

    // QR bitmap memoized
    val qrBitmap = remember(roomCode) {
        if (roomCode.isNotEmpty()) generateQrBitmap("worshipsync://join/$roomCode", size = 512)
        else null
    }

    // ── Kicked-out dialog ─────────────────────────────────────────────────────
    if (showKickedDialog) {
        AlertDialog(
            onDismissRequest = {},
            title   = { Text("Session Ended") },
            text    = { Text("The admin has ended the session.") },
            confirmButton = {
                TextButton(onClick = {
                    showKickedDialog = false
                    sessionViewModel.clearError()
                    onBack()
                }) { Text("OK") }
            }
        )
    }

    // ── Admin key-change dialog shown to members ───────────────────────────────
    if (showKeyChangeDialog && !isAdmin) {
        AlertDialog(
            onDismissRequest = { showKeyChangeDialog = false },
            title   = { Text("Admin Changed Key") },
            text    = {
                Text("The admin set the key to $keyChangeDialogKey $keyChangeDialogQuality. Switch to match?")
            },
            confirmButton = {
                TextButton(onClick = {
                    memberKey     = keyChangeDialogKey
                    memberQuality = keyChangeDialogQuality
                    showKeyChangeDialog = false
                }) { Text("Use This Key") }
            },
            dismissButton = {
                TextButton(onClick = { showKeyChangeDialog = false }) { Text("Keep Mine") }
            }
        )
    }

    // ── Member key-picker sheet ────────────────────────────────────────────────
    if (showKeyPicker && !isAdmin) {
        ModalBottomSheet(
            onDismissRequest = { showKeyPicker = false },
            sheetState       = sheetState
        ) {
            KeyPickerContent(
                title        = "My Key",
                subtitle     = "Choose the key that works for you. Other members are unaffected.",
                selectedKey  = memberKey,
                selectedQuality = memberQuality,
                onQualityChange = { memberQuality = it },
                onKeySelect  = { key ->
                    memberKey = key
                    scope.launch { sheetState.hide() }
                        .invokeOnCompletion { showKeyPicker = false }
                }
            )
        }
    }

    // ── Admin key-picker sheet (broadcasts to all) ─────────────────────────────
    if (showAdminKeyPicker && isAdmin) {
        ModalBottomSheet(
            onDismissRequest = { showAdminKeyPicker = false },
            sheetState       = adminSheetState
        ) {
            // Local state tracks selection before confirming
            var pickerKey     by remember { mutableStateOf(adminKey) }
            var pickerQuality by remember { mutableStateOf(adminQuality) }
            KeyPickerContent(
                title        = "Set Key for Everyone",
                subtitle     = "All members will be notified of the key change.",
                selectedKey  = pickerKey,
                selectedQuality = pickerQuality,
                onQualityChange = { pickerQuality = it },
                onKeySelect  = { key ->
                    pickerKey = key
                    sessionViewModel.setAdminKey(pickerKey, pickerQuality)
                    scope.launch { adminSheetState.hide() }
                        .invokeOnCompletion { showAdminKeyPicker = false }
                }
            )
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Canvas(Modifier.size(9.dp)) {
                                drawCircle(Color(0xFF4CAF50).copy(alpha = pulse))
                                drawCircle(Color(0xFF4CAF50), radius = size.minDimension * 0.38f)
                            }
                            Text(
                                if (roomCode.isNotEmpty()) "Room $roomCode" else "Live",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                if (isAdmin) "Admin · tap chords to call out"
                                else         "Member · tap key to change yours",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAdmin) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.secondary
                            )
                            Canvas(Modifier.size(3.dp)) { drawCircle(Color.Gray) }
                            Text("$participantCount connected",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isAdmin) sessionViewModel.endSession()
                        else         sessionViewModel.leaveSession()
                        onBack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        },
        bottomBar = {
            if (isAdmin) {
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick  = { sessionViewModel.previousSong() },
                            enabled  = (session?.currentSongIndex ?: 0) > 0
                        ) { Text("◀ Prev") }
                        Text("${(session?.currentSongIndex ?: 0) + 1} / $totalSongs",
                            style = MaterialTheme.typography.titleMedium)
                        Button(
                            onClick  = { sessionViewModel.nextSong() },
                            enabled  = (session?.currentSongIndex ?: 0) < totalSongs - 1
                        ) { Text("Next ▶") }
                    }
                }
            } else if (chordMap.isNotEmpty()) {
                BottomAppBar(tonalElevation = 4.dp) {
                    ChordMapStrip(
                        chordMap          = chordMap,
                        activeChordDegree = activeChordDegree,
                        pulse             = pulse
                    )
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null && song == null && !showKickedDialog -> Box(
                Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                Text(state.error ?: "Session ended",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge)
            }

            else -> {
                Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Admin: room code + QR card ─────────────────────────────
                    if (isAdmin && roomCode.isNotEmpty()) {
                        AdminRoomCard(roomCode, qrBitmap, participantCount)
                    }

                    // ── Active chord callout banner ────────────────────────────
                    if (activeChordDegree.isNotEmpty() && isAdmin) {
                        ActiveChordBanner(
                            degree     = activeChordDegree,
                            key        = displayKey,
                            quality    = displayQuality,
                            isAdmin    = true,
                            onClear    = { sessionViewModel.setActiveChord("") },
                            pulse      = pulse
                        )
                    }

                    if (song != null) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Spacer(Modifier.height(12.dp))

                            // ── Song title + key row ───────────────────────────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(song.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    if (isAdmin) {
                                        // Admin: tappable key row that opens key picker and broadcasts
                                        Surface(
                                            onClick = { showAdminKeyPicker = true },
                                            shape   = RoundedCornerShape(8.dp),
                                            color   = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text("Key:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("$displayKey  $displayQuality",
                                                    fontWeight = FontWeight.Bold,
                                                    color      = MaterialTheme.colorScheme.primary,
                                                    style      = MaterialTheme.typography.labelMedium)
                                                Text("▾",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                        Text("Tap key to change for everyone  •  Tap chord to call out",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        // Member: tappable key selector (local only)
                                        Surface(
                                            onClick = { showKeyPicker = true },
                                            shape   = RoundedCornerShape(8.dp),
                                            color   = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text("My Key:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("$displayKey  $displayQuality",
                                                    fontWeight = FontWeight.Bold,
                                                    color      = MaterialTheme.colorScheme.primary,
                                                    style      = MaterialTheme.typography.labelMedium)
                                                Text("▾",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }

                                // Transpose ▼ ▲ for members
                                if (!isAdmin) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(onClick = {
                                            memberKey = ChordEngine.transposeKey(memberKey, -1)
                                        }) { Text("▼") }
                                        OutlinedButton(onClick = {
                                            memberKey = ChordEngine.transposeKey(memberKey, 1)
                                        }) { Text("▲") }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(16.dp))

                            // ── Song parts ─────────────────────────────────────
                            if (song.parts.isNotEmpty()) {
                                song.parts.forEach { part ->
                                    LivePartSection(
                                        part              = part,
                                        currentKey        = displayKey,
                                        keyQuality        = displayQuality,
                                        activeChordDegree = activeChordDegree,
                                        onChordTap        = if (isAdmin) {
                                            { degree -> sessionViewModel.setActiveChord(degree) }
                                        } else null
                                    )
                                    Spacer(Modifier.height(16.dp))
                                }
                            } else {
                                ChordLyricView(
                                    lyrics            = song.lyrics,
                                    currentKey        = displayKey,
                                    keyQuality        = displayQuality,
                                    modifier          = Modifier.fillMaxWidth(),
                                    textSize          = 20,
                                    activeChordDegree = activeChordDegree,
                                    onChordTap        = if (isAdmin) {
                                        { degree -> sessionViewModel.setActiveChord(degree) }
                                    } else null
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    } else {
                        Box(
                            modifier         = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("Waiting for song…", style = MaterialTheme.typography.bodyLarge) }
                    }
                } // end Column

                // ── Member chord toast ─────────────────────────────────────────
                AnimatedVisibility(
                    visible  = toastVisible && !isAdmin,
                    enter    = slideInVertically { it / 2 } + fadeIn(tween(200)),
                    exit     = fadeOut(tween(500)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                ) {
                    Surface(
                        shape           = RoundedCornerShape(50.dp),
                        color           = ActiveChordColor,
                        shadowElevation = 10.dp
                    ) {
                        Row(
                            modifier              = Modifier.padding(horizontal = 32.dp, vertical = 14.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Canvas(Modifier.size(8.dp)) {
                                drawCircle(Color.White.copy(alpha = pulse * 0.6f + 0.4f))
                            }
                            Text(
                                text       = toastChordName,
                                color      = Color.White,
                                fontSize   = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                } // end Box
            }
        }
    }
}

// ── Shared key picker content (used by both admin and member sheets) ───────────
@Composable
private fun KeyPickerContent(
    title:           String,
    subtitle:        String,
    selectedKey:     String,
    selectedQuality: String,
    onQualityChange: (String) -> Unit,
    onKeySelect:     (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        // Quality chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Major", "Minor").forEach { q ->
                Surface(
                    onClick = { onQualityChange(q) },
                    shape   = RoundedCornerShape(20.dp),
                    color   = if (selectedQuality == q) MaterialTheme.colorScheme.primaryContainer
                              else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(q, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selectedQuality == q) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        // Key grid
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement   = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ChordEngine.allKeys.size) { index ->
                val key      = ChordEngine.allKeys[index]
                val selected = key == selectedKey
                Surface(
                    onClick = { onKeySelect(key) },
                    shape   = RoundedCornerShape(10.dp),
                    color   = if (selected) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = if (selected) 0.dp else 2.dp
                ) {
                    Text(
                        text       = key,
                        modifier   = Modifier
                            .padding(vertical = 12.dp)
                            .fillMaxWidth(),
                        textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = if (selected) MaterialTheme.colorScheme.onPrimary
                                     else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

// ── Active chord callout banner ────────────────────────────────────────────────
@Composable
private fun ActiveChordBanner(
    degree:  String,
    key:     String,
    quality: String,
    isAdmin: Boolean,
    onClear: () -> Unit,
    pulse:   Float
) {
    val resolved = ChordEngine.resolveChord(degree, key, quality)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = ActiveChordColor.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Canvas(Modifier.size(10.dp)) {
                drawCircle(ActiveChordColor.copy(alpha = pulse * 0.6f))
                drawCircle(ActiveChordColor, radius = size.minDimension * 0.35f)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isAdmin) "Calling out" else "Admin's chord",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text       = resolved,
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = ActiveChordColor
                )
            }

            if (isAdmin) {
                TextButton(onClick = onClear) {
                    Text("Clear", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Admin room code + QR card ──────────────────────────────────────────────────
@Composable
private fun AdminRoomCard(
    roomCode:         String,
    qrBitmap:         android.graphics.Bitmap?,
    participantCount: Int
) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (qrBitmap != null) {
                Surface(Modifier.size(100.dp), RoundedCornerShape(10.dp), Color.White) {
                    androidx.compose.foundation.Image(
                        bitmap             = qrBitmap.asImageBitmap(),
                        contentDescription = "QR",
                        modifier           = Modifier.padding(6.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Room Code", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text          = roomCode,
                    fontSize      = 40.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    fontFamily    = FontFamily.Monospace,
                    color         = MaterialTheme.colorScheme.primary,
                    letterSpacing = 6.sp
                )
                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF2E7D32).copy(alpha = 0.12f)) {
                    Text("$participantCount joined",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Member chord map strip ─────────────────────────────────────────────────────
@Composable
private fun ChordMapStrip(
    chordMap:          List<Pair<String, String>>,
    activeChordDegree: String,
    pulse:             Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            "Chord map",
            style  = MaterialTheme.typography.labelSmall,
            color  = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(chordMap) { (degree, resolved) ->
                val isActive = degree == activeChordDegree
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        isActive -> ActiveChordColor.copy(alpha = 0.12f + pulse * 0.10f)
                        else     -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text       = resolved,
                        modifier   = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                        fontSize   = if (isActive) 18.sp else 15.sp,
                        color      = if (isActive) ActiveChordColor
                                     else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Song part card ─────────────────────────────────────────────────────────────
@Composable
private fun LivePartSection(
    part:              SongPart,
    currentKey:        String,
    keyQuality:        String,
    activeChordDegree: String,
    onChordTap:        ((String) -> Unit)?
) {
    val color       = sessionPartColor(part.type)
    val displayName = when (part.type) {
        "Start", "End" -> part.type
        else           -> "${part.type} ${part.number}"
    }
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Canvas(Modifier.size(12.dp)) { drawCircle(color = color) }
                    Text(displayName, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = color)
                }
                if (part.repeatCount > 1) {
                    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.15f)) {
                        Text("×${part.repeatCount}",
                            modifier  = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style     = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold, color = color)
                    }
                }
            }
            HorizontalDivider(color = color.copy(alpha = 0.3f))
            ChordLyricView(
                lyrics            = part.lyrics,
                currentKey        = currentKey,
                keyQuality        = keyQuality,
                modifier          = Modifier.fillMaxWidth().padding(12.dp),
                textSize          = 20,
                activeChordDegree = activeChordDegree,
                onChordTap        = onChordTap
            )
        }
    }
}
