const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

const numberFormatter = new Intl.NumberFormat('pt-BR')

function toNumber(value) {
  if (value === null || value === undefined || value === '') {
    return null
  }
  const number = typeof value === 'string' ? Number(value) : value
  return Number.isNaN(number) ? null : number
}

export function formatCurrency(value) {
  const number = toNumber(value)
  return number === null ? '—' : currencyFormatter.format(number)
}

export function formatKm(value) {
  const number = toNumber(value)
  return number === null ? '—' : `${numberFormatter.format(number)} km`
}

export function formatLiters(value) {
  const number = toNumber(value)
  return number === null
    ? '—'
    : `${new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 3 }).format(number)} L`
}

/**
 * Formats ISO dates in pt-BR. Date-only strings ("2026-08-11") are parsed
 * as local dates on purpose: parsing them with `new Date` would interpret
 * them as UTC midnight and show the previous day in Brazilian time zones.
 */
export function formatDate(isoValue) {
  if (!isoValue) {
    return '—'
  }
  if (typeof isoValue === 'string' && !isoValue.includes('T')) {
    const [year, month, day] = isoValue.split('-').map(Number)
    return new Date(year, month - 1, day).toLocaleDateString('pt-BR')
  }
  return new Date(isoValue).toLocaleDateString('pt-BR')
}

/** "2026-08" → "ago/2026" */
export function formatMonth(yearMonth) {
  if (!yearMonth) {
    return '—'
  }
  const [year, month] = yearMonth.split('-').map(Number)
  const label = new Date(year, month - 1, 1).toLocaleDateString('pt-BR', { month: 'short' })
  return `${label.replace('.', '')}/${year}`
}

export function formatPercentage(value) {
  const number = toNumber(value)
  if (number === null) {
    return '—'
  }
  const signal = number > 0 ? '+' : ''
  return `${signal}${new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 }).format(number)}%`
}
