package com.dnd.puzzlemeet.domain.notification.service;

import com.dnd.puzzlemeet.domain.notification.config.WebPushProperties;
import com.dnd.puzzlemeet.domain.notification.dto.PushSubscriptionCreateRequest;
import com.dnd.puzzlemeet.domain.notification.dto.PushSubscriptionDeleteRequest;
import com.dnd.puzzlemeet.domain.notification.dto.PushVapidPublicKeyResponse;
import com.dnd.puzzlemeet.domain.notification.repository.PushSubscriptionRepository;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushSubscriptionService {

  private final PushSubscriptionRepository pushSubscriptionRepository;
  private final PushSubscriptionValidator pushSubscriptionValidator;
  private final UserRepository userRepository;
  private final WebPushProperties webPushProperties;

  public PushVapidPublicKeyResponse getVapidPublicKey() {
    return new PushVapidPublicKeyResponse(webPushProperties.vapid().publicKey());
  }

  @Transactional
  public void upsert(Long userId, PushSubscriptionCreateRequest request) {
    String p256dh = request.keys().p256dh();
    String auth = request.keys().auth();
    pushSubscriptionValidator.validate(request.endpoint(), p256dh, auth);
    userRepository
        .findActiveByIdForUpdate(userId)
        .orElseThrow(() -> ApiException.of(ErrorCode.USER_NOT_FOUND));

    pushSubscriptionRepository.upsertForActiveUser(
        userId, request.endpoint(), sha256(request.endpoint()), p256dh, auth);
  }

  @Transactional
  public void delete(Long userId, PushSubscriptionDeleteRequest request) {
    pushSubscriptionValidator.validateEndpoint(request.endpoint());
    pushSubscriptionRepository.deleteByUserIdAndEndpointHash(userId, sha256(request.endpoint()));
  }

  private String sha256(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm을 사용할 수 없습니다.", e);
    }
  }
}
