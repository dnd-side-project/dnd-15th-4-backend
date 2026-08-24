package com.dnd.puzzlemeet.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.dnd.puzzlemeet.domain.auth.repository.RefreshTokenRepository;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRole;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus;
import com.dnd.puzzlemeet.domain.meeting.entity.TransportType;
import com.dnd.puzzlemeet.domain.meeting.entity.TravelMode;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRouteRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.puzzle.entity.MemberImage;
import com.dnd.puzzlemeet.domain.puzzle.repository.MemberImageRepository;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.s3.AmazonS3Manager;
import com.dnd.puzzlemeet.global.security.client.KakaoUnlinkClient;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserWithdrawalServiceTest {

  private static final String DEFAULT_MEMBER_IMAGE_URL =
      "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/puzzles/_+(9)+4.png";

  @Mock private UserRepository userRepository;
  @Mock private MeetingRepository meetingRepository;
  @Mock private MeetingMemberRepository meetingMemberRepository;
  @Mock private MeetingMemberRouteRepository meetingMemberRouteRepository;
  @Mock private MemberImageRepository memberImageRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private KakaoUnlinkClient kakaoUnlinkClient;
  @Mock private AmazonS3Manager amazonS3Manager;
  @Mock private EntityManager entityManager;

  private UserWithdrawalService userWithdrawalService;

  @BeforeEach
  void setUp() {
    userWithdrawalService =
        new UserWithdrawalService(
            userRepository,
            meetingRepository,
            meetingMemberRepository,
            meetingMemberRouteRepository,
            memberImageRepository,
            refreshTokenRepository,
            kakaoUnlinkClient,
            amazonS3Manager,
            entityManager);
  }

  @Test
  @DisplayName("회원 탈퇴에 성공하면 카카오 연결과 토큰을 제거하고 사용자 및 약속 참여 정보를 익명화한다")
  void withdrawsAndAnonymizesUserData() {
    User user = user();
    MeetingMember meetingMember = meetingMember(user);
    MemberImage memberImage =
        new MemberImage(
            meetingMember,
            "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/puzzles/uploaded.png",
            false);
    MemberImage defaultMemberImage = new MemberImage(meetingMember, DEFAULT_MEMBER_IMAGE_URL, true);
    given(userRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.of(user));
    given(
            meetingRepository.existsByHostUserIdAndStatusIn(
                1L, List.of(MeetingStatus.WAITING, MeetingStatus.IN_PROGRESS)))
        .willReturn(false);
    given(meetingMemberRepository.findAllByUserId(1L)).willReturn(List.of(meetingMember));
    given(memberImageRepository.findAllByMeetingMemberIdIn(List.of(10L)))
        .willReturn(List.of(memberImage, defaultMemberImage));

    userWithdrawalService.withdraw(1L);

    InOrder inOrder =
        inOrder(
            meetingMemberRouteRepository,
            refreshTokenRepository,
            entityManager,
            kakaoUnlinkClient,
            amazonS3Manager);
    inOrder.verify(meetingMemberRouteRepository).deleteAllByMeetingMemberIdIn(List.of(10L));
    inOrder.verify(refreshTokenRepository).deleteAllByUserId(1L);
    inOrder.verify(entityManager).flush();
    inOrder.verify(kakaoUnlinkClient).unlink(100L);
    inOrder
        .verify(amazonS3Manager)
        .deletePuzzleImage(
            "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/puzzles/uploaded.png");
    verify(amazonS3Manager, never()).deletePuzzleImage(DEFAULT_MEMBER_IMAGE_URL);
    assertThat(user.getKakaoId()).isNull();
    assertThat(user.getEmail()).isNull();
    assertThat(user.getNickname()).isEqualTo("탈퇴한 사용자");
    assertThat(user.getProfileImageUrl()).isNull();
    assertThat(user.getDeletedAt()).isNotNull();
    assertThat(meetingMember.getNickname()).isEqualTo("탈퇴한 사용자");
    assertThat(meetingMember.isCustomNickname()).isFalse();
    assertThat(meetingMember.getTransportType()).isNull();
    assertThat(meetingMember.getTransportLine()).isNull();
    assertThat(meetingMember.getDepartedAt()).isNull();
    assertThat(meetingMember.getArrivedAt()).isNull();
    assertThat(meetingMember.getEstimatedDurationSeconds()).isNull();
    assertThat(meetingMember.getDurationCalculatedAt()).isNull();
    assertThat(meetingMember.getCurrentLatitude()).isNull();
    assertThat(meetingMember.getCurrentLongitude()).isNull();
    assertThat(meetingMember.getDepartureName()).isNull();
    assertThat(meetingMember.getDepartureLatitude()).isNull();
    assertThat(meetingMember.getDepartureLongitude()).isNull();
    assertThat(meetingMember.isLocationNotificationEnabled()).isFalse();
    assertThat(meetingMember.isFriendArrivalNotificationEnabled()).isFalse();
    assertThat(meetingMember.isChatBubbleNotificationEnabled()).isFalse();
    assertThat(memberImage.getImageUrl()).isEqualTo(DEFAULT_MEMBER_IMAGE_URL);
    assertThat(memberImage.isDefaultImage()).isTrue();
    assertThat(defaultMemberImage.getImageUrl()).isEqualTo(DEFAULT_MEMBER_IMAGE_URL);
    assertThat(defaultMemberImage.isDefaultImage()).isTrue();
  }

  @Test
  @DisplayName("DB 변경을 flush하고 카카오 연결을 해제한 뒤 S3 이미지를 삭제한다")
  void flushesLocalChangesBeforeS3ImageDeletionFails() {
    User user = user();
    MeetingMember meetingMember = meetingMember(user);
    String uploadedImageUrl =
        "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/puzzles/uploaded.png";
    MemberImage memberImage = new MemberImage(meetingMember, uploadedImageUrl, false);
    given(userRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.of(user));
    given(
            meetingRepository.existsByHostUserIdAndStatusIn(
                1L, List.of(MeetingStatus.WAITING, MeetingStatus.IN_PROGRESS)))
        .willReturn(false);
    given(meetingMemberRepository.findAllByUserId(1L)).willReturn(List.of(meetingMember));
    given(memberImageRepository.findAllByMeetingMemberIdIn(List.of(10L)))
        .willReturn(List.of(memberImage));
    willThrow(ApiException.of(ErrorCode.S3_DELETE_FAILED))
        .given(amazonS3Manager)
        .deletePuzzleImage(uploadedImageUrl);

    ApiException exception =
        assertThrows(ApiException.class, () -> userWithdrawalService.withdraw(1L));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.S3_DELETE_FAILED);
    InOrder inOrder = inOrder(entityManager, kakaoUnlinkClient, amazonS3Manager);
    inOrder.verify(entityManager).flush();
    inOrder.verify(kakaoUnlinkClient).unlink(100L);
    inOrder.verify(amazonS3Manager).deletePuzzleImage(uploadedImageUrl);
  }

  @Test
  @DisplayName("활성 약속의 방장은 탈퇴할 수 없고 카카오 연결 및 로컬 데이터를 변경하지 않는다")
  void rejectsWithdrawalWhenUserHostsActiveMeeting() {
    User user = user();
    given(userRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.of(user));
    given(
            meetingRepository.existsByHostUserIdAndStatusIn(
                1L, List.of(MeetingStatus.WAITING, MeetingStatus.IN_PROGRESS)))
        .willReturn(true);

    ApiException exception =
        assertThrows(ApiException.class, () -> userWithdrawalService.withdraw(1L));

    assertThat(exception.getErrorCode())
        .isEqualTo(ErrorCode.USER_WITHDRAWAL_BLOCKED_BY_HOSTED_MEETING);
    verifyNoInteractions(kakaoUnlinkClient);
    verify(refreshTokenRepository, never()).deleteAllByUserId(1L);
    assertThat(user.getKakaoId()).isEqualTo(100L);
    assertThat(user.getDeletedAt()).isNull();
  }

  @Test
  @DisplayName("DB 변경을 flush한 뒤 카카오 연결 해제에 실패하면 예외를 전달하고 S3 삭제를 건너뛴다")
  void propagatesKakaoUnlinkFailureAfterFlushingLocalChanges() {
    User user = user();
    given(userRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.of(user));
    given(
            meetingRepository.existsByHostUserIdAndStatusIn(
                1L, List.of(MeetingStatus.WAITING, MeetingStatus.IN_PROGRESS)))
        .willReturn(false);
    willThrow(new IllegalStateException("unlink failed")).given(kakaoUnlinkClient).unlink(100L);

    assertThrows(IllegalStateException.class, () -> userWithdrawalService.withdraw(1L));

    InOrder inOrder = inOrder(refreshTokenRepository, entityManager, kakaoUnlinkClient);
    inOrder.verify(refreshTokenRepository).deleteAllByUserId(1L);
    inOrder.verify(entityManager).flush();
    inOrder.verify(kakaoUnlinkClient).unlink(100L);
    verifyNoInteractions(amazonS3Manager);
  }

  @Test
  @DisplayName("활성 사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
  void rejectsWithdrawalWhenActiveUserNotFound() {
    given(userRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.empty());

    ApiException exception =
        assertThrows(ApiException.class, () -> userWithdrawalService.withdraw(1L));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    verifyNoInteractions(meetingRepository);
    verifyNoInteractions(kakaoUnlinkClient);
  }

  private User user() {
    User user = new User(100L, "효창", "https://img.kakao.com/profile.png", "hyochang@example.com");
    ReflectionTestUtils.setField(user, "id", 1L);
    return user;
  }

  private MeetingMember meetingMember(User user) {
    MeetingMember meetingMember =
        new MeetingMember(
            null, user, MeetingMemberRole.GUEST, "약속 닉네임", "https://img.kakao.com/profile.png");
    ReflectionTestUtils.setField(meetingMember, "id", 10L);
    meetingMember.updateTransport(TransportType.SUBWAY, "2호선");
    meetingMember.depart();
    meetingMember.arrive();
    meetingMember.updateEstimatedDuration(1_200);
    meetingMember.updateCurrentLocation(
        BigDecimal.valueOf(37.1234567), BigDecimal.valueOf(127.1234567));
    meetingMember.updateDeparture(
        "강남역", BigDecimal.valueOf(37.4979), BigDecimal.valueOf(127.0276), TravelMode.TRANSIT);
    meetingMember.changeNickname("개인 닉네임");
    meetingMember.updateNotificationSettings(true, true, true);
    return meetingMember;
  }
}
