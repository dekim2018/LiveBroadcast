# 배포 가이드 (AWS 계정이 없는 상태에서 시작)

이 문서는 AWS 계정 만들기부터 `https://내도메인` 으로 접속해서 휴대폰 송출까지 확인하는 과정을 순서대로 정리한 것입니다.

## 0. 왜 서버(VM)가 필요한가

이 프로젝트는 WebRTC 미디어 서버(MediaMTX)를 씁니다. WebRTC 영상은 HTTP 가 아니라 **UDP 8189 포트**로 브라우저와 서버가 직접 주고받기 때문에, 포트를 마음대로 열 수 있는 가상 서버가 필요합니다.
Render, Railway, Vercel 같은 PaaS 는 보통 HTTP 포트 하나만 열어 주기 때문에 이 구조를 올리기 어렵습니다.

전체 구성은 서버 1대에서 `docker compose` 로 실행합니다.

| 구성 요소 | 메모리(대략) |
| --- | --- |
| Spring 서비스 4개 (gateway, auth, broadcast, interaction) | 1.6 ~ 2.0GB |
| PostgreSQL, Redis, MediaMTX, Caddy | 0.3 ~ 0.5GB |
| 첫 빌드 시 Gradle, npm | 일시적으로 1 ~ 1.5GB 추가 |

그래서 **메모리 4GB 이상 + 스왑 2GB** 서버를 권장합니다.

## 1. AWS 계정 만들기

1. https://aws.amazon.com/ko/free 접속 → **무료 계정 생성**
2. 이메일, 계정 이름 입력 → 이메일 인증 코드 입력 → 루트 비밀번호 설정
3. 연락처 정보: **개인(Personal)** 선택, 영문 주소 입력
4. 결제 정보: 해외 결제가 되는 신용/체크카드 등록 (확인용 소액 승인 후 취소됨)
5. 휴대폰 본인 확인 (SMS 또는 음성)
6. 플랜 선택
   - 2025년 7월 이후 가입 계정은 **무료 플랜(Free plan)** 과 **유료 플랜(Paid plan)** 중 하나를 고릅니다.
   - 무료 플랜은 가입 크레딧(최대 200달러, 가입 시 100달러 + 온보딩 과제 완료 시 추가)을 6개월 동안 쓰고, 크레딧이 끝나거나 기간이 지나면 과금 없이 계정이 멈춥니다. 과제 기간 동안 쓰기에는 무료 플랜이 안전합니다.
   - 플랜 정책은 바뀔 수 있으니 가입 화면 안내를 기준으로 판단하세요.

### 가입 직후 꼭 할 것

- **루트 계정 MFA 설정**: 우측 상단 계정 이름 → 보안 자격 증명 → MFA 할당 (Google Authenticator 등)
- **예산 알림**: 콘솔 검색창에 `Budgets` → 예산 생성 → 월 10달러, 80% 도달 시 이메일 알림
- 우측 상단 리전을 **아시아 태평양(서울) ap-northeast-2** 로 변경

## 2. EC2 서버 만들기

콘솔 검색창에 `EC2` → **인스턴스 시작**

| 항목 | 값 |
| --- | --- |
| 이름 | `live-broadcast` |
| AMI | **Ubuntu Server 24.04 LTS** (64비트 x86) |
| 인스턴스 유형 | `c7i-flex.large` (2 vCPU, 4GB) 권장, 여유 있게 하려면 `m7i-flex.large` (8GB) |
| 키 페어 | **새 키 페어 생성** → 이름 `live-key`, 유형 RSA, 형식 `.pem` → 자동 다운로드된 파일 잘 보관 |
| 스토리지 | 30GiB, gp3 |

> 무료 플랜 계정은 "프리 티어 사용 가능" 표시가 붙은 유형만 고를 수 있습니다. 목록에서 표시를 확인하고 그중 메모리가 4GB 이상인 유형을 고르세요. 2GB 유형(t3.small)도 스왑을 넉넉히 주면 동작은 하지만 빌드가 매우 느립니다.

### 네트워크 설정 (보안 그룹)

**편집**을 눌러 인바운드 규칙을 아래처럼 만듭니다.

| 유형 | 프로토콜 | 포트 | 소스 | 용도 |
| --- | --- | --- | --- | --- |
| SSH | TCP | 22 | 내 IP | 서버 접속 |
| HTTP | TCP | 80 | 0.0.0.0/0 | 인증서 발급, HTTPS 리다이렉트 |
| HTTPS | TCP | 443 | 0.0.0.0/0 | 서비스 |
| 사용자 지정 UDP | UDP | 443 | 0.0.0.0/0 | HTTP/3 (선택) |
| 사용자 지정 UDP | UDP | 8189 | 0.0.0.0/0 | WebRTC 미디어 |
| 사용자 지정 TCP | TCP | 8189 | 0.0.0.0/0 | WebRTC 미디어 (UDP 가 막힌 네트워크용) |

**인스턴스 시작**을 누르면 1~2분 뒤 실행 중 상태가 됩니다.

### 고정 IP (탄력적 IP)

