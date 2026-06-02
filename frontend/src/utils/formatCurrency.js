export function formatCurrency(value) {
  if (typeof value !== 'number' || !Number.isFinite(value)) return '—'
  if (value >= 1_000_000) {
    return `€${(value / 1_000_000).toFixed(1)}M`
  }
  if (value >= 1_000) {
    return `€${(value / 1_000).toFixed(0)}K`
  }
  return `€${value}`
}
