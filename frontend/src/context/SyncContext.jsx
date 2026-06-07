import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { triggerSync, getSyncStatus } from '../api/external'
import { useToast } from './ToastContext'

const SyncContext = createContext(null)

const SYNC_STORAGE_KEY = 'futbix_syncId'
const STORAGE_TIME_KEY = 'futbix_sync_time'
const STORAGE_TTL_MS = 300_000 // 5 min
const POLL_INTERVAL_MS = 5000
const MAX_RETRIES = 60

export function SyncProvider({ children }) {
  const savedSyncId = (() => {
    const id = sessionStorage.getItem(SYNC_STORAGE_KEY)
    const time = sessionStorage.getItem(STORAGE_TIME_KEY)
    if (!id || !time) return null
    return Date.now() - parseInt(time) < STORAGE_TTL_MS ? id : null
  })()
  const [syncId, setSyncId] = useState(savedSyncId)
  const [resumed, setResumed] = useState(!!savedSyncId)
  const [status, setStatus] = useState(savedSyncId ? 'syncing' : 'idle')
  const [progress, setProgress] = useState({ totalTeams: 0, processedTeams: 0, playersCreated: 0, teams: [] })
  const retryCount = useRef(0)
  const syncArgsRef = useRef(null)
  const queryClient = useQueryClient()
  const addToast = useToast()

  const mutation = useMutation({
    mutationFn: ({ leagueIds, season }) => triggerSync(leagueIds, season),
    onSuccess: (data) => {
      retryCount.current = 0
      setResumed(false)
      setSyncId(data.syncId)
      setStatus('syncing')
      setProgress({ totalTeams: 0, processedTeams: 0, playersCreated: 0, teams: [] })
      sessionStorage.setItem(SYNC_STORAGE_KEY, data.syncId)
      sessionStorage.setItem(STORAGE_TIME_KEY, Date.now().toString())
      addToast('Sync started — fetching real data from API-Football...', 'info')
    },
    onError: (err) => {
      setStatus('error')
      addToast(err.friendlyMessage || 'Failed to start sync', 'error')
    },
  })

  useEffect(() => {
    if (!syncId || status !== 'syncing') return

    const interval = setInterval(async () => {
      try {
        const result = await getSyncStatus(syncId)
        retryCount.current = 0

        if (result.status === 'PROCESSING') {
          setProgress({
            totalTeams: result.totalTeams,
            processedTeams: result.processedTeams,
            playersCreated: result.playersCreated,
            totalLeagues: result.totalLeagues,
            processedLeagues: result.processedLeagues,
          })
          return
        }

        clearInterval(interval)
        sessionStorage.removeItem(SYNC_STORAGE_KEY)
        sessionStorage.removeItem(STORAGE_TIME_KEY)
        setSyncId(null)
        setProgress({
          totalTeams: result.totalTeams || 0,
          processedTeams: result.processedTeams || 0,
          playersCreated: result.playersCreated || 0,
          totalLeagues: result.totalLeagues || 0,
          processedLeagues: result.processedLeagues || 0,
          teams: result.teams || [],
        })

        if (result.status === 'SUCCESS') {
          setStatus('success')
          addToast(
            `Sync complete — ${result.playersCreated} players across ${result.processedTeams} teams.`,
            'success',
          )
        } else if (result.status === 'PARTIAL') {
          setStatus('success')
          const errors = result.errors?.slice(0, 2).join('; ')
          addToast(
            `Sync partial: ${result.playersCreated} players. Issues: ${errors || 'check logs'}`,
            'info',
          )
        } else {
          setStatus('error')
          addToast(`Sync failed: ${result.errors?.[0] || 'Unknown error'}`, 'error')
        }

        queryClient.invalidateQueries({ queryKey: ['teams'] })
        queryClient.invalidateQueries({ queryKey: ['players'] })
        queryClient.invalidateQueries({ queryKey: ['squad'] })
        queryClient.invalidateQueries({ queryKey: ['team'] })
      } catch (e) {
        if (e.response?.status === 404) {
          clearInterval(interval)
          sessionStorage.removeItem(SYNC_STORAGE_KEY)
          sessionStorage.removeItem(STORAGE_TIME_KEY)
          setSyncId(null)
          setStatus('error')
          addToast('Sync session lost — backend may have restarted. Try again.', 'error')
          return
        }
        retryCount.current += 1
        if (retryCount.current >= MAX_RETRIES) {
          clearInterval(interval)
          sessionStorage.removeItem(SYNC_STORAGE_KEY)
          sessionStorage.removeItem(STORAGE_TIME_KEY)
          setSyncId(null)
          setStatus('error')
          addToast('Sync timed out after several retries. Please try again.', 'error')
        }
      }
    }, POLL_INTERVAL_MS)

    return () => clearInterval(interval)
  }, [syncId, status, queryClient, addToast])

  const startSync = useCallback((leagueIds, season) => {
    syncArgsRef.current = { leagueIds, season }
    mutation.mutate({ leagueIds, season })
  }, [mutation])

  return (
    <SyncContext.Provider
      value={{
        startSync,
        syncStatus: status,
        isPending: mutation.isPending || status === 'syncing',
        progress,
        resumed,
      }}
    >
      {children}
    </SyncContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useSync() {
  const ctx = useContext(SyncContext)
  if (!ctx) throw new Error('useSync must be used within SyncProvider')
  return ctx
}
