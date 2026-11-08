import http from './http'

export const broadcastApi = {
  list: (params) => http.get('/broadcasts', { params }).then((r) => r.data),
  mine: (params) => http.get('/broadcasts/mine', { params }).then((r) => r.data),
  get: (id) => http.get(`/broadcasts/${id}`).then((r) => r.data),
  create: (body) => http.post('/broadcasts', body).then((r) => r.data),
  update: (id, body) => http.patch(`/broadcasts/${id}`, body).then((r) => r.data),
  remove: (id) => http.delete(`/broadcasts/${id}`),
  startLive: (id, sourceType) => http.post(`/broadcasts/${id}/live/start`, { sourceType }).then((r) => r.data),
  endLive: (id) => http.post(`/broadcasts/${id}/live/end`).then((r) => r.data),
  liveSession: (id) => http.get(`/broadcasts/${id}/live/session`).then((r) => r.data)
}

export const recordingApi = {
  list: (params) => http.get('/recordings', { params }).then((r) => r.data),
  get: (id) => http.get(`/recordings/${id}`).then((r) => r.data),
  remove: (id) => http.delete(`/recordings/${id}`)
}
