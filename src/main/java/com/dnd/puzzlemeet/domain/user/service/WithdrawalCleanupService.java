package com.dnd.puzzlemeet.domain.user.service;

import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.s3.AmazonS3Manager;
import com.dnd.puzzlemeet.global.security.client.KakaoUnlinkClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalCleanupService {
  private final UserRepository userRepository;
  private final KakaoUnlinkClient kakaoUnlinkClient;
  private final AmazonS3Manager amazonS3Manager;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void unlinkKakao(Long userId) {
    Long kakaoId = userRepository.findKakaoIdById(userId).orElse(null);
    if (kakaoId == null) {
      return;
    }
    // 로그인과 같은 유니크 인덱스로 잠가 인덱스와 PK의 잠금 순서가 뒤집히지 않게 한다.
    User user = userRepository.findByKakaoIdForUpdate(kakaoId).orElse(null);
    if (user == null || !user.getId().equals(userId) || !user.isWithdrawn()) {
      return;
    }
    try {
      kakaoUnlinkClient.unlink(user.getKakaoId());
    } catch (RuntimeException exception) {
      log.info("[탈퇴 정리 실패] kind=KAKAO_UNLINK, errorCode={}", resolveErrorCode(exception).getCode());
    }
    user.releaseKakaoIdentity();
  }

  public void deleteImages(List<String> imageUrls) {
    for (String imageUrl : imageUrls) {
      try {
        amazonS3Manager.deletePuzzleImage(imageUrl);
      } catch (RuntimeException exception) {
        log.info("[탈퇴 정리 실패] kind=S3_DELETE, errorCode={}", resolveErrorCode(exception).getCode());
      }
    }
  }

  private ErrorCode resolveErrorCode(RuntimeException exception) {
    return exception instanceof ApiException apiException
        ? apiException.getErrorCode()
        : ErrorCode.INTERNAL_SERVER_ERROR;
  }
}