서버를 재시작해도 IP 가 바뀌지 않도록 고정합니다.

1. EC2 왼쪽 메뉴 **탄력적 IP** → **탄력적 IP 주소 할당** → 할당
2. 만든 IP 선택 → 작업 → **탄력적 IP 주소 연결** → 인스턴스 `live-broadcast` 선택
3. 이 IP 를 메모해 둡니다. 이하 `3.35.10.20` 이라고 가정합니다.

> 공인 IPv4 주소는 시간당 소액 요금이 붙고 크레딧에서 차감됩니다. 평가가 끝나면 아래 "정리" 단계에서 해제하세요.

## 3. 무료 도메인 연결 (DuckDNS)

HTTPS 인증서를 받으려면 도메인이 필요합니다. 무료 서브도메인을 쓰면 됩니다.

1. https://www.duckdns.org 접속 → GitHub 또는 Google 로 로그인
2. subdomain 칸에 원하는 이름 입력 (예: `mylive`) → **add domain**
3. 생성된 `mylive.duckdns.org` 의 **current ip** 칸에 탄력적 IP(`3.35.10.20`) 입력 → **update ip**
4. 1~2분 뒤 PC 터미널에서 확인

```bash
nslookup mylive.duckdns.org
# Address: 3.35.10.20 이 나오면 성공
```

> 도메인을 이미 가지고 있다면 DNS 관리 화면에서 A 레코드를 탄력적 IP 로 지정하면 됩니다.

## 4. 서버 접속

다운로드한 키 파일(`live-key.pem`)이 있는 폴더에서 실행합니다.

**macOS / Linux**

```bash
chmod 400 live-key.pem
ssh -i live-key.pem ubuntu@3.35.10.20
```

**Windows (PowerShell)**

```powershell
ssh -i .\live-key.pem ubuntu@3.35.10.20
```

> Windows 에서 권한 오류가 나면 파일 속성 → 보안 → 고급에서 상속을 끄고 본인 계정만 읽기 권한을 남기세요.
> EC2 콘솔의 인스턴스 화면 → **연결** → **EC2 Instance Connect** 로 브라우저에서 바로 접속할 수도 있습니다.

## 5. 서버 준비 (한 번만)

```bash
# 패키지 업데이트
sudo apt update && sudo apt upgrade -y

# 스왑 2GB (빌드 중 메모리 부족 방지)
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Docker 설치 (공식 설치 스크립트)
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu

# 그룹 적용을 위해 재접속
exit
```

다시 `ssh` 로 접속한 뒤 확인합니다.

```bash
docker version
docker compose version
free -h      # Swap 2.0Gi 확인
```

## 6. 프로젝트 내려받기와 환경변수 설정

```bash
git clone https://github.com/dekim2018/LiveBroadcast.git
cd LiveBroadcast

# 도메인과 공인 IP 를 넣어서 .env 생성 (비밀 값은 무작위로 생성됨)
./scripts/generate-env.sh mylive.duckdns.org 3.35.10.20
```

### 토스페이먼츠 테스트 키 발급 (후원 기능용)

1. https://developers.tosspayments.com 회원가입, 로그인 (사업자 등록 없이 테스트 키 사용 가능)
2. 개발자센터 → **API 키** 메뉴 → **API 개별 연동 키** 의 테스트용 **클라이언트 키**(`test_ck_...`)와 **시크릿 키**(`test_sk_...`) 복사
3. 서버에서 `.env` 에 입력

```bash
nano .env
# TOSS_CLIENT_KEY=test_ck_xxxxxxxx
# TOSS_SECRET_KEY=test_sk_xxxxxxxx
# 저장: Ctrl+O, Enter / 종료: Ctrl+X
```

생성된 `.env` 의 핵심 값은 아래와 같은 모습이어야 합니다.

```dotenv
SITE_ADDRESS=mylive.duckdns.org
PUBLIC_HOST=3.35.10.20
COOKIE_SECURE=true
```

### 평가용 테스트 계정 (평가 기간에만 켜기)

README 의 원클릭 로그인이 동작해야 평가할 수 있습니다. `generate-env.sh` 로 만든 `.env` 는 이미 켜져 있으니 아래 두 줄만 확인합니다.

```bash
grep '^TEST_ACCOUNTS_ENABLED' .env    # TEST_ACCOUNTS_ENABLED=true 이어야 함
# TEST_ACCOUNT_PASSWORD 는 generate-env.sh 가 만든 무작위 값을 그대로 둔다 (문서, 채팅 등에 적지 않기)
```

`.env` 를 직접 만들었거나 꺼져 있다면 `TEST_ACCOUNTS_ENABLED=true` 로 바꾸고 `docker compose up -d auth-service` 로 재시작합니다.

평가가 끝나면 `TEST_ACCOUNTS_ENABLED=false` 로 바꾸고 같은 명령으로 재시작합니다. 원클릭 로그인이 막히고, 기존 테스트 계정도 비밀번호가 무작위로 바뀌어 로그인할 수 없습니다.
예전 `.env` 에 `TEST_ACCOUNT_PASSWORD=test1234!` 가 남아 있다면 무작위 값으로 바꾼 뒤 재시작하세요. 재시작할 때 기존 테스트 계정의 비밀번호도 새 값으로 바뀝니다.

