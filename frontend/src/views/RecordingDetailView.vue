<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { recordingApi } from '../api/broadcasts'
import { errorMessage } from '../api/http'
import StatusBadge from '../components/StatusBadge.vue'
import { formatDate, formatDuration } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'

/**
 * 녹화 영상은 MediaMTX 가 일정 길이의 세그먼트로 저장한다.
 * 서버가 내려준 구간 목록을 순서대로 이어서 재생한다.
 */
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()

const recording = ref(null)
const error = ref('')
const current = ref(0)
const videoEl = ref(null)

const segments = computed(() => recording.value?.segments || [])
const currentUrl = computed(() => segments.value[current.value]?.url)
const isOwner = computed(() => recording.value && auth.user?.id === recording.value.ownerId)

async function load() {
  try {
    recording.value = await recordingApi.get(route.params.id)
  } catch (e) {
    error.value = e?.response?.status === 404 ? '녹화 영상을 찾을 수 없습니다.' : errorMessage(e)
  }
}

function playSegment(index) {
  current.value = index
  requestAnimationFrame(() => videoEl.value?.play().catch(() => {}))
}

function onEnded() {
  if (current.value + 1 < segments.value.length) playSegment(current.value + 1)
}

async function remove() {
  if (!confirm('녹화 영상을 삭제할까요?')) return
  try {
    await recordingApi.remove(recording.value.id)
    toast.success('삭제했습니다.')
    router.replace('/recordings')
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

onMounted(load)
</script>

<template>
  <div v-if="error" class="empty">{{ error }}</div>
  <div v-else-if="recording" class="stack">
    <div class="video-box">
      <video v-if="currentUrl" ref="videoEl" :src="currentUrl" controls playsinline @ended="onEnded"></video>
      <div v-else class="video-overlay">
        <div v-if="recording.status === 'RECORDING'">아직 방송 중이라 녹화가 진행되고 있습니다.</div>
        <div v-else>재생할 수 있는 녹화 파일이 없습니다.</div>
      </div>
    </div>
    <div class="card stack">
      <div class="row">
        <StatusBadge :status="recording.status === 'READY' ? 'VOD' : recording.status" />
        <h1 style="margin:0; font-size:20px">{{ recording.title }}</h1>
      </div>
      <div class="small muted">
        {{ recording.ownerNickname }}, {{ formatDate(recording.startedAt) }} ~ {{ formatDate(recording.endedAt) }},
        길이 {{ formatDuration(recording.durationSeconds) }}
      </div>
      <div class="small muted">저장 위치: <code>{{ recording.storageLocation }}</code></div>
      <div v-if="segments.length > 1" class="row">
        <button v-for="(s, i) in segments" :key="s.start" class="btn sm" :class="{ primary: i === current }" @click="playSegment(i)">
          구간 {{ i + 1 }} ({{ formatDuration(s.durationSeconds) }})
        </button>
      </div>
      <div class="row">
        <RouterLink :to="`/broadcasts/${recording.broadcastId}`" class="btn sm">방송 페이지</RouterLink>
        <button v-if="isOwner" class="btn sm danger" @click="remove">녹화 삭제</button>
      </div>
    </div>
  </div>
</template>
