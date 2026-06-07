import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const message =
      err.response?.data?.message || err.message || 'An unexpected error occurred'
    const enhanced = new Error(message)
    enhanced.friendlyMessage = message
    enhanced.status = err.response?.status
    enhanced.response = err.response
    enhanced.code = err.code
    return Promise.reject(enhanced)
  },
)

export default api
