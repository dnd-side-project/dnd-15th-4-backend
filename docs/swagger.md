# API 문서

springdoc-openapi가 스펙을 `/v3/api-docs`로, Swagger UI를 `/swagger-ui.html`로 낸다.
설정은 `global/config/SwaggerConfig` 한 곳에 두고,
에러 응답 어노테이션 두 개는 `global/annotation`에 둔다.

## 컨트롤러에 붙이는 것

| 대상 | 어노테이션 |
|---|---|
| 컨트롤러 클래스 | `@Tag(name = ..., description = ...)` |
| 핸들러 메서드 | `@Operation(summary = ..., description = ...)` |
| 요청·응답 record 필드 | `@Schema(description = ..., example = ...)` |
| 에러 응답 | `@ApiErrorCodeExample` / `@ApiErrorCodeExamples` |

`name`·`summary`·`description`은 한국어 한 문장으로 쓴다.

## 에러 응답

- 에러 코드가 1개면 `@ApiErrorCodeExample(ErrorCode.X)`,
  2개 이상이면 `@ApiErrorCodeExamples({ErrorCode.X, ErrorCode.Y})`를 쓴다
- **한 메서드에 둘을 같이 붙이지 않는다.** `SwaggerConfig`가 복수를 먼저 보고 단수를 무시한다
- status·code·message와 예시 JSON은 `ErrorCode` 상수에서 만든다. 예시를 직접 쓰지 않는다
- 같은 status인 코드는 하나의 응답 아래 예시 여러 개로 묶인다
- 컨트롤러가 직접 던지지 않아도 **FE가 code로 분기해야 하는 것은 적는다.**
  서비스에서 던지는 코드도 포함이다

**왜 어노테이션에 나열하나**

`OperationCustomizer`가 받는 것은 `HandlerMethod`, 즉 리플렉션 정보뿐이라
메서드 본문의 `ApiException.of(ErrorCode.X)` 호출을 볼 수 없다.
예외는 대부분 서비스에서 던지므로 컨트롤러 바이트코드를 뒤져도 잡히지 않는다.

## ErrorCode를 인터페이스로 추출할 때

`error-code.md`의 추출을 하는 PR에서 두 어노테이션도 같이 바꾼다.
어노테이션 요소 타입은 primitive · String · Class · enum · 어노테이션과 그 배열만 허용해서,
`ErrorCode`가 인터페이스가 되는 순간 `ErrorCode value();`는 컴파일되지 않는다.
`Class<? extends ErrorCode>`와 상수명을 받는 형태로 바꾼다.
상수명은 문자열이라 오타를 컴파일러가 잡지 못하므로, 찾지 못한 상수명은 기동 시 예외로 드러낸다.

## 인증

`bearerAuth`(HTTP bearer, JWT) security scheme이 전역으로 걸려 있다.
Spring Security가 아직 없어서 Swagger UI의 Authorize는 요청에 헤더를 붙이는 것까지만 한다.
