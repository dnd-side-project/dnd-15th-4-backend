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
| 배포 | GitHub Actions → GHCR → EC2 (arm64) |

## 실행 방법

### 실행

```bash
./gradlew installGitHooks   # 최초 1회
./gradlew bootRun
```

`installGitHooks`는 `core.hooksPath`를 `.githooks`로 바꿔 커밋할 때 `spotlessCheck`가 돌게 한다.

`bootRun` 전에 로컬 MySQL 접속 정보를 환경변수로 넣는다. 셋 다 필수다.

| 환경변수 | 예 |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/puzzlemeet` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | |

빠뜨리면 `${DB_URL}`이 문자열 그대로 바인딩돼
`Failed to determine a suitable driver class`로 기동에 실패한다.
드라이버가 없을 때와 메시지가 같으므로 환경변수부터 확인한다.

테이블은 `ddl-auto: update`가 엔티티를 보고 만든다.
스키마 관리 방식과 Flyway 도입 시점은 [docs/entity.md](docs/entity.md)에 있다.

기동하면 두 곳으로 확인한다.

- 헬스체크: `GET http://localhost:8080/health`
- API 문서: `http://localhost:8080/swagger-ui.html`

### 테스트

```bash
./gradlew test
```

컨텍스트 로딩 테스트가 Testcontainers로 MySQL 컨테이너를 띄우므로 Docker가 실행 중이어야 한다.
테스트는 컨테이너 접속 정보를 쓰므로 위 환경변수가 필요 없다.

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
