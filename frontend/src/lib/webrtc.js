/**
 * WHIP(송출) / WHEP(시청) 공통 유틸.
 * WHIP: RFC 9725, WHEP: IETF draft. MediaMTX 가 두 프로토콜을 모두 지원한다.
 */
export const ICE_SERVERS = [{ urls: 'stun:stun.l.google.com:19302' }]

/** 후보(ICE candidate) 수집이 끝날 때까지 기다린다. Trickle ICE 없이 한 번의 HTTP 요청으로 협상하기 위함 */
export function waitIceGathering(pc, timeoutMs = 2500) {
  if (pc.iceGatheringState === 'complete') return Promise.resolve()
  return new Promise((resolve) => {
    const timer = setTimeout(done, timeoutMs)
    function done() {
      clearTimeout(timer)
      pc.removeEventListener('icegatheringstatechange', onChange)
      resolve()
    }
    function onChange() {
      if (pc.iceGatheringState === 'complete') done()
    }
    pc.addEventListener('icegatheringstatechange', onChange)
  })
}

/** 비디오 코덱을 H264 우선으로 정렬한다. iOS Safari 시청자와 녹화(fMP4) 호환성을 위해서다. */
export function preferH264(transceiver) {
  try {
    const caps = RTCRtpReceiver.getCapabilities?.('video')
    if (!caps || !transceiver.setCodecPreferences) return
    const h264 = caps.codecs.filter((c) => c.mimeType.toLowerCase() === 'video/h264')
    const others = caps.codecs.filter((c) => c.mimeType.toLowerCase() !== 'video/h264')
    if (h264.length) transceiver.setCodecPreferences([...h264, ...others])
  } catch (e) {
    console.warn('코덱 우선순위 설정 실패', e)
  }
}

/**
 * MediaMTX 는 Location 헤더로 세션 리소스 경로를 돌려준다.
 * Caddy/Vite 가 /media 접두어를 떼고 전달하므로, 상대 경로이면 접두어를 다시 붙인다.
 */
export function resolveResourceUrl(endpointUrl, location) {
  if (!location) return null
  if (/^https?:\/\//.test(location)) return location
  if (location.startsWith('/')) {
    if (endpointUrl.startsWith('/media/') && !location.startsWith('/media/')) return '/media' + location
    return location
  }
  return new URL(location, new URL(endpointUrl, window.location.origin)).pathname
}

export async function deleteResource(url, headers = {}) {
  if (!url) return
  try {
    await fetch(url, { method: 'DELETE', headers })
  } catch {
    // 세션이 이미 정리된 경우 무시
  }
}
