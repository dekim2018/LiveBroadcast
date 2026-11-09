<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { broadcastApi, recordingApi } from '../api/broadcasts'
import { errorMessage } from '../api/http'
import LivePlayer from '../components/LivePlayer.vue'
import ChatPanel from '../components/ChatPanel.vue'
import DonationPanel from '../components/DonationPanel.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { useLiveEvents } from '../composables/useLiveEvents'
import { elapsedSince, formatDate, formatDuration } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()
const broadcastId = route.params.id
const events = useLiveEvents(broadcastId)

const broadcast = ref(null)
const error = ref('')
const recordings = ref([])
const tick = ref(0)
let pollTimer = null
let tickTimer = null

const isOwner = computed(() => broadcast.value && auth.user?.id === broadcast.value.ownerId)
const isLive = computed(() => broadcast.value?.status === 'LIVE')

async function load() {
  try {
    broadcast.value = await broadcastApi.get(broadcastId)
    error.value = ''
  } catch (e) {
    error.value = e?.response?.status === 404 ? '존재하지 않거나 삭제된 방송입니다.' : errorMessage(e)
  }
}

async function loadRecordings() {
  try {
    recordings.value = (await recordingApi.list({ broadcastId, size: 5 })).content
  } catch {
    recordings.value = []
  }
}

async function remove() {
  if (!confirm('방송을 삭제할까요?')) return
  try {
    await broadcastApi.remove(broadcastId)
    toast.success('삭제했습니다.')
    router.replace('/me/broadcasts')
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

onMounted(async () => {
  await load()
  loadRecordings()
  events.connect()
  // 방송 시작/종료 상태 변화를 감지해서 플레이어를 켜고 끈다.
  pollTimer = setInterval(async () => {
    const wasLive = isLive.value
    await load()
    if (wasLive && !isLive.value) {
      toast.show('방송이 종료되었습니다.')
      loadRecordings()
    }
  }, 5000)
  tickTimer = setInterval(() => { tick.value++ }, 1000)
})

onBeforeUnmount(() => {
  clearInterval(pollTimer)
  clearInterval(tickTimer)
})
</script>

<template>
  <div v-if="error" class="empty">{{ error }}</div>
  <div v-else-if="broadcast" class="layout-watch">
    <div class="stack">
      <LivePlayer :url="broadcast.watchUrl" :active="isLive">
        <template #overlay="{ state }">
          <template v-if="!isLive">
            <div v-if="broadcast.status === 'ENDED'">방송이 종료되었습니다.</div>
            <div v-else>아직 방송이 시작되지 않았습니다.</div>
            <RouterLink v-if="recordings.length" :to="`/recordings/${recordings[0].id}`" class="btn primary sm">다시보기</RouterLink>
          </template>
          <div v-else-if="state === 'connecting'">연결 중...</div>
          <div v-else>방송자의 송출을 기다리는 중...</div>
        </template>
      </LivePlayer>

      <div class="card stack">
        <div class="row">
          <StatusBadge :status="broadcast.status" :online="broadcast.streamOnline" />
          <h1 style="margin:0; font-size:20px">{{ broadcast.title }}</h1>
        </div>
        <div class="row small muted">
          <strong style="color:var(--text)">{{ broadcast.ownerNickname }}</strong>
          <span v-if="isLive" :data-tick="tick">방송 시간 {{ elapsedSince(broadcast.startedAt) }}</span>
          <span v-else-if="broadcast.endedAt">종료 {{ formatDate(broadcast.endedAt) }}</span>
          <span v-if="broadcast.sourceType">{{ broadcast.sourceType === 'PC_SCREEN' ? 'PC 화면 송출' : '모바일 카메라 송출' }}</span>
        </div>
        <p v-if="broadcast.description" class="desc">{{ broadcast.description }}</p>
        <div v-if="isOwner" class="row">
          <RouterLink :to="`/studio/${broadcast.id}`" class="btn primary sm">스튜디오(송출 제어)</RouterLink>
          <RouterLink :to="`/broadcasts/${broadcast.id}/edit`" class="btn sm">정보 수정</RouterLink>
          <button class="btn sm danger" :disabled="isLive" @click="remove">삭제</button>
        </div>
      </div>

      <div v-if="recordings.length" class="card stack">
        <h3 style="margin:0">이 방송의 다시보기</h3>
        <RouterLink v-for="r in recordings" :key="r.id" :to="`/recordings/${r.id}`" class="row small rec">
          <StatusBadge :status="r.status === 'READY' ? 'VOD' : r.status" />
          <span>{{ formatDate(r.startedAt) }}</span>
          <span class="muted">{{ r.status === 'READY' ? formatDuration(r.durationSeconds) : '녹화 중' }}</span>
        </RouterLink>
      </div>
    </div>

    <div class="stack">
      <DonationPanel :broadcast="broadcast" :events="events" />
      <ChatPanel :broadcast-id="broadcastId" :events="events" height="360px" />
    </div>
  </div>
</template>

<style scoped>
.desc { white-space: pre-wrap; margin: 0; color: #cfd3da; }
.rec { padding: 6px 0; border-top: 1px solid var(--border); }
</style>
