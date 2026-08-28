package com.dnd.puzzlemeet.domain.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Size;

public record MeetingMemberNicknameUpdateRequest(
    @Schema(
            description = "약속방에서 사용할 닉네임. nicknameSet이 true일 때만 사용한다",
            example = "효창",
            maxLength = 30,
            requiredMode = RequiredMode.NOT_REQUIRED)
        @Size(max = 30)
        String nickname,
    @Schema(
            description = "닉네임 설정 여부. false면 사용자 기본 닉네임으로 되돌린다. 생략하면 true로 본다",
            example = "true",
            requiredMode = RequiredMode.NOT_REQUIRED)
        Boolean nicknameSet) {}
