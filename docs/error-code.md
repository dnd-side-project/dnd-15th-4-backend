# 에러 코드

## ErrorCode

`global/response/ErrorCode` 하나의 enum에 공통 에러와 도메인 에러를 전부 모아서 관리한다.
인터페이스나 도메인별 `<도메인>ErrorCode`로 쪼개지 않는다.

도메인 에러가 필요하면 이 enum에 상수를 바로 추가한다.
HTTP 상태 오름차순으로 그룹을 나누고 `// 4xx ...` 주석으로 구분한다 — 기존 그룹에 없는
상태면 새 그룹을 추가한다.

## 상수 규칙

- 도메인에서 추가하는 code 문자열은 `<도메인>_` 접두사로 시작한다. 예: `AUTH_TOKEN_INVALID`,
  `MEETING_NOT_FOUND`
- `message`는 사용자에게 그대로 노출해도 되는 한국어 문장으로 쓴다.
  FE는 code로 분기하고, message는 기본 노출 문구다

## SuccessCode

- 기본은 `OK`를 쓴다. HTTP 상태나 메시지가 달라야 하는 응답에만 상수를 추가한다.
  201로 응답하는 생성 API는 `CREATED`를 쓴다
