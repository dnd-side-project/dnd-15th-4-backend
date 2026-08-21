package com.dnd.puzzlemeet.global.security.client;

import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.security.KakaoUnlinkProperties;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class KakaoUnlinkClient {

  private static final int CONNECT_TIMEOUT_MILLIS = 2_000;
  private static final int READ_TIMEOUT_MILLIS = 3_000;
  private static final int ALREADY_UNLINKED_CODE = -101;
  private static final String AUTHORIZATION_SCHEME = "KakaoAK ";
  private static final String TARGET_ID_TYPE = "user_id";

  private final RestClient restClient;
  private final String unlinkUri;
  private final String adminKey;

  public KakaoUnlinkClient(KakaoUnlinkProperties kakaoUnlinkProperties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
    requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);
    this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    this.unlinkUri = kakaoUnlinkProperties.uri();
    this.adminKey = kakaoUnlinkProperties.adminKey();
  }

  public Long unlink(Long kakaoId) {
    long start = System.currentTimeMillis();
    try {
      return restClient
          .post()
          .uri(unlinkUri)
          .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_SCHEME + adminKey)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(unlinkRequestBody(kakaoId))
          .exchangeForRequiredValue(
              (request, response) -> handleResponse(response, kakaoId, start));
    } catch (RestClientException e) {
      log.info("[카카오 연동] 연결 해제 실패, elapsedMs={}", System.currentTimeMillis() - start);
      throw ApiException.of(ErrorCode.AUTH_KAKAO_UNLINK_FAILED);
    }
  }

  private Long handleResponse(ConvertibleClientHttpResponse response, Long kakaoId, long start)
      throws IOException {
    HttpStatusCode status = response.getStatusCode();
    if (status.is2xxSuccessful()) {
      KakaoUnlinkResponse body = response.bodyTo(KakaoUnlinkResponse.class);
      if (body == null || body.id() == null || !body.id().equals(kakaoId)) {
        log.info("[카카오 연동] 연결 해제 실패, 응답 id 불일치, elapsedMs={}", System.currentTimeMillis() - start);
        throw ApiException.of(ErrorCode.AUTH_KAKAO_UNLINK_FAILED);
      }
      log.info("[카카오 연동] 연결 해제 성공, elapsedMs={}", System.currentTimeMillis() - start);
      return body.id();
    }

    if (status.value() == 400 && isAlreadyUnlinked(response)) {
      log.info("[카카오 연동] 이미 연결 해제됨, elapsedMs={}", System.currentTimeMillis() - start);
      return kakaoId;
    }

    log.info(
        "[카카오 연동] 연결 해제 실패, status={}, elapsedMs={}",
        status.value(),
        System.currentTimeMillis() - start);
    throw ApiException.of(ErrorCode.AUTH_KAKAO_UNLINK_FAILED);
  }

  private boolean isAlreadyUnlinked(ConvertibleClientHttpResponse response) {
    try {
      KakaoErrorResponse errorResponse = response.bodyTo(KakaoErrorResponse.class);
      return errorResponse != null
          && errorResponse.code() != null
          && errorResponse.code() == ALREADY_UNLINKED_CODE;
    } catch (RuntimeException e) {
      return false;
    }
  }

  private MultiValueMap<String, String> unlinkRequestBody(Long kakaoId) {
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("target_id_type", TARGET_ID_TYPE);
    body.add("target_id", String.valueOf(kakaoId));
    return body;
  }
}
