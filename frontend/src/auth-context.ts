import { createContext, useContext } from 'react'
import type { Role, User, WorkspaceBadges } from './types'

export interface AuthState {
  token: string | null
  user: User | null
  badges: WorkspaceBadges | null
  initializing: boolean
  signIn: (email: string, password: string) => Promise<void>
  signUp: (input: { name: string; email: string; password: string; phone: string; role: User['role'] }) => Promise<void>
  switchRole: (role: Role) => Promise<void>
  activateRole: (role: Role) => Promise<void>
  refreshBadges: () => Promise<void>
  updateUser: (user: User) => void
  signOut: () => void
}

export const AuthContext = createContext<AuthState | null>(null)

export function useAuth() {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used inside AuthProvider')
  return value
}