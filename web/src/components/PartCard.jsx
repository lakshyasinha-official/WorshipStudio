import { useState } from 'react'
import { parseLyrics } from '../chordEngine'

const TYPE_COLORS = {
  Start:  '#2E7D32',
  Verse:  '#1565C0',
  Chorus: '#E65100',
  Bridge: '#6A1B9A',
  Other:  '#37474F',
  End:    '#C62828',
}

export default function PartCard({
  part, partIndex, totalParts,
  currentKey, keyQuality, degrees,
  onUpdate, onRemove, onMoveUp, onMoveDown,
  onInsertDegree, onTextareaFocus,
}) {
  const [showPreview, setShowPreview] = useState(false)

  const color       = TYPE_COLORS[part.type] || '#37474F'
  const displayName = (part.type === 'Start' || part.type === 'End')
    ? part.type
    : `${part.type} ${part.number}`

  return (
    <div className="part-card" style={{ '--part-color': color }}>

      {/* ── Header ──────────────────────────────────────────────────────── */}
      <div className="part-header">
        <div className="part-header-left">
          <span className="part-label" style={{ color }}>{displayName}</span>

          <label className="repeat-wrap">
            Repeat
            <input
              type="number" min="1" max="10"
              value={part.repeatCount}
              onChange={e => onUpdate({ repeatCount: Math.max(1, Number(e.target.value)) })}
              className="repeat-input"
            />
            ×
          </label>
        </div>

        <div className="part-header-right">
          <button
            className="icon-btn"
            onClick={() => setShowPreview(p => !p)}
            title={showPreview ? 'Hide preview' : 'Show chord preview'}
          >
            {showPreview ? '⌨️' : '👁'}
          </button>
          <button
            className="icon-btn"
            onClick={onMoveUp}
            disabled={partIndex === 0}
            title="Move up"
          >↑</button>
          <button
            className="icon-btn"
            onClick={onMoveDown}
            disabled={partIndex === totalParts - 1}
            title="Move down"
          >↓</button>
          <button
            className="icon-btn danger"
            onClick={onRemove}
            title="Remove section"
          >✕</button>
        </div>
      </div>

      {/* ── Chord degree insert buttons ──────────────────────────────────── */}
      <div className="degree-buttons">
        <span className="degree-label">Insert chord:</span>
        {degrees.map(d => (
          <button
            key={d}
            className="degree-btn"
            onClick={() => onInsertDegree(d)}
            title={`Insert [${d}]`}
          >
            {d}
          </button>
        ))}
      </div>

      {/* ── Lyrics textarea ──────────────────────────────────────────────── */}
      <textarea
        className="lyrics-textarea"
        value={part.lyrics}
        onChange={e => onUpdate({ lyrics: e.target.value })}
        onFocus={e => onTextareaFocus(e.target)}
        placeholder={`Lyrics for ${displayName}…\nClick a chord button above to insert at cursor, e.g. [I]Amazing [IV]grace`}
        rows={6}
        spellCheck={false}
      />

      {/* ── Live chord preview ───────────────────────────────────────────── */}
      {showPreview && (
        <div className="lyrics-preview">
          <div className="preview-label">Preview · {currentKey} {keyQuality}</div>
          <LyricsPreview lyrics={part.lyrics} currentKey={currentKey} keyQuality={keyQuality} />
        </div>
      )}
    </div>
  )
}

// ── Lyrics renderer (mirrors Android ChordLyricView) ─────────────────────────
function LyricsPreview({ lyrics, currentKey, keyQuality }) {
  return (
    <div className="preview-content">
      {lyrics.split('\n').map((line, li) => {
        const tokens = parseLyrics(line, currentKey, keyQuality)
        const segs   = groupTokens(tokens)

        if (segs.every(s => !s.chord && !s.text.trim())) {
          return <div key={li} className="preview-blank-line" />
        }

        return (
          <div key={li} className="preview-line">
            {segs.map((seg, si) => (
              <span key={si} className="preview-seg">
                {seg.chord
                  ? <span className="preview-chord">{seg.chord}</span>
                  : <span className="preview-chord-placeholder"> </span>
                }
                <span className="preview-text">{seg.text}</span>
              </span>
            ))}
          </div>
        )
      })}
    </div>
  )
}

/** Pair each chord token with the text that follows it (same grouping as Android). */
function groupTokens(tokens) {
  const segs = []
  let i = 0
  while (i < tokens.length) {
    const t = tokens[i]
    if (t.type === 'chord') {
      const text = (i + 1 < tokens.length && tokens[i + 1].type === 'text')
        ? tokens[++i].content
        : ''
      segs.push({ chord: t.resolved, text })
    } else {
      segs.push({ chord: null, text: t.content })
    }
    i++
  }
  return segs
}
