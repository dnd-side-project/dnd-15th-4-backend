package com.dnd.puzzlemeet.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import com.dnd.puzzlemeet.domain.user.dto.UserMeResponse;
import com.dnd.puzzlemeet.domain.user.dto.UserNotificationSettingsResponse;
import com.dnd.puzzlemeet.domain.user.dto.UserNotificationSettingsUpdateRequest;
import com.dnd.puzzlemeet.domain.user.dto.UserUpdateRequest;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository);
  }

  @Test
  @DisplayName("존재하는 사용자를 조회하면 프로필 정보를 응답으로 돌려준다")
  void returnsUserWhenFound() {
    User user = new User(100L, "효창", "https://img.kakao.com/a.jpg", "puzzlemeet@example.com");
    ReflectionTestUtils.setField(user, "id", 1L);
    given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));

    UserMeResponse response = userService.getMe(1L);

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.email()).isEqualTo("puzzlemeet@example.com");
    assertThat(response.nickname()).isEqualTo("효창");
    assertThat(response.profileImageUrl()).isEqualTo("https://img.kakao.com/a.jpg");
  }

  @Test
  @DisplayName("존재하지 않는 사용자를 조회하면 USER_NOT_FOUND 예외가 발생한다")
  void throwsWhenUserNotFound() {
    given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.empty());

    ApiException exception = assertThrows(ApiException.class, () -> userService.getMe(1L));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("존재하는 사용자의 닉네임을 수정하면 변경된 프로필 정보를 돌려준다")
  void updatesNicknameWhenUserFound() {
    User user = new User(100L, "효창", "https://img.kakao.com/a.jpg", "puzzlemeet@example.com");
    ReflectionTestUtils.setField(user, "id", 1L);
    given(userRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.of(user));

    UserMeResponse response = userService.updateMe(1L, new UserUpdateRequest("새닉네임"));

    assertThat(user.getNickname()).isEqualTo("새닉네임");
    assertThat(response.email()).isEqualTo("puzzlemeet@example.com");
    assertThat(response.nickname()).isEqualTo("새닉네임");
    assertThat(response.profileImageUrl()).isEqualTo("https://img.kakao.com/a.jpg");
  }

  @Test
  @DisplayName("신규 사용자의 알림 기본 설정은 모두 활성화되어 있다")
  void returnsEnabledNotificationDefaults() {
    User user = new User(100L, "효창", "https://img.kakao.com/a.jpg", "puzzlemeet@example.com");
    given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));

    UserNotificationSettingsResponse response = userService.getNotificationSettings(1L);

    assertThat(response.locationPermission()).isTrue();
    assertThat(response.friendArrival()).isTrue();
    assertThat(response.chatBubble()).isTrue();
  }

  @Test
  @DisplayName("알림 기본 설정을 전체 교체하고 변경된 값을 반환한다")
  void replacesNotificationSettings() {
    User user = new User(100L, "효창", "https://img.kakao.com/a.jpg", "puzzlemeet@example.com");
    given(userRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.of(user));

    UserNotificationSettingsResponse response =
        userService.updateNotificationSettings(
            1L, new UserNotificationSettingsUpdateRequest(false, true, false));

    assertThat(user.isLocationNotificationEnabled()).isFalse();
    assertThat(user.isFriendArrivalNotificationEnabled()).isTrue();
    assertThat(user.isChatBubbleNotificationEnabled()).isFalse();
    assertThat(response.locationPermission()).isFalse();
    assertThat(response.friendArrival()).isTrue();
    assertThat(response.chatBubble()).isFalse();
  }
}
