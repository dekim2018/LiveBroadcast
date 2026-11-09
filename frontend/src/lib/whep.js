import { ICE_SERVERS, deleteResource, resolveResourceUrl, waitIceGathering } from './webrtc'

/**
 * WHEP 으로 방송을 수신한다. 로그인 없이 시청할 수 있다.
 * 송출이 아직 연결되지 않았으면 미디어 서버가 404 를 주므로 NotPublishingError 를 던진다.
 */
export class NotPublishingError extends Error {}

export async function startWhepPlay({ url, onTrack, onStateChange }) {
  const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS })
  pc.addTransceiver('video', { direction: 'recvonly' })
  pc.addTransceiver('audio', { direction: 'recvonly' })
  const stream = new MediaStream()
  pc.addEventListener('track', (e) => {
    stream.addTrack(e.track)
    onTrack?.(stream)
  })
  pc.addEventListener('connectionstatechange', () => onStateChange?.(pc.connectionState))

  await pc.setLocalDescription(await pc.createOffer())
  await waitIceGathering(pc)

  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/sdp' },
    body: pc.localDescription.sdp
  })
  if (res.status !== 201) {
    pc.close()
    if (res.status === 404 || res.status === 400) throw new NotPublishingError('송출 대기 중')
    if (res.status === 401) throw new NotPublishingError('방송 중이 아닙니다')
    throw new Error(`미디어 서버 응답 오류(${res.status})`)
  }
  const resourceUrl = resolveResourceUrl(url, res.headers.get('Location'))
  await pc.setRemoteDescription({ type: 'answer', sdp: await res.text() })

  return {
    pc,
    async stop() {
      await deleteResource(resourceUrl)
      pc.close()
    }
  }
}
