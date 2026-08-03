# PuzzleMeet Backend

약속 장소까지 각자 얼마나 걸리는지 공유하고, 도착한 사람의 퍼즐 조각을 공개하는
모임 서비스의 백엔드입니다.

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 4.0.7 (Spring Framework 7) |
| 빌드 | Gradle 9.5.1 (wrapper) |
| 데이터베이스 | MySQL |
| API 문서 | springdoc-openapi 3.0.3 (Swagger UI) |
| 코드 포맷 | Spotless + google-java-format 1.22.0 |
| 배포 | GitHub Actions → GHCR → EC2 (arm64) |

## 실행 방법

### 환경 설정

- JDK 21
- 로컬 MySQL 
- Docker 

### 실행

```bash
./gradlew installGitHooks   # 최초 1회
./gradlew bootRun
```

`installGitHooks`는 `core.hooksPath`를 `.githooks`로 바꿔 커밋할 때 `spotlessCheck`가 돌게 합니다.

`bootRun` 실행 전에 환경변수 작성

```bash
cp .env.example .env
```


### 확인

- 헬스체크: `GET http://localhost:8080/health`
- API 문서: http://localhost:8080/swagger-ui.html

### 테스트

```bash
./gradlew test
```

### 커밋 전

```bash
./gradlew spotlessApply
```

pre-commit hook은 `spotlessCheck`로 검사만 하고 자동 수정은 하지 않습니다.
