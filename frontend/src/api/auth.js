import { api, setAccessToken } from './client.js'

export async function register(name, email, password) {
  const { data } = await api.post('/auth/register', { name, email, password })
  setAccessToken(data.accessToken)
  return data.user
}

export async function login(email, password) {
  const { data } = await api.post('/auth/login', { email, password })
  setAccessToken(data.accessToken)
  return data.user
}

export async function restoreSession() {
  const { data } = await api.post('/auth/refresh')
  setAccessToken(data.accessToken)
  return data.user
}

export async function logout() {
  try {
    await api.post('/auth/logout')
  } finally {
    setAccessToken(null)
  }
}

export async function getCurrentUser() {
  const { data } = await api.get('/auth/me')
  return data
}
