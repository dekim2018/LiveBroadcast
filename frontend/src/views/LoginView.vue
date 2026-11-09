<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '../api/auth'
import { errorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'

const auth = useAuthStore()
const toast = useToastStore()
const route = useRoute()
const router = useRouter()

const username = ref('')
const password = ref('')
const error = ref('')
const submitting = ref(false)
const testAccounts = ref([])

function goNext() {
  const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/') ? route.query.redirect : '/'
  router.replace(redirect)
}

async function submit() {
  error.value = ''
  submitting.value = true
  try {
    await auth.login(username.value, password.value)
    toast.success(`${auth.user.nickname}님 환영합니다.`)
    goNext()
  } catch (e) {
    error.value = errorMessage(e, '로그인에 실패했습니다.')
  } finally {
    submitting.value = false
  }
}

async function quickLogin(account) {
  submitting.value = true
  try {
    await auth.testLogin(account.username)
    toast.success(`테스트 계정 ${account.nickname}(으)로 로그인했습니다.`)
    goNext()
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try { testAccounts.value = await authApi.testAccounts() } catch { testAccounts.value = [] }
})
</script>

<template>
  <div class="auth-page">
    <form class="card stack" @submit.prevent="submit">
      <h1>로그인</h1>
      <label class="field">아이디
        <input v-model.trim="username" class="input" autocomplete="username" required />
      </label>
      <label class="field">비밀번호
        <input v-model="password" class="input" type="password" autocomplete="current-password" required />
      </label>
      <div v-if="error" class="error-text">{{ error }}</div>
      <button class="btn primary block" :disabled="submitting">로그인</button>
      <div class="small muted">계정이 없나요? <RouterLink to="/signup" class="accent">회원가입</RouterLink></div>
    </form>

    <div v-if="testAccounts.length" class="card stack">
      <h3>평가용 테스트 계정으로 바로 로그인</h3>
      <p class="small muted" style="margin:0">
        방송 소유자와 다른 사용자의 권한 차이를 확인할 수 있도록 두 개의 계정을 준비했습니다.
      </p>
      <div class="row">
        <button v-for="a in testAccounts" :key="a.username" class="btn" :disabled="submitting" @click="quickLogin(a)">
          {{ a.nickname }} ({{ a.username }})
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page { max-width: 420px; margin: 24px auto; display: flex; flex-direction: column; gap: 16px; }
.accent { color: var(--accent); }
</style>
