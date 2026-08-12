import {
  attachAuthHeader,
  extractErrorMessage,
  getAccessToken,
  isAuthPath,
  setAccessToken,
} from './client.js'

afterEach(() => setAccessToken(null))

describe('access token storage', () => {
  it('keeps the token in memory only', () => {
    setAccessToken('token-123')
    expect(getAccessToken()).toBe('token-123')
    // Nothing is persisted, so an XSS payload cannot read it back.
    expect(window.localStorage.getItem('accessToken')).toBeNull()
    expect(window.sessionStorage.getItem('accessToken')).toBeNull()
  })
})

describe('attachAuthHeader', () => {
  it('adds the bearer header when a token is set', () => {
    setAccessToken('token-123')
    expect(attachAuthHeader({ headers: {} }).headers.Authorization).toBe('Bearer token-123')
  })

  it('leaves anonymous requests untouched', () => {
    expect(attachAuthHeader({ headers: {} }).headers.Authorization).toBeUndefined()
  })
})

describe('isAuthPath', () => {
  it.each(['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout'])(
    'treats %s as an auth path so a 401 there never triggers a refresh retry',
    (path) => {
      expect(isAuthPath(path)).toBe(true)
    },
  )

  it('treats business endpoints as refreshable', () => {
    expect(isAuthPath('/vehicles')).toBe(false)
    expect(isAuthPath('/dashboard')).toBe(false)
  })
})

describe('extractErrorMessage', () => {
  it('joins field validation errors', () => {
    const error = {
      response: {
        data: {
          fieldErrors: [
            { field: 'brand', message: 'Marca é obrigatória' },
            { field: 'model', message: 'Modelo é obrigatório' },
          ],
        },
      },
    }
    expect(extractErrorMessage(error)).toBe('Marca é obrigatória. Modelo é obrigatório')
  })

  it('prefers the API message', () => {
    const error = { response: { data: { message: 'Placa já cadastrada' } } }
    expect(extractErrorMessage(error)).toBe('Placa já cadastrada')
  })

  it('falls back when the server sent nothing usable', () => {
    expect(extractErrorMessage(new Error('network down'), 'Tente de novo')).toBe('Tente de novo')
  })
})
