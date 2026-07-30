# 에러 코드

## 도메인 에러 코드를 추가하는 순서

도메인 에러가 필요하면 공통 enum에 상수를 추가하지 말고 아래 순서로 한다.
도메인 `ErrorCode`가 필요한 첫 PR에서, 상수를 추가하기 전에 이 추출부터 한다.

1. 현재 `ErrorCode` enum을 `ErrorCode` **인터페이스**로 추출한다.
   메서드는 `HttpStatus getHttpStatus()` / `String getCode()` / `String getMessage()` 3개뿐이고,
   레지스트리나 default 메서드는 두지 않는다
2. 기존 enum 본체는 `CommonErrorCode implements ErrorCode`로 바꾼다.
   호출부는 `grep -rn "ErrorCode\." src/`로 전부 확인한 뒤 치환한다
3. 도메인별로 `<도메인>ErrorCode implements ErrorCode`를 `domain.<도메인>.exception`에 만든다
4. `global.annotation`의 `@ApiErrorCodeExample`·`@ApiErrorCodeExamples`를 같은 PR에서 바꾼다.
   인터페이스는 어노테이션 요소 타입이 될 수 없어서 그대로 두면 컴파일되지 않는다 (`swagger.md`)

인터페이스와 `CommonErrorCode`는 지금 enum이 있는 `global.response`에 그대로 둔다.

## 상수 규칙

- 도메인 `ErrorCode`의 code 문자열은 `<도메인>_` 접두사로 시작한다. 예: `MEETING_NOT_FOUND`
- `message`는 사용자에게 그대로 노출해도 되는 한국어 문장으로 쓴다.
  FE는 code로 분기하고, message는 기본 노출 문구다

## SuccessCode

- 기본은 `OK`를 쓴다. HTTP 상태나 메시지가 달라야 하는 응답에만 상수를 추가한다.
  201로 응답하는 생성 API는 `CREATED`를 쓴다
