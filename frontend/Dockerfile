# 1단계: Vue 앱 빌드
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# 2단계: Caddy 가 정적 파일 서빙 + HTTPS + 리버스 프록시를 담당
FROM caddy:2.10-alpine
COPY --from=build /app/dist /srv
