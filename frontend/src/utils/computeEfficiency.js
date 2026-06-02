export function computeEfficiency(goals = 0, assists = 0, matches = 0) {
  if (!matches) return 0
  return parseFloat(((goals + assists) / matches).toFixed(2))
}

export function efficiencyColor(value) {
  if (value >= 1.0) return '#22C55E'
  if (value >= 0.5) return '#F59E0B'
  return '#EF4444'
}
