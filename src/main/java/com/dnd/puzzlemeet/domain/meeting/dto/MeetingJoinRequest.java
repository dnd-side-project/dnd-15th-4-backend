package com.dnd.puzzlemeet.domain.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MeetingJoinRequest(
    @Schema(
            description = "약속 초대 코드",
            example = "ABCD1234",
            maxLength = 20,
            requiredMode = RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 20)
        String inviteCode,
    @Schema(
            description = "약속방 내 닉네임. 없으면 기본 닉네임을 쓴다",
            example = "효창",
            maxLength = 30,
            nullable = true)
        @Size(max = 30)
        String nickname) {}
