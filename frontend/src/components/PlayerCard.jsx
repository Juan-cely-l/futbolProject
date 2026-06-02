import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import PositionBadge from './PositionBadge'
import StatBar from './StatBar'
import { formatCurrency } from '../utils/formatCurrency'
import { computeEfficiency } from '../utils/computeEfficiency'

export default function PlayerCard({ player, delay = 0 }) {
  const navigate = useNavigate()
  const [hover, setHover] = useState(false)
  const efficiency = computeEfficiency(player.goals, player.assists, player.matches)

  return (
    <div
      onClick={() => navigate(`/players/${player.id}`)}
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
      role="button"
      tabIndex={0}
      aria-label={`${player.name} — ${player.position}`}
      onKeyDown={(e) => e.key === 'Enter' && navigate(`/players/${player.id}`)}
      className="animate-fade-slide-up"
      style={{
        background: '#11291B',
        border: '1px solid #1E422E',
        borderRadius: 12,
        padding: 20,
        cursor: 'pointer',
        transition: 'all 200ms ease',
        transform: hover ? 'translateY(-4px)' : 'translateY(0)',
        boxShadow: hover ? '0 8px 32px rgba(0,0,0,0.3)' : 'none',
        animationDelay: `${delay}ms`,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 14 }}>
        <div style={{
          width: 48,
          height: 48,
          borderRadius: '50%',
          background: '#B8FF47',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontFamily: "'Oswald', sans-serif",
          fontSize: 20,
          fontWeight: 700,
          color: '#0A1A12',
          flexShrink: 0,
        }}>
          {player.goals ?? '—'}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{
            fontSize: 15,
            fontWeight: 600,
            color: '#fff',
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            marginBottom: 3,
          }}>
            {player.name}
          </div>
          <PositionBadge position={player.position} />
        </div>
      </div>

      <div style={{ display: 'flex', gap: 16, marginBottom: 12 }}>
        <div>
          <div style={{ fontSize: 11, color: '#64748B', marginBottom: 2 }}>Age</div>
          <div style={{ fontSize: 16, fontWeight: 700, fontFamily: "'Oswald', sans-serif", color: '#fff' }}>
            {player.age}
          </div>
        </div>
        <div>
          <div style={{ fontSize: 11, color: '#64748B', marginBottom: 2 }}>Value</div>
          <div style={{ fontSize: 16, fontWeight: 700, fontFamily: "'Oswald', sans-serif", color: '#B8FF47' }}>
            {formatCurrency(player.valueMarket)}
          </div>
        </div>
        {hover && (
          <div className="animate-fade-slide-up" style={{ marginLeft: 'auto', textAlign: 'right' }}>
            <div style={{ fontSize: 11, color: '#64748B', marginBottom: 2 }}>Efficiency</div>
            <div style={{ fontSize: 16, fontWeight: 700, fontFamily: "'Oswald', sans-serif", color: efficiency >= 1 ? '#22C55E' : efficiency >= 0.5 ? '#F59E0B' : '#EF4444' }}>
              {efficiency.toFixed(2)}
            </div>
          </div>
        )}
      </div>

      <StatBar goals={player.goals} assists={player.assists} matches={player.matches} />
    </div>
  )
}
