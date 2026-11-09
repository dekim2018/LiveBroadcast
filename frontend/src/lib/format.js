const dateFormatter = new Intl.DateTimeFormat('ko-KR', {
  month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
})

export function formatDate(value) {
  if (!value) return '-'
  return dateFormatter.format(new Date(value))
}

export function formatDuration(totalSeconds) {
  const s = Math.max(0, Math.floor(totalSeconds || 0))
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  const pad = (n) => String(n).padStart(2, '0')
  return h > 0 ? `${h}:${pad(m)}:${pad(sec)}` : `${m}:${pad(sec)}`
}

export function formatWon(amount) {
  return `${Number(amount || 0).toLocaleString('ko-KR')}원`
}

export function elapsedSince(value) {
  if (!value) return ''
  return formatDuration((Date.now() - new Date(value).getTime()) / 1000)
}
