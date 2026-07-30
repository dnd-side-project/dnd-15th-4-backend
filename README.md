# PuzzleMeet Backend

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 4.0.7 (Spring Framework 7) |
| 빌드 | Gradle 9.5.1 (wrapper) |
| 데이터베이스 | MySQL |
| API 문서 | springdoc-openapi 3.0.3 (Swagger UI) |
| 코드 포맷 | Spotless + google-java-format 1.22.0 |
| 배포 | GitHub Actions → Docker Hub → EC2 (arm64) |

## 실행 방법

### 실행

```bash
./gradlew installGitHooks   # 최초 1회
./gradlew bootRun
```

`installGitHooks`는 `core.hooksPath`를 `.githooks`로 바꿔 커밋할 때 `spotlessCheck`가 돌게 한다.

기동하면 두 곳으로 확인한다.

- 헬스체크: `GET http://localhost:8080/health`
- API 문서: `http://localhost:8080/swagger-ui.html`

### 커밋 전

```bash
./gradlew spotlessApply
```

pre-commit hook은 `spotlessCheck`로 검사만 하고 자동 수정은 하지 않는다.
포맷이 어긋나면 커밋이 막히므로 미리 맞춰 둔다.

## 규약 문서

- 브랜치·커밋·PR 규칙: [docs/git.md](docs/git.md)
- 코드 규약은 `docs/` 아래에 있다. 어떤 작업에 어떤 문서를 읽는지는
  [CLAUDE.md](CLAUDE.md)의 표를 따른다
