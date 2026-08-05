package com.dnd.puzzlemeet.global.security.client;

import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.security.KakaoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class KakaoTokenClient {

  private static final int CONNECT_TIMEOUT_MILLIS = 2_000;
  private static final int READ_TIMEOUT_MILLIS = 3_000;
  private static final String GRANT_TYPE = "authorization_code";

  private final RestClient restClient;
  private final KakaoProperties kakaoProperties;

  public KakaoTokenClient(KakaoProperties kakaoProperties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
    requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);
    this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    this.kakaoProperties = kakaoProperties;
  }

  public String exchangeAuthorizationCode(String authorizationCode) {
    long start = System.currentTimeMillis();
    try {
      KakaoTokenResponse response =
          restClient
              .post()
              .uri(kakaoProperties.tokenUri())
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(tokenRequestBody(authorizationCode))
              .retrieve()
              .body(KakaoTokenResponse.class);
      if (response == null || !StringUtils.hasText(response.accessToken())) {
        log.info(
            "[카카오 연동] 토큰 교환 실패, 응답에 access_token이 없음, elapsedMs={}",
            System.currentTimeMillis() - start);
        throw ApiException.of(ErrorCode.AUTH_KAKAO_UNAVAILABLE);
      }
      log.info("[카카오 연동] 토큰 교환 성공, elapsedMs={}", System.currentTimeMillis() - start);
      return response.accessToken();
    } catch (HttpClientErrorException e) {
      log.info(
          "[카카오 연동] 토큰 교환 실패, status={}, elapsedMs={}",
          e.getStatusCode().value(),
          System.currentTimeMillis() - start);
      throw ApiException.of(ErrorCode.AUTH_KAKAO_CODE_INVALID);
    } catch (RestClientException e) {
      log.info("[카카오 연동] 토큰 교환 실패, elapsedMs={}", System.currentTimeMillis() - start);
      throw ApiException.of(ErrorCode.AUTH_KAKAO_UNAVAILABLE);
    }
  }

  private MultiValueMap<String, String> tokenRequestBody(String authorizationCode) {
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", GRANT_TYPE);
    body.add("client_id", kakaoProperties.clientId());
    body.add("redirect_uri", kakaoProperties.redirectUri());
    body.add("code", authorizationCode);
    if (StringUtils.hasText(kakaoProperties.clientSecret())) {
      body.add("client_secret", kakaoProperties.clientSecret());
    }
    return body;
  }
}
