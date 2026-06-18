import { lazy, Suspense } from 'react'
import { Routes, Route, Navigate, useNavigate } from 'react-router-dom'
import Navbar from './components/Navbar'

const Dashboard = lazy(() => import('./pages/Dashboard'))
const Teams = lazy(() => import('./pages/Teams'))
const TeamDetail = lazy(() => import('./pages/TeamDetail'))
const Players = lazy(() => import('./pages/Players'))
const PlayerProfile = lazy(() => import('./pages/PlayerProfile'))
const NotFound = lazy(() => import('./pages/NotFound'))

export default function App() {
  const navigate = useNavigate()

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Navbar onNewClick={() => navigate('/teams')} />
      <main style={{ flex: 1 }}>
        <Suspense fallback={<div style={{ padding: 24 }}>Cargando...</div>}>
          <Routes>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/teams" element={<Teams />} />
            <Route path="/teams/:name" element={<TeamDetail />} />
            <Route path="/players" element={<Players />} />
            <Route path="/players/:id" element={<PlayerProfile />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </Suspense>
      </main>
    </div>
  )
}
