import { useAllTeams } from '../hooks/useTeams'
import { useAllPlayers } from '../hooks/usePlayers'
import { useSync } from '../context/SyncContext'
import PlayerCard from '../components/PlayerCard'
import TeamCard from '../components/TeamCard'
import SkeletonLoader from '../components/SkeletonLoader'
import { formatCurrency } from '../utils/formatCurrency'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'

export default function Dashboard() {
  const { data: teamsData, isLoading: teamsLoading } = useAllTeams()
  const { data: playersData, isLoading: playersLoading } = useAllPlayers()
  const { syncStatus, progress, resumed } = useSync()

  const teams = teamsData?.content || []
  const players = playersData?.content || []

  const totalTeams = teams.length
  const totalPlayers = players.length
  const highestBudget = teams.length ? Math.max(...teams.map((t) => t.budget || 0)) : 0
  const highestBudgetTeam = teams.find((t) => t.budget === highestBudget)
  const mostValuable = players.length ? Math.max(...players.map((p) => p.valueMarket || 0)) : 0
  const mostValuablePlayer = players.find((p) => p.valueMarket === mostValuable)

  const top5Players = [...players].sort((a, b) => (b.valueMarket || 0) - (a.valueMarket || 0)).slice(0, 5)
  const recentTeams = [...teams].sort((a, b) => {
    if (!a.createdAt) return 1
    if (!b.createdAt) return -1
    return new Date(b.createdAt) - new Date(a.createdAt)
  }).slice(0, 5)

  const chartData = [...teams]
    .sort((a, b) => (b.budget || 0) - (a.budget || 0))
    .slice(0, 8)
    .map((t) => ({
      name: t.name.charAt(0).toUpperCase() + t.name.slice(1),
      budget: t.budget || 0,
    }))

  const MetricCard = ({ label, value, sub }) => (
    <div className="animate-fade-slide-up" style={{
      background: '#11291B',
      border: '1px solid #1E422E',
      borderRadius: 12,
      padding: 20,
    }}>
      <div style={{ fontSize: 12, color: '#64748B', fontWeight: 500, marginBottom: 6, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
        {label}
      </div>
      <div style={{ fontSize: 28, fontWeight: 700, fontFamily: "'Oswald', sans-serif", color: '#fff', lineHeight: 1.1 }}>
        {value}
      </div>
      {sub && (
        <div style={{ fontSize: 13, color: '#94A3B8', marginTop: 4 }}>{sub}</div>
      )}
    </div>
  )

  if (teamsLoading || playersLoading) {
    return (
      <div style={{ padding: 32, maxWidth: 1280, margin: '0 auto' }}>
        <h1 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24, fontFamily: "'Oswald', sans-serif" }}>
          Dashboard
        </h1>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 32 }}>
          <SkeletonLoader type="metric" count={4} />
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 20 }}>
          <SkeletonLoader type="card" count={3} />
        </div>
      </div>
    )
  }

  return (
    <div style={{ padding: 32, maxWidth: 1280, margin: '0 auto' }}>
      <h1 style={{ fontSize: 24, fontWeight: 700, marginBottom: 24, fontFamily: "'Oswald', sans-serif", letterSpacing: '0.02em' }}>
        Dashboard
      </h1>

      {/* Sync Progress */}
      {syncStatus === 'syncing' && (
        <div style={{
          background: '#11291B',
          border: '1px solid #1E422E',
          borderRadius: 12,
          padding: 20,
          marginBottom: 20,
          display: 'flex',
          alignItems: 'center',
          gap: 16,
        }}>
          <div style={{
            width: 8,
            height: 8,
            borderRadius: '50%',
            background: '#22C55E',
            animation: 'pulse 1.5s ease-in-out infinite',
          }} />
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 14, fontWeight: 600, color: '#fff', fontFamily: "'Oswald', sans-serif", letterSpacing: '0.03em' }}>
              Syncing external data...
            </div>
            <div style={{ fontSize: 13, color: '#94A3B8', marginTop: 2 }}>
              {progress.totalTeams > 0
                ? `Team ${progress.processedTeams} of ${progress.totalTeams} • ${progress.playersCreated} players created`
                : resumed ? 'Resuming previous sync...' : 'Starting sync...'}
            </div>
          </div>
        </div>
      )}

      {/* Hero Metrics */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
        gap: 16,
        marginBottom: 36,
      }}>
        <MetricCard label="Total Teams" value={totalTeams} />
        <MetricCard label="Total Players" value={totalPlayers} />
        <MetricCard
          label="Highest Budget"
          value={formatCurrency(highestBudget)}
          sub={highestBudgetTeam ? highestBudgetTeam.name.charAt(0).toUpperCase() + highestBudgetTeam.name.slice(1) : ''}
        />
        <MetricCard
          label="Most Valuable Player"
          value={formatCurrency(mostValuable)}
          sub={mostValuablePlayer ? mostValuablePlayer.name : ''}
        />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 32, marginBottom: 36 }}>
        {/* Top 5 Players */}
        <div>
          <h2 style={{ fontSize: 16, fontWeight: 600, color: '#fff', marginBottom: 14 }}>
            Top 5 Players by Market Value
          </h2>
          {top5Players.length === 0 ? (
            <p style={{ color: '#64748B', fontSize: 14 }}>No players yet.</p>
          ) : (
            <div style={{ display: 'flex', gap: 16, overflowX: 'auto', paddingBottom: 8 }}>
              {top5Players.map((p, i) => (
                <div key={p.id} style={{ minWidth: 260 }}>
                  <PlayerCard player={p} delay={i * 80} />
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Recent Teams */}
        <div>
          <h2 style={{ fontSize: 16, fontWeight: 600, color: '#fff', marginBottom: 14 }}>
            Recently Added Teams
          </h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {recentTeams.length === 0 ? (
              <p style={{ color: '#64748B', fontSize: 14 }}>No teams yet.</p>
            ) : (
              recentTeams.map((t, i) => (
                <TeamCard key={t.id} team={t} delay={i * 60} />
              ))
            )}
          </div>
        </div>
      </div>

      {/* Budget Chart */}
      {chartData.length > 0 && (
        <div>
          <h2 style={{ fontSize: 16, fontWeight: 600, color: '#fff', marginBottom: 14 }}>
            Budget Overview — Top 8 Teams
          </h2>
          <div style={{
            background: '#11291B',
            border: '1px solid #1E422E',
            borderRadius: 12,
            padding: 24,
          }}>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={chartData} layout="vertical" margin={{ left: 80, right: 24, top: 8, bottom: 8 }}>
                <XAxis type="number" tick={{ fill: '#64748B', fontSize: 12 }} axisLine={false} tickLine={false} />
                <YAxis type="category" dataKey="name" tick={{ fill: '#94A3B8', fontSize: 13, fontWeight: 500 }} axisLine={false} tickLine={false} />
                <Tooltip
                  contentStyle={{ background: '#11291B', border: '1px solid #1E422E', borderRadius: 8, fontSize: 13 }}
                  formatter={(v) => formatCurrency(v)}
                  itemStyle={{ color: '#fff' }}
                  labelStyle={{ color: '#94A3B8' }}
                />
                <Bar dataKey="budget" fill="#B8FF47" radius={[0, 4, 4, 0]} barSize={20} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}
    </div>
  )
}
