export default function SkeletonLoader({ type = 'card', count = 1 }) {
  if (type === 'card') {
    return (
      <>
        {Array.from({ length: count }).map((_, i) => (
          <div
            key={i}
            className="animate-pulse-skeleton"
            style={{
              background: '#11291B',
              borderRadius: 12,
              padding: 24,
              border: '1px solid #1E422E',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
              <div style={{ width: 48, height: 48, borderRadius: '50%', background: '#163522' }} />
              <div style={{ flex: 1 }}>
                <div style={{ height: 16, width: '60%', background: '#163522', borderRadius: 4, marginBottom: 6 }} />
                <div style={{ height: 12, width: '40%', background: '#163522', borderRadius: 4 }} />
              </div>
            </div>
            <div style={{ height: 10, background: '#163522', borderRadius: 4, marginBottom: 8 }} />
            <div style={{ height: 10, background: '#163522', borderRadius: 4, width: '70%' }} />
          </div>
        ))}
      </>
    )
  }

  if (type === 'table-row') {
    return (
      <>
        {Array.from({ length: count }).map((_, i) => (
          <div
            key={i}
            className="animate-pulse-skeleton"
            style={{
              display: 'flex',
              gap: 16,
              padding: '14px 16px',
              borderBottom: '1px solid #1E422E',
              alignItems: 'center',
            }}
          >
            <div style={{ width: 24, height: 24, borderRadius: '50%', background: '#163522' }} />
            <div style={{ flex: 2, height: 14, background: '#163522', borderRadius: 4 }} />
            <div style={{ flex: 1, height: 14, background: '#163522', borderRadius: 4 }} />
            <div style={{ flex: 1, height: 14, background: '#163522', borderRadius: 4 }} />
            <div style={{ flex: 1, height: 14, background: '#163522', borderRadius: 4 }} />
          </div>
        ))}
      </>
    )
  }

  // metric card skeleton
  return (
    <>
      {Array.from({ length: count }).map((_, i) => (
        <div
          key={i}
          className="animate-pulse-skeleton"
          style={{
            background: '#11291B',
            borderRadius: 12,
            padding: 20,
            border: '1px solid #1E422E',
          }}
        >
          <div style={{ height: 12, width: '50%', background: '#163522', borderRadius: 4, marginBottom: 12 }} />
          <div style={{ height: 28, width: '70%', background: '#163522', borderRadius: 4 }} />
        </div>
      ))}
    </>
  )
}
