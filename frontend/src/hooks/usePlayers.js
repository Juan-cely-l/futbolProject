import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { fetchPlayers, fetchPlayerById, createPlayer, updatePlayer, deletePlayer } from '../api/players'

export function usePlayers(page = 0, size = 15, sortBy = 'name', sortDir = 'asc', search) {
  return useQuery({
    queryKey: ['players', page, size, sortBy, sortDir, search],
    queryFn: () => fetchPlayers(page, size, sortBy, sortDir, search),
  })
}

export function useAllPlayers() {
  return useQuery({
    queryKey: ['players', 'all'],
    queryFn: () => fetchPlayers(0, 1000, 'name', 'asc'),
  })
}

export function usePlayerById(id) {
  return useQuery({
    queryKey: ['player', id],
    queryFn: () => fetchPlayerById(id),
    enabled: !!id,
  })
}

export function useCreatePlayer() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: createPlayer,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['players'] })
      qc.invalidateQueries({ queryKey: ['squad'] })
    },
  })
}

export function useUpdatePlayer() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }) => updatePlayer(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['players'] })
      qc.invalidateQueries({ queryKey: ['player'] })
      qc.invalidateQueries({ queryKey: ['squad'] })
    },
  })
}

export function useDeletePlayer() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: deletePlayer,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['players'] })
      qc.invalidateQueries({ queryKey: ['player'] })
      qc.invalidateQueries({ queryKey: ['squad'] })
    },
  })
}
