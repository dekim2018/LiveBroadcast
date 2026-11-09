import http from './http'

export const authApi = {
  signup: (body) => http.post('/auth/signup', body).then((r) => r.data),
  login: (body) => http.post('/auth/login', body).then((r) => r.data),
  testLogin: (username) => http.post('/auth/test-login', { username }).then((r) => r.data),
  testAccounts: () => http.get('/auth/test-accounts', { skipAuth: true }).then((r) => r.data),
  refresh: () => http.post('/auth/refresh', null, { skipAuth: true }).then((r) => r.data),
  logout: (withToken = true) => http.post('/auth/logout', null, { skipAuth: !withToken, _retried: true }),
  me: () => http.get('/auth/me').then((r) => r.data)
}
