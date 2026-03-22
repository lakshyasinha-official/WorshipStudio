import { useState } from 'react'
import { signInWithEmailAndPassword } from 'firebase/auth'
import { doc, getDoc } from 'firebase/firestore'
import { auth, db } from '../firebase'

export default function Login({ onLogin }) {
  const [church,   setChurch]   = useState(localStorage.getItem('ws_church') || '')
  const [email,    setEmail]    = useState('')
  const [password, setPassword] = useState('')
  const [loading,  setLoading]  = useState(false)
  const [error,    setError]    = useState('')

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)

    const churchId = church.trim().toLowerCase()
    if (!churchId) { setError('Church name is required'); setLoading(false); return }

    try {
      const cred = await signInWithEmailAndPassword(auth, email.trim(), password)
      const uid  = cred.user.uid

      // Verify the membership document exists for this uid + church combination
      const membershipId = `${uid}_${churchId}`
      const snap = await getDoc(doc(db, 'memberships', membershipId))

      if (!snap.exists()) {
        await auth.signOut()
        setError(`You are not registered with church "${churchId}". Check the name or ask your admin.`)
        setLoading(false)
        return
      }

      localStorage.setItem('ws_church', churchId)
      onLogin({ ...snap.data(), churchId, uid })
    } catch (err) {
      const msg = err.code === 'auth/invalid-credential' || err.code === 'auth/wrong-password'
        ? 'Incorrect email or password.'
        : err.message
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-screen">
      <div className="login-card">
        <div className="login-brand">
          <div className="brand-icon">♪</div>
          <h1>WorshipSync</h1>
          <p className="brand-sub">Song Manager</p>
        </div>

        <form onSubmit={handleSubmit} noValidate>
          <div className="field">
            <label htmlFor="church">Church name</label>
            <input
              id="church" type="text"
              value={church} onChange={e => setChurch(e.target.value)}
              placeholder="e.g. grace-community"
              autoCapitalize="none" autoCorrect="off" spellCheck={false}
              required
            />
            <span className="field-hint">Lowercase, same as used in the mobile app</span>
          </div>

          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email" type="email"
              value={email} onChange={e => setEmail(e.target.value)}
              placeholder="you@example.com" required
            />
          </div>

          <div className="field">
            <label htmlFor="password">Password</label>
            <input
              id="password" type="password"
              value={password} onChange={e => setPassword(e.target.value)}
              placeholder="••••••••" required
            />
          </div>

          {error && <div className="error-box">{error}</div>}

          <button type="submit" className="btn-primary btn-full" disabled={loading}>
            {loading ? 'Signing in…' : 'Sign In'}
          </button>
        </form>
      </div>
    </div>
  )
}
