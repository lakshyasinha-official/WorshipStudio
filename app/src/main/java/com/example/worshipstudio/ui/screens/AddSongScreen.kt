package com.example.worshipstudio.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worshipstudio.data.model.Song
import com.example.worshipstudio.data.model.SongPart
import com.example.worshipstudio.engine.ChordEngine
import com.example.worshipstudio.ui.theme.Mint
import com.example.worshipstudio.viewmodel.AuthViewModel
import com.example.worshipstudio.viewmodel.SongViewModel
import com.example.worshipstudio.viewmodel.TagViewModel
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

// ── Colours per section type — tuned for the dark background ─────────────────
private fun partColor(type: String): Color = when (type) {
    "Start"  -> Color(0xFF66BB6A)
    "Verse"  -> Color(0xFF64B5F6)
    "Chorus" -> Color(0xFFFFB74D)
    "Bridge" -> Color(0xFFCE93D8)
    "Other"  -> Color(0xFF4DD0E1)
    "End"    -> Color(0xFFEF5350)
    else     -> Color(0xFF90A4AE)
}

// ── Shared dark text-field colours ─────────────────────────────────────────────
@Composable
private fun mintFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor        = Mint.TextPrimary,
    unfocusedTextColor      = Mint.TextPrimary,
    focusedBorderColor      = Mint.Accent.copy(alpha = 0.7f),
    unfocusedBorderColor    = Mint.BorderField,
    cursorColor             = Mint.Accent,
    focusedContainerColor   = Mint.Field,
    unfocusedContainerColor = Mint.Field
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddSongScreen(
    songId:        String?,
    authViewModel: AuthViewModel,
    songViewModel: SongViewModel,
    tagViewModel:  TagViewModel,
    onSaved:       () -> Unit,
    onBack:        () -> Unit
) {
    val authState   by authViewModel.state.collectAsState()
    val detailState by songViewModel.detailState.collectAsState()
    val tagState    by tagViewModel.state.collectAsState()
    val isAdmin     = authState.role == "admin"

    var name       by remember { mutableStateOf("") }
    var rootKey    by remember { mutableStateOf("C") }
    var keyQuality by remember { mutableStateOf("Major") }

    val parts        = remember { mutableStateListOf<PartEntry>() }
    val selectedTags = remember { mutableStateListOf<String>() }  // selected tag IDs

    // Load tags for church
    LaunchedEffect(authState.churchId) {
        if (authState.churchId.isNotEmpty()) tagViewModel.loadTags(authState.churchId)
    }

    // Load existing song when editing
    LaunchedEffect(songId) { if (songId != null) songViewModel.loadSong(songId) }
    LaunchedEffect(detailState.song) {
        detailState.song?.takeIf { songId != null }?.let { song ->
            name       = song.name
            rootKey    = song.rootKey
            keyQuality = song.keyQuality
            parts.clear()
            selectedTags.clear()
            selectedTags.addAll(song.tags)
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

    fun movePart(id: String, delta: Int) {
        val idx = parts.indexOfFirst { it.id == id }
        if (idx < 0) return
        val targetIdx = idx + delta
        if (targetIdx !in parts.indices) return

        val part = parts.removeAt(idx)
        parts.add(targetIdx, part)

        // Renumber
        val types = parts.map { it.type }.toSet()
        types.forEach { t ->
            var n = 1
            for (i in parts.indices) {
                if (parts[i].type == t) {
                    parts[i] = parts[i].copy(number = n++)
                }
            }
        }
    }

    fun saveSong() {
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
            churchId   = authState.churchId,
            tags       = selectedTags.toList()
        )
        if (songId != null) songViewModel.updateSong(song) { onSaved() }
        else               songViewModel.addSong(song)    { onSaved() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Mint.BgTop, Mint.BgBottom)))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor             = Color.Transparent,
                        navigationIconContentColor = Mint.TextPrimary,
                        titleContentColor          = Mint.TextPrimary
                    ),
                    title = {
                        Text(
                            if (songId != null) "Edit Song" else "Add Song",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            },
            bottomBar = {
                // ── Sticky action bar: Cancel · Save Song ─────────────────────
                Surface(color = Mint.BgTop.copy(alpha = 0.97f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onBack) {
                            Text(
                                "Cancel",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color      = Mint.TextSecondary
                            )
                        }
                        Button(
                            onClick  = { saveSong() },
                            enabled  = name.isNotBlank() && parts.isNotEmpty(),
                            shape    = RoundedCornerShape(50),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor         = Mint.Accent,
                                contentColor           = Mint.OnAccent,
                                disabledContainerColor = Mint.Accent.copy(alpha = 0.25f),
                                disabledContentColor   = Mint.OnAccent.copy(alpha = 0.5f)
                            ),
                            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 14.dp)
                        ) {
                            Text(
                                if (songId != null) "Update Song" else "Save Song",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(4.dp))

                // ── Song name ────────────────────────────────────────────────────
                SectionLabel(null, "Song Name")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Amazing Grace", color = Mint.TextSecondary.copy(alpha = 0.7f)) },
                    singleLine = true,
                    shape    = RoundedCornerShape(16.dp),
                    colors   = mintFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))

                // ── Key Selection card ────────────────────────────────────────────
                SectionCard {
                    SectionLabel(Icons.Default.Piano, "Key Selection")
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChordEngine.allKeys.forEach { key ->
                            val selected = rootKey == key
                            Surface(
                                onClick = { rootKey = key },
                                shape   = RoundedCornerShape(10.dp),
                                color   = if (selected) Mint.Accent else Mint.Field,
                                border  = BorderStroke(
                                    1.dp,
                                    if (selected) Mint.Accent else Mint.BorderSubtle
                                )
                            ) {
                                Text(
                                    text       = key,
                                    modifier   = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                    fontSize   = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color      = if (selected) Mint.OnAccent else Mint.TextSecondary
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                // ── Key Quality card — segmented control ─────────────────────────
                SectionCard {
                    SectionLabel(Icons.Default.Speed, "Key Quality")
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Mint.Field, RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ChordEngine.qualities.forEach { q ->
                            val selected = keyQuality == q
                            val label = when (q) {
                                "Diminished" -> "Dim / Harm."
                                else         -> q
                            }
                            Surface(
                                onClick  = { keyQuality = q },
                                shape    = RoundedCornerShape(11.dp),
                                color    = if (selected) Mint.Accent else Color.Transparent,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text       = label,
                                    modifier   = Modifier.padding(vertical = 10.dp),
                                    fontSize   = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color      = if (selected) Mint.OnAccent else Mint.TextSecondary,
                                    textAlign  = TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    val hint = when (keyQuality) {
                        "Minor"      -> "Natural minor — i ii° III iv v VI VII"
                        "Diminished" -> "Harmonic minor — i ii° III iv V VI vii°"
                        else         -> "Major (Ionian) — I ii iii IV V vi vii°"
                    }
                    Text(hint, fontSize = 12.sp, color = Mint.TextSecondary)
                }
                Spacer(Modifier.height(14.dp))

                // ── Dynamic scale reference ──────────────────────────────────────
                ScaleHelperRow(rootKey = rootKey, keyQuality = keyQuality)

                // ── Tags (admin only) ─────────────────────────────────────────────
                if (isAdmin && tagState.tags.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    SectionLabel(Icons.Outlined.LocalOffer, "Tags")
                    Spacer(Modifier.height(10.dp))
                    SectionCard {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement   = Arrangement.spacedBy(8.dp)
                        ) {
                            tagState.tags.forEach { tag ->
                                val selected = tag.id in selectedTags
                                Surface(
                                    onClick = {
                                        if (selected) selectedTags.remove(tag.id)
                                        else          selectedTags.add(tag.id)
                                    },
                                    shape  = RoundedCornerShape(50),
                                    color  = if (selected) Mint.IndigoBg else Mint.Field,
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) Mint.Indigo.copy(alpha = 0.5f) else Mint.BorderSubtle
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text       = if (selected) tag.name else "+ ${tag.name}",
                                            fontSize   = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color      = if (selected) Mint.Indigo else Mint.TextSecondary
                                        )
                                        if (selected) {
                                            Text("✕", fontSize = 12.sp, color = Mint.Indigo)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Add section buttons ──────────────────────────────────────────
                SectionLabel(Icons.Outlined.LibraryMusic, "Add Section")
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SECTION_TYPES.forEach { type ->
                        val disabled = type in SINGLETONS && countOf(type) >= 1
                        val color    = partColor(type)
                        Surface(
                            onClick = { if (!disabled) addPart(type) },
                            shape   = RoundedCornerShape(50),
                            color   = color.copy(alpha = if (disabled) 0.08f else 0.18f),
                            border  = BorderStroke(
                                1.dp,
                                color.copy(alpha = if (disabled) 0.15f else 0.45f)
                            )
                        ) {
                            Text(
                                text       = "+ $type",
                                modifier   = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = color.copy(alpha = if (disabled) 0.4f else 1f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Part cards ───────────────────────────────────────────────────
                if (parts.isEmpty()) {
                    Text(
                        "Tap a section button above to start building your song.",
                        fontSize = 13.sp,
                        color    = Mint.TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                parts.forEachIndexed { index, part ->
                    SongPartCard(
                        part           = part,
                        chordDegrees   = chordDegrees,
                        isFirst        = index == 0,
                        isLast         = index == parts.size - 1,
                        onLyricsChange = { updateLyrics(part.id, it) },
                        onRepeatChange = { updateRepeat(part.id, it) },
                        onDelete       = { removePart(part.id) },
                        onMoveUp       = { movePart(part.id, -1) },
                        onMoveDown     = { movePart(part.id, 1) }
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── Mint section label with optional icon ─────────────────────────────────────
@Composable
private fun SectionLabel(icon: ImageVector?, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Mint.Accent, modifier = Modifier.size(18.dp))
        }
        Text(
            text       = label,
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Mint.Accent
        )
    }
}

// ── Rounded dark card container ────────────────────────────────────────────────
@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = Mint.Card,
        border = BorderStroke(1.dp, Mint.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) { content() }
    }
}

// ── Scale helper ──────────────────────────────────────────────────────────────
@Composable
private fun ScaleHelperRow(rootKey: String, keyQuality: String) {
    val degrees = ChordEngine.degreesForQuality(keyQuality)
    SectionCard {
        Text(
            "Scale Reference  —  $rootKey $keyQuality",
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold,
            color      = Mint.Accent
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            degrees.forEachIndexed { index, degree ->
                val chord = ChordEngine.resolveChord(degree, rootKey, keyQuality)
                Surface(
                    shape  = RoundedCornerShape(10.dp),
                    color  = Mint.Field,
                    border = BorderStroke(1.dp, Mint.BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${index + 1}", fontSize = 10.sp, color = Mint.TextSecondary)
                        Text(degree, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = Mint.Accent, textAlign = TextAlign.Center)
                        Text(chord, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                            color = Mint.TextPrimary, textAlign = TextAlign.Center)
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
    isFirst: Boolean,
    isLast: Boolean,
    onLyricsChange: (TextFieldValue) -> Unit,
    onRepeatChange: (Int) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val color = partColor(part.type)

    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = Mint.Card,
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
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
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = color
                    )
                }

                // Right: repeat counter  ×N  with − and +, then move / delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Decrease
                    Surface(
                        onClick = { onRepeatChange(-1) },
                        shape   = RoundedCornerShape(6.dp),
                        color   = if (part.repeatCount > 1) color.copy(alpha = 0.15f) else Mint.Field
                    ) {
                        Text(
                            text     = "−",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style    = MaterialTheme.typography.labelLarge,
                            color    = if (part.repeatCount > 1) color else Mint.TextSecondary
                        )
                    }

                    // ×N badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (part.repeatCount > 1) color.copy(alpha = 0.18f) else Mint.Field
                    ) {
                        Text(
                            text       = "×${part.repeatCount}",
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 13.sp,
                            color      = if (part.repeatCount > 1) color else Mint.TextSecondary
                        )
                    }

                    // Increase
                    Surface(
                        onClick = { onRepeatChange(+1) },
                        shape   = RoundedCornerShape(6.dp),
                        color   = color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text     = "+",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style    = MaterialTheme.typography.labelLarge,
                            color    = color
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    // Move Up
                    IconButton(
                        onClick  = onMoveUp,
                        modifier = Modifier.size(30.dp),
                        enabled  = !isFirst
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp, "Move Up",
                            tint = if (isFirst) Mint.TextSecondary.copy(alpha = 0.3f) else Mint.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Move Down
                    IconButton(
                        onClick  = onMoveDown,
                        modifier = Modifier.size(30.dp),
                        enabled  = !isLast
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown, "Move Down",
                            tint = if (isLast) Mint.TextSecondary.copy(alpha = 0.3f) else Mint.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(2.dp))

                    // Delete
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Outlined.Delete, "Remove",
                            tint     = Mint.TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(color.copy(alpha = 0.3f))
            )

            Column(modifier = Modifier.padding(12.dp)) {
                // Chord degree insert buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    chordDegrees.forEach { degree ->
                        Surface(
                            onClick = {
                                val cursor = part.lyrics.selection.start
                                val text   = part.lyrics.text
                                val insert = "[$degree]"
                                onLyricsChange(TextFieldValue(
                                    text      = text.substring(0, cursor) + insert + text.substring(cursor),
                                    selection = TextRange(cursor + insert.length)
                                ))
                            },
                            shape  = RoundedCornerShape(8.dp),
                            color  = color.copy(alpha = 0.13f),
                            border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text       = degree,
                                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = color
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Auto-growing lyrics field — no fixed height, so it never scrolls
                // internally and the page scroll stays smooth under the finger.
                OutlinedTextField(
                    value         = part.lyrics,
                    onValueChange = onLyricsChange,
                    placeholder   = {
                        Text(
                            "Type lyrics for ${part.displayName}…",
                            color = Mint.TextSecondary.copy(alpha = 0.7f)
                        )
                    },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = mintFieldColors()
                )
            }
        }
    }
}
