package com.dnd.puzzlemeet.domain.auth.controller;

import static com.dnd.puzzlemeet.domain.auth.service.AuthCookieProvider.OAUTH_STATE_COOKIE;
import static com.dnd.puzzlemeet.domain.auth.service.AuthCookieProvider.REFRESH_TOKEN_COOKIE;

import com.dnd.puzzlemeet.domain.auth.dto.AuthReissueResponse;
import com.dnd.puzzlemeet.domain.auth.service.AuthCookieProvider;
import com.dnd.puzzlemeet.domain.auth.service.AuthService;
import com.dnd.puzzlemeet.domain.auth.service.AuthService.TokenPair;
import com.dnd.puzzlemeet.global.annotation.ApiErrorCodeExamples;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ApiResult;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.security.FrontendProperties;
import com.dnd.puzzlemeet.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "인증", description = "카카오 소셜 로그인과 자체 토큰 발급·재발급·로그아웃 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final AuthCookieProvider authCookieProvider;
  private final FrontendProperties frontendProperties;

  @Operation(
      summary = "카카오 로그인 시작",
      description =
          "state를 발급해 쿠키에 심고 카카오 인가 페이지로 302 리다이렉트한다. 브라우저 주소 이동으로 호출해야 하며 XHR로는 동작하지 않는다.")
  @GetMapping("/kakao/authorize")
  public ResponseEntity<Void> authorizeWithKakao() {
    String state = UUID.randomUUID().toString();
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.SET_COOKIE, authCookieProvider.oauthState(state).toString())
        .location(authService.buildKakaoAuthorizeUri(state))
        .build();
  }

  @Operation(
      summary = "카카오 로그인 콜백",
      description =
          "카카오가 인가 코드를 실어 호출한다. state를 검증하고 토큰을 발급한 뒤 refresh token을 쿠키에 심고 프론트로 302 리다이렉트한다."
              + " 실패하면 프론트 주소에 error 쿼리 파라미터를 붙여 리다이렉트한다.")
  @GetMapping("/kakao/callback")
  public ResponseEntity<Void> handleKakaoCallback(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String error,
      @CookieValue(name = OAUTH_STATE_COOKIE, required = false) String storedState) {

    ResponseEntity.BodyBuilder response =
        ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.SET_COOKIE, authCookieProvider.expiredOauthState().toString());

    try {
      authService.verifyKakaoCallback(error, state, storedState);
      TokenPair tokenPair = authService.loginWithKakao(code);
      return response
          .header(
              HttpHeaders.SET_COOKIE,
              authCookieProvider.refreshToken(tokenPair.refreshToken()).toString())
          .location(URI.create(frontendProperties.loginRedirectUri()))
          .build();
    } catch (ApiException e) {
      return response.location(failureUri(e.getErrorCode())).build();
    }
  }

  @Operation(
      summary = "토큰 재발급",
      description =
          "refresh token 쿠키를 검증하고 회전시켜 새 access token을 응답 본문으로, 새 refresh token을 쿠키로 내린다."
              + " Content-Type이 application/json이어야 한다.",
      requestBody =
          @RequestBody(
              description =
                  "서버가 읽지 않는 자리로, 크로스사이트 요청을 막기 위한 Content-Type 요구를 Swagger UI가 지키게 하려고 선언한다.",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(example = "{}"))))
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_REFRESH_TOKEN_INVALID,
    ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED,
    ErrorCode.UNSUPPORTED_MEDIA_TYPE
  })
  @PostMapping(value = "/reissue", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResult<AuthReissueResponse>> reissue(
      @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
      HttpServletResponse response) {
    TokenPair tokenPair = authService.reissue(refreshToken);
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        authCookieProvider.refreshToken(tokenPair.refreshToken()).toString());
    return ApiResult.success(AuthReissueResponse.from(tokenPair.accessToken()));
  }

  @Operation(
      summary = "로그아웃",
      description =
          "refresh token 쿠키에 해당하는 세션 하나만 삭제하고 쿠키를 만료시킨다. Content-Type이 application/json이어야 한다.",
      requestBody =
          @RequestBody(
              description =
                  "서버가 읽지 않는 자리로, 크로스사이트 요청을 막기 위한 Content-Type 요구를 Swagger UI가 지키게 하려고 선언한다.",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(example = "{}"))))
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.AUTH_REFRESH_TOKEN_INVALID,
    ErrorCode.UNSUPPORTED_MEDIA_TYPE
  })
  @PostMapping(value = "/logout", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApiResult<Void>> logout(
      @AuthenticationPrincipal UserPrincipal principal,
      @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
      HttpServletResponse response) {
    authService.logout(principal.id(), refreshToken);
    response.addHeader(HttpHeaders.SET_COOKIE, authCookieProvider.expiredRefreshToken().toString());
    return ApiResult.success(null);
  }

  private URI failureUri(ErrorCode errorCode) {
    return UriComponentsBuilder.fromUriString(frontendProperties.loginRedirectUri())
        .queryParam("error", errorCode.getCode())
        .encode()
        .build()
        .toUri();
  }
}
