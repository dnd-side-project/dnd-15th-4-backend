package com.dnd.puzzlemeet.global.security.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.security.KakaoUnlinkProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class KakaoUnlinkClientTest {

  private static final Long KAKAO_ID = 123456789L;
  private static final String ADMIN_KEY = "test-admin-key";

  private final AtomicReference<RecordedRequest> recordedRequest = new AtomicReference<>();
  private final AtomicReference<MockResponse> mockResponse = new AtomicReference<>();

  private HttpServer server;
  private KakaoUnlinkClient kakaoUnlinkClient;

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/v1/user/unlink", this::handleRequest);
    server.start();
    KakaoUnlinkProperties properties =
        new KakaoUnlinkProperties(
            ADMIN_KEY, "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/user/unlink");
    kakaoUnlinkClient = new KakaoUnlinkClient(properties);
  }

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  @DisplayName("카카오 사용자 연결을 해제하면 응답 회원번호를 반환한다")
  void unlinkReturnsKakaoId() {
    respond(200, "{\"id\":" + KAKAO_ID + "}");

    Long unlinkedId = kakaoUnlinkClient.unlink(KAKAO_ID);

    assertThat(unlinkedId).isEqualTo(KAKAO_ID);
    RecordedRequest request = recordedRequest.get();
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.authorization()).isEqualTo("KakaoAK " + ADMIN_KEY);
    assertThat(request.contentType()).startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
    assertThat(request.form())
        .containsEntry("target_id_type", "user_id")
        .containsEntry("target_id", String.valueOf(KAKAO_ID));
  }

  @Test
  @DisplayName("이미 카카오 연결이 해제된 회원은 멱등 성공으로 처리한다")
  void alreadyUnlinkedIsIdempotent() {
    respond(400, "{\"msg\":\"NotRegisteredUserException\",\"code\":-101}");

    Long unlinkedId = kakaoUnlinkClient.unlink(KAKAO_ID);

    assertThat(unlinkedId).isEqualTo(KAKAO_ID);
  }

  @Test
  @DisplayName("카카오 성공 응답의 회원번호가 요청과 다르면 실패한다")
  void mismatchedResponseIdFails() {
    respond(200, "{\"id\":987654321}");

    ApiException exception =
        assertThrows(ApiException.class, () -> kakaoUnlinkClient.unlink(KAKAO_ID));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_KAKAO_UNLINK_FAILED);
  }

  @Test
  @DisplayName("카카오 연결 해제 API의 다른 오류는 서비스 예외로 변환한다")
  void otherKakaoErrorIsConverted() {
    respond(400, "{\"msg\":\"NotExistUserException\",\"code\":-103}");

    ApiException exception =
        assertThrows(ApiException.class, () -> kakaoUnlinkClient.unlink(KAKAO_ID));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_KAKAO_UNLINK_FAILED);
  }

  @Test
  @DisplayName("카카오 연결 해제 API의 서버 오류는 서비스 예외로 변환한다")
  void kakaoServerErrorIsConverted() {
    respond(503, "{\"msg\":\"service unavailable\",\"code\":-2}");

    ApiException exception =
        assertThrows(ApiException.class, () -> kakaoUnlinkClient.unlink(KAKAO_ID));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_KAKAO_UNLINK_FAILED);
  }

  @Test
  @DisplayName("카카오 연결 해제 API와 통신할 수 없으면 서비스 예외로 변환한다")
  void communicationFailureIsConverted() {
    server.stop(0);
    server = null;

    ApiException exception =
        assertThrows(ApiException.class, () -> kakaoUnlinkClient.unlink(KAKAO_ID));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_KAKAO_UNLINK_FAILED);
  }

  private void respond(int status, String body) {
    mockResponse.set(new MockResponse(status, body));
  }

  private void handleRequest(HttpExchange exchange) throws IOException {
    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    recordedRequest.set(
        new RecordedRequest(
            exchange.getRequestMethod(),
            exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION),
            exchange.getRequestHeaders().getFirst(HttpHeaders.CONTENT_TYPE),
            form(body)));

    MockResponse response = mockResponse.get();
    byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    exchange.sendResponseHeaders(response.status(), bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  private Map<String, String> form(String body) {
    return Arrays.stream(body.split("&"))
        .map(parameter -> parameter.split("=", 2))
        .collect(
            Collectors.toMap(
                pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
  }

  private record RecordedRequest(
      String method, String authorization, String contentType, Map<String, String> form) {}

  private record MockResponse(int status, String body) {}
}
