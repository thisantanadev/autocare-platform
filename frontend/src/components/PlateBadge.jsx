export default function PlateBadge({ plate }) {
  if (!plate) {
    return null
  }
  return <span className="plate">{plate}</span>
}
