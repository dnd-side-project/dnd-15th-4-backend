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

`ErrorCode`는 인터페이스라 어노테이션 요소 타입으로 쓸 수 없다(primitive · String · Class · enum ·
어노테이션과 그 배열만 허용된다). 그래서 두 어노테이션은 `Class<? extends ErrorCode> enumClass()`와
`String name()`을 받는다.

- 에러 코드가 1개면 `@ApiErrorCodeExample(enumClass = CommonErrorCode.class, name = "RESOURCE_NOT_FOUND")`,
  2개 이상이면
  `@ApiErrorCodeExamples({@ApiErrorCodeExample(enumClass = CommonErrorCode.class, name = "X"), @ApiErrorCodeExample(enumClass = CommonErrorCode.class, name = "Y")})`를 쓴다.
  `ApiErrorCodeExample`은 `value()`가 없는 키워드 인자 전용 형태라 위치 인자로 못 쓴다.
  `ApiErrorCodeExamples`는 `value()`가 있어서 `{...}` 배열은 그대로 위치 인자로 쓴다
- **한 메서드에 둘을 같이 붙이지 않는다.** `SwaggerConfig`가 복수를 먼저 보고 단수를 무시한다
- status·code·message와 예시 JSON은 `enumClass`에서 `name`과 일치하는 상수를 찾아 만든다. 예시를 직접 쓰지 않는다
- 같은 status인 코드는 하나의 응답 아래 예시 여러 개로 묶인다
- 컨트롤러가 직접 던지지 않아도 **FE가 code로 분기해야 하는 것은 적는다.**
  서비스에서 던지는 코드도 포함이다
- `name`은 문자열이라 오타를 컴파일러가 잡지 못한다. `SwaggerConfig`가 `enumClass.getEnumConstants()`를
  순회해도 못 찾으면 `IllegalStateException`을 던진다. 다만 `OperationCustomizer`는 springdoc이 문서를
  만들 때(첫 `/v3/api-docs` 요청 시점)만 호출되므로 **애플리케이션 기동 자체는 실패하지 않는다.**
  오타는 Swagger UI를 열어봐야 드러난다


## 인증

`bearerAuth`(HTTP bearer, JWT) security scheme이 전역으로 걸려 있다.
Spring Security 7의 `oauth2ResourceServer`가 Bearer access token을 실제로 검증하므로,
Swagger UI에서 Authorize에 access token을 넣으면 그 토큰으로 인증된 요청이 나간다.