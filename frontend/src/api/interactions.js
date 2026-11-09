import http from './http'

export const commentApi = {
  list: (broadcastId, params) => http.get(`/broadcasts/${broadcastId}/comments`, { params }).then((r) => r.data),
  write: (broadcastId, content) => http.post(`/broadcasts/${broadcastId}/comments`, { content }).then((r) => r.data),
  edit: (commentId, content) => http.patch(`/comments/${commentId}`, { content }).then((r) => r.data),
  remove: (commentId) => http.delete(`/comments/${commentId}`)
}

export const donationApi = {
  config: () => http.get('/donations/config').then((r) => r.data),
  createOrder: (body) => http.post('/donations/orders', body).then((r) => r.data),
  confirm: (body) => http.post('/donations/confirm', body).then((r) => r.data),
  cancel: (orderId, reason) => http.post(`/donations/orders/${orderId}/cancel`, null, { params: { reason } }),
  list: (broadcastId, params) => http.get(`/broadcasts/${broadcastId}/donations`, { params }).then((r) => r.data),
  summary: (broadcastId) => http.get(`/broadcasts/${broadcastId}/donations/summary`).then((r) => r.data)
}
