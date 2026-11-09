import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import './assets/main.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)

// 새로고침해도 로그인이 유지되도록, 화면을 그리기 전에 Refresh Token 쿠키로 Access Token 을 다시 받아 온다.
const auth = useAuthStore(pinia)
auth.bootstrap().finally(() => {
  app.use(router)
  app.mount('#app')
})
