export default function EmptyState({ title, message, actionLabel, onAction }) {
  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '60px 24px',
      textAlign: 'center',
    }}>
      <svg width="96" height="96" viewBox="0 0 96 96" fill="none" style={{ marginBottom: 24, opacity: 0.5 }}>
        <rect x="20" y="30" width="56" height="40" rx="4" stroke="#B8FF47" strokeWidth="2" fill="none" />
        <rect x="30" y="40" width="36" height="3" rx="1.5" fill="#B8FF47" opacity="0.4" />
        <rect x="30" y="48" width="28" height="3" rx="1.5" fill="#B8FF47" opacity="0.3" />
        <rect x="30" y="56" width="32" height="3" rx="1.5" fill="#B8FF47" opacity="0.25" />
        <circle cx="48" cy="24" r="8" stroke="#B8FF47" strokeWidth="2" fill="none" />
        <line x1="48" y1="16" x2="48" y2="12" stroke="#B8FF47" strokeWidth="2" strokeLinecap="round" />
        <line x1="48" y1="32" x2="48" y2="36" stroke="#B8FF47" strokeWidth="2" strokeLinecap="round" />
        <line x1="40" y1="24" x2="36" y2="24" stroke="#B8FF47" strokeWidth="2" strokeLinecap="round" />
        <line x1="56" y1="24" x2="60" y2="24" stroke="#B8FF47" strokeWidth="2" strokeLinecap="round" />
      </svg>
      <h3 style={{ fontSize: 18, fontWeight: 600, margin: '0 0 6px', color: '#fff' }}>{title}</h3>
      <p style={{ fontSize: 14, color: '#94A3B8', margin: '0 0 24px', maxWidth: 360 }}>{message}</p>
      {actionLabel && onAction && (
        <button
          onClick={onAction}
          style={{
            background: '#B8FF47',
            color: '#0A1A12',
            border: 'none',
            padding: '10px 24px',
            borderRadius: 8,
            fontSize: 14,
            fontWeight: 600,
            cursor: 'pointer',
            transition: 'opacity 200ms',
          }}
          onMouseEnter={(e) => (e.currentTarget.style.opacity = '0.85')}
          onMouseLeave={(e) => (e.currentTarget.style.opacity = '1')}
        >
          {actionLabel}
        </button>
      )}
    </div>
  )
}
