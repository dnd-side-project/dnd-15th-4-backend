# 이미지 안에서 Gradle을 돌리지 않는다.
# 워크플로가 Gradle 캐시를 쓰며 빌드한 jar를 그대로 복사하는 편이 빠르다.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# build.gradle에서 plain jar를 껐으므로 build/libs에는 실행 가능한 jar 하나만 있다.
COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
