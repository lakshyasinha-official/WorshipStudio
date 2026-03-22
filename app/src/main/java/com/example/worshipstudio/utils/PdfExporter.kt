package com.example.worshipstudio.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.worshipstudio.data.model.Song
import com.example.worshipstudio.engine.ChordEngine
import com.example.worshipstudio.engine.LyricToken
import java.io.File

// A4 at 72 dpi
private const val PAGE_W    = 595
private const val PAGE_H    = 842
private const val MARGIN    = 48f
private const val TEXT_W    = PAGE_W - MARGIN * 2

object PdfExporter {

    fun exportAndShare(context: Context, songs: List<Song>) {
        val file = buildPdf(context, songs)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "WorshipSync Songs")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Songs PDF"))
    }

    // ── Build PDF ──────────────────────────────────────────────────────────────
    private fun buildPdf(context: Context, songs: List<Song>): File {
        val doc = PdfDocument()

        // Paint objects (reused across pages)
        val paintTitle = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 20f
            color    = Color.parseColor("#1C1B1F")
        }
        val paintKey = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textSize = 12f
            color    = Color.parseColor("#6750A4")
        }
        val paintSectionLabel = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 11f
            color    = Color.parseColor("#FF6D00")
        }
        val paintChord = Paint().apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 11f
            color    = Color.parseColor("#6750A4")
        }
        val paintLyric = Paint().apply {
            typeface = Typeface.DEFAULT
            textSize = 14f
            color    = Color.parseColor("#1C1B1F")
        }
        val paintBrand = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textSize = 9f
            color    = Color.parseColor("#79747E")
        }

        var pageNum   = 1
        var pageInfo  = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create()
        var page      = doc.startPage(pageInfo)
        var canvas    = page.canvas
        var y         = MARGIN

        fun newPage() {
            // Footer on current page
            canvas.drawText(
                "WorshipSync  ·  Page $pageNum",
                MARGIN, PAGE_H - 20f, paintBrand
            )
            doc.finishPage(page)
            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create()
            page     = doc.startPage(pageInfo)
            canvas   = page.canvas
            y        = MARGIN
        }

        fun checkY(needed: Float) {
            if (y + needed > PAGE_H - 40f) newPage()
        }

        for ((songIdx, song) in songs.withIndex()) {

            // ── Each song starts on its own fresh page ──────────────────────
            if (songIdx > 0) newPage()

            canvas.drawText(song.name, MARGIN, y, paintTitle)
            y += 6f
            canvas.drawText("${song.rootKey} ${song.keyQuality}", MARGIN, y + 14f, paintKey)
            y += 32f

            // ── Sections ────────────────────────────────────────────────────
            val parts = song.parts.ifEmpty {
                // Legacy song: one implicit "Verse" part
                listOf(com.example.worshipstudio.data.model.SongPart(
                    type   = "Verse",
                    number = 1,
                    lyrics = song.lyrics
                ))
            }

            for (part in parts) {
                checkY(40f)

                // Section label
                val repeatSuffix = if (part.repeatCount > 1) " ×${part.repeatCount}" else ""
                canvas.drawText(
                    "${part.displayName}$repeatSuffix",
                    MARGIN, y, paintSectionLabel
                )
                y += 18f

                // Chord-above-lyrics rendering
                if (part.lyrics.isNotBlank()) {
                    val lines = part.lyrics.split('\n')
                    for (line in lines) {
                        val tokens = ChordEngine.parseLyrics(line, song.rootKey, song.keyQuality)

                        // Group: pair each chord with the text that follows it
                        data class Seg(val chord: String?, val text: String)
                        val segs = mutableListOf<Seg>()
                        var i = 0
                        while (i < tokens.size) {
                            when (val t = tokens[i]) {
                                is LyricToken.Chord -> {
                                    val txt = if (i + 1 < tokens.size && tokens[i + 1] is LyricToken.Text)
                                        (tokens[++i] as LyricToken.Text).content
                                    else ""
                                    segs += Seg(t.resolved, txt)
                                }
                                is LyricToken.Text -> segs += Seg(null, t.content)
                            }
                            i++
                        }

                        val hasChords = segs.any { it.chord != null }
                        val isBlank   = segs.all { it.text.isBlank() && it.chord == null }

                        if (isBlank) {
                            y += 8f
                            continue
                        }

                        checkY(if (hasChords) 32f else 18f)

                        // Measure each segment width and draw chord row + lyric row
                        var x = MARGIN
                        for (seg in segs) {
                            val chordW = if (seg.chord != null) paintChord.measureText(seg.chord + " ") else 0f
                            val lyricW = paintLyric.measureText(seg.text)
                            val segW   = maxOf(chordW, lyricW)

                            if (hasChords) {
                                if (seg.chord != null) {
                                    canvas.drawText(seg.chord, x, y, paintChord)
                                }
                            }

                            // Wrap to next line if overflowing
                            if (x + segW > PAGE_W - MARGIN && x > MARGIN) {
                                x  = MARGIN
                                y += if (hasChords) 32f else 18f
                                checkY(if (hasChords) 32f else 18f)
                                if (seg.chord != null && hasChords) {
                                    canvas.drawText(seg.chord, x, y, paintChord)
                                }
                            }

                            canvas.drawText(seg.text, x, y + if (hasChords) 16f else 0f, paintLyric)
                            x += segW
                        }
                        y += if (hasChords) 32f else 18f
                    }
                }
                y += 10f // spacing between sections
            }
            y += 8f
        }

        // Footer on last page
        canvas.drawText(
            "WorshipSync  ·  Page $pageNum",
            MARGIN, PAGE_H - 20f, paintBrand
        )
        doc.finishPage(page)

        // Write to cache
        val dir  = File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = File(dir, "WorshipSync_Songs.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
