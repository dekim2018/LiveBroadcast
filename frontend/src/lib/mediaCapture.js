/**
 * 송출용 미디어 캡처.
 *  - PC: 화면 공유(getDisplayMedia) + 탭/시스템 소리 + 마이크를 Web Audio 로 하나의 오디오 트랙으로 합친다.
 *  - 모바일: 카메라 + 마이크(getUserMedia), 전면/후면 전환 지원
 */

export const isMobileDevice = () =>
  /Android|iPhone|iPad|iPod|Mobile/i.test(navigator.userAgent) ||
  (navigator.maxTouchPoints > 1 && /Macintosh/.test(navigator.userAgent))

export const supportsScreenShare = () => !!navigator.mediaDevices?.getDisplayMedia && !isMobileDevice()

export const supportsCamera = () => !!navigator.mediaDevices?.getUserMedia

export async function capturePcScreen({ withMic = true } = {}) {
  if (!supportsScreenShare()) {
    throw new Error('이 브라우저는 화면 공유를 지원하지 않습니다. PC 의 Chrome 또는 Edge 를 사용해 주세요.')
  }
  const display = await navigator.mediaDevices.getDisplayMedia({
    video: { frameRate: { ideal: 30, max: 30 }, width: { ideal: 1920 }, height: { ideal: 1080 } },
    // 탭 소리(Chrome 탭 공유) 또는 시스템 소리(Windows 화면 전체 공유)를 함께 요청한다.
    audio: { echoCancellation: false, noiseSuppression: false, autoGainControl: false, suppressLocalAudioPlayback: false },
    systemAudio: 'include',
    selfBrowserSurface: 'exclude',
    surfaceSwitching: 'include'
  })

  let mic = null
  if (withMic) {
    try {
      mic = await navigator.mediaDevices.getUserMedia({
        audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true },
        video: false
      })
    } catch (e) {
      console.warn('마이크를 사용할 수 없습니다', e)
    }
  }

  const ctx = new AudioContext()
  if (ctx.state === 'suspended') await ctx.resume()
  const destination = ctx.createMediaStreamDestination()

  const systemGain = ctx.createGain()
  const micGain = ctx.createGain()
  systemGain.connect(destination)
  micGain.connect(destination)

  const hasSystemAudio = display.getAudioTracks().length > 0
  if (hasSystemAudio) {
    ctx.createMediaStreamSource(new MediaStream(display.getAudioTracks())).connect(systemGain)
  }
  const hasMic = !!mic && mic.getAudioTracks().length > 0
  if (hasMic) {
    ctx.createMediaStreamSource(mic).connect(micGain)
  }

  // 송출 스트림 = 화면 영상 1개 + 합쳐진 오디오 1개
  const output = new MediaStream([...display.getVideoTracks(), ...destination.stream.getAudioTracks()])

  return {
    stream: output,
    preview: new MediaStream(display.getVideoTracks()),
    hasSystemAudio,
    hasMic,
    contentHint: 'detail',
    setMicMuted(muted) { micGain.gain.value = muted ? 0 : 1 },
    setSystemMuted(muted) { systemGain.gain.value = muted ? 0 : 1 },
    /** 사용자가 브라우저의 "공유 중지" 버튼을 눌렀을 때 */
    onEnded(callback) {
      display.getVideoTracks()[0]?.addEventListener('ended', callback, { once: true })
    },
    stop() {
      display.getTracks().forEach((t) => t.stop())
      mic?.getTracks().forEach((t) => t.stop())
      destination.stream.getTracks().forEach((t) => t.stop())
      ctx.close().catch(() => {})
    }
  }
}

export async function captureCamera({ facingMode = 'user' } = {}) {
  if (!supportsCamera()) {
    throw new Error('이 브라우저는 카메라를 지원하지 않습니다. HTTPS 주소로 접속했는지 확인해 주세요.')
  }
  const stream = await navigator.mediaDevices.getUserMedia({
    video: {
      facingMode: { ideal: facingMode },
      width: { ideal: 1280 },
      height: { ideal: 720 },
      frameRate: { ideal: 30, max: 30 }
    },
    audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true }
  })
  let currentFacing = facingMode

  const capture = {
    stream,
    preview: new MediaStream(stream.getVideoTracks()),
    hasSystemAudio: false,
    hasMic: stream.getAudioTracks().length > 0,
    contentHint: 'motion',
    get facingMode() { return currentFacing },
    setMicMuted(muted) { stream.getAudioTracks().forEach((t) => { t.enabled = !muted }) },
    setSystemMuted() {},
    onEnded(callback) {
      stream.getVideoTracks()[0]?.addEventListener('ended', callback, { once: true })
    },
    /** 전면/후면 카메라 전환. 새 비디오 트랙을 돌려주면 송출 중인 sender 에서 replaceTrack 한다. */
    async switchCamera() {
      const next = currentFacing === 'user' ? 'environment' : 'user'
      const fresh = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: next }, width: { ideal: 1280 }, height: { ideal: 720 } },
        audio: false
      })
      const newTrack = fresh.getVideoTracks()[0]
      const oldTrack = stream.getVideoTracks()[0]
      stream.removeTrack(oldTrack)
      oldTrack.stop()
      stream.addTrack(newTrack)
      capture.preview = new MediaStream([newTrack])
      currentFacing = next
      return newTrack
    },
    stop() {
      stream.getTracks().forEach((t) => t.stop())
    }
  }
  return capture
}

export function describeMediaError(e) {
  const name = e?.name
  if (name === 'NotAllowedError') return '권한이 거부되었습니다. 브라우저 주소창의 권한 설정에서 허용해 주세요.'
  if (name === 'NotFoundError') return '사용 가능한 카메라 또는 마이크를 찾지 못했습니다.'
  if (name === 'NotReadableError') return '다른 앱이 카메라나 마이크를 사용 중입니다.'
  if (name === 'AbortError') return '장치 준비가 취소되었습니다.'
  if (!window.isSecureContext) return 'HTTPS(또는 localhost) 주소에서만 카메라와 화면 공유를 사용할 수 있습니다.'
  return e?.message || '장치를 준비하지 못했습니다.'
}
