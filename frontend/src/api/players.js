import api from './axiosInstance'

export const fetchPlayers = (page = 0, size = 15, sortBy = 'name', sortDir = 'asc', search) =>
  api.get('/players', { params: { page, size, sortBy, sortDir, search: search || undefined } }).then((r) => r.data)

export const fetchPlayerById = (id) =>
  api.get(`/players/${id}`).then((r) => r.data)

export const fetchPlayerEfficiency = (id) =>
  api.get(`/players/efficiency/${id}`).then((r) => r.data)

export const createPlayer = (data) =>
  api.post('/players', data).then((r) => r.data)

export const updatePlayer = (id, data) =>
  api.put(`/players/${id}`, data).then((r) => r.data)

export const deletePlayer = (id) =>
  api.delete(`/players/${id}`).then((r) => r.data)
