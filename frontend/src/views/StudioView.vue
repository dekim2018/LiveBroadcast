<script setup>
import { computed, onBeforeUnmount, onMounted, ref, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { broadcastApi } from '../api/broadcasts'
import { errorMessage } from '../api/http'
import StatusBadge from '../components/StatusBadge.vue'
import ChatPanel from '../components/ChatPanel.vue'
import { useLiveEvents } from '../composables/useLiveEvents'
import { capturePcScreen, captureCamera, describeMediaError, isMobileDevice, supportsScreenShare } from '../lib/mediaCapture'
import { startWhipPublish } from '../lib/whip'
import { elapsedSince } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'

/**
 * 방송자 전용 스튜디오.
 * 1) 송출 방식 선택(PC 화면 공유 / 모바일 카메라) -> 2) 장치 준비(미리보기) -> 3) 방송 시작(WHIP 송출) -> 4) 방송 종료
 */
const route = useRoute()
const auth = useAuthStore()
const toast = useToastStore()
const broadcastId = route.params.id
const events = useLiveEvents(broadcastId)

const broadcast = ref(null)
const loadError = ref('')
const sourceType = ref(supportsScreenShare() ? 'PC_SCREEN' : 'MOBILE_CAMERA')
const capture = ref(null)
const publisher = ref(null)
const connectionState = ref('new')
const busy = ref(false)
const micMuted = ref(false)
const systemMuted = ref(false)
const previewEl = ref(null)
const tick = ref(0)
let pollTimer = null
let tickTimer = null

const isOwner = computed(() => broadcast.value && auth.user?.id === broadcast.value.ownerId)
const isLive = computed(() => broadcast.value?.status === 'LIVE')
const canScreenShare = supportsScreenShare()
const mobile = isMobileDevice()
const watchLink = computed(() => `${window.location.origin}/broadcasts/${broadcastId}`)

async function loadBroadcast() {
  try {
    broadcast.value = await broadcastApi.get(broadcastId)
  } catch (e) {
    loadError.value = errorMessage(e, '방송을 불러오지 못했습니다.')
  }
}

async function attachPreview() {
  await nextTick()
  if (previewEl.value && capture.value) {
    previewEl.value.srcObject = capture.value.preview
    previewEl.value.play().catch(() => {})
  }
}

/** 장치 준비: 반드시 사용자 클릭으로 호출되어야 브라우저가 화면 공유/카메라 권한 창을 띄운다. */
async function prepareDevice() {
  releaseDevice()
  busy.value = true
  try {
    capture.value = sourceType.value === 'PC_SCREEN'
      ? await capturePcScreen({ withMic: true })
      : await captureCamera({ facingMode: 'user' })
    capture.value.onEnded(() => {
      toast.show('화면 공유 또는 카메라가 중지되어 방송을 종료합니다.')
      if (isLive.value) endLive()
      else releaseDevice()
    })
    if (sourceType.value === 'PC_SCREEN' && !capture.value.hasSystemAudio) {
      toast.show('탭/시스템 소리가 포함되지 않았습니다. 공유 창에서 "탭 오디오 공유" 또는 "시스템 오디오 공유"를 켜 주세요.', 'info', 7000)
    }
    await attachPreview()
  } catch (e) {
    capture.value = null
    toast.error(describeMediaError(e))
  } finally {
    busy.value = false
  }
}

function releaseDevice() {
  capture.value?.stop()
  capture.value = null
  if (previewEl.value) previewEl.value.srcObject = null
}

async function publish(session) {
  publisher.value = await startWhipPublish({
    url: session.publishUrl,
    token: session.publishToken,
    stream: capture.value.stream,
    contentHint: capture.value.contentHint,
    maxBitrate: sourceType.value === 'PC_SCREEN' ? 3_000_000 : 2_000_000,
    onStateChange: (s) => { connectionState.value = s }
  })
}

async function startLive() {
  if (!capture.value) {
    toast.error('먼저 장치를 준비해 주세요.')
    return
  }
  busy.value = true
  try {
    const session = await broadcastApi.startLive(broadcastId, sourceType.value)
    broadcast.value = session.broadcast
    try {
      await publish(session)
      toast.success('방송을 시작했습니다.')
    } catch (e) {
      // 송출 연결에 실패하면 방송 상태도 되돌린다.
      await broadcastApi.endLive(broadcastId).catch(() => {})
      await loadBroadcast()
      throw e
    }
  } catch (e) {
    toast.error(errorMessage(e, '방송을 시작하지 못했습니다.'))
  } finally {
    busy.value = false
  }
}

/** 새로고침 등으로 송출이 끊긴 상태에서 같은 방송으로 다시 송출한다. */
async function reconnect() {
  if (!capture.value) {
    toast.error('먼저 장치를 준비해 주세요.')
    return
  }
  busy.value = true
  try {
    const session = await broadcastApi.liveSession(broadcastId)
    await publish(session)
    toast.success('송출을 다시 연결했습니다.')
  } catch (e) {
    toast.error(errorMessage(e, '다시 연결하지 못했습니다.'))
  } finally {
    busy.value = false
  }
}

async function endLive() {
  busy.value = true
  try {
    await publisher.value?.stop()
    publisher.value = null
    releaseDevice()
    broadcast.value = await broadcastApi.endLive(broadcastId)
    toast.success('방송을 종료했습니다.')
  } catch (e) {
    toast.error(errorMessage(e, '방송 종료 중 오류가 발생했습니다.'))
    await loadBroadcast()
  } finally {
    busy.value = false
    connectionState.value = 'closed'
  }
}

async function switchCamera() {
  if (!capture.value?.switchCamera) return
  try {
    const track = await capture.value.switchCamera()
    await publisher.value?.replaceVideoTrack(track)
    await attachPreview()
  } catch (e) {
    toast.error(describeMediaError(e))
  }
}

function toggleMic() {
  micMuted.value = !micMuted.value
  capture.value?.setMicMuted(micMuted.value)
}

function toggleSystem() {
  systemMuted.value = !systemMuted.value
  capture.value?.setSystemMuted(systemMuted.value)
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(watchLink.value)
    toast.success('시청 링크를 복사했습니다.')
  } catch {
    toast.show(watchLink.value)
  }
}

