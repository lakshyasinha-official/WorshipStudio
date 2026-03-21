package com.example.worshipstudio.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worshipstudio.data.model.SongPart
import com.example.worshipstudio.ui.components.ChordLyricView
import com.example.worshipstudio.viewmodel.SessionViewModel
import com.example.worshipstudio.viewmodel.SongViewModel

private fun partColor(type: String): Color = when (type) {
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
    sessionId: String,
    isAdmin: Boolean,
    sessionViewModel: SessionViewModel,
    songViewModel: SongViewModel,
    onBack: () -> Unit
) {
    val state            by sessionViewModel.state.collectAsState()
    val song             = state.currentSong
    val session          = state.session
    val currentKey       = song?.rootKey    ?: "C"
    val keyQuality       = song?.keyQuality ?: "Major"
    val totalSongs       = state.set?.songs?.size ?: 0
    val participantCount = state.participantCount

    LaunchedEffect(sessionId, isAdmin) {
        if (isAdmin) sessionViewModel.connectAsAdmin(sessionId)
        else         sessionViewModel.joinSession(sessionId, "")
    }

    // Pulsing dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label         = "liveDotAlpha"
    )

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        // Session ID + live dot
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Canvas(modifier = Modifier.size(9.dp)) {
                                drawCircle(color = Color(0xFF4CAF50).copy(alpha = pulse))
                                drawCircle(color = Color(0xFF4CAF50), radius = size.minDimension * 0.38f)
                            }
                            Text(sessionId, fontWeight = FontWeight.Bold)
                        }
                        // Role + device count
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                if (isAdmin) "Admin · controlling" else "Member · read-only",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAdmin) MaterialTheme.colorScheme.primary
                                        else         MaterialTheme.colorScheme.secondary
                            )
                            // Separator dot
                            Canvas(modifier = Modifier.size(3.dp)) {
                                drawCircle(color = Color.Gray)
                            }
                            Text(
                                "$participantCount connected",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isAdmin) sessionViewModel.endSession()
                        else         sessionViewModel.leaveSession()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (isAdmin) {
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { sessionViewModel.previousSong() },
                            enabled = (session?.currentSongIndex ?: 0) > 0
                        ) { Text("◀ Prev") }

                        Text(
                            "${(session?.currentSongIndex ?: 0) + 1} / $totalSongs",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Button(
                            onClick = { sessionViewModel.nextSong() },
                            enabled = (session?.currentSongIndex ?: 0) < totalSongs - 1
                        ) { Text("Next ▶") }
                    }
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            song != null -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))

                    // Song title + key
                    Text(song.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Key: $currentKey  •  $keyQuality",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    // Parts-based rendering (same as SongDetailScreen)
                    if (song.parts.isNotEmpty()) {
                        song.parts.forEach { part ->
                            LivePartSection(
                                part       = part,
                                currentKey = currentKey,
                                keyQuality = keyQuality
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    } else {
                        // Legacy single-block lyrics fallback
                        ChordLyricView(
                            lyrics     = song.lyrics,
                            currentKey = currentKey,
                            keyQuality = keyQuality,
                            modifier   = Modifier.fillMaxWidth(),
                            textSize   = 20
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Waiting for song…", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun LivePartSection(
    part: SongPart,
    currentKey: String,
    keyQuality: String
) {
    val color = partColor(part.type)
    val displayName = when (part.type) {
        "Start", "End" -> part.type
        else           -> "${part.type} ${part.number}"
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) { drawCircle(color = color) }
                    Text(
                        text       = displayName,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = color
                    )
                }
                if (part.repeatCount > 1) {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                        color = color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text      = "×${part.repeatCount}",
                            modifier  = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style     = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color     = color
                        )
                    }
                }
            }
            HorizontalDivider(color = color.copy(alpha = 0.3f))
            ChordLyricView(
                lyrics     = part.lyrics,
                currentKey = currentKey,
                keyQuality = keyQuality,
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                textSize   = 20   // slightly larger for live use
            )
        }
    }
}
