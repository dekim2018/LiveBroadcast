<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { broadcastApi } from '../api/broadcasts'
import { errorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()
const isEdit = computed(() => !!route.params.id)
const form = ref({ title: '', description: '' })
const error = ref('')
const submitting = ref(false)

onMounted(async () => {
  if (!isEdit.value) return
  try {
    const b = await broadcastApi.get(route.params.id)
    if (b.ownerId !== auth.user?.id) {
      error.value = '방송 소유자만 수정할 수 있습니다.'
    }
    form.value = { title: b.title, description: b.description }
  } catch (e) {
    error.value = errorMessage(e)
  }
})

async function submit() {
  error.value = ''
  submitting.value = true
  try {
    if (isEdit.value) {
      await broadcastApi.update(route.params.id, form.value)
      toast.success('방송 정보를 수정했습니다.')
      router.replace(`/broadcasts/${route.params.id}`)
    } else {
      const created = await broadcastApi.create(form.value)
      toast.success('방송을 만들었습니다. 송출 방식을 선택해 방송을 시작하세요.')
      router.replace(`/studio/${created.id}`)
    }
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="form-page">
    <form class="card stack" @submit.prevent="submit">
      <h1>{{ isEdit ? '방송 정보 수정' : '새 방송 만들기' }}</h1>
      <label class="field">방송 제목
        <input v-model="form.title" class="input" maxlength="100" required placeholder="예) 오늘은 백엔드 라이브 코딩" />
      </label>
      <label class="field">방송 설명
        <textarea v-model="form.description" class="input" maxlength="1000" placeholder="시청자에게 보여줄 설명"></textarea>
      </label>
      <div v-if="error" class="error-text">{{ error }}</div>
      <div class="row">
        <button class="btn primary" :disabled="submitting">{{ isEdit ? '저장' : '만들고 스튜디오로 이동' }}</button>
        <button type="button" class="btn" @click="router.back()">취소</button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.form-page { max-width: 640px; margin: 0 auto; }
</style>
