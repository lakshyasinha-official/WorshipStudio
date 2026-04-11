import { useState, useEffect } from 'react'
import { collection, query, where, getDocs, onSnapshot, deleteDoc, doc } from 'firebase/firestore'
import { db } from '../firebase'

export default function SongSidebar({ churchId, selectedId, isAdmin, onSelect, onNew, onDeleted }) {
  const [songs,   setSongs]   = useState([])
  const [search,  setSearch]  = useState('')
  const [loading, setLoading] = useState(true)
  const [error,   setError]   = useState('')
  const [debug,   setDebug]   = useState('')

  useEffect(() => {
    // ── Diagnostic: check total songs in collection (ignoring churchId) ────────
    getDocs(collection(db, 'songs')).then(snap => {
      const allChurchIds = [...new Set(snap.docs.map(d => d.data().churchId))]
      console.log(`[Debug] Total songs in DB: ${snap.size}`)
      console.log(`[Debug] churchIds found:`, allChurchIds)
      console.log(`[Debug] Filtering for churchId="${churchId}"`)
      setDebug(`DB total: ${snap.size} song(s) | churchIds: [${allChurchIds.join(', ')}] | querying: "${churchId}"`)
    }).catch(err => {
      console.error('[Debug] Read error:', err.message)
      setDebug(`Read blocked: ${err.message}`)
    })

    // ── Main real-time listener ────────────────────────────────────────────────
    const q = query(
      collection(db, 'songs'),
      where('churchId', '==', churchId)
    )
    const unsub = onSnapshot(q, (snap) => {
      const list = snap.docs.map(d => ({ id: d.id, ...d.data() }))
      list.sort((a, b) =>
        (a.nameLowercase || a.name || '').localeCompare(b.nameLowercase || b.name || '')
      )
      setSongs(list)
      setLoading(false)
      setError('')
    }, (err) => {
      console.error('Firestore error:', err)
      setError(err.message)
      setLoading(false)
    })

    return unsub
  }, [churchId])

  async function handleDelete(e, song) {
    e.stopPropagation()
    if (!window.confirm(`Delete "${song.name}"? This cannot be undone.`)) return
    await deleteDoc(doc(db, 'songs', song.id))
    if (selectedId === song.id) onDeleted?.()
  }

  const term = search.trim().toLowerCase()
  const filtered = term
    ? songs.filter(s => (s.nameLowercase || s.name?.toLowerCase() || '').includes(term))
    : songs

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <span className="sidebar-title">
          Songs
          {!loading && <span className="count-badge">{songs.length}</span>}
        </span>
        <button className="btn-primary btn-sm" onClick={onNew}>+ New</button>
      </div>

      <div className="sidebar-search">
        <input
          type="search"
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="Search songs…"
        />
      </div>

      <div className="song-list">
        {debug && <div className="list-state" style={{fontSize:11,color:'var(--text-muted)',lineHeight:1.4,textAlign:'left',padding:'8px 10px'}}>{debug}</div>}
        {error && <div className="list-state" style={{color:'var(--error)',fontSize:12}}>{error}</div>}
        {loading ? (
          <div className="list-state">Loading…</div>
        ) : filtered.length === 0 ? (
          <div className="list-state">
            {term ? 'No results' : 'No songs yet — add one!'}
          </div>
        ) : (
          filtered.map(song => (
            <div
              key={song.id}
              className={`song-item ${selectedId === song.id ? 'selected' : ''}`}
              onClick={() => onSelect(song.id)}
            >
              <div className="song-item-text">
                <span className="song-item-name">{song.name}</span>
                <span className="song-item-meta">
                  {song.rootKey} {song.keyQuality}
                  {song.parts?.length > 0 && ` · ${song.parts.length} parts`}
                </span>
              </div>
              {isAdmin && (
                <button
                  className="song-item-delete"
                  onClick={e => handleDelete(e, song)}
                  title="Delete song"
                >
                  🗑
                </button>
              )}
            </div>
          ))
        )}
      </div>
    </aside>
  )
}
