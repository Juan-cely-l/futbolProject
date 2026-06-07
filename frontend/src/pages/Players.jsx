import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { usePlayers, useDeletePlayer } from '../hooks/usePlayers'
import PlayerCard from '../components/PlayerCard'
import PlayerAvatar from '../components/PlayerAvatar'
import PositionBadge from '../components/PositionBadge'
import SkeletonLoader from '../components/SkeletonLoader'
import Pagination from '../components/Pagination'
import EmptyState from '../components/EmptyState'
import ConfirmModal from '../components/ConfirmModal'
import { useToast } from '../context/ToastContext'
import { formatCurrency } from '../utils/formatCurrency'
import { computeEfficiency, efficiencyColor } from '../utils/computeEfficiency'
import { POSITIONS } from '../utils/positionColor'

const SORT_OPTIONS = [
  { label: 'Name A–Z', sortBy: 'name', sortDir: 'asc' },
  { label: 'Name Z–A', sortBy: 'name', sortDir: 'desc' },
  { label: 'Value high–low', sortBy: 'valueMarket', sortDir: 'desc' },
  { label: 'Value low–high', sortBy: 'valueMarket', sortDir: 'asc' },
  { label: 'Goals high–low', sortBy: 'goals', sortDir: 'desc' },
]

export default function Players() {
  const navigate = useNavigate()
  const toast = useToast()

  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [positionFilter, setPositionFilter] = useState('All')
  const [sortIdx, setSortIdx] = useState(0)
  const [viewMode, setViewMode] = useState('table')
  const [deleteTarget, setDeleteTarget] = useState(null)

  const { data, isLoading } = usePlayers(page, 15, SORT_OPTIONS[sortIdx].sortBy, SORT_OPTIONS[sortIdx].sortDir, search || undefined)
  const deleteMutation = useDeletePlayer()

  const players = data?.content || []

  const filtered = players.filter((p) =>
    positionFilter === 'All' || p.position === positionFilter
  )

  const handleDelete = async () => {
    if (!deleteTarget) return
    try {
      await deleteMutation.mutateAsync(deleteTarget.id)
      toast('Player deleted.', 'info')
      setDeleteTarget(null)
    } catch (err) {
      toast(err.friendlyMessage || 'Delete failed.', 'error')
    }
  }

  const colHeader = {
    padding: '10px 12px',
    fontSize: 11,
    fontWeight: 600,
    color: '#64748B',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
    textAlign: 'left',
    borderBottom: '1px solid #1E422E',
    whiteSpace: 'nowrap',
  }

  const cellStyle = {
    padding: '10px 12px',
    fontSize: 13,
    color: '#fff',
    borderBottom: '1px solid rgba(30,66,46,0.5)',
    verticalAlign: 'middle',
  }

  return (
    <div style={{ padding: 32, maxWidth: 1280, margin: '0 auto' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20, flexWrap: 'wrap', gap: 10 }}>
        <h1 style={{ fontSize: 24, fontWeight: 700, fontFamily: "'Oswald', sans-serif", margin: 0 }}>
          All Players
        </h1>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <input
            placeholder="Search players…"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0) }}
            style={{
              padding: '8px 12px', borderRadius: 8, border: '1px solid #1E422E',
              background: '#0A1A12', color: '#fff', fontSize: 13, outline: 'none', width: 180,
            }}
            aria-label="Search players by name"
          />
          <select
            value={sortIdx}
            onChange={(e) => { setSortIdx(Number(e.target.value)); setPage(0) }}
            style={{
              padding: '8px 12px', borderRadius: 8, border: '1px solid #1E422E',
              background: '#0A1A12', color: '#fff', fontSize: 13, outline: 'none',
            }}
            aria-label="Sort players"
          >
            {SORT_OPTIONS.map((opt, i) => (
              <option key={i} value={i}>{opt.label}</option>
            ))}
          </select>
          <button
            onClick={() => setViewMode(viewMode === 'table' ? 'cards' : 'table')}
            style={{
              padding: '8px 10px', borderRadius: 8, border: '1px solid #1E422E',
              background: 'transparent', color: '#94A3B8', cursor: 'pointer', display: 'flex',
            }}
            aria-label={viewMode === 'table' ? 'Switch to card view' : 'Switch to table view'}
            title={viewMode === 'table' ? 'Card view' : 'Table view'}
          >
            {viewMode === 'table' ? (
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
                <rect x="1" y="1" width="7" height="7" rx="1.5" stroke="currentColor" strokeWidth="1.5" />
                <rect x="10" y="1" width="7" height="7" rx="1.5" stroke="currentColor" strokeWidth="1.5" />
                <rect x="1" y="10" width="7" height="7" rx="1.5" stroke="currentColor" strokeWidth="1.5" />
                <rect x="10" y="10" width="7" height="7" rx="1.5" stroke="currentColor" strokeWidth="1.5" />
              </svg>
            ) : (
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
                <rect x="1" y="1" width="16" height="3" rx="1" stroke="currentColor" strokeWidth="1.5" />
                <rect x="1" y="7.5" width="16" height="3" rx="1" stroke="currentColor" strokeWidth="1.5" />
                <rect x="1" y="14" width="16" height="3" rx="1" stroke="currentColor" strokeWidth="1.5" />
              </svg>
            )}
          </button>
        </div>
      </div>

      {/* Position Filters */}
      <div style={{ display: 'flex', gap: 6, marginBottom: 20, flexWrap: 'wrap' }}>
        {['All', ...POSITIONS].map((p) => (
          <button
            key={p}
            onClick={() => { setPositionFilter(p); setPage(0) }}
            style={{
              padding: '4px 14px',
              borderRadius: 9999,
              fontSize: 12,
              fontWeight: 600,
              border: positionFilter === p ? '1px solid #B8FF47' : '1px solid #1E422E',
              background: positionFilter === p ? 'rgba(184,255,71,0.1)' : 'transparent',
              color: positionFilter === p ? '#B8FF47' : '#94A3B8',
              cursor: 'pointer',
            }}
          >
            {p === 'All' ? 'All' : p.charAt(0) + p.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {isLoading ? (
        viewMode === 'table' ? (
          <div style={{ background: '#11291B', border: '1px solid #1E422E', borderRadius: 12, overflow: 'hidden' }}>
            <SkeletonLoader type="table-row" count={8} />
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 20 }}>
            <SkeletonLoader type="card" count={6} />
          </div>
        )
      ) : filtered.length === 0 ? (
        <EmptyState
          title="No players found"
          message={search || positionFilter !== 'All' ? 'Try different filters.' : 'Add players to get started.'}
        />
      ) : viewMode === 'table' ? (
        <div style={{ background: '#11291B', border: '1px solid #1E422E', borderRadius: 12, overflow: 'hidden' }}>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: 700 }}>
              <thead>
                <tr>
                  <th style={colHeader}>#</th>
                  <th style={colHeader}>Name</th>
                  <th style={colHeader}>Position</th>
                  <th style={colHeader}>Age</th>
                  <th style={colHeader}>Team</th>
                  <th style={colHeader}>G</th>
                  <th style={colHeader}>A</th>
                  <th style={colHeader}>M</th>
                  <th style={colHeader}>Eff.</th>
                  <th style={colHeader}>Value</th>
                  <th style={colHeader}></th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((p, i) => {
                  const eff = computeEfficiency(p.goals, p.assists, p.matches)
                  return (
                    <tr key={p.id} style={{ cursor: 'pointer', transition: 'background 150ms' }}
                      onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(255,255,255,0.02)')}
                      onMouseLeave={(e) => (e.currentTarget.style.background = '')}
                      onClick={() => navigate(`/players/${p.id}`)}
                    >
                      <td style={{ ...cellStyle, color: '#64748B' }}>{page * 15 + i + 1}</td>
                      <td style={{ ...cellStyle, fontWeight: 600 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          <PlayerAvatar name={p.name} photo={p.photo} size="sm" />
                          {p.name}
                        </div>
                      </td>
                      <td style={cellStyle}><PositionBadge position={p.position} /></td>
                      <td style={cellStyle}>{p.age}</td>
                      <td style={{ ...cellStyle, color: '#94A3B8' }}>{p.teamName || '—'}</td>
                      <td style={cellStyle}>{p.goals ?? 0}</td>
                      <td style={cellStyle}>{p.assists ?? 0}</td>
                      <td style={cellStyle}>{p.matches ?? 0}</td>
                      <td style={{ ...cellStyle, color: efficiencyColor(eff), fontWeight: 700 }}>{eff.toFixed(2)}</td>
                      <td style={{ ...cellStyle, fontFamily: "'Oswald', sans-serif", fontWeight: 600 }}>{formatCurrency(p.valueMarket)}</td>
                      <td style={cellStyle}>
                        <div style={{ display: 'flex', gap: 4 }} onClick={(e) => e.stopPropagation()}>
                          <button onClick={() => navigate(`/players/${p.id}`)}
                            style={iconBtnStyle} aria-label={`View ${p.name}`}>
                            <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="1.5" fill="currentColor"/><circle cx="8" cy="8" r="6" stroke="currentColor" strokeWidth="1.5"/></svg>
                          </button>
                          <button onClick={() => setDeleteTarget(p)}
                            style={{ ...iconBtnStyle, color: '#EF4444' }} aria-label={`Delete ${p.name}`}>
                            <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><line x1="5" y1="4" x2="11" y2="10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/><line x1="11" y1="4" x2="5" y2="10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/></svg>
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
          <div style={{ padding: '0 16px' }}>
            <Pagination page={page} totalPages={data?.totalPages || 1} totalElements={data?.totalElements || 0} size={15} onPageChange={setPage} />
          </div>
        </div>
      ) : (
        <>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 20 }}>
            {filtered.map((p, i) => (
              <PlayerCard key={p.id} player={p} delay={i * 60} />
            ))}
          </div>
          <Pagination page={page} totalPages={data?.totalPages || 1} totalElements={data?.totalElements || 0} size={15} onPageChange={setPage} />
        </>
      )}

      <ConfirmModal
        open={!!deleteTarget}
        title="Delete Player"
        message={`Remove ${deleteTarget?.name || ''} from the system? This action cannot be undone.`}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
        isLoading={deleteMutation.isPending}
      />
    </div>
  )
}

const iconBtnStyle = {
  background: 'none',
  border: 'none',
  color: '#94A3B8',
  cursor: 'pointer',
  padding: 4,
  borderRadius: 4,
  display: 'flex',
  transition: 'color 200ms',
}
