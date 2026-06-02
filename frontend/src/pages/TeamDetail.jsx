import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useTeamByName, useUpdateTeam, useDeleteTeam } from '../hooks/useTeams'
import { useTeamSquad, useTeamValue } from '../hooks/useTeamSquad'
import { useCreatePlayer } from '../hooks/usePlayers'
import PlayerCard from '../components/PlayerCard'
import PositionBadge from '../components/PositionBadge'
import SkeletonLoader from '../components/SkeletonLoader'
import EmptyState from '../components/EmptyState'
import ConfirmModal from '../components/ConfirmModal'
import { useToast } from '../context/ToastContext'
import { formatCurrency } from '../utils/formatCurrency'
import { positionColor, POSITIONS } from '../utils/positionColor'

const POSITION_FILTERS = ['All', 'GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD']

export default function TeamDetail() {
  const { name } = useParams()
  const navigate = useNavigate()
  const toast = useToast()

  const { data: team, isLoading: teamLoading } = useTeamByName(name)
  const { data: squad = [], isLoading: squadLoading } = useTeamSquad(name)
  const { data: teamValue } = useTeamValue(name)

  const updateMutation = useUpdateTeam()
  const deleteMutation = useDeleteTeam()
  const createPlayerMutation = useCreatePlayer()

  const [activeTab, setActiveTab] = useState('squad')
  const [positionFilter, setPositionFilter] = useState('All')
  const [showEdit, setShowEdit] = useState(false)
  const [editForm, setEditForm] = useState({ name: '', city: '', budget: '' })
  const [showDelete, setShowDelete] = useState(false)
  const [showAddPlayer, setShowAddPlayer] = useState(false)
  const [playerForm, setPlayerForm] = useState({
    name: '', position: 'FORWARD', age: '', goals: '0', assists: '0', matches: '0', valueMarket: '',
  })
  const [hoveredPlayer, setHoveredPlayer] = useState(null)

  const filteredSquad = positionFilter === 'All'
    ? squad
    : squad.filter((p) => p.position === positionFilter)

  const handleEdit = async () => {
    try {
      await updateMutation.mutateAsync({ id: team.id, data: editForm })
      toast('Team updated!', 'success')
      setShowEdit(false)
    } catch (err) {
      toast(err.friendlyMessage || 'Update failed.', 'error')
    }
  }

  const handleDelete = async () => {
    try {
      await deleteMutation.mutateAsync(team.id)
      toast('Team deleted.', 'info')
      navigate('/teams')
    } catch (err) {
      toast(err.friendlyMessage || 'Delete failed.', 'error')
    }
  }

  const handleAddPlayer = async () => {
    try {
      await createPlayerMutation.mutateAsync({
        name: playerForm.name.trim(),
        position: playerForm.position,
        age: Number(playerForm.age),
        goals: Number(playerForm.goals),
        assists: Number(playerForm.assists),
        matches: Number(playerForm.matches),
        valueMarket: Number(playerForm.valueMarket),
        teamName: name,
      })
      toast('Player added to squad!', 'success')
      setShowAddPlayer(false)
      setPlayerForm({ name: '', position: 'FORWARD', age: '', goals: '0', assists: '0', matches: '0', valueMarket: '' })
    } catch (err) {
      toast(err.friendlyMessage || 'Failed to add player.', 'error')
    }
  }

  const inputStyle = {
    width: '100%',
    padding: '10px 14px',
    borderRadius: 8,
    border: '1px solid #1E422E',
    background: '#0A1A12',
    color: '#fff',
    fontSize: 14,
    outline: 'none',
    boxSizing: 'border-box',
  }

  if (teamLoading) {
    return (
      <div style={{ padding: 32, maxWidth: 1280, margin: '0 auto' }}>
        <SkeletonLoader type="card" count={1} />
        <div style={{ marginTop: 24 }}><SkeletonLoader type="card" count={3} /></div>
      </div>
    )
  }

  if (!team) {
    return <EmptyState title="Team not found" message="This team doesn't exist." actionLabel="Back to Teams" onAction={() => navigate('/teams')} />
  }

  return (
    <div style={{ padding: 32, maxWidth: 1280, margin: '0 auto' }}>
      {/* Team Header */}
      <div style={{
        background: '#11291B',
        border: '1px solid #1E422E',
        borderRadius: 16,
        padding: 28,
        marginBottom: 28,
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
          <div>
            <span style={{ fontSize: 12, color: '#64748B', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              {team.city}
            </span>
            <h1 style={{
              fontSize: 32, fontWeight: 700, fontFamily: "'Oswald', sans-serif",
              margin: '4px 0 0', color: '#fff', letterSpacing: '0.02em',
            }}>
              {team.name}
            </h1>
          </div>
          <div style={{ display: 'flex', gap: 10 }}>
            <button onClick={() => { setEditForm({ name: team.name, city: team.city, budget: team.budget }); setShowEdit(true) }}
              style={{
                padding: '8px 18px', borderRadius: 8, border: '1px solid #1E422E',
                background: 'transparent', color: '#fff', fontSize: 13, fontWeight: 500, cursor: 'pointer',
              }}>
              Edit
            </button>
            <button onClick={() => setShowDelete(true)}
              style={{
                padding: '8px 18px', borderRadius: 8, border: '1px solid #EF4444',
                background: 'rgba(239,68,68,0.1)', color: '#EF4444', fontSize: 13, fontWeight: 500, cursor: 'pointer',
              }}>
              Delete
            </button>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 32, marginTop: 20 }}>
          <div>
            <div style={{ fontSize: 11, color: '#64748B', marginBottom: 2 }}>Budget</div>
            <div style={{ fontSize: 22, fontWeight: 700, fontFamily: "'Oswald', sans-serif", color: '#B8FF47' }}>
              {formatCurrency(team.budget)}
            </div>
          </div>
          <div>
            <div style={{ fontSize: 11, color: '#64748B', marginBottom: 2 }}>Total Squad Value</div>
            <div style={{ fontSize: 22, fontWeight: 700, fontFamily: "'Oswald', sans-serif", color: '#fff' }}>
              {teamValue ? formatCurrency(teamValue.totalValue) : '—'}
            </div>
          </div>
          <div>
            <div style={{ fontSize: 11, color: '#64748B', marginBottom: 2 }}>Squad Size</div>
            <div style={{ fontSize: 22, fontWeight: 700, fontFamily: "'Oswald', sans-serif", color: '#fff' }}>
              {squad.length}
            </div>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: 0, borderBottom: '1px solid #1E422E', marginBottom: 24 }}>
        {['squad', 'stats'].map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            style={{
              padding: '10px 20px',
              fontSize: 14,
              fontWeight: 600,
              color: activeTab === tab ? '#B8FF47' : '#64748B',
              background: 'none',
              border: 'none',
              borderBottom: activeTab === tab ? '2px solid #B8FF47' : '2px solid transparent',
              cursor: 'pointer',
              marginBottom: -1,
              textTransform: 'capitalize',
            }}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* Squad Tab */}
      {activeTab === 'squad' && (
        <>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20, flexWrap: 'wrap', gap: 10 }}>
            <div style={{ display: 'flex', gap: 6 }}>
              {POSITION_FILTERS.map((p) => (
                <button
                  key={p}
                  onClick={() => setPositionFilter(p)}
                  style={{
                    padding: '4px 12px',
                    borderRadius: 9999,
                    fontSize: 12,
                    fontWeight: 600,
                    border: positionFilter === p ? '1px solid #B8FF47' : '1px solid #1E422E',
                    background: positionFilter === p ? 'rgba(184,255,71,0.1)' : 'transparent',
                    color: positionFilter === p ? '#B8FF47' : '#94A3B8',
                    cursor: 'pointer',
                  }}
                >
                  {p === 'All' ? 'All' : <PositionBadge position={p} />}
                </button>
              ))}
            </div>
            <button onClick={() => setShowAddPlayer(true)} style={{
              background: '#B8FF47', color: '#0A1A12', border: 'none',
              padding: '8px 16px', borderRadius: 8, fontSize: 13, fontWeight: 600, cursor: 'pointer',
            }}>
              + Add Player
            </button>
          </div>

          {squadLoading ? (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 20 }}>
              <SkeletonLoader type="card" count={3} />
            </div>
          ) : filteredSquad.length === 0 ? (
            <EmptyState
              title="No players in this squad"
              message={positionFilter !== 'All' ? 'No players at this position.' : 'Add players to build your team.'}
              actionLabel={positionFilter === 'All' ? '+ Add Player' : undefined}
              onAction={() => setShowAddPlayer(true)}
            />
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 20 }}>
              {filteredSquad.map((p, i) => (
                <PlayerCard key={p.id} player={p} delay={i * 60} />
              ))}
            </div>
          )}
        </>
      )}

      {/* Stats Tab */}
      {activeTab === 'stats' && (
        <div style={{
          background: '#11291B',
          border: '1px solid #1E422E',
          borderRadius: 16,
          padding: 24,
          position: 'relative',
        }}>
          <h3 style={{ fontSize: 16, fontWeight: 600, margin: '0 0 16px', color: '#fff' }}>Pitch View</h3>

          {/* SVG Pitch */}
          {squad.length === 0 ? (
            <p style={{ color: '#64748B', fontSize: 14, textAlign: 'center', padding: 40 }}>No players to display.</p>
          ) : (
            <div style={{ position: 'relative' }}>
              <svg viewBox="0 0 400 500" style={{ width: '100%', maxHeight: 500 }}>
                {/* Pitch background */}
                <rect x="0" y="0" width="400" height="500" fill="#0D1F16" rx="8" />
                {/* Center circle */}
                <circle cx="200" cy="250" r="40" fill="none" stroke="#1E422E" strokeWidth="1.5" />
                <line x1="200" y1="0" x2="200" y2="500" stroke="#1E422E" strokeWidth="1.5" />
                {/* Penalty areas */}
                <rect x="80" y="20" width="240" height="120" fill="none" stroke="#1E422E" strokeWidth="1.5" />
                <rect x="80" y="360" width="240" height="120" fill="none" stroke="#1E422E" strokeWidth="1.5" />
                {/* Goal areas */}
                <rect x="130" y="70" width="140" height="50" fill="none" stroke="#1E422E" strokeWidth="1.5" />
                <rect x="130" y="380" width="140" height="50" fill="none" stroke="#1E422E" strokeWidth="1.5" />

                {/* Position players by role */}
                {(() => {
                  const gks = squad.filter((p) => p.position === 'GOALKEEPER')
                  const defs = squad.filter((p) => p.position === 'DEFENDER')
                  const mids = squad.filter((p) => p.position === 'MIDFIELDER')
                  const fwds = squad.filter((p) => p.position === 'FORWARD')

                  const elems = []

                  gks.forEach((p, i) => {
                    const x = 200 + (i - (gks.length - 1) / 2) * 50
                    elems.push({ p, x, y: 70, label: p.name.split(' ').pop() || p.name })
                  })

                  defs.forEach((p, i) => {
                    const spread = Math.max(1, defs.length - 1)
                    const x = 200 + (i - spread / 2) * 60
                    elems.push({ p, x, y: 150, label: p.name.split(' ').pop() || p.name })
                  })

                  mids.forEach((p, i) => {
                    const spread = Math.max(1, mids.length - 1)
                    const x = 200 + (i - spread / 2) * 60
                    elems.push({ p, x, y: 260, label: p.name.split(' ').pop() || p.name })
                  })

                  fwds.forEach((p, i) => {
                    const spread = Math.max(1, fwds.length - 1)
                    const x = 200 + (i - spread / 2) * 60
                    elems.push({ p, x, y: 370, label: p.name.split(' ').pop() || p.name })
                  })

                  return elems.map(({ p, x, y, label }) => {
                    const colors = positionColor(p.position)
                    return (
                      <g key={p.id} style={{ cursor: 'pointer' }}
                        onMouseEnter={() => setHoveredPlayer(p)}
                        onMouseLeave={() => setHoveredPlayer(null)}
                      >
                        <circle cx={x} cy={y} r="18" fill={colors.bg} stroke={colors.text} strokeWidth="2" />
                        <text x={x} y={y + 1} textAnchor="middle" dominantBaseline="central"
                          fill="#fff" fontSize="11" fontWeight="700" fontFamily="'Oswald', sans-serif">
                          {p.goals || '0'}
                        </text>
                        <text x={x} y={y + 28} textAnchor="middle" fill="#94A3B8" fontSize="9" fontWeight="500">
                          {label}
                        </text>
                      </g>
                    )
                  })
                })()}
              </svg>

              {/* Efficiency Tooltip */}
              {hoveredPlayer && (
                <div className="animate-fade-slide-up" style={{
                  position: 'absolute',
                  top: 12,
                  right: 12,
                  background: '#1A3A26',
                  border: '1px solid #2D5A3D',
                  borderRadius: 8,
                  padding: '10px 14px',
                  fontSize: 13,
                }}>
                  <div style={{ fontWeight: 600, color: '#fff', marginBottom: 4 }}>{hoveredPlayer.name}</div>
                  <div style={{ color: '#94A3B8', fontSize: 12 }}>
                    Efficiency: <span style={{
                      color: (() => {
                        const e = ((hoveredPlayer.goals || 0) + (hoveredPlayer.assists || 0)) / (hoveredPlayer.matches || 1)
                        return e >= 1 ? '#22C55E' : e >= 0.5 ? '#F59E0B' : '#EF4444'
                      })(),
                      fontWeight: 700,
                    }}>
                      {(((hoveredPlayer.goals || 0) + (hoveredPlayer.assists || 0)) / (hoveredPlayer.matches || 1)).toFixed(2)}
                    </span>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Edit Modal */}
      {showEdit && (
        <div style={{ position: 'fixed', inset: 0, zIndex: 900, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.6)' }} onClick={() => setShowEdit(false)} />
          <div style={{ position: 'relative', background: '#11291B', border: '1px solid #1E422E', borderRadius: 16, padding: 28, width: 420, maxWidth: '90vw' }}>
            <h2 style={{ fontSize: 18, fontWeight: 600, margin: '0 0 20px', color: '#fff' }}>Edit Team</h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 14, marginBottom: 24 }}>
              <div>
                <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Name</label>
                <input value={editForm.name} onChange={(e) => setEditForm({ ...editForm, name: e.target.value })} style={inputStyle} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>City</label>
                <input value={editForm.city} onChange={(e) => setEditForm({ ...editForm, city: e.target.value })} style={inputStyle} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Budget</label>
                <input type="number" value={editForm.budget} onChange={(e) => setEditForm({ ...editForm, budget: e.target.value })} style={inputStyle} />
              </div>
            </div>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
              <button onClick={() => setShowEdit(false)} style={{ padding: '8px 20px', borderRadius: 8, border: '1px solid #1E422E', background: 'transparent', color: '#fff', fontSize: 14, cursor: 'pointer' }}>Cancel</button>
              <button onClick={handleEdit} disabled={updateMutation.isPending} style={{ padding: '8px 20px', borderRadius: 8, border: 'none', background: '#B8FF47', color: '#0A1A12', fontSize: 14, fontWeight: 600, cursor: 'pointer' }}>
                {updateMutation.isPending ? 'Saving…' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Modal */}
      <ConfirmModal
        open={showDelete}
        title="Delete Team"
        message={`Are you sure you want to delete "${team.name}"? This will also remove all associated players.`}
        onConfirm={handleDelete}
        onCancel={() => setShowDelete(false)}
        isLoading={deleteMutation.isPending}
      />

      {/* Add Player Modal */}
      {showAddPlayer && (
        <div style={{ position: 'fixed', inset: 0, zIndex: 900, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.6)' }} onClick={() => setShowAddPlayer(false)} />
          <div style={{ position: 'relative', background: '#11291B', border: '1px solid #1E422E', borderRadius: 16, padding: 28, width: 420, maxWidth: '90vw', maxHeight: '90vh', overflowY: 'auto' }}>
            <h2 style={{ fontSize: 18, fontWeight: 600, margin: '0 0 20px', color: '#fff' }}>Add Player to {team.name}</h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginBottom: 24 }}>
              <div>
                <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Name</label>
                <input value={playerForm.name} onChange={(e) => setPlayerForm({ ...playerForm, name: e.target.value })} style={inputStyle} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Position</label>
                <select value={playerForm.position} onChange={(e) => setPlayerForm({ ...playerForm, position: e.target.value })} style={inputStyle}>
                  {POSITIONS.map((p) => <option key={p} value={p}>{p}</option>)}
                </select>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                <div>
                  <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Age</label>
                  <input type="number" value={playerForm.age} onChange={(e) => setPlayerForm({ ...playerForm, age: e.target.value })} style={inputStyle} />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Market Value</label>
                  <input type="number" value={playerForm.valueMarket} onChange={(e) => setPlayerForm({ ...playerForm, valueMarket: e.target.value })} style={inputStyle} />
                </div>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10 }}>
                <div>
                  <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Goals</label>
                  <input type="number" value={playerForm.goals} onChange={(e) => setPlayerForm({ ...playerForm, goals: e.target.value })} style={inputStyle} />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Assists</label>
                  <input type="number" value={playerForm.assists} onChange={(e) => setPlayerForm({ ...playerForm, assists: e.target.value })} style={inputStyle} />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Matches</label>
                  <input type="number" value={playerForm.matches} onChange={(e) => setPlayerForm({ ...playerForm, matches: e.target.value })} style={inputStyle} />
                </div>
              </div>
            </div>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
              <button onClick={() => setShowAddPlayer(false)} style={{ padding: '8px 20px', borderRadius: 8, border: '1px solid #1E422E', background: 'transparent', color: '#fff', fontSize: 14, cursor: 'pointer' }}>Cancel</button>
              <button onClick={handleAddPlayer} disabled={createPlayerMutation.isPending} style={{ padding: '8px 20px', borderRadius: 8, border: 'none', background: '#B8FF47', color: '#0A1A12', fontSize: 14, fontWeight: 600, cursor: createPlayerMutation.isPending ? 'not-allowed' : 'pointer', opacity: createPlayerMutation.isPending ? 0.6 : 1 }}>
                {createPlayerMutation.isPending ? 'Adding…' : 'Add Player'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
