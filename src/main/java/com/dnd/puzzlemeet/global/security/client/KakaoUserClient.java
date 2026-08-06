package com.dnd.puzzlemeet.global.security.client;

import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.security.KakaoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class KakaoUserClient {

  private static final int CONNECT_TIMEOUT_MILLIS = 2_000;
  private static final int READ_TIMEOUT_MILLIS = 3_000;

  private final RestClient restClient;
  private final String userInfoUri;

  public KakaoUserClient(KakaoProperties kakaoProperties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
    requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);
    this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    this.userInfoUri = kakaoProperties.userInfoUri();
  }

  public KakaoUserResponse getUserInfo(String kakaoAccessToken) {
    long start = System.currentTimeMillis();
    try {
      KakaoUserResponse response =
          restClient
              .get()
              .uri(userInfoUri)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
              .retrieve()
              .body(KakaoUserResponse.class);
      if (response == null || response.id() == null) {
        log.info(
            "[카카오 연동] 사용자 조회 실패, 응답 본문에 id가 없음, elapsedMs={}", System.currentTimeMillis() - start);
        throw ApiException.of(ErrorCode.AUTH_KAKAO_UNAVAILABLE);
      }
      log.info("[카카오 연동] 사용자 조회 성공, elapsedMs={}", System.currentTimeMillis() - start);
      return response;
    } catch (HttpClientErrorException.Unauthorized e) {
      log.info("[카카오 연동] 사용자 조회 실패, status=401, elapsedMs={}", System.currentTimeMillis() - start);
      throw ApiException.of(ErrorCode.AUTH_KAKAO_UNAUTHORIZED);
    } catch (RestClientException e) {
      log.info("[카카오 연동] 사용자 조회 실패, elapsedMs={}", System.currentTimeMillis() - start);
      throw ApiException.of(ErrorCode.AUTH_KAKAO_UNAVAILABLE);
    }
  }
}
