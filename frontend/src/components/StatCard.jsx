export default function StatCard({ label, value, detail, mono = true }) {
  return (
    <div className="card stat-card">
      <span className="stat-label">{label}</span>
      <div className={`stat-value ${mono ? 'money' : ''}`}>{value}</div>
      {detail && <div className="stat-detail">{detail}</div>}
    </div>
  )
}
