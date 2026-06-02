import { useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { usePlayerById } from '../hooks/usePlayers'
import { fetchPlayerEfficiency } from '../api/players'
import { useUpdatePlayer, useDeletePlayer } from '../hooks/usePlayers'
import { useAllTeams } from '../hooks/useTeams'
import { useQuery } from '@tanstack/react-query'
import PositionBadge from '../components/PositionBadge'
import SkeletonLoader from '../components/SkeletonLoader'
import ConfirmModal from '../components/ConfirmModal'
import EmptyState from '../components/EmptyState'
import { useToast } from '../context/ToastContext'
import { formatCurrency } from '../utils/formatCurrency'
import { computeEfficiency, efficiencyColor } from '../utils/computeEfficiency'
import { POSITIONS } from '../utils/positionColor'
import { BarChart, Bar, Cell, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'

export default function PlayerProfile() {
  const { id } = useParams()
  const navigate = useNavigate()
  const toast = useToast()

  const { data: player, isLoading, isError } = usePlayerById(id)

  const { data: efficiency } = useQuery({
    queryKey: ['efficiency', id],
    queryFn: () => fetchPlayerEfficiency(id),
    enabled: !!id,
  })

  const { data: teamsData } = useAllTeams()
  const teams = teamsData?.content || []

  const [showEdit, setShowEdit] = useState(false)
  const [showDelete, setShowDelete] = useState(false)
  const [editForm, setEditForm] = useState({})

  const updateMutation = useUpdatePlayer()
  const deleteMutation = useDeletePlayer()

  if (isLoading) {
    return (
      <div style={{ padding: 32, maxWidth: 960, margin: '0 auto' }}>
        <SkeletonLoader type="card" count={1} />
        <div style={{ marginTop: 24, display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 16 }}>
          <SkeletonLoader type="metric" count={4} />
        </div>
      </div>
    )
  }

  if (isError || !player) {
    return <EmptyState title="Player not found" message="This player doesn't exist." actionLabel="Back to Players" onAction={() => navigate('/players')} />
  }

  const eff = computeEfficiency(player.goals, player.assists, player.matches)
  const effColor = efficiencyColor(eff)
  const circumference = 2 * Math.PI * 54
  const offset = circumference - (Math.min(eff, 3) / 3) * circumference

  const handleEdit = async () => {
    try {
      await updateMutation.mutateAsync({ id, data: editForm })
      toast('Player updated!', 'success')
      setShowEdit(false)
    } catch (err) {
      toast(err.friendlyMessage || 'Update failed.', 'error')
    }
  }

  const handleDelete = async () => {
    try {
      await deleteMutation.mutateAsync(id)
      toast('Player deleted.', 'info')
      navigate('/players')
    } catch (err) {
      toast(err.friendlyMessage || 'Delete failed.', 'error')
    }
  }

  const chartData = [
    { name: 'Goals', value: player.goals || 0, fill: '#B8FF47' },
    { name: 'Assists', value: player.assists || 0, fill: '#3B82F6' },
  ]

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

  return (
    <div style={{ padding: 32, maxWidth: 960, margin: '0 auto' }}>
      {/* Hero */}
      <div style={{
        background: '#11291B', border: '1px solid #1E422E', borderRadius: 16,
        padding: 32, marginBottom: 28, display: 'flex', alignItems: 'center',
        gap: 28, flexWrap: 'wrap',
      }}>
        <div style={{
          width: 80, height: 80, borderRadius: '50%', background: '#B8FF47',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontFamily: "'Oswald', sans-serif", fontSize: 36, fontWeight: 700,
          color: '#0A1A12', flexShrink: 0,
        }}>
          {player.goals || '0'}
        </div>

        <div style={{ flex: 1 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6, flexWrap: 'wrap' }}>
            <h1 style={{ fontSize: 32, fontWeight: 700, fontFamily: "'Oswald', sans-serif", margin: 0, color: '#fff', letterSpacing: '0.02em' }}>
              {player.name}
            </h1>
            <PositionBadge position={player.position} size="lg" />
          </div>
          {player.teamName && (
            <Link to={`/teams/${encodeURIComponent(player.teamName)}`}
              style={{ fontSize: 14, color: '#94A3B8', textDecoration: 'none', borderBottom: '1px dashed #1E422E', paddingBottom: 1 }}>
              {player.teamName}
            </Link>
          )}
        </div>

        <div style={{ display: 'flex', gap: 8 }}>
          <button onClick={() => { setEditForm({ name: player.name, position: player.position, age: player.age, goals: player.goals, assists: player.assists, matches: player.matches, valueMarket: player.valueMarket, teamName: player.teamName }); setShowEdit(true) }}
            style={{ padding: '8px 18px', borderRadius: 8, border: '1px solid #1E422E', background: 'transparent', color: '#fff', fontSize: 13, fontWeight: 500, cursor: 'pointer' }}>
            Edit
          </button>
          <button onClick={() => setShowDelete(true)}
            style={{ padding: '8px 18px', borderRadius: 8, border: '1px solid #EF4444', background: 'rgba(239,68,68,0.1)', color: '#EF4444', fontSize: 13, fontWeight: 500, cursor: 'pointer' }}>
            Delete
          </button>
        </div>
      </div>

      {/* Stat Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 32 }}>
        {[
          { label: 'Goals', value: player.goals ?? 0 },
          { label: 'Assists', value: player.assists ?? 0 },
          { label: 'Matches', value: player.matches ?? 0 },
          { label: 'Market Value', value: formatCurrency(player.valueMarket) },
        ].map((s) => (
          <div key={s.label} style={{ background: '#11291B', border: '1px solid #1E422E', borderRadius: 12, padding: 20 }}>
            <div style={{ fontSize: 12, color: '#64748B', fontWeight: 500, marginBottom: 6, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              {s.label}
            </div>
            <div style={{ fontSize: 24, fontWeight: 700, fontFamily: "'Oswald', sans-serif", color: '#fff', lineHeight: 1.1 }}>
              {s.value}
            </div>
          </div>
        ))}
      </div>

      {/* Efficiency Ring + Chart */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24, marginBottom: 32 }}>
        <div style={{ background: '#11291B', border: '1px solid #1E422E', borderRadius: 16, padding: 28, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
          <h3 style={{ fontSize: 14, fontWeight: 600, color: '#94A3B8', margin: '0 0 16px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Efficiency Score
          </h3>
          <svg width="140" height="140" viewBox="0 0 140 140">
            <circle cx="70" cy="70" r="54" fill="none" stroke="#1E422E" strokeWidth="8" />
            <circle cx="70" cy="70" r="54" fill="none" stroke={effColor} strokeWidth="8"
              strokeDasharray={circumference} strokeDashoffset={offset}
              strokeLinecap="round" transform="rotate(-90 70 70)"
              style={{ transition: 'stroke-dashoffset 600ms ease' }}
            />
            <text x="70" y="64" textAnchor="middle" fill="#fff" fontSize="28" fontWeight="700" fontFamily="'Oswald', sans-serif">
              {eff.toFixed(2)}
            </text>
            <text x="70" y="84" textAnchor="middle" fill="#64748B" fontSize="12">per match</text>
          </svg>
          <div style={{ display: 'flex', gap: 16, marginTop: 12 }}>
            <span style={{ fontSize: 12, color: '#22C55E' }}>≥1.0 Elite</span>
            <span style={{ fontSize: 12, color: '#F59E0B' }}>0.5–0.99 Good</span>
            <span style={{ fontSize: 12, color: '#EF4444' }}>&lt;0.5 Low</span>
          </div>
        </div>

        <div style={{ background: '#11291B', border: '1px solid #1E422E', borderRadius: 16, padding: 24 }}>
          <h3 style={{ fontSize: 14, fontWeight: 600, color: '#94A3B8', margin: '0 0 16px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Goals vs Assists
          </h3>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={chartData} margin={{ top: 8, right: 16, bottom: 8, left: 8 }}>
              <XAxis dataKey="name" tick={{ fill: '#94A3B8', fontSize: 13 }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fill: '#64748B', fontSize: 12 }} axisLine={false} tickLine={false} />
              <Tooltip
                contentStyle={{ background: '#11291B', border: '1px solid #1E422E', borderRadius: 8, fontSize: 13 }}
                itemStyle={{ color: '#fff' }} labelStyle={{ color: '#94A3B8' }}
              />
              <Bar dataKey="value" radius={[4, 4, 0, 0]} barSize={48}>
                {chartData.map((entry, idx) => (
                  <Cell key={idx} fill={entry.fill} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Edit Drawer */}
      {showEdit && (
        <div style={{ position: 'fixed', inset: 0, zIndex: 900, display: 'flex', justifyContent: 'flex-end' }}>
          <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.5)' }} onClick={() => setShowEdit(false)} />
          <div style={{
            position: 'relative', width: 420, maxWidth: '90vw', background: '#11291B',
            borderLeft: '1px solid #1E422E', padding: 28, overflowY: 'auto',
            animation: 'fadeSlideUp 200ms ease-out both',
          }}>
            <h2 style={{ fontSize: 18, fontWeight: 600, margin: '0 0 24px', color: '#fff' }}>Edit Player</h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 14, marginBottom: 24 }}>
              <div>
                <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Name</label>
                <input value={editForm.name || ''} onChange={(e) => setEditForm({ ...editForm, name: e.target.value })} style={inputStyle} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Position</label>
                <select value={editForm.position || ''} onChange={(e) => setEditForm({ ...editForm, position: e.target.value })} style={inputStyle}>
                  {POSITIONS.map((p) => <option key={p} value={p}>{p}</option>)}
                </select>
              </div>
              <div>
                <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Age</label>
                <input type="number" value={editForm.age || ''} onChange={(e) => setEditForm({ ...editForm, age: Number(e.target.value) })} style={inputStyle} />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10 }}>
                <div>
                  <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Goals</label>
                  <input type="number" value={editForm.goals || 0} onChange={(e) => setEditForm({ ...editForm, goals: Number(e.target.value) })} style={inputStyle} />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Assists</label>
                  <input type="number" value={editForm.assists || 0} onChange={(e) => setEditForm({ ...editForm, assists: Number(e.target.value) })} style={inputStyle} />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Matches</label>
                  <input type="number" value={editForm.matches || 0} onChange={(e) => setEditForm({ ...editForm, matches: Number(e.target.value) })} style={inputStyle} />
                </div>
              </div>
              <div>
                <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Market Value</label>
                <input type="number" value={editForm.valueMarket || 0} onChange={(e) => setEditForm({ ...editForm, valueMarket: Number(e.target.value) })} style={inputStyle} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Team</label>
                <select value={editForm.teamName || ''} onChange={(e) => setEditForm({ ...editForm, teamName: e.target.value })} style={inputStyle}>
                  <option value="">—</option>
                  {teams.map((t) => <option key={t.id} value={t.name}>{t.name}</option>)}
                </select>
              </div>
            </div>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
              <button onClick={() => setShowEdit(false)} style={{ padding: '8px 20px', borderRadius: 8, border: '1px solid #1E422E', background: 'transparent', color: '#fff', fontSize: 14, cursor: 'pointer' }}>Cancel</button>
              <button onClick={handleEdit} disabled={updateMutation.isPending} style={{ padding: '8px 20px', borderRadius: 8, border: 'none', background: '#B8FF47', color: '#0A1A12', fontSize: 14, fontWeight: 600, cursor: updateMutation.isPending ? 'not-allowed' : 'pointer', opacity: updateMutation.isPending ? 0.6 : 1 }}>
                {updateMutation.isPending ? 'Saving…' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}

      <ConfirmModal
        open={showDelete}
        title="Delete Player"
        message={`Remove ${player.name} from the system? This cannot be undone.`}
        onConfirm={handleDelete}
        onCancel={() => setShowDelete(false)}
        isLoading={deleteMutation.isPending}
      />
    </div>
  )
}
