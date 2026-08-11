import {
  Bar,
  BarChart,
  LabelList,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

import { formatCurrency } from '../../utils/format.js'

function ChartTooltip({ active, payload }) {
  if (!active || !payload?.length) {
    return null
  }
  return (
    <div className="card" style={{ padding: '0.55rem 0.75rem', fontSize: '0.82rem' }}>
      {payload[0].payload.label}: <span className="money">{formatCurrency(payload[0].value)}</span>
    </div>
  )
}

/**
 * Horizontal magnitude chart, single hue with direct value labels.
 * data: [{ label: 'Freios', total: number }]
 */
export default function CategoryBarChart({ data }) {
  const height = Math.max(data.length * 44 + 16, 120)
  return (
    <div className="chart-block" style={{ minHeight: height }}>
      <ResponsiveContainer width="100%" height={height}>
        <BarChart data={data} layout="vertical" margin={{ top: 4, right: 84, left: 0, bottom: 4 }}>
          <XAxis type="number" hide />
          <YAxis
            type="category"
            dataKey="label"
            width={118}
            tickLine={false}
            axisLine={false}
            tick={{ fill: '#1b1f24', fontSize: 12.5 }}
          />
          <Tooltip content={<ChartTooltip />} cursor={{ fill: 'rgba(27, 31, 36, 0.05)' }} />
          <Bar dataKey="total" fill="#3e6db5" radius={[0, 4, 4, 0]} barSize={18}>
            <LabelList
              dataKey="total"
              position="right"
              formatter={formatCurrency}
              style={{ fill: '#55616c', fontSize: 11.5, fontFamily: "'IBM Plex Mono', monospace" }}
            />
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
