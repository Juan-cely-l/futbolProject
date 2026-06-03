import { useNavigate } from 'react-router-dom'

const links = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/teams', label: 'Teams' },
  { to: '/players', label: 'Players' },
]

export default function Navbar({ onNewClick }) {
  const navigate = useNavigate()

  return (
    <nav style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 32px',
      height: 64,
      background: '#0D1F16',
      borderBottom: '1px solid #1E422E',
      position: 'sticky',
      top: 0,
      zIndex: 100,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 48 }}>
        <button
          onClick={() => navigate('/dashboard')}
          style={{
            fontFamily: "'Oswald', sans-serif",
            fontSize: 24,
            fontWeight: 700,
            color: '#B8FF47',
            letterSpacing: '0.08em',
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            padding: 0,
          }}
        >
          FUTBIX
        </button>

        <div style={{ display: 'flex', gap: 4 }}>
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              style={({ isActive }) => ({
                padding: '8px 16px',
                borderRadius: 6,
                fontSize: 14,
                fontWeight: 500,
                color: isActive ? '#fff' : '#64748B',
                textDecoration: 'none',
                transition: 'all 200ms',
                borderBottom: isActive ? '2px solid #B8FF47' : '2px solid transparent',
                marginBottom: -1,
              })}
            >
              {link.label}
            </NavLink>
          ))}
        </div>
      </div>

      <button
        onClick={onNewClick}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 6,
          background: '#B8FF47',
          color: '#0A1A12',
          border: 'none',
          padding: '8px 18px',
          borderRadius: 8,
          fontSize: 14,
          fontWeight: 600,
          cursor: 'pointer',
          transition: 'opacity 200ms',
        }}
        onMouseEnter={(e) => (e.currentTarget.style.opacity = '0.85')}
        onMouseLeave={(e) => (e.currentTarget.style.opacity = '1')}
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <line x1="8" y1="3" x2="8" y2="13" stroke="#0A1A12" strokeWidth="2" strokeLinecap="round" />
          <line x1="3" y1="8" x2="13" y2="8" stroke="#0A1A12" strokeWidth="2" strokeLinecap="round" />
        </svg>
        New
      </button>
    </nav>
  )
}
