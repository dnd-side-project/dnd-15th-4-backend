package com.dnd.puzzlemeet.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.s3.AmazonS3Manager;
import com.dnd.puzzlemeet.global.security.client.KakaoUnlinkClient;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WithdrawalCleanupServiceTest {
  @Mock private UserRepository userRepository;
  @Mock private KakaoUnlinkClient kakaoUnlinkClient;
  @Mock private AmazonS3Manager amazonS3Manager;
  private WithdrawalCleanupService service;

  @BeforeEach
  void setUp() {
    service = new WithdrawalCleanupService(userRepository, kakaoUnlinkClient, amazonS3Manager);
  }

  @Test
  @DisplayName("카카오 해제 실패를 기록하고 재가입할 수 있도록 이전 식별자를 비운다")
  void releasesIdentityEvenWhenUnlinkFails() {
    User user = new User(100L, "사용자", null);
    user.withdraw();
    ReflectionTestUtils.setField(user, "id", 1L);
    given(userRepository.findKakaoIdById(1L)).willReturn(Optional.of(100L));
    given(userRepository.findByKakaoIdForUpdate(100L)).willReturn(Optional.of(user));
    willThrow(ApiException.of(ErrorCode.AUTH_KAKAO_UNLINK_FAILED))
        .given(kakaoUnlinkClient)
        .unlink(100L);
    service.unlinkKakao(1L);
    assertThat(user.getKakaoId()).isNull();
    assertThat(user.isWithdrawn()).isTrue();
    verify(kakaoUnlinkClient).unlink(100L);
  }

  @Test
  @DisplayName("재가입이 먼저 이전 식별자를 해제했으면 새 카카오 연결을 건드리지 않는다")
  void skipsUnlinkWhenRejoinAlreadyReleasedIdentity() {
    given(userRepository.findKakaoIdById(1L)).willReturn(Optional.empty());
    service.unlinkKakao(1L);
    verifyNoInteractions(kakaoUnlinkClient);
  }

  @Test
  @DisplayName("카카오 식별자를 조회한 뒤 재가입되면 새 사용자 행을 찾아도 연결을 해제하지 않는다")
  void skipsNewAccountAfterIdentityLookup() {
    User newUser = new User(100L, "새 사용자", null);
    ReflectionTestUtils.setField(newUser, "id", 2L);
    given(userRepository.findKakaoIdById(1L)).willReturn(Optional.of(100L));
    given(userRepository.findByKakaoIdForUpdate(100L)).willReturn(Optional.of(newUser));
    service.unlinkKakao(1L);
    verifyNoInteractions(kakaoUnlinkClient);
  }

  @Test
  @DisplayName("활성 계정은 정리 요청으로 카카오 연결을 해제하지 않는다")
  void skipsActiveUser() {
    User user = new User(100L, "사용자", null);
    ReflectionTestUtils.setField(user, "id", 1L);
    given(userRepository.findKakaoIdById(1L)).willReturn(Optional.of(100L));
    given(userRepository.findByKakaoIdForUpdate(100L)).willReturn(Optional.of(user));
    service.unlinkKakao(1L);
    assertThat(user.getKakaoId()).isEqualTo(100L);
    verifyNoInteractions(kakaoUnlinkClient);
  }

  @Test
  @DisplayName("한 이미지 삭제가 실패해도 나머지 이미지는 각각 한 번 시도한다")
  void continuesAfterImageDeletionFailure() {
    willThrow(ApiException.of(ErrorCode.S3_DELETE_FAILED))
        .given(amazonS3Manager)
        .deletePuzzleImage("first");
    service.deleteImages(List.of("first", "second"));
    verify(amazonS3Manager).deletePuzzleImage("first");
    verify(amazonS3Manager).deletePuzzleImage("second");
  }
}
