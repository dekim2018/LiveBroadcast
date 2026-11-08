import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 로컬 개발 시 운영과 같은 경로 구조를 쓰기 위해 프록시를 둔다.
//   /api      -> Spring Cloud Gateway
//   /media    -> MediaMTX WebRTC(WHIP/WHEP)
//   /playback -> MediaMTX 녹화 재생 서버
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    host: true,
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/media': {
        target: 'http://localhost:8889',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/media/, '')
      },
      '/playback': {
        target: 'http://localhost:9996',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/playback/, '')
      }
    }
  }
})
