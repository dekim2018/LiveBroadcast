<script setup>
import { onMounted, ref } from 'vue'
import { recordingApi } from '../api/broadcasts'
import { errorMessage } from '../api/http'
import StatusBadge from '../components/StatusBadge.vue'
import { formatDate, formatDuration } from '../lib/format'

const items = ref([])
const loading = ref(true)
const error = ref('')
const page = ref(0)
const totalPages = ref(0)

async function load() {
  loading.value = true
  try {
    const res = await recordingApi.list({ page: page.value, size: 20 })
    items.value = res.content
    totalPages.value = res.totalPages
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

function move(delta) {
  page.value += delta
  load()
}

onMounted(load)
</script>

<template>
  <div class="stack">
    <h1>다시보기</h1>
    <div v-if="error" class="empty">{{ error }}</div>
    <div v-else-if="!loading && !items.length" class="empty">아직 녹화된 방송이 없습니다. 방송을 하면 자동으로 녹화됩니다.</div>
    <div v-else class="grid">
      <RouterLink v-for="r in items" :key="r.id" :to="`/recordings/${r.id}`" class="card stack rec">
        <div class="row">
          <StatusBadge :status="r.status === 'READY' ? 'VOD' : r.status" />
          <span class="spacer"></span>
          <span class="small muted">{{ r.status === 'READY' ? formatDuration(r.durationSeconds) : '녹화 중' }}</span>
        </div>
        <div class="title">{{ r.title }}</div>
        <div class="small muted">{{ r.ownerNickname }}, {{ formatDate(r.startedAt) }}</div>
      </RouterLink>
    </div>
    <div v-if="totalPages > 1" class="pagination">
      <button class="btn sm" :disabled="page === 0" @click="move(-1)">이전</button>
      <button class="btn sm" :disabled="page + 1 >= totalPages" @click="move(1)">다음</button>
    </div>
  </div>
</template>

<style scoped>
.rec { gap: 8px; }
.title { font-weight: 700; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
