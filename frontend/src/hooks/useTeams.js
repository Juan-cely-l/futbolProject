import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { fetchTeams, fetchTeamByName, createTeam, updateTeam, deleteTeam } from '../api/teams'

export function useTeams(page = 0, size = 9, sortBy = 'name', sortDir = 'asc', search) {
  return useQuery({
    queryKey: ['teams', page, size, sortBy, sortDir, search],
    queryFn: () => fetchTeams(page, size, sortBy, sortDir, search),
  })
}

export function useAllTeams() {
  return useQuery({
    queryKey: ['teams', 'all'],
    queryFn: () => fetchTeams(0, 100, 'name', 'asc'),
  })
}

export function useTeamByName(name) {
  return useQuery({
    queryKey: ['team', name],
    queryFn: () => fetchTeamByName(name),
    enabled: !!name,
  })
}

export function useCreateTeam() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: createTeam,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['teams'] })
    },
  })
}

export function useUpdateTeam() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }) => updateTeam(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['teams'] })
    },
  })
}

export function useDeleteTeam() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: deleteTeam,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['teams'] })
    },
  })
}
