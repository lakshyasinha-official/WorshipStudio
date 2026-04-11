import { useState } from 'react'
import { signOut } from 'firebase/auth'
import { auth } from '../firebase'
import SongSidebar from './SongSidebar'
import SongDetail  from './SongDetail'
import SongEditor  from './SongEditor'

// mode: 'empty' | 'view' | 'edit' | 'new'
export default function Layout({ membership }) {
  const [selectedId, setSelectedId] = useState(null)
  const [mode,       setMode]       = useState('empty')
  const [editorKey,  setEditorKey]  = useState(0)

  const { churchId, role, displayName, uid } = membership

  function openNew() {
    setSelectedId(null)
    setMode('new')
    setEditorKey(k => k + 1)
  }

  function openSong(id) {
    setSelectedId(id)
    setMode('view')
    setEditorKey(k => k + 1)
  }

  function handleEdit() {
    setMode('edit')
  }

  function handleSaved(id) {
    setSelectedId(id)
    setMode('view')
  }

  function handleDeleted() {
    setSelectedId(null)
    setMode('empty')
  }

  async function handleSignOut() {
    localStorage.removeItem('ws_church')
    await signOut(auth)
  }

  return (
    <div className="app-layout">
      {/* ── Top bar ─────────────────────────────────────────────────────── */}
      <header className="topbar">
        <div className="topbar-left">
          <span className="topbar-logo">♪ WorshipSync</span>
          <span className="topbar-church">{churchId}</span>
        </div>
        <div className="topbar-right">
          <span className="topbar-user">{displayName || auth.currentUser?.email}</span>
          {role === 'admin' && <span className="role-badge">Admin</span>}
          <button className="btn-ghost" onClick={handleSignOut}>Sign out</button>
        </div>
      </header>

      {/* ── Body ────────────────────────────────────────────────────────── */}
      <div className="app-body">
        <SongSidebar
          churchId={churchId}
          selectedId={selectedId}
          isAdmin={role === 'admin'}
          onSelect={openSong}
          onNew={openNew}
          onDeleted={handleDeleted}
        />

        <main className="main-content">
          {mode === 'empty' && (
            <div className="empty-state">
              <div className="empty-icon">🎵</div>
              <h2>No song open</h2>
              <p>Select a song from the sidebar, or start a new one.</p>
              <button className="btn-primary" onClick={openNew}>+ New Song</button>
            </div>
          )}

          {mode === 'view' && (
            <SongDetail
              key={selectedId}
              songId={selectedId}
              onEdit={handleEdit}
            />
          )}

          {(mode === 'edit' || mode === 'new') && (
            <SongEditor
              key={editorKey}
              songId={mode === 'new' ? null : selectedId}
              churchId={churchId}
              userId={uid}
              isAdmin={role === 'admin'}
              onSaved={handleSaved}
              onDeleted={handleDeleted}
            />
          )}
        </main>
      </div>
    </div>
  )
}
