FROM --platform=$BUILDPLATFORM alpine:3.21 AS newrelic-agent

RUN apk add --no-cache curl && \
    curl -fsSL -o /tmp/newrelic-agent.jar \
      https://repo1.maven.org/maven2/com/newrelic/agent/java/newrelic-agent/9.4.0/newrelic-agent-9.4.0.jar && \
    curl -fsSL -o /tmp/newrelic-agent.jar.sha1 \
      https://repo1.maven.org/maven2/com/newrelic/agent/java/newrelic-agent/9.4.0/newrelic-agent-9.4.0.jar.sha1 && \
    echo "$(cat /tmp/newrelic-agent.jar.sha1)  /tmp/newrelic-agent.jar" | sha1sum -c -

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/*-SNAPSHOT.jar app.jar
COPY --from=newrelic-agent /tmp/newrelic-agent.jar newrelic-agent.jar

EXPOSE 8080

USER nobody

# alpine에는 tzdata가 없지만 JDK가 자체 tzdb를 들고 있어 zone ID를 해석할 수 있다.
# MaxRAMPercentage는 컨테이너 메모리 제한 기준이므로 인스턴스 크기와 무관하게 동작한다.
ENV NEW_RELIC_APP_NAME="PuzzleMeet" \
    NEW_RELIC_LOG_FILE_NAME="STDOUT" \
    NEW_RELIC_APPLICATION_LOGGING_FORWARDING_ENABLED="true"

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-XX:MaxRAMPercentage=65", "-javaagent:/app/newrelic-agent.jar", "-jar", "app.jar"]
