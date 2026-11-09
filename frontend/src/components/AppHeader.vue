<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'

const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()

async function logout() {
  await auth.logout()
  toast.success('로그아웃되었습니다.')
  router.push('/')
}
</script>

<template>
  <header class="header">
    <div class="header-inner">
      <RouterLink to="/" class="logo">
        <span class="dot"></span>LiveBroadcast
      </RouterLink>
      <nav class="nav">
        <RouterLink to="/">방송</RouterLink>
        <RouterLink to="/recordings">다시보기</RouterLink>
        <RouterLink v-if="auth.isLoggedIn" to="/me/broadcasts">내 방송</RouterLink>
      </nav>
      <div class="spacer"></div>
      <template v-if="auth.isLoggedIn">
        <RouterLink to="/broadcasts/new" class="btn primary sm">방송하기</RouterLink>
        <span class="nickname small">{{ auth.user.nickname }}</span>
        <button class="btn ghost sm" @click="logout">로그아웃</button>
      </template>
      <template v-else>
        <RouterLink to="/login" class="btn primary sm">로그인</RouterLink>
      </template>
    </div>
  </header>
</template>

<style scoped>
.header {
  position: sticky; top: 0; z-index: 10;
  background: rgba(14, 15, 18, .92);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--border);
}
.header-inner {
  max-width: 1280px; margin: 0 auto; padding: 10px 16px;
  display: flex; align-items: center; gap: 12px;
}
.logo { display: flex; align-items: center; gap: 8px; font-weight: 800; font-size: 18px; }
.dot { width: 12px; height: 12px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 10px var(--accent); }
.nav { display: flex; gap: 14px; margin-left: 12px; }
.nav a { color: var(--text-sub); font-weight: 600; font-size: 14px; }
.nav a.router-link-exact-active { color: var(--text); }
.nickname { color: var(--text-sub); max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.spacer { flex: 1; }
@media (max-width: 600px) {
  .nav { gap: 10px; margin-left: 4px; }
  .nickname { display: none; }
  .logo { font-size: 0; gap: 0; }
}
</style>
