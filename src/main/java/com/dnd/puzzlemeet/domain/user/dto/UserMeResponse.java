package com.dnd.puzzlemeet.domain.user.dto;

import com.dnd.puzzlemeet.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record UserMeResponse(
    @Schema(description = "사용자 ID", example = "1", requiredMode = RequiredMode.REQUIRED) Long id,
    @Schema(
            description = "카카오 계정 이메일. 제공에 동의하지 않았거나 유효·인증되지 않았으면 null이다",
            example = "puzzlemeet@example.com",
            maxLength = 320,
            nullable = true)
        String email,
    @Schema(
            description = "닉네임",
            example = "효창",
            maxLength = 50,
            requiredMode = RequiredMode.REQUIRED)
        String nickname,
    @Schema(
            description = "프로필 이미지 URL. 카카오에 등록된 이미지가 없으면 null이다",
            example = "https://img.kakao.com/profile.jpg",
            maxLength = 500,
            nullable = true)
        String profileImageUrl) {

  public static UserMeResponse from(User user) {
    return new UserMeResponse(
        user.getId(), user.getEmail(), user.getNickname(), user.getProfileImageUrl());
  }
}
