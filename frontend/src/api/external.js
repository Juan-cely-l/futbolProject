import api from './axiosInstance'

export const triggerSync = (leagueIds, season) =>
  api.post('/sync', { leagueIds, season }).then(r => r.data)

export const getSyncStatus = (syncId) =>
  api.get(`/sync/${syncId}`).then(r => r.data)

export const fetchSyncLeagues = () =>
  api.get('/sync/leagues').then(r => r.data)

export const fetchSyncSeasons = () =>
  api.get('/sync/seasons').then(r => r.data)
