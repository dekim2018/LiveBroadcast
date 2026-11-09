# 백엔드 마이크로서비스 공용 Dockerfile
#   docker build --build-arg MODULE=auth-service -t live-broadcast/auth-service .
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
# 의존성 캐시 레이어를 살리기 위해 빌드 스크립트를 먼저 복사한다.
COPY common/build.gradle ./common/
COPY gateway/build.gradle ./gateway/
COPY auth-service/build.gradle ./auth-service/
COPY broadcast-service/build.gradle ./broadcast-service/
COPY interaction-service/build.gradle ./interaction-service/
RUN chmod +x gradlew && ./gradlew --no-daemon help > /dev/null
COPY common ./common
COPY gateway ./gateway
COPY auth-service ./auth-service
COPY broadcast-service ./broadcast-service
COPY interaction-service ./interaction-service
ARG MODULE
RUN ./gradlew --no-daemon :${MODULE}:bootJar -x test

FROM eclipse-temurin:25-jre
ARG MODULE
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /workspace/${MODULE}/build/libs/*.jar /app/app.jar
USER app
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
