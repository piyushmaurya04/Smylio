import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import { api, configureAuthBridge } from './api'
import { AuthContext, type AuthState } from './auth-context'
import type { AuthResponse, Role, User, WorkspaceBadges } from './types'

const LEGACY_STORAGE_KEY = 'tom-auth-session'
const WORKSPACE_SYNC_STORAGE_KEY = 'tom-active-workspace-switch'

interface MemorySession {
  token: string
  user: User
  expiresAt: number
}

// Survives React 18 StrictMode double-mount on initial boot so the single-use
// refresh cookie is never replayed concurrently.
let bootRefreshPromise: Promise<AuthResponse | null> | null = null

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<MemorySession | null>(null)
  const [badges, setBadges] = useState<WorkspaceBadges | null>(null)
  const [initializing, setInitializing] = useState(true)
  const sessionRef = useRef<MemorySession | null>(null)
  const refreshTimerRef = useRef<number | null>(null)

  const clearTimer = useCallback(() => {
    if (refreshTimerRef.current !== null) {
      window.clearTimeout(refreshTimerRef.current)
      refreshTimerRef.current = null
    }
  }, [])

  const clearSession = useCallback(() => {
    clearTimer()
    sessionRef.current = null
    setSession(null)
    setBadges(null)
  }, [clearTimer])

  const applyAuthResponse = useCallback((response: AuthResponse) => {
    const nextSession: MemorySession = {
      token: response.accessToken,
      user: response.user,
      expiresAt: Date.now() + response.expiresIn * 1000,
    }
    sessionRef.current = nextSession
    setSession(nextSession)
    return nextSession
  }, [])

  const silentRefresh = useCallback(async (): Promise<string | null> => {
    try {
      const response = await api.refresh()
      applyAuthResponse(response)
      return response.accessToken
    } catch {
      clearSession()
      return null
    }
  }, [applyAuthResponse, clearSession])

  const refreshBadges = useCallback(async () => {
    const currentToken = sessionRef.current?.token
    if (!currentToken) {
      setBadges(null)
      return
    }
    try {
      const res = await api.workspaceBadges(currentToken)
      setBadges(res)
    } catch {
      // Non-blocking
    }
  }, [])

  const switchRole = useCallback(async (role: Role) => {
    const currentToken = sessionRef.current?.token
    if (!currentToken) throw new Error('Not authenticated')
    const response = await api.switchRole(currentToken, role)
    applyAuthResponse(response)
    try {
      localStorage.setItem(WORKSPACE_SYNC_STORAGE_KEY, `${role}:${Date.now()}`)
    } catch {
      // Non-blocking storage
    }
    try {
      const res = await api.workspaceBadges(response.accessToken)
      setBadges(res)
    } catch {
      // Non-blocking
    }
  }, [applyAuthResponse])

  const activateRole = useCallback(async (role: Role) => {
    const currentToken = sessionRef.current?.token
    if (!currentToken) throw new Error('Not authenticated')
    const response = await api.activateRole(currentToken, role)
    applyAuthResponse(response)
    try {
      localStorage.setItem(WORKSPACE_SYNC_STORAGE_KEY, `${role}:${Date.now()}`)
    } catch {
      // Non-blocking storage
    }
    try {
      const res = await api.workspaceBadges(response.accessToken)
      setBadges(res)
    } catch {
      // Non-blocking
    }
  }, [applyAuthResponse])

  useEffect(() => {
    clearTimer()
    if (!session) return
    const msUntilRefresh = Math.max(10_000, (session.expiresAt - Date.now()) * 0.8)
    refreshTimerRef.current = window.setTimeout(() => {
      void silentRefresh()
    }, msUntilRefresh)
    return clearTimer
  }, [session, silentRefresh, clearTimer])

  useEffect(() => {
    if (session?.token) {
      void refreshBadges()
    } else {
      setBadges(null)
    }
  }, [session?.token, refreshBadges])

  useEffect(() => {
    const handleStorage = (event: StorageEvent) => {
      if (event.key === WORKSPACE_SYNC_STORAGE_KEY && event.newValue) {
        void silentRefresh().then(() => {
          void refreshBadges()
        })
      }
    }
    window.addEventListener('storage', handleStorage)
    return () => window.removeEventListener('storage', handleStorage)
  }, [silentRefresh, refreshBadges])

  useEffect(() => {
    configureAuthBridge({
      refreshToken: silentRefresh,
      onUnauthorized: clearSession,
    })
  }, [silentRefresh, clearSession])

  useEffect(() => {
    localStorage.removeItem(LEGACY_STORAGE_KEY)
    let active = true

    if (!bootRefreshPromise) {
      bootRefreshPromise = api.refresh().catch(() => null)
    }

    void bootRefreshPromise.then((response) => {
      if (!active) return
      if (response) {
        applyAuthResponse(response)
      }
      setInitializing(false)
    })

    return () => {
      active = false
    }
  }, [applyAuthResponse])

  const signIn = useCallback(async (email: string, password: string) => {
    const response = await api.login(email.trim(), password)
    applyAuthResponse(response)
    setInitializing(false)
  }, [applyAuthResponse])

  const signUp = useCallback(async (input: { name: string; email: string; password: string; phone: string; role: User['role'] }) => {
    await api.register(input)
    await signIn(input.email, input.password)
  }, [signIn])

  const signOut = useCallback(() => {
    clearSession()
    void api.logout().catch(() => undefined)
  }, [clearSession])

  const updateUser = useCallback((user: User) => {
    const current = sessionRef.current
    if (!current) return
    const nextSession = { ...current, user }
    sessionRef.current = nextSession
    setSession(nextSession)
  }, [])

  const value: AuthState = {
    token: session?.token ?? null,
    user: session?.user ?? null,
    badges,
    initializing,
    signIn,
    signUp,
    switchRole,
    activateRole,
    refreshBadges,
    updateUser,
    signOut,
  }
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
