import { useState, useEffect, useRef } from 'react'
import { doc, getDoc, setDoc, updateDoc, deleteDoc } from 'firebase/firestore'
import { db } from '../firebase'
import { ALL_KEYS, QUALITIES, PART_TYPES, degreesForQuality, resolveChord } from '../chordEngine'
import PartCard from './PartCard'

// ── Helpers ───────────────────────────────────────────────────────────────────

function nanoid() {
  return Math.random().toString(36).slice(2, 10) + Date.now().toString(36)
}

/**
 * Recalculate .number on every part so that Verse 1/2/3… etc. stay consistent
 * after any add, remove, or reorder operation.
 */
function computeNumbers(parts) {
  const counters = {}
  return parts.map(part => {
    if (part.type === 'Start' || part.type === 'End') return { ...part, number: 1 }
    counters[part.type] = (counters[part.type] || 0) + 1
    return { ...part, number: counters[part.type] }
  })
}

const BLANK_DRAFT = {
  name:       '',
  rootKey:    'G',
  keyQuality: 'Major',
  parts:      [],
  tags:       [],
  lyrics:     '',
}

// ── Component ─────────────────────────────────────────────────────────────────

export default function SongEditor({ songId, churchId, userId, isAdmin, onSaved, onDeleted }) {
  const [draft,             setDraft]             = useState(BLANK_DRAFT)
  const [loading,           setLoading]           = useState(!!songId)
  const [saving,            setSaving]            = useState(false)
  const [error,             setError]             = useState('')
  const [dirty,             setDirty]             = useState(false)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)

  // Track the textarea that was last focused — lets us insert degrees at cursor
  const activeTextareaRef  = useRef(null)
  const activePartIndexRef = useRef(null)

  // ── Load existing song ───────────────────────────────────────────────────
  useEffect(() => {
    if (!songId) { setLoading(false); return }

    setLoading(true)
    getDoc(doc(db, 'songs', songId))
      .then(snap => {
        if (snap.exists()) {
          const d = snap.data()
          setDraft({
            name:       d.name       || '',
            rootKey:    d.rootKey    || 'G',
            keyQuality: d.keyQuality || 'Major',
            parts:      d.parts      || [],
            tags:       d.tags       || [],
            lyrics:     d.lyrics     || '',
          })
        }
      })
      .finally(() => { setLoading(false); setDirty(false) })
  }, [songId])

  // ── Draft mutation helpers ───────────────────────────────────────────────
  function update(changes) {
    setDraft(d => ({ ...d, ...changes }))
    setDirty(true)
  }

  function addPart(type) {
    update({ parts: computeNumbers([...draft.parts, { type, number: 1, lyrics: '', repeatCount: 1 }]) })
  }

  function updatePart(index, changes) {
    const next = [...draft.parts]
    next[index] = { ...next[index], ...changes }
    update({ parts: computeNumbers(next) })
  }

  function removePart(index) {
    update({ parts: computeNumbers(draft.parts.filter((_, i) => i !== index)) })
  }

  function movePart(index, dir) {
    const next = [...draft.parts]
    const target = index + dir
    if (target < 0 || target >= next.length) return
    ;[next[index], next[target]] = [next[target], next[index]]
    update({ parts: computeNumbers(next) })
  }

  // ── Chord degree insertion ───────────────────────────────────────────────
  function insertDegree(degree, partIndex) {
    const marker   = `[${degree}]`
    const textarea = activeTextareaRef.current

    if (!textarea || activePartIndexRef.current !== partIndex) {
      // No active cursor info — append to end of that part's lyrics
      updatePart(partIndex, { lyrics: draft.parts[partIndex].lyrics + marker })
      return
    }

    const start    = textarea.selectionStart
    const end      = textarea.selectionEnd
    const lyrics   = draft.parts[partIndex].lyrics
    const newLyrics = lyrics.substring(0, start) + marker + lyrics.substring(end)

    updatePart(partIndex, { lyrics: newLyrics })

    // Restore cursor just after the inserted marker
    const newPos = start + marker.length
    requestAnimationFrame(() => {
      if (activeTextareaRef.current) {
        activeTextareaRef.current.selectionStart = newPos
        activeTextareaRef.current.selectionEnd   = newPos
        activeTextareaRef.current.focus()
      }
    })
  }

  // ── Save ──────────────────────────────────────────────────────────────────
  async function handleSave() {
    setError('')
    if (!draft.name.trim()) { setError('Song name is required.'); return }

    setSaving(true)
    const id = songId || nanoid()

    const payload = {
      id,
      name:          draft.name.trim(),
      nameLowercase: draft.name.trim().toLowerCase(),
      rootKey:       draft.rootKey,
      keyQuality:    draft.keyQuality,
      parts:         draft.parts,
      tags:          draft.tags,
      lyrics:        draft.lyrics,   // legacy field — keep for compat
      churchId,
      createdBy:     userId,
    }

    try {
      if (songId) {
        await updateDoc(doc(db, 'songs', songId), payload)
      } else {
        await setDoc(doc(db, 'songs', id), { ...payload, createdAt: Date.now() })
      }
      setDirty(false)
      onSaved(id)
    } catch (e) {
      setError(e.message)
    } finally {
      setSaving(false)
    }
  }

  // ── Delete ────────────────────────────────────────────────────────────────
  async function handleDelete() {
    try {
      await deleteDoc(doc(db, 'songs', songId))
      onDeleted()
    } catch (e) {
      setError(e.message)
    }
  }

  // ── Render ────────────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="editor-loading">
        <div className="spinner" />
        <span>Loading song…</span>
      </div>
    )
  }

  const degrees = degreesForQuality(draft.keyQuality)

  return (
    <div className="song-editor">

      {/* ── Editor header ──────────────────────────────────────────────── */}
      <div className="editor-header">
        <input
          className="song-name-input"
          value={draft.name}
          onChange={e => update({ name: e.target.value })}
          placeholder="Song name…"
        />
        <div className="editor-actions">
          {dirty && <span className="unsaved-dot" title="Unsaved changes">●</span>}
          {songId && (
            <button
              className="btn-danger-ghost"
              onClick={() => setShowDeleteConfirm(true)}
            >
              Delete
            </button>
          )}
          <button className="btn-primary" onClick={handleSave} disabled={saving}>
            {saving ? 'Saving…' : songId ? 'Update Song' : 'Save Song'}
          </button>
        </div>
      </div>

      {error && <div className="error-bar">{error}</div>}

      {/* ── Key + Quality ──────────────────────────────────────────────── */}
      <div className="key-section">
        <div className="key-group">
          <div className="key-group-label">Root Key</div>
          <div className="key-chips">
            {ALL_KEYS.map(k => (
              <button
                key={k}
                className={`key-chip ${draft.rootKey === k ? 'selected' : ''}`}
                onClick={() => update({ rootKey: k })}
              >
                {k}
              </button>
            ))}
          </div>
        </div>

        <div className="quality-group">
          <div className="key-group-label">Quality</div>
          <div className="quality-chips">
            {QUALITIES.map(q => (
              <button
                key={q}
                className={`quality-chip ${draft.keyQuality === q ? 'selected' : ''}`}
                onClick={() => update({ keyQuality: q })}
              >
                {q}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* ── Scale reference ─────────────────────────────────────────────── */}
      <div className="scale-reference">
        <span className="scale-reference-title">Scale reference — {draft.rootKey} {draft.keyQuality}</span>
        <div className="scale-pills">
          {degrees.map(d => (
            <span key={d} className="scale-pill">
              <span className="scale-degree">{d}</span>
              <span className="scale-chord">{resolveChord(d, draft.rootKey, draft.keyQuality)}</span>
            </span>
          ))}
        </div>
      </div>

      {/* ── Song parts ──────────────────────────────────────────────────── */}
      <div className="parts-section">
        <h3 className="parts-heading">Song Parts</h3>

        {draft.parts.length === 0 && (
          <div className="no-parts-hint">
            No sections yet. Use the buttons below to add your first section.
          </div>
        )}

        {draft.parts.map((part, i) => (
          <PartCard
            key={`${part.type}-${i}`}
            part={part}
            partIndex={i}
            totalParts={draft.parts.length}
            currentKey={draft.rootKey}
            keyQuality={draft.keyQuality}
            degrees={degrees}
            onUpdate={changes => updatePart(i, changes)}
            onRemove={() => removePart(i)}
            onMoveUp={() => movePart(i, -1)}
            onMoveDown={() => movePart(i, 1)}
            onInsertDegree={degree => insertDegree(degree, i)}
            onTextareaFocus={el => {
              activeTextareaRef.current  = el
              activePartIndexRef.current = i
            }}
          />
        ))}

        {/* Add section row */}
        <div className="add-part-row">
          <span className="add-part-label">Add section:</span>
          {PART_TYPES.map(type => (
            <button key={type} className="add-part-btn" onClick={() => addPart(type)}>
              + {type}
            </button>
          ))}
        </div>
      </div>

      {/* ── Delete confirmation modal ─────────────────────────────────── */}
      {showDeleteConfirm && (
        <div className="modal-overlay" onClick={() => setShowDeleteConfirm(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>Delete "{draft.name}"?</h3>
            <p>This will permanently remove the song from the database. This cannot be undone.</p>
            <div className="modal-actions">
              <button className="btn-ghost" onClick={() => setShowDeleteConfirm(false)}>Cancel</button>
              <button className="btn-danger" onClick={handleDelete}>Delete Song</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
