import { useNavigate } from 'react-router-dom'
import EmptyState from '../components/EmptyState'

export default function NotFound() {
  const navigate = useNavigate()
  return (
    <EmptyState
      title="404 — Page Not Found"
      message="The page you're looking for doesn't exist or has been moved."
      actionLabel="Back to Dashboard"
      onAction={() => navigate('/dashboard')}
    />
  )
}
