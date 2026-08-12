import {
  formatConsumption,
  formatCurrency,
  formatDate,
  formatKm,
  formatLiters,
  formatMonth,
  formatPercentage,
} from './format.js'

// Intl output uses non-breaking spaces; comparing on digits keeps these
// assertions readable and independent of the separator characters.
function digitsOf(value) {
  return value.replace(/\s/g, ' ')
}

describe('formatCurrency', () => {
  it('formats numbers as Brazilian reais', () => {
    expect(digitsOf(formatCurrency(1234.5))).toContain('1.234,50')
  })

  it('accepts numeric strings, as BigDecimal payloads may arrive', () => {
    expect(digitsOf(formatCurrency('249.90'))).toContain('249,90')
  })

  it('renders a dash for missing values', () => {
    expect(formatCurrency(null)).toBe('—')
    expect(formatCurrency(undefined)).toBe('—')
    expect(formatCurrency('')).toBe('—')
  })
})

describe('formatKm and formatLiters', () => {
  it('adds the unit and groups thousands', () => {
    expect(formatKm(45000)).toBe('45.000 km')
  })

  it('keeps up to three decimals for litres', () => {
    expect(formatLiters(41.567)).toBe('41,567 L')
  })

  it('renders a dash for missing values', () => {
    expect(formatKm(null)).toBe('—')
    expect(formatLiters(null)).toBe('—')
  })
})

describe('formatDate', () => {
  it('keeps date-only strings on the same calendar day', () => {
    // Parsing "2026-08-11" with `new Date` would yield UTC midnight and
    // render as 10/08 in Brazilian time zones.
    expect(formatDate('2026-08-11')).toBe('11/08/2026')
  })

  it('formats full instants', () => {
    expect(formatDate('2026-08-11T14:30:00Z')).toMatch(/^\d{2}\/\d{2}\/2026$/)
  })

  it('renders a dash for missing values', () => {
    expect(formatDate(null)).toBe('—')
  })
})

describe('formatMonth', () => {
  it('turns an ISO year-month into a short label', () => {
    expect(formatMonth('2026-08')).toBe('ago/2026')
  })

  it('renders a dash for missing values', () => {
    expect(formatMonth('')).toBe('—')
  })
})

describe('formatPercentage', () => {
  it('prefixes growth with a plus sign', () => {
    expect(formatPercentage(12.34)).toBe('+12,3%')
  })

  it('keeps the minus sign for a drop', () => {
    expect(formatPercentage(-8.5)).toBe('-8,5%')
  })

  it('renders a dash when the backend could not compute it', () => {
    expect(formatPercentage(null)).toBe('—')
  })
})

describe('formatConsumption', () => {
  it('formats km per litre with two decimals', () => {
    expect(formatConsumption(11.436)).toBe('11,44 km/L')
  })

  it('renders a dash when there is not enough data', () => {
    expect(formatConsumption(null)).toBe('—')
  })
})
