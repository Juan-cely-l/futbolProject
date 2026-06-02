import { useState } from 'react'
import { useTeams, useCreateTeam } from '../hooks/useTeams'
import TeamCard from '../components/TeamCard'
import SkeletonLoader from '../components/SkeletonLoader'
import Pagination from '../components/Pagination'
import EmptyState from '../components/EmptyState'
import { useToast } from '../context/ToastContext'

const SORT_OPTIONS = [
  { label: 'Name A–Z', sortBy: 'name', sortDir: 'asc' },
  { label: 'Name Z–A', sortBy: 'name', sortDir: 'desc' },
  { label: 'Budget high–low', sortBy: 'budget', sortDir: 'desc' },
  { label: 'Budget low–high', sortBy: 'budget', sortDir: 'asc' },
]

export default function Teams() {
  const toast = useToast()
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [sortIdx, setSortIdx] = useState(0)
  const [showModal, setShowModal] = useState(false)
  const [form, setForm] = useState({ name: '', city: '', budget: '' })
  const [formError, setFormError] = useState('')

  const { data, isLoading } = useTeams(page, 9, SORT_OPTIONS[sortIdx].sortBy, SORT_OPTIONS[sortIdx].sortDir, search || undefined)
  const createMutation = useCreateTeam()

  const teams = data?.content || []

  const handleCreate = async () => {
    setFormError('')
    if (!form.name.trim() || !form.city.trim() || !form.budget) {
      setFormError('All fields are required.')
      return
    }
    try {
      await createMutation.mutateAsync({
        name: form.name.trim(),
        city: form.city.trim(),
        budget: Number(form.budget),
      })
      toast('Team created successfully!', 'success')
      setShowModal(false)
      setForm({ name: '', city: '', budget: '' })
    } catch (err) {
      if (err.response?.status === 409) {
        setFormError('A team with this name already exists.')
      } else {
        setFormError(err.friendlyMessage || 'Failed to create team.')
      }
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

  return (
    <div style={{ padding: 32, maxWidth: 1280, margin: '0 auto' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24, flexWrap: 'wrap', gap: 12 }}>
        <h1 style={{ fontSize: 24, fontWeight: 700, fontFamily: "'Oswald', sans-serif", margin: 0 }}>
          All Teams
        </h1>
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          <input
            placeholder="Search teams…"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0) }}
            style={inputStyle}
            aria-label="Search teams by name"
          />
          <select
            value={sortIdx}
            onChange={(e) => { setSortIdx(Number(e.target.value)); setPage(0) }}
            style={{ ...inputStyle, width: 'auto' }}
            aria-label="Sort teams"
          >
            {SORT_OPTIONS.map((opt, i) => (
              <option key={i} value={i}>{opt.label}</option>
            ))}
          </select>
          <button
            onClick={() => setShowModal(true)}
            style={{
              background: '#B8FF47',
              color: '#0A1A12',
              border: 'none',
              padding: '10px 20px',
              borderRadius: 8,
              fontSize: 14,
              fontWeight: 600,
              cursor: 'pointer',
              whiteSpace: 'nowrap',
            }}
          >
            + New Team
          </button>
        </div>
      </div>

      {/* Grid */}
      {isLoading ? (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 20 }}>
          <SkeletonLoader type="card" count={6} />
        </div>
      ) : teams.length === 0 ? (
        <EmptyState
          title="No teams found"
          message={search ? 'Try a different search term.' : 'Get started by creating your first team.'}
          actionLabel={search ? undefined : '+ New Team'}
          onAction={search ? undefined : () => setShowModal(true)}
        />
      ) : (
        <>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 20 }}>
            {teams.map((t, i) => (
              <TeamCard key={t.id} team={t} delay={i * 60} />
            ))}
          </div>
          <Pagination
            page={page}
            totalPages={data?.totalPages || 1}
            totalElements={data?.totalElements || 0}
            size={9}
            onPageChange={setPage}
          />
        </>
      )}

      {/* New Team Modal */}
      {showModal && (
        <div style={{
          position: 'fixed', inset: 0, zIndex: 900,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.6)' }} onClick={() => setShowModal(false)} />
          <div style={{
            position: 'relative',
            background: '#11291B',
            border: '1px solid #1E422E',
            borderRadius: 16,
            padding: 28,
            width: 420,
            maxWidth: '90vw',
            animation: 'fadeSlideUp 200ms ease-out both',
          }}>
            <h2 style={{ fontSize: 18, fontWeight: 600, margin: '0 0 20px', color: '#fff' }}>Create New Team</h2>

            {formError && (
              <div style={{
                background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)',
                borderRadius: 8, padding: '8px 12px', fontSize: 13, color: '#EF4444', marginBottom: 16,
              }}>
                {formError}
              </div>
            )}

            <div style={{ display: 'flex', flexDirection: 'column', gap: 14, marginBottom: 24 }}>
              <div>
                <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Team Name</label>
                <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="e.g. FC Barcelona" style={inputStyle} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>City</label>
                <input value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} placeholder="e.g. Barcelona" style={inputStyle} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#94A3B8', marginBottom: 4 }}>Budget</label>
                <input type="number" value={form.budget} onChange={(e) => setForm({ ...form, budget: e.target.value })} placeholder="e.g. 500000000" style={inputStyle} />
              </div>
            </div>

            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
              <button onClick={() => setShowModal(false)} disabled={createMutation.isPending} style={{
                padding: '8px 20px', borderRadius: 8, border: '1px solid #1E422E',
                background: 'transparent', color: '#fff', fontSize: 14, fontWeight: 500, cursor: 'pointer',
              }}>Cancel</button>
              <button onClick={handleCreate} disabled={createMutation.isPending} style={{
                padding: '8px 20px', borderRadius: 8, border: 'none',
                background: '#B8FF47', color: '#0A1A12', fontSize: 14, fontWeight: 600,
                cursor: createMutation.isPending ? 'not-allowed' : 'pointer', opacity: createMutation.isPending ? 0.6 : 1,
              }}>
                {createMutation.isPending ? 'Creating…' : 'Create Team'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
