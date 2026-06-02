import api from './axiosInstance'

export const fetchTeams = (page = 0, size = 9, sortBy = 'name', sortDir = 'asc', search) =>
  api.get('/teams', { params: { page, size, sortBy, sortDir, search: search || undefined } }).then((r) => r.data)

export const fetchTeamById = (id) =>
  api.get(`/teams/${id}`).then((r) => r.data)

export const fetchTeamByName = (name) =>
  api.get(`/teams/name/${encodeURIComponent(name)}`).then((r) => r.data)

export const fetchTeamSquad = (name) =>
  api.get(`/teams/${encodeURIComponent(name)}/squad`).then((r) => r.data)

export const fetchTeamValue = (name) =>
  api.get(`/teams/${encodeURIComponent(name)}/value`).then((r) => r.data)

export const createTeam = (data) =>
  api.post('/teams', data).then((r) => r.data)

export const updateTeam = (id, data) =>
  api.put(`/teams/${id}`, data).then((r) => r.data)

export const deleteTeam = (id) =>
  api.delete(`/teams/${id}`).then((r) => r.data)
