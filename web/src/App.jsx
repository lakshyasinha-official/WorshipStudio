import { useState, useEffect } from 'react'
import { onAuthStateChanged } from 'firebase/auth'
import { doc, getDoc } from 'firebase/firestore'
import { auth, db } from './firebase'
import Login from './components/Login'
import Layout from './components/Layout'

export default function App() {
  const [status, setStatus] = useState('loading') // 'loading' | 'unauthenticated' | 'ready'
  const [membership, setMembership] = useState(null)

  useEffect(() => {
    return onAuthStateChanged(auth, async (user) => {
      if (!user) {
        setStatus('unauthenticated')
        setMembership(null)
        return
      }

      // churchId is persisted in localStorage so we can reconstruct the membership doc ID
      const churchId = localStorage.getItem('ws_church')
      if (!churchId) {
        // User is auth'd but we lost the churchId (e.g. cleared storage) — force re-login
        setStatus('unauthenticated')
        return
      }

      try {
        const snap = await getDoc(doc(db, 'memberships', `${user.uid}_${churchId}`))
        if (snap.exists()) {
          setMembership({ ...snap.data(), churchId, uid: user.uid })
          setStatus('ready')
        } else {
          setStatus('unauthenticated')
        }
      } catch {
        setStatus('unauthenticated')
      }
    })
  }, [])

  if (status === 'loading') {
    return (
      <div className="splash">
        <div className="splash-logo">♪</div>
        <div className="spinner" />
      </div>
    )
  }

  if (status === 'unauthenticated') {
    return (
      <Login
        onLogin={(m) => {
          setMembership(m)
          setStatus('ready')
        }}
      />
    )
  }

  return <Layout membership={membership} />
}
