<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { broadcastApi } from '../api/broadcasts'
import { errorMessage } from '../api/http'
import BroadcastCard from '../components/BroadcastCard.vue'

const tab = ref('LIVE')
const items = ref([])
const loading = ref(false)
const error = ref('')
const page = ref(0)
const totalPages = ref(0)
let timer = null

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: 20 }
    if (tab.value !== 'ALL') params.status = tab.value
    const res = await broadcastApi.list(params)
    items.value = res.content
    totalPages.value = res.totalPages
  } catch (e) {
    error.value = errorMessage(e, '방송 목록을 불러오지 못했습니다.')
  } finally {
    loading.value = false
  }
}

function changeTab(next) {
  tab.value = next
  page.value = 0
  load()
}

function move(delta) {
  page.value += delta
  load()
}

onMounted(() => {
  load()
  // 방송 시작/종료가 목록에 반영되도록 주기적으로 갱신
  timer = setInterval(load, 15000)
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<template>
  <div>
    <div class="tabs">
      <button :class="{ active: tab === 'LIVE' }" @click="changeTab('LIVE')">방송 중</button>
      <button :class="{ active: tab === 'ALL' }" @click="changeTab('ALL')">전체</button>
      <button :class="{ active: tab === 'ENDED' }" @click="changeTab('ENDED')">종료된 방송</button>
    </div>

    <div v-if="error" class="empty">{{ error }}</div>
    <div v-else-if="!loading && !items.length" class="empty">
      {{ tab === 'LIVE' ? '지금 방송 중인 채널이 없습니다.' : '방송이 없습니다.' }}
      <div style="margin-top:12px"><RouterLink to="/broadcasts/new" class="btn primary sm">첫 방송 만들기</RouterLink></div>
    </div>
    <div v-else class="grid">
      <BroadcastCard v-for="b in items" :key="b.id" :broadcast="b" />
    </div>

    <div v-if="totalPages > 1" class="pagination">
      <button class="btn sm" :disabled="page === 0" @click="move(-1)">이전</button>
      <span class="muted small" style="align-self:center">{{ page + 1 }} / {{ totalPages }}</span>
      <button class="btn sm" :disabled="page + 1 >= totalPages" @click="move(1)">다음</button>
    </div>
  </div>
</template>
