package com.example.worshipstudio.engine

object ChordEngine {

    // ── Major scales (12-note chromatic) ──────────────────────────────────────
    private val majorScales = mapOf(
        "C"  to listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"),
        "C#" to listOf("C#", "D", "D#", "E", "E#", "F#", "G", "G#", "A", "A#", "B", "B#"),
        "D"  to listOf("D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B", "C", "C#"),
        "Eb" to listOf("Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B", "C", "Db", "D"),
        "E"  to listOf("E", "F", "F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#"),
        "F"  to listOf("F", "Gb", "G", "Ab", "A", "Bb", "B", "C", "Db", "D", "Eb", "E"),
        "F#" to listOf("F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "E#"),
        "G"  to listOf("G", "Ab", "A", "Bb", "B", "C", "C#", "D", "Eb", "E", "F", "F#"),
        "Ab" to listOf("Ab", "A", "Bb", "B", "C", "Db", "D", "Eb", "E", "F", "Gb", "G"),
        "A"  to listOf("A", "Bb", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#"),
        "Bb" to listOf("Bb", "B", "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A"),
        "B"  to listOf("B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#")
    )

    // ── Natural minor scales (12-note chromatic) ──────────────────────────────
    private val minorScales = mapOf(
        "C"  to listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"),
        "C#" to listOf("C#", "D", "D#", "E", "E#", "F#", "G", "G#", "A", "A#", "B", "B#"),
        "D"  to listOf("D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B", "C", "C#"),
        "Eb" to listOf("Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B", "C", "Db", "D"),
        "E"  to listOf("E", "F", "F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#"),
        "F"  to listOf("F", "Gb", "G", "Ab", "A", "Bb", "B", "C", "Db", "D", "Eb", "E"),
        "F#" to listOf("F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "E#"),
        "G"  to listOf("G", "Ab", "A", "Bb", "B", "C", "C#", "D", "Eb", "E", "F", "F#"),
        "Ab" to listOf("Ab", "A", "Bb", "B", "C", "Db", "D", "Eb", "E", "F", "Gb", "G"),
        "A"  to listOf("A", "Bb", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#"),
        "Bb" to listOf("Bb", "B", "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A"),
        "B"  to listOf("B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#")
    )

    // ── Harmonic minor scales (12-note chromatic) ─────────────────────────────
    private val harmonicMinorScales = mapOf(
        "C"  to listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"),
        "C#" to listOf("C#", "D", "D#", "E", "E#", "F#", "G", "G#", "A", "A#", "B", "B#"),
        "D"  to listOf("D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B", "C", "C#"),
        "Eb" to listOf("Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B", "C", "Db", "D"),
        "E"  to listOf("E", "F", "F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#"),
        "F"  to listOf("F", "Gb", "G", "Ab", "A", "Bb", "B", "C", "Db", "D", "Eb", "E"),
        "F#" to listOf("F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "E#"),
        "G"  to listOf("G", "Ab", "A", "Bb", "B", "C", "C#", "D", "Eb", "E", "F", "F#"),
        "Ab" to listOf("Ab", "A", "Bb", "B", "C", "Db", "D", "Eb", "E", "F", "Gb", "G"),
        "A"  to listOf("A", "Bb", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#"),
        "Bb" to listOf("Bb", "B", "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A"),
        "B"  to listOf("B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#")
    )

    val allKeys = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")

    val qualities = listOf("Major", "Minor", "Diminished")

    // ── Degree → (scaleIndex, chordSuffix) ───────────────────────────────────
    private val degreeInfo: Map<String, Pair<Int, String>> = mapOf(
        // ── Major mode degrees ──────────────────────────────────
        "I"     to Pair(0, ""),
        "♭II"   to Pair(1, ""),
        "II"    to Pair(2, ""),
        "ii"    to Pair(2, "m"),
        "♭III"  to Pair(3, ""),
        "III"   to Pair(4, ""),
        "iii"   to Pair(4, "m"),
        "IV"    to Pair(5, ""),
        "♯IV"   to Pair(6, ""),
        "♭V"    to Pair(6, ""),
        "V"     to Pair(7, ""),
        "♭VI"   to Pair(8, ""),
        "VI"    to Pair(9, ""),
        "vi"    to Pair(9, "m"),
        "♭VII"  to Pair(10, ""),
        "VII"   to Pair(11, ""),
        "vii°"  to Pair(11, "dim"),

        // ── Minor / Harmonic-minor mode degrees ─────────────────
        "i"      to Pair(0, "m"),
        "I"      to Pair(0, ""),
        "♭ii"    to Pair(1, ""),
        "ii"     to Pair(2, "m"),
        "II"     to Pair(2, ""),
        "ii°"    to Pair(2, "dim"),
        "♭iii"   to Pair(3, ""),
        "III"    to Pair(3, ""),
        "iii"    to Pair(4, "m"),
        "iv"     to Pair(5, "m"),
        "IV"     to Pair(5, ""),
        "♯iv"    to Pair(6, "m"),
        "♭v"     to Pair(6, ""),
        "v"      to Pair(7, "m"),
        "V"      to Pair(7, ""),
        "♭vi"    to Pair(8, ""),
        "VI"     to Pair(8, ""),
        "vi"     to Pair(9, "m"),
        "♭vii"   to Pair(10, ""),
        "VII"    to Pair(10, ""),
        "vii"    to Pair(11, "m"),
        "vii°"   to Pair(11, "dim")
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /** Resolve a Roman-numeral degree to a real chord name (e.g. "vi" + G-major → "Em"). */
    fun resolveChord(degree: String, key: String, quality: String = "Major"): String {
        if (degree.contains("/")) {
            val parts = degree.split("/")
            if (parts.size == 2) {
                val base = resolveChord(parts[0], key, quality)
                val bass = resolveBassNote(parts[1], key, quality)
                if (base != parts[0] || bass != parts[1]) {
                    return "$base/$bass"
                }
            }
        }
        val scale = scaleFor(key, quality) ?: return degree
        val (index, suffix) = degreeInfo[degree] ?: return degree
        val note = scale[index]
        return "$note$suffix"
    }

    private fun resolveBassNote(degree: String, key: String, quality: String): String {
        // Try degree map first
        degreeInfo[degree]?.let { (index, _) ->
            return scaleFor(key, quality)?.get(index) ?: degree
        }
        // Try numeric 1-7
        val num = degree.toIntOrNull()
        if (num != null && num in 1..7) {
            val scale = scaleFor(key, quality) ?: return degree
            val index = when (num) {
                1 -> 0
                2 -> 2
                3 -> 4
                4 -> 5
                5 -> 7
                6 -> 9
                7 -> 11
                else -> 0
            }
            return scale[index]
        }
        return degree
    }

    /** Move the key up or down by semitones. */
    fun transposeKey(currentKey: String, semitones: Int): String {
        val index = allKeys.indexOf(currentKey)
        if (index == -1) return currentKey
        val newIndex = ((index + semitones) % 12 + 12) % 12
        return allKeys[newIndex]
    }

    /** Parse lyrics that contain [degree] markers and return display tokens. */
    fun parseLyrics(lyrics: String, key: String, quality: String = "Major"): List<LyricToken> {
        val tokens = mutableListOf<LyricToken>()
        val regex = Regex("\\[([^\\]]+)\\]")
        var lastEnd = 0
        for (match in regex.findAll(lyrics)) {
            if (match.range.first > lastEnd) {
                tokens.add(LyricToken.Text(lyrics.substring(lastEnd, match.range.first)))
            }
            val degree = match.groupValues[1]
            tokens.add(LyricToken.Chord(degree, resolveChord(degree, key, quality)))
            lastEnd = match.range.last + 1
        }
        if (lastEnd < lyrics.length) {
            tokens.add(LyricToken.Text(lyrics.substring(lastEnd)))
        }
        return tokens
    }

    /** Return the chord degrees appropriate for a given quality (for the Add-Song UI). */
    fun degreesForQuality(quality: String): List<String> = when (quality) {
        "Minor"      -> listOf("i", "♭ii", "ii", "♭iii", "iii", "iv", "♭v", "v", "♭vi", "vi", "♭vii", "vii")
        "Diminished" -> listOf("i", "ii°", "III", "iv", "V",  "VI", "vii°")
        else         -> listOf("I", "♭II", "ii", "♭III", "iii", "IV", "♯IV", "V", "♭VI", "vi", "♭VII", "vii°")
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun scaleFor(key: String, quality: String): List<String>? = when (quality) {
        "Minor"      -> minorScales[key]
        "Diminished" -> harmonicMinorScales[key]
        else         -> majorScales[key]
    }
}

sealed class LyricToken {
    data class Text(val content: String)  : LyricToken()
    data class Chord(val degree: String, val resolved: String) : LyricToken()
}