## 7. 실행

```bash
docker compose up -d --build
```

첫 빌드는 5~15분 정도 걸립니다. 진행 상황은 아래로 확인합니다.

```bash
docker compose ps                    # 모든 컨테이너가 running 인지
docker compose logs -f web           # 인증서 발급 로그 (certificate obtained successfully)
docker compose logs -f broadcast-service
```

브라우저에서 `https://mylive.duckdns.org` 접속 → 자물쇠 아이콘과 함께 홈 화면이 뜨면 성공입니다.

## 8. 배포 후 점검 순서

1. 다른 기기에서 `curl https://<도메인>/api/auth/test-accounts` → `tester1`, `tester2` 두 계정이 나오는지 확인 (빈 목록이면 원클릭 로그인이 꺼진 상태)
2. PC 에서 **테스터1** 원클릭 로그인 → 방송 만들기 → PC 화면 공유로 방송 시작
3. 휴대폰(LTE 로 바꿔서 다른 네트워크에서) 같은 주소 접속 → 방송 시청 확인
4. 휴대폰에서 **테스터2** 원클릭 로그인 → 새 방송 → 카메라 송출 → PC 에서 시청 확인
5. 후원 테스트 결제 → 합계 갱신 확인
6. 방송 종료 → **다시보기**에서 녹화 재생 확인
7. README 의 서비스 주소와 시연 영상 링크 칸을 실제 값으로 바꿔서 커밋

### 토스 웹훅 등록 (선택)

토스 개발자센터 → **웹훅** → 웹훅 등록

- URL: `https://mylive.duckdns.org/api/donations/webhook`
- 이벤트: `PAYMENT_STATUS_CHANGED`

## 9. 코드 수정 후 다시 배포

```bash
cd ~/LiveBroadcast
git pull
docker compose up -d --build
```

## 10. 문제 해결

| 증상 | 확인할 것 |
| --- | --- |
| 사이트 접속 자체가 안 됨 | 보안 그룹 80/443 개방, `docker compose ps` 에서 web 컨테이너 상태 |
| 인증서 발급 실패 | `nslookup` 으로 도메인이 탄력적 IP 를 가리키는지, 80 포트가 열려 있는지. 수정 후 `docker compose restart web` |
| 방송 시작은 되는데 시청 화면이 계속 "송출 대기" | 보안 그룹 UDP/TCP 8189 개방, `.env` 의 `PUBLIC_HOST` 가 탄력적 IP 인지. 수정 후 `docker compose up -d mediamtx` |
| 같은 와이파이에서는 되는데 LTE 에서 안 됨 | 위와 동일 (대부분 8189 포트 또는 PUBLIC_HOST 문제) |
| 휴대폰에서 카메라 권한 창이 안 뜸 | 반드시 `https://` 주소로 접속했는지 |
| 화면 공유 시 소리가 안 나감 | 공유 창에서 "탭 오디오 공유" 또는 "시스템 오디오 공유" 체크. macOS 는 전체 화면 시스템 소리를 지원하지 않으므로 Chrome 탭 공유 사용 |
| 빌드 중 서버가 멈춤 | `free -h` 로 스왑 확인, 메모리 4GB 이상 유형으로 변경 |
| 로그인 후 새로고침하면 로그아웃됨 | HTTPS 인데 `COOKIE_SECURE=false` 이거나, HTTP 인데 `true` 로 되어 있지 않은지 |
| 후원 버튼이 비활성화 | `.env` 의 `TOSS_CLIENT_KEY`, `TOSS_SECRET_KEY` 입력 후 `docker compose up -d interaction-service` |

유용한 명령어

```bash
docker compose logs --tail=200 mediamtx      # 송출/시청 인증 로그
docker compose exec postgres psql -U livebroadcast -d broadcast_db -c "select id,title,status,stream_online from broadcasts;"
docker stats                                 # 컨테이너별 메모리 사용량
```

## 11. 정리 (비용 방지)

평가가 끝나면 아래 순서로 정리합니다.

1. EC2 → 인스턴스 선택 → 인스턴스 상태 → **종료(삭제)**
2. EC2 → 탄력적 IP → 선택 → 작업 → **탄력적 IP 주소 릴리스**
3. EC2 → 볼륨 목록에 남은 볼륨이 있으면 삭제

## 다른 선택지

| 방법 | 장점 | 단점 |
| --- | --- | --- |
| AWS EC2 (이 문서) | 문서와 자료가 가장 많음, 서울 리전 | 크레딧 소진 후 유료 |
| Oracle Cloud Always Free (ARM 4코어, 24GB) | 기간 제한 없는 무료 VM | 가입 승인이 까다롭고, 인기 리전은 자원 부족으로 생성 실패가 잦음. 이 프로젝트의 이미지는 모두 ARM 을 지원하므로 같은 절차로 배포 가능 |
| 집 PC + 공유기 포트포워딩 | 비용 없음 | 공인 IP, 포트 개방, 상시 전원 필요 |
