<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { commentApi } from '../api/interactions'
import { errorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'

/**
 * 방송별 댓글(채팅). 목록은 REST 로 불러오고, 새 댓글/수정/삭제는 SSE 이벤트로 실시간 반영한다.
 */
const props = defineProps({
  broadcastId: { type: [String, Number], required: true },
  events: { type: Object, required: true },
  height: { type: String, default: '520px' }
})

const auth = useAuthStore()
const toast = useToastStore()
const comments = ref([])
const content = ref('')
const sending = ref(false)
const editingId = ref(null)
const editingText = ref('')
const listEl = ref(null)
const hasMore = ref(false)
let page = 0

async function load(more = false) {
  try {
    const res = await commentApi.list(props.broadcastId, { page, size: 50 })
    const items = [...res.content].reverse()
    comments.value = more ? [...items, ...comments.value] : items
    hasMore.value = res.page + 1 < res.totalPages
    if (!more) scrollToBottom()
  } catch (e) {
    toast.error(errorMessage(e, '댓글을 불러오지 못했습니다.'))
  }
}

function loadMore() {
  page += 1
  load(true)
}

function upsert(c) {
  const idx = comments.value.findIndex((x) => x.id === c.id)
  if (idx >= 0) comments.value[idx] = { ...comments.value[idx], ...c }
  else {
    comments.value.push(c)
    scrollToBottom()
  }
}

async function scrollToBottom() {
  await nextTick()
  if (listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight
}

async function send() {
  if (!content.value.trim() || sending.value) return
  sending.value = true
  try {
    const created = await commentApi.write(props.broadcastId, content.value)
    upsert(created)
    content.value = ''
  } catch (e) {
    toast.error(errorMessage(e))
  } finally {
    sending.value = false
  }
}

function startEdit(c) {
  editingId.value = c.id
  editingText.value = c.content
}

async function saveEdit(c) {
  try {
    upsert(await commentApi.edit(c.id, editingText.value))
    editingId.value = null
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

async function remove(c) {
  if (!confirm('댓글을 삭제할까요?')) return
  try {
    await commentApi.remove(c.id)
    comments.value = comments.value.filter((x) => x.id !== c.id)
  } catch (e) {
    toast.error(errorMessage(e))
  }
}

props.events.on('comment.created', upsert)
props.events.on('comment.updated', upsert)
props.events.on('comment.deleted', ({ id }) => {
  comments.value = comments.value.filter((x) => x.id !== id)
})

onMounted(() => load())
</script>

<template>
  <section class="chat card">
    <div class="row chat-head">
      <h3 style="margin:0">채팅</h3>
      <span class="spacer"></span>
      <span class="small" :class="events.connected.value ? 'ok' : 'muted'">
        {{ events.connected.value ? '● 실시간' : '○ 연결 중' }}
      </span>
    </div>
    <div ref="listEl" class="chat-list" :style="{ height }">
      <button v-if="hasMore" class="btn sm ghost block" @click="loadMore">이전 댓글 보기</button>
      <div v-if="!comments.length" class="muted small center">첫 댓글을 남겨 보세요.</div>
      <div v-for="c in comments" :key="c.id" class="chat-item">
        <template v-if="editingId === c.id">
          <input v-model="editingText" class="input" maxlength="300" @keyup.enter="saveEdit(c)" />
          <div class="row">
            <button class="btn sm primary" @click="saveEdit(c)">저장</button>
            <button class="btn sm" @click="editingId = null">취소</button>
          </div>
        </template>
        <template v-else>
          <span class="author">{{ c.authorNickname }}</span>
          <span class="text">{{ c.content }}</span>
          <span v-if="c.edited" class="muted small"> (수정됨)</span>
          <span v-if="auth.user && auth.user.id === c.authorId" class="actions">
            <button class="link" @click="startEdit(c)">수정</button>
            <button class="link" @click="remove(c)">삭제</button>
          </span>
        </template>
      </div>
    </div>
    <form v-if="auth.isLoggedIn" class="row chat-form" @submit.prevent="send">
      <input v-model="content" class="input" maxlength="300" placeholder="채팅을 입력하세요" />
      <button class="btn primary" :disabled="sending || !content.trim()">전송</button>
    </form>
    <div v-else class="muted small center">
      <RouterLink :to="{ name: 'login', query: { redirect: $route.fullPath } }" class="ok">로그인</RouterLink> 후 채팅에 참여할 수 있습니다.
    </div>
  </section>
</template>

<style scoped>
.chat { display: flex; flex-direction: column; gap: 10px; }
.chat-list { overflow-y: auto; display: flex; flex-direction: column; gap: 6px; padding-right: 4px; }
.chat-item { font-size: 14px; word-break: break-all; line-height: 1.45; }
.author { color: var(--accent); font-weight: 700; margin-right: 6px; }
.actions { margin-left: 6px; }
.link { background: none; border: none; color: var(--text-sub); font-size: 12px; cursor: pointer; padding: 0 3px; }
.link:hover { color: var(--text); }
.ok { color: var(--accent); }
.center { text-align: center; padding: 8px 0; }
.chat-form { flex-wrap: nowrap; }
@media (max-width: 960px) { .chat-list { height: 320px !important; } }
</style>
