import { ICE_SERVERS, deleteResource, preferH264, resolveResourceUrl, waitIceGathering } from './webrtc'

/**
 * 브라우저에서 MediaMTX 로 WHIP 송출을 시작한다.
 * 송출 토큰은 Authorization: Bearer 헤더와 쿼리스트링에 함께 실어 보낸다(미디어 서버 버전 차이 대응).
 *
 * @returns {{ pc: RTCPeerConnection, stop: () => Promise<void>, replaceVideoTrack: (t: MediaStreamTrack) => Promise<void> }}
 */
export async function startWhipPublish({ url, token, stream, contentHint = 'motion', maxBitrate = 2_500_000, onStateChange }) {
  const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS })
  pc.addEventListener('connectionstatechange', () => onStateChange?.(pc.connectionState))

  let videoSender = null
  for (const track of stream.getTracks()) {
    if (track.kind === 'video' && 'contentHint' in track) track.contentHint = contentHint
    const transceiver = pc.addTransceiver(track, { direction: 'sendonly', streams: [stream] })
    if (track.kind === 'video') {
      preferH264(transceiver)
      videoSender = transceiver.sender
    }
  }

  await pc.setLocalDescription(await pc.createOffer())
  await waitIceGathering(pc)

  const endpoint = `${url}?token=${encodeURIComponent(token)}`
  const authHeader = { Authorization: `Bearer ${token}` }
  const res = await fetch(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/sdp', ...authHeader },
    body: pc.localDescription.sdp
  })
  if (res.status !== 201) {
    pc.close()
    const reason = res.status === 401 ? '송출 권한이 없습니다(토큰 만료 또는 방송 종료).' : `미디어 서버 응답 오류(${res.status})`
    throw new Error(reason)
  }
  const resourceUrl = resolveResourceUrl(url, res.headers.get('Location'))
  await pc.setRemoteDescription({ type: 'answer', sdp: await res.text() })

  if (videoSender && maxBitrate) {
    try {
      const params = videoSender.getParameters()
      params.encodings = params.encodings?.length ? params.encodings : [{}]
      params.encodings[0].maxBitrate = maxBitrate
      await videoSender.setParameters(params)
    } catch (e) {
      console.warn('비트레이트 설정 실패', e)
    }
  }

  return {
    pc,
    async replaceVideoTrack(track) {
      if (videoSender) await videoSender.replaceTrack(track)
    },
    async stop() {
      await deleteResource(resourceUrl, authHeader)
      pc.getSenders().forEach((s) => s.track && s.track.stop())
      pc.close()
    }
  }
}
