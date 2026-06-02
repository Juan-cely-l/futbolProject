import { useNavigate } from 'react-router-dom'
import { formatCurrency } from '../utils/formatCurrency'

export default function TeamCard({ team, delay = 0, onClick }) {
  const navigate = useNavigate()

  const handleClick = () => {
    if (onClick) {
      onClick()
    } else {
      navigate(`/teams/${encodeURIComponent(team.name)}`)
    }
  }

  const handleKey = (e) => {
    if (e.key === 'Enter') handleClick()
  }

  return (
    <div
      onClick={handleClick}
      onKeyDown={handleKey}
      role="button"
      tabIndex={0}
      aria-label={`${team.name} — ${team.city}`}
      className="animate-fade-slide-up"
      style={{
        background: '#11291B',
        border: '1px solid #1E422E',
        borderRadius: 12,
        padding: 24,
        cursor: 'pointer',
        transition: 'all 200ms ease',
        animationDelay: `${delay}ms`,
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.transform = 'translateY(-4px)'
        e.currentTarget.style.boxShadow = '0 8px 32px rgba(0,0,0,0.3)'
        e.currentTarget.style.borderColor = '#2D5A3D'
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.transform = 'translateY(0)'
        e.currentTarget.style.boxShadow = 'none'
        e.currentTarget.style.borderColor = '#1E422E'
      }}
    >
      <div style={{
        display: 'inline-block',
        padding: '3px 10px',
        borderRadius: 9999,
        background: 'rgba(255,255,255,0.05)',
        fontSize: 11,
        fontWeight: 500,
        color: '#94A3B8',
        marginBottom: 10,
      }}>
        {team.city}
      </div>

      <h3 style={{
        fontSize: 20,
        fontWeight: 700,
        color: '#fff',
        margin: '0 0 16px',
        fontFamily: "'Oswald', sans-serif",
        letterSpacing: '0.02em',
      }}>
        {team.name}
      </h3>

      <div style={{ display: 'flex', gap: 20 }}>
        <div>
          <div style={{ fontSize: 11, color: '#64748B', marginBottom: 2 }}>Budget</div>
          <div style={{ fontSize: 18, fontWeight: 700, fontFamily: "'Oswald', sans-serif", color: '#B8FF47' }}>
            {formatCurrency(team.budget)}
          </div>
        </div>
        <div>
          <div style={{ fontSize: 11, color: '#64748B', marginBottom: 2 }}>Squad</div>
          <div style={{ fontSize: 18, fontWeight: 700, fontFamily: "'Oswald', sans-serif", color: '#fff' }}>
            {team.squadCount ?? '—'}
          </div>
        </div>
      </div>
    </div>
  )
}
