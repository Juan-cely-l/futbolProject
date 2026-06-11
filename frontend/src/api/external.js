import api from './axiosInstance'

export const triggerSync = (leagueIds, season, maxTeams) =>
  api.post('/sync', { leagueIds, season, maxTeams }).then(r => r.data)

export const getSyncStatus = (syncId) =>
  api.get(`/sync/${syncId}`).then(r => r.data)

export const fetchSyncLeagues = () =>
  api.get('/sync/leagues').then(r => r.data)

export const fetchSyncSeasons = () =>
  api.get('/sync/seasons').then(r => r.data)
