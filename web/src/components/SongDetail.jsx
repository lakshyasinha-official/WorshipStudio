import { useState, useEffect } from 'react'
import { doc, getDoc } from 'firebase/firestore'
import { db } from '../firebase'
import { parseLyrics, resolveChord, degreesForQuality } from '../chordEngine'

const TYPE_COLORS = {
  Start:  '#2E7D32',
  Verse:  '#1565C0',
  Chorus: '#E65100',
  Bridge: '#6A1B9A',
  Other:  '#37474F',
  End:    '#C62828',
}

export default function SongDetail({ songId, onEdit }) {
  const [song,    setSong]    = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!songId) return
    setLoading(true)
    setSong(null)
    getDoc(doc(db, 'songs', songId))
      .then(snap => { if (snap.exists()) setSong({ id: snap.id, ...snap.data() }) })
      .finally(() => setLoading(false))
  }, [songId])

  if (loading) {
    return (
      <div className="editor-loading">
        <div className="spinner" />
        <span>Loading song…</span>
      </div>
    )
  }

  if (!song) return null

  const { name, rootKey = 'G', keyQuality = 'Major', parts = [] } = song
  const degrees = degreesForQuality(keyQuality)

  return (
    <div className="song-detail">

      {/* ── Header ── */}
      <div className="detail-header">
        <div className="detail-title-row">
          <h1 className="detail-title">{name}</h1>
          <span className="detail-key-badge">{rootKey} {keyQuality}</span>
        </div>
        <button className="btn-primary btn-sm" onClick={onEdit}>✏ Edit Song</button>
      </div>

      {/* ── Scale reference ── */}
      <div className="scale-reference">
        <span className="scale-reference-title">Scale — {rootKey} {keyQuality}</span>
        <div className="scale-pills">
          {degrees.map(d => (
            <span key={d} className="scale-pill">
              <span className="scale-degree">{d}</span>
              <span className="scale-chord">{resolveChord(d, rootKey, keyQuality)}</span>
            </span>
          ))}
        </div>
      </div>

      {/* ── Song parts ── */}
      <div className="detail-parts">
        {parts.length === 0 && (
          <div className="no-parts-hint">No sections — click Edit Song to add content.</div>
        )}
        {parts.map((part, i) => {
          const color = TYPE_COLORS[part.type] || '#37474F'
          const label = (part.type === 'Start' || part.type === 'End')
            ? part.type
            : `${part.type} ${part.number}`
          return (
            <div key={i} className="detail-part" style={{ '--part-color': color }}>
              <div className="detail-part-header">
                <span className="detail-part-label" style={{ color }}>{label}</span>
                {part.repeatCount > 1 && (
                  <span className="detail-repeat">× {part.repeatCount}</span>
                )}
              </div>
              <div className="detail-lyrics">
                <LyricsView lyrics={part.lyrics || ''} currentKey={rootKey} keyQuality={keyQuality} />
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

// ── Chord-above-lyrics renderer (same logic as PartCard preview) ──────────────
function LyricsView({ lyrics, currentKey, keyQuality }) {
  return (
    <div className="preview-content detail-preview-content">
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
                  : <span className="preview-chord-placeholder" />
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
