/**
 * Client-side mirrors of the backend constraints. They exist to give instant
 * feedback, never as the authoritative check: every rule here is enforced
 * again by Bean Validation and the domain services on the server.
 */

export const REQUIRED_MESSAGE = 'Campo obrigatório'

// Same pattern as VehicleService.PLATE_PATTERN: covers the old Brazilian
// layout (ABC1234) and the Mercosul one (ABC1D23).
export const PLATE_PATTERN = /^[A-Z]{3}\d[A-Z0-9]\d{2}$/

/** BCrypt only hashes the first 72 bytes, so the backend caps the password there. */
export const PASSWORD_MIN_LENGTH = 8
export const PASSWORD_MAX_LENGTH = 72

export function requireText(value, { max, message = REQUIRED_MESSAGE } = {}) {
  const text = value?.trim() ?? ''
  if (!text) {
    return message
  }
  if (max && text.length > max) {
    return `Use no máximo ${max} caracteres`
  }
  return undefined
}

export function requireEmail(value) {
  const text = value?.trim() ?? ''
  if (!text) {
    return REQUIRED_MESSAGE
  }
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(text) ? undefined : 'Informe um e-mail válido'
}

export function requirePassword(value) {
  if (!value) {
    return REQUIRED_MESSAGE
  }
  if (value.length < PASSWORD_MIN_LENGTH) {
    return `Use pelo menos ${PASSWORD_MIN_LENGTH} caracteres`
  }
  if (value.length > PASSWORD_MAX_LENGTH) {
    return `Use no máximo ${PASSWORD_MAX_LENGTH} caracteres`
  }
  return undefined
}

/**
 * Validates a numeric text input. Empty values are only rejected when
 * `required`, so optional fields can stay blank.
 */
export function requireNumber(value, { required = true, min, max, integer = false } = {}) {
  const text = typeof value === 'string' ? value.trim() : value
  if (text === '' || text === null || text === undefined) {
    return required ? REQUIRED_MESSAGE : undefined
  }
  const number = Number(text)
  if (Number.isNaN(number)) {
    return 'Informe um número válido'
  }
  if (integer && !Number.isInteger(number)) {
    return 'Informe um número inteiro'
  }
  if (min !== undefined && number < min) {
    return `Não pode ser menor que ${min}`
  }
  if (max !== undefined && number > max) {
    return `Não pode ser maior que ${max}`
  }
  return undefined
}

/** Normalizes a typed plate the same way the backend does before validating. */
export function normalizePlate(value) {
  return (value ?? '').replace(/[\s-]/g, '').toUpperCase()
}

export function validatePlate(value) {
  const plate = normalizePlate(value)
  if (!plate) {
    return undefined
  }
  return PLATE_PATTERN.test(plate) ? undefined : 'Use o formato brasileiro, como ABC1D23'
}

/** Drops the `undefined` entries so `Object.keys(errors).length` means "is invalid". */
export function collectErrors(candidates) {
  return Object.fromEntries(Object.entries(candidates).filter(([, message]) => Boolean(message)))
}

/** Today's date as `yyyy-MM-dd` in local time, for `max` on date inputs. */
export function todayIso() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}
