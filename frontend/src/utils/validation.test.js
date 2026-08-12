import {
  collectErrors,
  normalizePlate,
  requireEmail,
  requireNumber,
  requirePassword,
  requireText,
  validatePlate,
} from './validation.js'

describe('requireText', () => {
  it('rejects blank input', () => {
    expect(requireText('   ')).toBe('Campo obrigatório')
  })

  it('accepts text within the limit', () => {
    expect(requireText('Fiat', { max: 60 })).toBeUndefined()
  })

  it('rejects text past the limit', () => {
    expect(requireText('a'.repeat(61), { max: 60 })).toBe('Use no máximo 60 caracteres')
  })
})

describe('requireEmail', () => {
  it('accepts a well-formed address', () => {
    expect(requireEmail('motorista@autocare.dev')).toBeUndefined()
  })

  it.each(['sem-arroba', 'a@b', 'a b@c.dev'])('rejects %s', (value) => {
    expect(requireEmail(value)).toBe('Informe um e-mail válido')
  })
})

describe('requirePassword', () => {
  it('mirrors the 8 character minimum enforced by the backend', () => {
    expect(requirePassword('1234567')).toBe('Use pelo menos 8 caracteres')
    expect(requirePassword('12345678')).toBeUndefined()
  })

  it('mirrors the 72 byte BCrypt ceiling', () => {
    expect(requirePassword('a'.repeat(73))).toBe('Use no máximo 72 caracteres')
  })
})

describe('requireNumber', () => {
  it('requires a value by default', () => {
    expect(requireNumber('')).toBe('Campo obrigatório')
  })

  it('allows blanks for optional fields', () => {
    expect(requireNumber('', { required: false })).toBeUndefined()
  })

  it('enforces the range', () => {
    expect(requireNumber('-1', { min: 0 })).toBe('Não pode ser menor que 0')
    expect(requireNumber('2100', { max: 2027 })).toBe('Não pode ser maior que 2027')
  })

  it('rejects non-numeric and non-integer input', () => {
    expect(requireNumber('abc')).toBe('Informe um número válido')
    expect(requireNumber('10.5', { integer: true })).toBe('Informe um número inteiro')
  })

  it('accepts decimals when integers are not required', () => {
    expect(requireNumber('41.567')).toBeUndefined()
  })
})

describe('plate handling', () => {
  it('strips spaces and hyphens and upper-cases', () => {
    expect(normalizePlate(' abc-1d23 ')).toBe('ABC1D23')
  })

  it('accepts both Brazilian layouts', () => {
    expect(validatePlate('ABC1D23')).toBeUndefined()
    expect(validatePlate('abc1234')).toBeUndefined()
  })

  it('treats an empty plate as valid, since it is optional', () => {
    expect(validatePlate('')).toBeUndefined()
  })

  it.each(['AB1234', 'ABCD123', 'A1C1D23'])('rejects %s', (value) => {
    expect(validatePlate(value)).toBe('Use o formato brasileiro, como ABC1D23')
  })
})

describe('collectErrors', () => {
  it('drops the fields that validated cleanly', () => {
    expect(collectErrors({ a: undefined, b: 'erro', c: undefined })).toEqual({ b: 'erro' })
  })

  it('yields an empty object when everything is valid', () => {
    expect(Object.keys(collectErrors({ a: undefined })).length).toBe(0)
  })
})
