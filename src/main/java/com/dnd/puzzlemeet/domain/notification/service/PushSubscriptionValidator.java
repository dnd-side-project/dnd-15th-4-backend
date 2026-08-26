package com.dnd.puzzlemeet.domain.notification.service;

import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import nl.martijndwars.webpush.Utils;
import org.springframework.stereotype.Component;

@Component
public class PushSubscriptionValidator {

  private static final int PUBLIC_KEY_LENGTH = 65;
  private static final int AUTH_SECRET_LENGTH = 16;
  private static final int UNCOMPRESSED_POINT_PREFIX = 0x04;
  private static final List<String> ALLOWED_HOST_SUFFIXES =
      List.of(
          "fcm.googleapis.com",
          "push.services.mozilla.com",
          "push.apple.com",
          "notify.windows.com");

  public void validate(String endpoint, String p256dh, String auth) {
    try {
      validateEndpointValue(endpoint);
      validateP256dh(p256dh);
      validateAuth(auth);
    } catch (RuntimeException | GeneralSecurityException e) {
      throw ApiException.of(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  public void validateEndpoint(String endpoint) {
    try {
      validateEndpointValue(endpoint);
    } catch (RuntimeException e) {
      throw ApiException.of(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  public void validateEndpointForSending(String endpoint) {
    try {
      validateEndpointValue(endpoint);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("허용되지 않은 WebPush endpoint입니다.");
    }
  }

  private void validateEndpointValue(String endpoint) {
    URI uri = URI.create(endpoint);
    String host = uri.getHost();
    if (!"https".equalsIgnoreCase(uri.getScheme())
        || host == null
        || uri.getRawUserInfo() != null
        || uri.getRawFragment() != null
        || (uri.getPort() != -1 && uri.getPort() != 443)
        || !isAllowedHost(host)) {
      throw new IllegalArgumentException("허용되지 않은 WebPush endpoint입니다.");
    }
  }

  private void validateP256dh(String p256dh) throws GeneralSecurityException {
    byte[] decoded = Base64.getUrlDecoder().decode(p256dh);
    if (decoded.length != PUBLIC_KEY_LENGTH
        || Byte.toUnsignedInt(decoded[0]) != UNCOMPRESSED_POINT_PREFIX) {
      throw new IllegalArgumentException("유효하지 않은 WebPush 공개키입니다.");
    }
    Utils.loadPublicKey(decoded);
  }

  private void validateAuth(String auth) {
    byte[] decoded = Base64.getUrlDecoder().decode(auth);
    if (decoded.length != AUTH_SECRET_LENGTH) {
      throw new IllegalArgumentException("유효하지 않은 WebPush auth secret입니다.");
    }
  }

  private boolean isAllowedHost(String host) {
    String normalizedHost = host.toLowerCase(Locale.ROOT);
    return ALLOWED_HOST_SUFFIXES.stream()
        .anyMatch(suffix -> normalizedHost.equals(suffix) || normalizedHost.endsWith("." + suffix));
  }
}
