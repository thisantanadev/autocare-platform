import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

import { formatCurrency, formatMonth } from '../../utils/format.js'

// Categorical pair validated for color-vision deficiency (ΔE ≥ 25, see docs).
const SERIES = [
  { key: 'maintenance', name: 'Manutenção', color: '#3e6db5' },
  { key: 'fuel', name: 'Combustível', color: '#c88a00' },
]

const compactCurrency = new Intl.NumberFormat('pt-BR', {
  notation: 'compact',
  style: 'currency',
  currency: 'BRL',
})

function ChartTooltip({ active, payload, label }) {
  if (!active || !payload?.length) {
    return null
  }
  return (
    <div className="card" style={{ padding: '0.6rem 0.8rem', fontSize: '0.82rem' }}>
      <strong>{formatMonth(label)}</strong>
      {payload.map((entry) => (
        <div key={entry.dataKey} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <span className="swatch" style={{ background: entry.color }} />
          {entry.name}: <span className="money">{formatCurrency(entry.value)}</span>
        </div>
      ))}
    </div>
  )
}

/** data: [{ month: '2026-08', maintenance: number, fuel: number }] */
export default function MonthlyExpensesChart({ data }) {
  return (
    <div className="chart-block">
      <div className="chart-legend">
        {SERIES.map((series) => (
          <span key={series.key}>
            <span className="swatch" style={{ background: series.color }} />
            {series.name}
          </span>
        ))}
      </div>
      <ResponsiveContainer width="100%" height={250}>
        <BarChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }} barCategoryGap="28%">
          <CartesianGrid vertical={false} stroke="#e4e9ed" />
          <XAxis
            dataKey="month"
            tickFormatter={formatMonth}
            tickLine={false}
            axisLine={{ stroke: '#b9c2cb' }}
            tick={{ fill: '#55616c', fontSize: 12 }}
          />
          <YAxis
            tickFormatter={(value) => compactCurrency.format(value)}
            tickLine={false}
            axisLine={false}
            tick={{ fill: '#7c8894', fontSize: 11.5 }}
            width={72}
          />
          <Tooltip content={<ChartTooltip />} cursor={{ fill: 'rgba(27, 31, 36, 0.05)' }} />
          {SERIES.map((series, index) => (
            <Bar
              key={series.key}
              dataKey={series.key}
              name={series.name}
              stackId="month"
              fill={series.color}
              stroke="#ffffff"
              strokeWidth={2}
              radius={index === SERIES.length - 1 ? [4, 4, 0, 0] : 0}
            />
          ))}
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
