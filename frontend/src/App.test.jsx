import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { ToastProvider } from './context/ToastContext'
import { SyncProvider } from './context/SyncContext'
import App from './App'

vi.mock('./pages/Dashboard', () => ({
  default: () => <h1>Dashboard Page</h1>,
}))
vi.mock('./pages/Teams', () => ({
  default: () => <h1>Teams Page</h1>,
}))
vi.mock('./pages/TeamDetail', () => ({
  default: () => <h1>Team Detail Page</h1>,
}))
vi.mock('./pages/Players', () => ({
  default: () => <h1>Players Page</h1>,
}))
vi.mock('./pages/PlayerProfile', () => ({
  default: () => <h1>Player Profile Page</h1>,
}))
vi.mock('./pages/NotFound', () => ({
  default: () => <h1>Not Found Page</h1>,
}))

function renderApp(initialEntry = '/') {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <ToastProvider>
          <SyncProvider>
            <App />
          </SyncProvider>
        </ToastProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('App routing', () => {
  it('shows a lazy route fallback while loading the dashboard route', async () => {
    renderApp('/dashboard')

    expect(screen.getByText('Cargando...')).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Dashboard Page' })).toBeInTheDocument()
  })

  it('redirects the root path to the dashboard route', async () => {
    renderApp('/')

    expect(await screen.findByRole('heading', { name: 'Dashboard Page' })).toBeInTheDocument()
  })
})
