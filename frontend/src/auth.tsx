import { useCallback, useRef, useState, type ReactNode } from 'react'
import { api } from './api'
import { AuthContext, type AuthState } from './auth-context'
import type { User } from './types'

const AUTH_STORAGE_KEY = 'tom-auth-session'

interface StoredSession {
  token: string
  user: User
  expiresAt: number
}

function loadSession(): StoredSession | null {
  const stored = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!stored) return null

  try {
    const session = JSON.parse(stored) as StoredSession
    if (!session.token || !session.user || typeof session.expiresAt !== 'number' || session.expiresAt <= Date.now()) {
      localStorage.removeItem(AUTH_STORAGE_KEY)
      return null
    }
    return session
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<StoredSession | null>(loadSession)
  const sessionRef = useRef(session)

  const signIn = useCallback(async (email: string, password: string) => {
    const response = await api.login(email.trim(), password)
    const nextSession = {
      token: response.accessToken,
      user: response.user,
      expiresAt: Date.now() + response.expiresIn * 1000,
    }
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextSession))
    sessionRef.current = nextSession
    setSession(nextSession)
  }, [])

  const signUp = useCallback(async (input: { name: string; email: string; password: string; phone: string; role: User['role'] }) => {
    await api.register(input)
    await signIn(input.email, input.password)
  }, [signIn])

  const signOut = useCallback(() => {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    sessionRef.current = null
    setSession(null)
  }, [])

  const updateUser = useCallback((user: User) => {
    const current = sessionRef.current
    if (!current) return
    const nextSession = { ...current, user }
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextSession))
    sessionRef.current = nextSession
    setSession(nextSession)
  }, [])

  const value: AuthState = { token: session?.token ?? null, user: session?.user ?? null, signIn, signUp, updateUser, signOut }
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}