function onBeforeUnload(e) {
  if (publisher.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}

onMounted(async () => {
  await loadBroadcast()
  events.connect()
  pollTimer = setInterval(async () => {
    try { broadcast.value = await broadcastApi.get(broadcastId) } catch { /* 무시 */ }
  }, 5000)
  tickTimer = setInterval(() => { tick.value++ }, 1000)
  window.addEventListener('beforeunload', onBeforeUnload)
})

onBeforeUnmount(() => {
  clearInterval(pollTimer)
  clearInterval(tickTimer)
  window.removeEventListener('beforeunload', onBeforeUnload)
  publisher.value?.stop()
  releaseDevice()
})
</script>

<template>
  <div v-if="loadError" class="empty">{{ loadError }}</div>
  <div v-else-if="broadcast && !isOwner" class="empty">방송 소유자만 스튜디오에 들어올 수 있습니다.</div>
  <div v-else-if="broadcast" class="layout-watch">
    <div class="stack">
      <div class="video-box">
        <video ref="previewEl" autoplay playsinline muted></video>
        <div v-if="!capture" class="video-overlay">
          <div>송출 방식을 고른 뒤 <strong>장치 준비</strong>를 눌러 미리보기를 켜세요.</div>
        </div>
      </div>

      <div class="card stack">
        <div class="row">
          <StatusBadge :status="broadcast.status" :online="broadcast.streamOnline" />
          <h2 style="margin:0">{{ broadcast.title }}</h2>
          <span class="spacer"></span>
          <span v-if="isLive" class="muted small" :data-tick="tick">방송 시간 {{ elapsedSince(broadcast.startedAt) }}</span>
        </div>

        <div class="stack">
          <div class="small muted">송출 방식</div>
          <div class="row">
            <label class="source" :class="{ active: sourceType === 'PC_SCREEN', disabled: !canScreenShare }">
              <input v-model="sourceType" type="radio" value="PC_SCREEN" :disabled="!canScreenShare || !!publisher" />
              PC 화면 공유 + 소리 + 마이크
            </label>
            <label class="source" :class="{ active: sourceType === 'MOBILE_CAMERA' }">
              <input v-model="sourceType" type="radio" value="MOBILE_CAMERA" :disabled="!!publisher" />
              카메라 + 마이크 (모바일)
            </label>
          </div>
          <div v-if="!canScreenShare" class="small muted">이 기기는 화면 공유를 지원하지 않아 카메라 송출만 가능합니다.</div>
        </div>

        <div class="row">
          <button class="btn" :disabled="busy || !!publisher" @click="prepareDevice">
            {{ capture ? '장치 다시 선택' : '장치 준비' }}
          </button>
          <button v-if="!isLive" class="btn primary" :disabled="busy || !capture" @click="startLive">방송 시작</button>
          <template v-else>
            <button v-if="!publisher" class="btn primary" :disabled="busy || !capture" @click="reconnect">송출 다시 연결</button>
            <button class="btn danger" :disabled="busy" @click="endLive">방송 종료</button>
          </template>
        </div>

        <div v-if="capture" class="row">
          <button class="btn sm" @click="toggleMic">{{ micMuted ? '마이크 켜기' : '마이크 끄기' }}</button>
          <button v-if="capture.hasSystemAudio" class="btn sm" @click="toggleSystem">
            {{ systemMuted ? '탭/시스템 소리 켜기' : '탭/시스템 소리 끄기' }}
          </button>
          <button v-if="sourceType === 'MOBILE_CAMERA' && mobile" class="btn sm" @click="switchCamera">카메라 전환</button>
          <span class="small muted">
            마이크 {{ capture.hasMic ? '연결됨' : '없음' }}
            <template v-if="sourceType === 'PC_SCREEN'">, 탭/시스템 소리 {{ capture.hasSystemAudio ? '포함' : '미포함' }}</template>
          </span>
        </div>

        <div class="small muted">
          송출 연결: <strong>{{ publisher ? connectionState : '없음' }}</strong>,
          미디어 서버 수신: <strong>{{ broadcast.streamOnline ? '정상' : '대기' }}</strong>
        </div>
        <div class="row small">
          <span class="muted">시청 링크</span>
          <code class="link">{{ watchLink }}</code>
          <button class="btn sm" @click="copyLink">복사</button>
          <RouterLink :to="`/broadcasts/${broadcastId}`" class="btn sm" target="_blank">시청 화면 열기</RouterLink>
        </div>
      </div>
    </div>

    <ChatPanel :broadcast-id="broadcastId" :events="events" height="420px" />
  </div>
</template>

<style scoped>
.source {
  display: flex; align-items: center; gap: 8px; padding: 10px 12px;
  border: 1px solid var(--border); border-radius: 10px; cursor: pointer; font-size: 14px;
}
.source.active { border-color: var(--accent); background: rgba(0,229,160,.08); }
.source.disabled { opacity: .5; cursor: not-allowed; }
.link { background: var(--bg); padding: 4px 8px; border-radius: 6px; overflow: hidden; text-overflow: ellipsis; max-width: 100%; }
</style>
