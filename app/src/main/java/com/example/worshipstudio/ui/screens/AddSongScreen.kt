package com.example.worshipstudio.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worshipstudio.data.model.Song
import com.example.worshipstudio.data.model.SongPart
import com.example.worshipstudio.engine.ChordEngine
import com.example.worshipstudio.viewmodel.AuthViewModel
import com.example.worshipstudio.viewmodel.SongViewModel
import java.util.UUID

// ── Internal state holder for each part card ──────────────────────────────────
private data class PartEntry(
    val id: String          = UUID.randomUUID().toString(),
    val type: String,
    val number: Int,
    val lyrics: TextFieldValue = TextFieldValue(""),
    val repeatCount: Int    = 1
) {
    val displayName: String get() = when (type) {
        "Start", "End" -> type
        else           -> "$type $number"
    }
}

// ── Section types ─────────────────────────────────────────────────────────────
private val SECTION_TYPES = listOf("Start", "Verse", "Chorus", "Bridge", "Other", "End")
private val SINGLETONS    = setOf("Start", "End")

// ── Colours per section type ──────────────────────────────────────────────────
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
fun AddSongScreen(
    songId: String?,
    authViewModel: AuthViewModel,
    songViewModel: SongViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val authState   by authViewModel.state.collectAsState()
    val detailState by songViewModel.detailState.collectAsState()

    var name       by remember { mutableStateOf("") }
    var rootKey    by remember { mutableStateOf("C") }
    var keyQuality by remember { mutableStateOf("Major") }

    val parts = remember { mutableStateListOf<PartEntry>() }

    // Load existing song when editing
    LaunchedEffect(songId) { if (songId != null) songViewModel.loadSong(songId) }
    LaunchedEffect(detailState.song) {
        detailState.song?.takeIf { songId != null }?.let { song ->
            name       = song.name
            rootKey    = song.rootKey
            keyQuality = song.keyQuality
            parts.clear()
            if (song.parts.isNotEmpty()) {
                song.parts.forEach { p ->
                    parts.add(PartEntry(
                        type        = p.type,
                        number      = p.number,
                        lyrics      = TextFieldValue(p.lyrics),
                        repeatCount = p.repeatCount
                    ))
                }
            } else if (song.lyrics.isNotBlank()) {
                parts.add(PartEntry(type = "Verse", number = 1,
                    lyrics = TextFieldValue(song.lyrics)))
            }
        }
    }

    val chordDegrees = ChordEngine.degreesForQuality(keyQuality)

    fun countOf(type: String) = parts.count { it.type == type }

    fun addPart(type: String) {
        parts.add(PartEntry(type = type, number = countOf(type) + 1))
    }

    fun removePart(id: String) {
        val idx = parts.indexOfFirst { it.id == id }
        if (idx < 0) return
        parts.removeAt(idx)
        val types = parts.map { it.type }.toSet()
        types.forEach { t ->
            var n = 1
            for (i in parts.indices) {
                if (parts[i].type == t) parts[i] = parts[i].copy(number = n++)
            }
        }
    }

    fun updateLyrics(id: String, value: TextFieldValue) {
        val idx = parts.indexOfFirst { it.id == id }
        if (idx >= 0) parts[idx] = parts[idx].copy(lyrics = value)
    }

    fun updateRepeat(id: String, delta: Int) {
        val idx = parts.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val newCount = (parts[idx].repeatCount + delta).coerceIn(1, 9)
            parts[idx] = parts[idx].copy(repeatCount = newCount)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (songId != null) "Edit Song" else "Add Song") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))

            // ── Song name ────────────────────────────────────────────────────
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Song Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // ── Root key — compact horizontal chip scroll ─────────────────────
            Text("Root Key", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChordEngine.allKeys.forEach { key ->
                    val selected = rootKey == key
                    Surface(
                        onClick      = { rootKey = key },
                        shape        = RoundedCornerShape(8.dp),
                        color        = if (selected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = if (selected) 0.dp else 1.dp
                    ) {
                        Text(
                            text      = key,
                            modifier  = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            style     = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color     = if (selected)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // ── Key Quality ──────────────────────────────────────────────────
            Text("Key Quality", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChordEngine.qualities.forEach { q ->
                    val label = when (q) {
                        "Major"      -> "♩ Major"
                        "Minor"      -> "♩ Minor"
                        "Diminished" -> "♩ Dim / Harm."
                        else         -> q
                    }
                    FilterChip(
                        selected = keyQuality == q,
                        onClick  = { keyQuality = q },
                        label    = { Text(label) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
            val hint = when (keyQuality) {
                "Minor"      -> "Natural minor — i ii° III iv v VI VII"
                "Diminished" -> "Harmonic minor — i ii° III iv V VI vii°"
                else         -> "Major (Ionian) — I ii iii IV V vi vii°"
            }
            Text(hint, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(10.dp))

            // ── Dynamic scale reference ──────────────────────────────────────
            ScaleHelperRow(rootKey = rootKey, keyQuality = keyQuality)

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // ── Add section buttons ──────────────────────────────────────────
            Text("Add Section", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SECTION_TYPES.forEach { type ->
                    val disabled = type in SINGLETONS && countOf(type) >= 1
                    Button(
                        onClick  = { addPart(type) },
                        enabled  = !disabled,
                        shape    = RoundedCornerShape(8.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = partColor(type),
                            disabledContainerColor = partColor(type).copy(alpha = 0.3f)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text  = "+ $type",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Part cards ───────────────────────────────────────────────────
            if (parts.isEmpty()) {
                Text(
                    "Tap a section button above to start building your song.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            parts.forEach { part ->
                SongPartCard(
                    part           = part,
                    chordDegrees   = chordDegrees,
                    onLyricsChange = { updateLyrics(part.id, it) },
                    onRepeatChange = { updateRepeat(part.id, it) },
                    onDelete       = { removePart(part.id) }
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))

            // ── Save ─────────────────────────────────────────────────────────
            Button(
                onClick = {
                    val songParts = parts.map { p ->
                        SongPart(
                            type        = p.type,
                            number      = p.number,
                            lyrics      = p.lyrics.text,
                            repeatCount = p.repeatCount
                        )
                    }
                    val song = Song(
                        id         = songId ?: "",
                        name       = name,
                        parts      = songParts,
                        lyrics     = "",
                        rootKey    = rootKey,
                        keyQuality = keyQuality,
                        createdBy  = authState.userId,
                        churchId   = authState.churchId
                    )
                    if (songId != null) songViewModel.updateSong(song) { onSaved() }
                    else               songViewModel.addSong(song)    { onSaved() }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled  = name.isNotBlank() && parts.isNotEmpty()
            ) {
                Text(if (songId != null) "Update Song" else "Save Song")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Scale helper ──────────────────────────────────────────────────────────────
@Composable
private fun ScaleHelperRow(rootKey: String, keyQuality: String) {
    val degrees = ChordEngine.degreesForQuality(keyQuality)
    val headerColor = when (keyQuality) {
        "Minor"      -> Color(0xFF1565C0)
        "Diminished" -> Color(0xFF6A1B9A)
        else         -> Color(0xFF2E7D32)
    }
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(containerColor = headerColor.copy(alpha = 0.07f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                "Scale Reference  —  $rootKey $keyQuality",
                style = MaterialTheme.typography.labelSmall,
                color = headerColor, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                degrees.forEachIndexed { index, degree ->
                    val chord = ChordEngine.resolveChord(degree, rootKey, keyQuality)
                    Surface(shape = RoundedCornerShape(8.dp), color = headerColor.copy(alpha = 0.12f)) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("${index + 1}", style = MaterialTheme.typography.labelSmall,
                                color = headerColor.copy(alpha = 0.7f))
                            Text(degree, style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold, color = headerColor,
                                textAlign = TextAlign.Center)
                            Text(chord, style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

// ── Section card ──────────────────────────────────────────────────────────────
@Composable
private fun SongPartCard(
    part: PartEntry,
    chordDegrees: List<String>,
    onLyricsChange: (TextFieldValue) -> Unit,
    onRepeatChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    val color = partColor(part.type)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // ── Card header ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: dot + section name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) { drawCircle(color = color) }
                    Text(
                        text       = part.displayName,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = color
                    )
                }

                // Right: repeat counter  ×N  with − and +, then delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Decrease
                    Surface(
                        onClick = { onRepeatChange(-1) },
                        shape   = RoundedCornerShape(6.dp),
                        color   = if (part.repeatCount > 1)
                            color.copy(alpha = 0.12f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text     = "−",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style    = MaterialTheme.typography.labelLarge,
                            color    = if (part.repeatCount > 1) color
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // ×N badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (part.repeatCount > 1)
                            color.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text      = "×${part.repeatCount}",
                            modifier  = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style     = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize  = 13.sp,
                            color     = if (part.repeatCount > 1) color
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Increase
                    Surface(
                        onClick = { onRepeatChange(+1) },
                        shape   = RoundedCornerShape(6.dp),
                        color   = color.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text     = "+",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style    = MaterialTheme.typography.labelLarge,
                            color    = color
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    // Delete
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.Delete, "Remove",
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = color.copy(alpha = 0.3f))

            Column(modifier = Modifier.padding(12.dp)) {
                // Chord degree insert buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    chordDegrees.forEach { degree ->
                        OutlinedButton(
                            onClick = {
                                val cursor = part.lyrics.selection.start
                                val text   = part.lyrics.text
                                val insert = "[$degree]"
                                onLyricsChange(TextFieldValue(
                                    text      = text.substring(0, cursor) + insert + text.substring(cursor),
                                    selection = TextRange(cursor + insert.length)
                                ))
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(degree, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value         = part.lyrics,
                    onValueChange = onLyricsChange,
                    placeholder   = { Text("Type lyrics for ${part.displayName}…") },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    maxLines = 20,
                    shape    = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}
