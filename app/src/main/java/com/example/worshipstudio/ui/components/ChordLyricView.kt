package com.example.worshipstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worshipstudio.engine.ChordEngine
import com.example.worshipstudio.engine.LyricToken

/** Colour used for the admin's active-chord callout. */
val ActiveChordColor = Color(0xFFFF6D00)   // deep orange — stands out from primary

private val chordMarkerRegex = Regex("""\[.*?]""")

@Composable
fun ChordLyricView(
    lyrics:             String,
    currentKey:         String,
    keyQuality:         String  = "Major",
    modifier:           Modifier = Modifier,
    textSize:           Int     = 18,
    simplified:         Boolean = false,
    /** Degree currently called out by admin — matching chords are highlighted. */
    activeChordDegree:  String  = "",
    /** Non-null only for admin — called with the tapped degree. */
    onChordTap:         ((String) -> Unit)? = null
) {
    Column(modifier = modifier) {
        lyrics.split("\n").forEach { line ->
            if (simplified) {
                SimplifiedLyricLine(line = line, textSize = textSize)
            } else {
                ChordLyricLine(
                    line              = line,
                    currentKey        = currentKey,
                    keyQuality        = keyQuality,
                    textSize          = textSize,
                    activeChordDegree = activeChordDegree,
                    onChordTap        = onChordTap
                )
            }
            Spacer(modifier = Modifier.height(if (simplified) 5.dp else 8.dp))
        }
    }
}

// ── Simplified (lyrics-only) line ─────────────────────────────────────────────
@Composable
private fun SimplifiedLyricLine(line: String, textSize: Int) {
    val cleanLine = remember(line) { chordMarkerRegex.replace(line, "").trimStart() }
    if (cleanLine.isBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
    } else {
        Text(
            text       = cleanLine,
            fontSize   = textSize.sp,
            lineHeight = (textSize + 6).sp,
            color      = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Full chord + lyric line ───────────────────────────────────────────────────
@Composable
private fun ChordLyricLine(
    line:              String,
    currentKey:        String,
    keyQuality:        String,
    textSize:          Int,
    activeChordDegree: String,
    onChordTap:        ((String) -> Unit)?
) {
    val tokens = ChordEngine.parseLyrics(line, currentKey, keyQuality)

    // Segment carries the DEGREE so we can compare against activeChordDegree
    data class Segment(val chord: String?, val degree: String?, val text: String)

    val segments = mutableListOf<Segment>()
    var i = 0
    while (i < tokens.size) {
        when (val token = tokens[i]) {
            is LyricToken.Chord -> {
                val text = if (i + 1 < tokens.size && tokens[i + 1] is LyricToken.Text) {
                    i++; (tokens[i] as LyricToken.Text).content
                } else ""
                segments.add(Segment(token.resolved, token.degree, text))
            }
            is LyricToken.Text -> segments.add(Segment(null, null, token.content))
        }
        i++
    }

    Row(verticalAlignment = Alignment.Bottom) {
        segments.forEach { (chord, degree, text) ->
            Column(horizontalAlignment = Alignment.Start) {
                if (chord != null && degree != null) {
                    val isActive = degree == activeChordDegree
                    val chordColor = if (isActive) ActiveChordColor
                                     else MaterialTheme.colorScheme.primary

                    // Chord cell — highlighted if active, clickable if admin
                    val chordMod = Modifier
                        .then(
                            if (isActive)
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ActiveChordColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            else Modifier
                        )
                        .then(
                            if (onChordTap != null)
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onChordTap(degree) }
                                    .padding(horizontal = if (isActive) 0.dp else 2.dp)
                            else Modifier
                        )

                    Text(
                        text       = chord,
                        color      = chordColor,
                        fontSize   = if (isActive) (textSize).sp else (textSize - 2).sp,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                        lineHeight = (textSize + 2).sp,
                        modifier   = chordMod
                    )
                } else {
                    // Placeholder to keep lyric rows aligned
                    Text(text = " ", fontSize = (textSize - 2).sp, lineHeight = (textSize + 2).sp)
                }
                Text(text = text, fontSize = textSize.sp, lineHeight = (textSize + 4).sp)
            }
        }
    }
}
