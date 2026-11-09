import { defineStore } from 'pinia'
import { authApi } from '../api/auth'
import { onRefreshNeeded, setAccessToken } from '../api/http'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    expiresAt: 0
  }),
  getters: {
    isLoggedIn: (state) => !!state.user
  },
  actions: {
    applyToken(res) {
      setAccessToken(res.accessToken)
      this.user = res.user
      this.expiresAt = Date.now() + res.expiresIn * 1000
    },
    clear() {
      setAccessToken(null)
      this.user = null
      this.expiresAt = 0
    },
    async bootstrap() {
      onRefreshNeeded(() => this.refresh())
      try {
        await this.refresh()
      } catch {
        this.clear()
      }
    },
    async refresh() {
      try {
        const res = await authApi.refresh()
        this.applyToken(res)
        return res
      } catch (e) {
        this.clear()
        throw e
      }
    },
    async login(username, password) {
      this.applyToken(await authApi.login({ username, password }))
    },
    async testLogin(username) {
      this.applyToken(await authApi.testLogin(username))
    },
    async logout() {
      try {
        await authApi.logout(true)
      } catch {
        // Access Token 이 이미 만료된 경우에도 Refresh Token 쿠키는 지워야 하므로 토큰 없이 한 번 더 호출한다.
        try { await authApi.logout(false) } catch { /* 무시 */ }
      }
      this.clear()
    }
  }
})
