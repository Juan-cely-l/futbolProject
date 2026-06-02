export default function StatBar({ goals, assists, matches, showLabels = true }) {
  const total = (goals || 0) + (assists || 0)
  const goalPct = total > 0 ? (goals / total) * 100 : 0
  const assistPct = total > 0 ? (assists / total) * 100 : 0

  return (
    <div style={{ width: '100%' }}>
      {showLabels && (
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          fontSize: 11,
          color: '#94A3B8',
          marginBottom: 4,
        }}>
          <span>{goals} goals</span>
          <span>{assists} assists</span>
        </div>
      )}
      <div style={{
        height: 6,
        background: '#1E422E',
        borderRadius: 3,
        overflow: 'hidden',
        display: 'flex',
      }}>
        <div style={{
          width: `${goalPct}%`,
          background: '#B8FF47',
          transition: 'width 300ms ease',
          minWidth: total > 0 ? 4 : 0,
        }} />
        <div style={{
          width: `${assistPct}%`,
          background: '#3B82F6',
          transition: 'width 300ms ease',
          minWidth: total > 0 ? 4 : 0,
        }} />
      </div>
      {showLabels && (
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          fontSize: 11,
          color: '#64748B',
          marginTop: 4,
        }}>
          <span>{matches} matches</span>
        </div>
      )}
    </div>
  )
}
