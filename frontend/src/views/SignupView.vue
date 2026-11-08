<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../api/auth'
import { errorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'

const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()
const form = ref({ username: '', password: '', passwordConfirm: '', nickname: '' })
const error = ref('')
const submitting = ref(false)

async function submit() {
  error.value = ''
  if (form.value.password !== form.value.passwordConfirm) {
    error.value = '비밀번호가 서로 다릅니다.'
    return
  }
  submitting.value = true
  try {
    await authApi.signup({ username: form.value.username, password: form.value.password, nickname: form.value.nickname })
    await auth.login(form.value.username, form.value.password)
    toast.success('가입을 환영합니다.')
    router.replace('/')
  } catch (e) {
    error.value = errorMessage(e, '회원가입에 실패했습니다.')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <form class="card stack" @submit.prevent="submit">
      <h1>회원가입</h1>
      <label class="field">아이디 (영문 소문자, 숫자, _ 4~20자)
        <input v-model.trim="form.username" class="input" pattern="[a-z0-9_]{4,20}" autocomplete="username" required />
      </label>
      <label class="field">닉네임 (20자 이하)
        <input v-model.trim="form.nickname" class="input" maxlength="20" required />
      </label>
      <label class="field">비밀번호 (8자 이상)
        <input v-model="form.password" class="input" type="password" minlength="8" maxlength="64" autocomplete="new-password" required />
      </label>
      <label class="field">비밀번호 확인
        <input v-model="form.passwordConfirm" class="input" type="password" autocomplete="new-password" required />
      </label>
      <div v-if="error" class="error-text">{{ error }}</div>
      <button class="btn primary block" :disabled="submitting">가입하기</button>
      <div class="small muted">이미 계정이 있나요? <RouterLink to="/login" style="color:var(--accent)">로그인</RouterLink></div>
    </form>
  </div>
</template>

<style scoped>
.auth-page { max-width: 420px; margin: 24px auto; }
</style>
