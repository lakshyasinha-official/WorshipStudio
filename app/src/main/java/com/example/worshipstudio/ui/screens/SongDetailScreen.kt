package com.example.worshipstudio.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worshipstudio.data.model.SongPart
import com.example.worshipstudio.engine.ChordEngine
import com.example.worshipstudio.ui.components.ChordLyricView
import com.example.worshipstudio.viewmodel.SongViewModel
import kotlinx.coroutines.launch

private fun partColor(type: String): Color = when (type) {
    "Start"  -> Color(0xFF2E7D32)
    "Verse"  -> Color(0xFF1565C0)
    "Chorus" -> Color(0xFFE65100)
    "Bridge" -> Color(0xFF6A1B9A)
    "Other"  -> Color(0xFF00838F)
    "End"    -> Color(0xFFC62828)
    else     -> Color(0xFF37474F)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreen(
    songId: String,
    songViewModel: SongViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val state       by songViewModel.detailState.collectAsState()
    val sheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope       = rememberCoroutineScope()
    var showKeyPicker by remember { mutableStateOf(false) }

    LaunchedEffect(songId) { songViewModel.loadSong(songId) }

    // ── Key picker bottom sheet ───────────────────────────────────────────────
    if (showKeyPicker) {
        ModalBottomSheet(
            onDismissRequest = { showKeyPicker = false },
            sheetState       = sheetState
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Select Key",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Current: ${state.currentKey}  •  ${state.currentQuality}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                // 4-column grid of all 12 keys
                LazyVerticalGrid(
                    columns             = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement   = Arrangement.spacedBy(10.dp),
                    modifier            = Modifier.fillMaxWidth()
                ) {
                    items(ChordEngine.allKeys) { key ->
                        val isSelected = key == state.currentKey
                        Surface(
                            onClick = {
                                songViewModel.setKey(key)
                                scope.launch { sheetState.hide() }
                                    .invokeOnCompletion { showKeyPicker = false }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = if (isSelected) 0.dp else 2.dp
                        ) {
                            Column(
                                modifier            = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text       = key,
                                    style      = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color      = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                // Show resolved tonic chord for context
                                val tonicDegree = if (state.currentQuality == "Major") "I" else "i"
                                val tonic = ChordEngine.resolveChord(tonicDegree, key, state.currentQuality)
                                Text(
                                    text  = tonic,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(state.song?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.song != null -> {
                val song = state.song!!
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))

                    // ── Key row ──────────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            // Tappable key selector
                            Row(
                                modifier = Modifier
                                    .clickable { showKeyPicker = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Key:",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                // Key value in primary colour — looks tappable
                                Text(
                                    text       = state.currentKey,
                                    style      = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.primary,
                                    fontSize   = 20.sp
                                )
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Change key",
                                    tint     = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )

                                // Quality badge
                                val chipLabel = when (state.currentQuality) {
                                    "Minor"      -> "Minor"
                                    "Diminished" -> "Dim"
                                    else         -> "Major"
                                }
                                val chipColor = when (state.currentQuality) {
                                    "Minor"      -> MaterialTheme.colorScheme.tertiaryContainer
                                    "Diminished" -> MaterialTheme.colorScheme.errorContainer
                                    else         -> MaterialTheme.colorScheme.secondaryContainer
                                }
                                SuggestionChip(
                                    onClick = { showKeyPicker = true },
                                    label   = { Text(chipLabel, style = MaterialTheme.typography.labelSmall) },
                                    colors  = SuggestionChipDefaults.suggestionChipColors(containerColor = chipColor)
                                )
                            }
                            Text(
                                "${song.parts.size} sections  •  tap key to change",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Fine-tune transpose
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { songViewModel.transposeDown() }) { Text("▼") }
                            OutlinedButton(onClick = { songViewModel.transposeUp() })   { Text("▲") }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    // ── Parts ────────────────────────────────────────────────
                    if (song.parts.isNotEmpty()) {
                        song.parts.forEach { part ->
                            SongPartSection(
                                part       = part,
                                currentKey = state.currentKey,
                                keyQuality = state.currentQuality
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    } else {
                        ChordLyricView(
                            lyrics     = song.lyrics,
                            currentKey = state.currentKey,
                            keyQuality = state.currentQuality,
                            modifier   = Modifier.fillMaxWidth(),
                            textSize   = 18
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

// ── Section card ──────────────────────────────────────────────────────────────
@Composable
private fun SongPartSection(part: SongPart, currentKey: String, keyQuality: String) {
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
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Canvas(modifier = Modifier.size(12.dp)) { drawCircle(color = color) }
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
                lyrics     = part.lyrics,
                currentKey = currentKey,
                keyQuality = keyQuality,
                modifier   = Modifier.fillMaxWidth().padding(12.dp),
                textSize   = 18
            )
        }
    }
}
