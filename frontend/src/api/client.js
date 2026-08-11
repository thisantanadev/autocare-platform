import axios from 'axios'

// The access token lives only in memory: it is never written to
// localStorage, so an XSS payload cannot simply read a persisted token.
// Sessions are restored through the HttpOnly refresh cookie instead.
let accessToken = null
let unauthorizedHandler = null

export function setAccessToken(token) {
  accessToken = token
}

export function getAccessToken() {
  return accessToken
}

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

export const api = axios.create({
  baseURL: '/api/v1',
  withCredentials: true,
})

export function attachAuthHeader(config) {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
}

api.interceptors.request.use(attachAuthHeader)

const AUTH_PATHS = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout']

export function isAuthPath(url = '') {
  return AUTH_PATHS.some((path) => url.includes(path))
}

// A single refresh call is shared between all requests that hit 401 at
// the same time, so an expired token never triggers a refresh stampede.
let refreshPromise = null

function refreshAccessToken() {
  refreshPromise =
    refreshPromise ??
    api
      .post('/auth/refresh')
      .then((response) => {
        setAccessToken(response.data.accessToken)
        return response.data
      })
      .finally(() => {
        refreshPromise = null
      })
  return refreshPromise
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { config, response } = error
    if (response?.status === 401 && config && !config._retried && !isAuthPath(config.url)) {
      config._retried = true
      try {
        await refreshAccessToken()
        return api(config)
      } catch {
        setAccessToken(null)
        unauthorizedHandler?.()
      }
    }
    return Promise.reject(error)
  },
)

export function extractErrorMessage(error, fallback = 'Algo deu errado. Tente novamente.') {
  const data = error?.response?.data
  if (data?.fieldErrors?.length) {
    return data.fieldErrors.map((field) => field.message).join('. ')
  }
  return data?.message ?? fallback
}
