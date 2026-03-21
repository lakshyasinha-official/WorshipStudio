package com.example.worshipstudio.engine

object ChordEngine {

    // ── Major scales ──────────────────────────────────────────────────────────
    private val majorScales = mapOf(
        "C"  to listOf("C",  "D",  "E",  "F",  "G",  "A",  "B"),
        "C#" to listOf("C#", "D#", "E#", "F#", "G#", "A#", "B#"),
        "D"  to listOf("D",  "E",  "F#", "G",  "A",  "B",  "C#"),
        "Eb" to listOf("Eb", "F",  "G",  "Ab", "Bb", "C",  "D"),
        "E"  to listOf("E",  "F#", "G#", "A",  "B",  "C#", "D#"),
        "F"  to listOf("F",  "G",  "A",  "Bb", "C",  "D",  "E"),
        "F#" to listOf("F#", "G#", "A#", "B",  "C#", "D#", "E#"),
        "G"  to listOf("G",  "A",  "B",  "C",  "D",  "E",  "F#"),
        "Ab" to listOf("Ab", "Bb", "C",  "Db", "Eb", "F",  "G"),
        "A"  to listOf("A",  "B",  "C#", "D",  "E",  "F#", "G#"),
        "Bb" to listOf("Bb", "C",  "D",  "Eb", "F",  "G",  "A"),
        "B"  to listOf("B",  "C#", "D#", "E",  "F#", "G#", "A#")
    )

    // ── Natural minor (Aeolian) scales ────────────────────────────────────────
    //   Formula: W H W W H W W  (relative to major: degrees 1 2 b3 4 5 b6 b7)
    private val minorScales = mapOf(
        "C"  to listOf("C",  "D",  "Eb", "F",  "G",  "Ab", "Bb"),
        "C#" to listOf("C#", "D#", "E",  "F#", "G#", "A",  "B"),
        "D"  to listOf("D",  "E",  "F",  "G",  "A",  "Bb", "C"),
        "Eb" to listOf("Eb", "F",  "Gb", "Ab", "Bb", "B",  "Db"),
        "E"  to listOf("E",  "F#", "G",  "A",  "B",  "C",  "D"),
        "F"  to listOf("F",  "G",  "Ab", "Bb", "C",  "Db", "Eb"),
        "F#" to listOf("F#", "G#", "A",  "B",  "C#", "D",  "E"),
        "G"  to listOf("G",  "A",  "Bb", "C",  "D",  "Eb", "F"),
        "Ab" to listOf("Ab", "Bb", "B",  "Db", "Eb", "E",  "Gb"),
        "A"  to listOf("A",  "B",  "C",  "D",  "E",  "F",  "G"),
        "Bb" to listOf("Bb", "C",  "Db", "Eb", "F",  "Gb", "Ab"),
        "B"  to listOf("B",  "C#", "D",  "E",  "F#", "G",  "A")
    )

    // ── Harmonic minor scales (raised 7th — creates the "diminished" tension) ─
    //   Formula: W H W W H A H  (A = augmented second between b6 and 7)
    private val harmonicMinorScales = mapOf(
        "C"  to listOf("C",  "D",  "Eb", "F",  "G",  "Ab", "B"),
        "C#" to listOf("C#", "D#", "E",  "F#", "G#", "A",  "B#"),
        "D"  to listOf("D",  "E",  "F",  "G",  "A",  "Bb", "C#"),
        "Eb" to listOf("Eb", "F",  "Gb", "Ab", "Bb", "B",  "D"),
        "E"  to listOf("E",  "F#", "G",  "A",  "B",  "C",  "D#"),
        "F"  to listOf("F",  "G",  "Ab", "Bb", "C",  "Db", "E"),
        "F#" to listOf("F#", "G#", "A",  "B",  "C#", "D",  "E#"),
        "G"  to listOf("G",  "A",  "Bb", "C",  "D",  "Eb", "F#"),
        "Ab" to listOf("Ab", "Bb", "B",  "Db", "Eb", "E",  "G"),
        "A"  to listOf("A",  "B",  "C",  "D",  "E",  "F",  "G#"),
        "Bb" to listOf("Bb", "C",  "Db", "Eb", "F",  "Gb", "A"),
        "B"  to listOf("B",  "C#", "D",  "E",  "F#", "G",  "A#")
    )

    val allKeys = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")

    val qualities = listOf("Major", "Minor", "Diminished")

    // ── Degree → (scaleIndex, chordSuffix) ───────────────────────────────────
    //
    //  Major key degrees  : I  ii  iii  IV  V  vi  vii°
    //  Minor key degrees  : i  ii°  III  iv  v  VI  VII
    //  Diminished (harm.) : i  ii°  III  iv  V  VI  vii°   ← V is major, vii° is dim
    //
    private val degreeInfo: Map<String, Pair<Int, String>> = mapOf(
        // ── Major mode degrees ──────────────────────────────────
        "I"    to Pair(0, ""),       // Tonic major
        "ii"   to Pair(1, "m"),      // Supertonic minor
        "iii"  to Pair(2, "m"),      // Mediant minor
        "IV"   to Pair(3, ""),       // Subdominant major
        "V"    to Pair(4, ""),       // Dominant major
        "vi"   to Pair(5, "m"),      // Submediant minor
        "vii°" to Pair(6, "dim"),    // Leading-tone diminished
        "vii"  to Pair(6, "dim"),    // Alternate notation

        // ── Minor / Harmonic-minor mode degrees ─────────────────
        "i"    to Pair(0, "m"),      // Tonic minor
        "ii°"  to Pair(1, "dim"),    // Supertonic diminished
        "III"  to Pair(2, ""),       // Mediant major
        "iv"   to Pair(3, "m"),      // Subdominant minor
        "v"    to Pair(4, "m"),      // Dominant minor  (natural minor only)
        "VI"   to Pair(5, ""),       // Submediant major
        "VII"  to Pair(6, "")        // Subtonic major
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /** Resolve a Roman-numeral degree to a real chord name (e.g. "vi" + G-major → "Em"). */
    fun resolveChord(degree: String, key: String, quality: String = "Major"): String {
        val scale = scaleFor(key, quality) ?: return degree
        val (index, suffix) = degreeInfo[degree] ?: return degree
        val note = scale[index]
        return "$note$suffix"
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
        "Minor"      -> listOf("i", "ii°", "III", "iv", "v",  "VI", "VII")
        "Diminished" -> listOf("i", "ii°", "III", "iv", "V",  "VI", "vii°")
        else         -> listOf("I", "ii",  "iii", "IV", "V",  "vi", "vii°")
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
