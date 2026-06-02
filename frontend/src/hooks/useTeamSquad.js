import { useQuery } from '@tanstack/react-query'
import { fetchTeamSquad, fetchTeamValue } from '../api/teams'
import { fetchPlayerEfficiency } from '../api/players'

export function useTeamSquad(teamName) {
  return useQuery({
    queryKey: ['squad', teamName],
    queryFn: () => fetchTeamSquad(teamName),
    enabled: !!teamName,
  })
}

export function useTeamValue(teamName) {
  return useQuery({
    queryKey: ['teamValue', teamName],
    queryFn: () => fetchTeamValue(teamName),
    enabled: !!teamName,
  })
}

export function usePlayerEfficiency(id) {
  return useQuery({
    queryKey: ['efficiency', id],
    queryFn: () => fetchPlayerEfficiency(id),
    enabled: !!id,
  })
}
