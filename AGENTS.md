# PuzzleMeet Backend
\
Java 21 / Spring Boot 4.0.7 / Spring Framework 7 / MySQL.

## 작업 전에 읽을 문서

| 지금 하려는 일 | 먼저 읽을 문서 |
|---|---|
| 엔티티·리포지토리 작성 | `docs/entity.md` |
| 요청·응답 DTO 작성 | `docs/dto.md` |
| 에러 코드 추가·예외 처리 | `docs/error-code.md` |
| 컨트롤러에 Swagger 어노테이션 붙이기 | `docs/swagger.md` |
| 로그 남기기 | `docs/logging.md` |
| 클래스·메서드·API 경로·테스트 이름 짓기, 패키지 구조 | `docs/naming.md` |
| 브랜치 만들기·커밋 메시지 쓰기·PR 올리기 | `docs/git.md` |

문서 간에 같은 규칙이 다르게 적혀 있으면 이 파일을 기준으로 한다.
그런 부분을 발견하면 `docs/` 쪽을 이 파일에 맞춰 고친다.

## 항상 적용

### 1. 참여자의 실시간 위치 좌표를 남기지 않는다

- 엔티티·응답 DTO·로그 어디에도 남기지 않는다. 디버깅 편의를 이유로도 예외를 두지 않는다
- 요청을 처리하는 동안 지도 API 호출에만 쓰고 버린다. DB에는 계산 결과인 소요시간만 저장한다
- `meeting`의 목적지 좌표만 예외다. 지도에 핀을 찍어야 하므로 정상 저장 대상이다
- 참여자 위치(현재 위치·출발지)를 받는 요청 record는 `toString()`을 오버라이드해서
  좌표를 마스킹한다. 방법과 예외는 `docs/dto.md`

### 2. Spring Boot 3 예제를 그대로 옮기지 않는다

- 의존성·패키지는 Boot 4 기준으로 쓴다. `spring-boot-starter-webmvc`(`-web` 아님),
  Jackson은 `tools.jackson.*`(`com.fasterxml` 아님). 이 둘은 틀리면 빌드가 잡는다
- `@WebMvcTest`·`MockMvc`는 `spring-boot-starter-webmvc-test`를 따로 추가한다.
  `starter`가 빠진 `spring-boot-webmvc-test`도 BOM에 있어서 버전 없이 해결되고
  `@WebMvcTest`까지 컴파일되므로, 이름을 틀려도 빌드가 잡지 못한다.
  대신 `spring-boot-starter-jackson-test`·`spring-boot-resttestclient`가 딸려오지 않는다
- **Jackson 3는 enum을 `name()`이 아니라 `toString()`으로 직렬화한다.**
  API에 노출되는 enum에 `toString()`을 오버라이드하지 않는다.
  표시용 문자열이 필요하면 별도 필드로 둔다. 이것도 빌드와 테스트가 잡지 못한다

### 3. 에러는 `throw ApiException.of(ErrorCode.XXX)`로 던진다

- `GlobalExceptionHandler`가 로깅과 에러 응답 조립을 책임진다.
  컨트롤러에서 `ApiResult.fail(...)`을 직접 반환하지 않는다
- `success(...)`와 `fail(...)`은 둘 다 `ResponseEntity`를 반환한다. 다시 감싸지 않는다

### 4. 원격에 올리기 전에 사람에게 확인받는다

- 로컬 커밋까지는 확인 없이 진행한다. `git push`·`gh pr create`·`gh pr ready`·`gh pr merge`는
  실행 전에 사람에게 확인받는다
- 권한 확인을 건너뛰는 모드(bypass permissions, `--dangerously-skip-permissions`,
  자동 승인 설정)에서도 예외를 두지 않는다.
  도구 실행을 자동 승인한 것이 무엇을 공개할지까지 승인한 것은 아니다
- 한 번 확인받았다고 다음 푸시까지 확인받은 것으로 보지 않는다. 푸시마다 확인한다
- `develop`에 직접 푸시하거나 force-push하는 것은 확인 여부와 무관하게 하지 않는다 (`docs/git.md`)

로컬 커밋은 `git reset`으로 조용히 되돌아가지만 원격은 아니다.
푸시된 브랜치와 열린 PR은 팀 전체에 보이고, CI 실행과 리뷰 요청 알림이 이미 나간 뒤다.

## 커밋 전

`./gradlew spotlessApply`를 직접 실행한다.
pre-commit hook은 `spotlessCheck`로 검사만 하고 자동 수정은 하지 않으며,
`./gradlew installGitHooks`를 실행한 사람에게만 활성화된다 (`docs/git.md`).
