package com.dnd.puzzlemeet.domain.user.service;

import com.dnd.puzzlemeet.domain.user.dto.UserMeResponse;
import com.dnd.puzzlemeet.domain.user.dto.UserNotificationSettingsResponse;
import com.dnd.puzzlemeet.domain.user.dto.UserNotificationSettingsUpdateRequest;
import com.dnd.puzzlemeet.domain.user.dto.UserUpdateRequest;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public UserMeResponse getMe(Long userId) {
    User user =
        userRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> ApiException.of(ErrorCode.USER_NOT_FOUND));
    return UserMeResponse.from(user);
  }

  @Transactional
  public UserMeResponse updateMe(Long userId, UserUpdateRequest request) {
    User user =
        userRepository
            .findActiveByIdForUpdate(userId)
            .orElseThrow(() -> ApiException.of(ErrorCode.USER_NOT_FOUND));
    user.changeNickname(request.nickname());
    return UserMeResponse.from(user);
  }

  @Transactional(readOnly = true)
  public UserNotificationSettingsResponse getNotificationSettings(Long userId) {
    User user =
        userRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> ApiException.of(ErrorCode.USER_NOT_FOUND));
    return UserNotificationSettingsResponse.from(user);
  }

  @Transactional
  public UserNotificationSettingsResponse updateNotificationSettings(
      Long userId, UserNotificationSettingsUpdateRequest request) {
    User user =
        userRepository
            .findActiveByIdForUpdate(userId)
            .orElseThrow(() -> ApiException.of(ErrorCode.USER_NOT_FOUND));
    user.updateNotificationSettings(
        request.locationPermission(), request.friendArrival(), request.chatBubble());
    return UserNotificationSettingsResponse.from(user);
  }
}
