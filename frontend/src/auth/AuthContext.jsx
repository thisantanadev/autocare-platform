import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'

import * as authApi from '../api/auth.js'
import { setUnauthorizedHandler } from '../api/client.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [initializing, setInitializing] = useState(true)

  useEffect(() => {
    setUnauthorizedHandler(() => setUser(null))
    // Try to restore the session from the HttpOnly refresh cookie. A 401
    // simply means "not logged in" and is not an error.
    authApi
      .restoreSession()
      .then(setUser)
      .catch(() => {})
      .finally(() => setInitializing(false))
    return () => setUnauthorizedHandler(null)
  }, [])

  const login = useCallback(async (email, password) => {
    const loggedUser = await authApi.login(email, password)
    setUser(loggedUser)
    return loggedUser
  }, [])

  const register = useCallback(async (name, email, password) => {
    const newUser = await authApi.register(name, email, password)
    setUser(newUser)
    return newUser
  }, [])

  const logout = useCallback(async () => {
    await authApi.logout().catch(() => {})
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, initializing, login, register, logout }),
    [user, initializing, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside an AuthProvider')
  }
  return context
}
