package com.example.worshipstudio.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worshipstudio.engine.ChordEngine
import com.example.worshipstudio.engine.LyricToken

@Composable
fun ChordLyricView(
    lyrics: String,
    currentKey: String,
    keyQuality: String = "Major",
    modifier: Modifier = Modifier,
    textSize: Int = 18
) {
    Column(modifier = modifier) {
        lyrics.split("\n").forEach { line ->
            ChordLyricLine(
                line       = line,
                currentKey = currentKey,
                keyQuality = keyQuality,
                textSize   = textSize
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChordLyricLine(
    line: String,
    currentKey: String,
    keyQuality: String,
    textSize: Int
) {
    val tokens = ChordEngine.parseLyrics(line, currentKey, keyQuality)

    data class Segment(val chord: String?, val text: String)
    val segments = mutableListOf<Segment>()
    var i = 0
    while (i < tokens.size) {
        when (val token = tokens[i]) {
            is LyricToken.Chord -> {
                val text = if (i + 1 < tokens.size && tokens[i + 1] is LyricToken.Text) {
                    i++; (tokens[i] as LyricToken.Text).content
                } else ""
                segments.add(Segment(token.resolved, text))
            }
            is LyricToken.Text -> segments.add(Segment(null, token.content))
        }
        i++
    }

    Row(verticalAlignment = Alignment.Bottom) {
        segments.forEach { (chord, text) ->
            Column(horizontalAlignment = Alignment.Start) {
                if (chord != null) {
                    Text(
                        text       = chord,
                        color      = MaterialTheme.colorScheme.primary,
                        fontSize   = (textSize - 2).sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = (textSize + 2).sp
                    )
                } else {
                    Text(text = " ", fontSize = (textSize - 2).sp, lineHeight = (textSize + 2).sp)
                }
                Text(text = text, fontSize = textSize.sp, lineHeight = (textSize + 4).sp)
            }
        }
    }
}
