import axios from 'axios'

/**
 * 모든 API 호출은 같은 도메인의 /api 로 보낸다(Caddy -> Gateway -> 각 서비스).
 * Access Token 은 메모리에만 두고, 만료되면 HttpOnly 쿠키의 Refresh Token 으로 한 번 갱신한 뒤 재시도한다.
 */
const http = axios.create({ baseURL: '/api', withCredentials: true, timeout: 15000 })

let accessToken = null
let refreshHandler = null
let refreshing = null

export function setAccessToken(token) {
  accessToken = token
}

export function getAccessToken() {
  return accessToken
}

export function onRefreshNeeded(handler) {
  refreshHandler = handler
}

http.interceptors.request.use((config) => {
  if (accessToken && !config.skipAuth) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

http.interceptors.response.use(
  (res) => res,
  async (error) => {
    const { config, response } = error
    const isAuthCall = config?.url?.startsWith('/auth/refresh') || config?.url?.startsWith('/auth/login')
    if (response?.status === 401 && config && !config._retried && !isAuthCall && refreshHandler) {
      config._retried = true
      try {
        refreshing = refreshing || refreshHandler()
        await refreshing
      } catch {
        // 갱신 실패: 비로그인 상태로 전환. 공개 API(GET)는 토큰 없이 한 번 더 시도한다.
        if ((config.method || 'get').toLowerCase() === 'get') {
          config.skipAuth = true
          delete config.headers.Authorization
          return http(config)
        }
        return Promise.reject(error)
      } finally {
        refreshing = null
      }
      return http(config)
    }
    return Promise.reject(error)
  }
)

export function errorMessage(error, fallback = '요청을 처리하지 못했습니다.') {
  return error?.response?.data?.message || error?.message || fallback
}

export default http
