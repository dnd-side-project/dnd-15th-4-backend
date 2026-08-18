package com.dnd.puzzlemeet.domain.meeting.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dnd.puzzlemeet.TestcontainersConfiguration;
import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRole;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.security.service.JwtProvider;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MeetingControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private MeetingRepository meetingRepository;
  @Autowired private MeetingMemberRepository meetingMemberRepository;
  @Autowired private JwtProvider jwtProvider;

  @Test
  @DisplayName("인증된 참여자가 약속방 닉네임을 수정한다")
  void updateMemberNicknameUpdatesOnlyMeetingMember() throws Exception {
    MeetingMember member = saveMeetingMember("기본닉네임");
    String accessToken = jwtProvider.createAccessToken(member.getUser().getId());

    mockMvc
        .perform(
            patch("/api/v1/meetings/{meetingId}/members/me/nickname", member.getMeeting().getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"방별닉네임\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.meetingId").value(member.getMeeting().getId()))
        .andExpect(jsonPath("$.data.nickname").value("방별닉네임"));

    MeetingMember stored = meetingMemberRepository.findById(member.getId()).orElseThrow();
    assertThat(stored.getNickname()).isEqualTo("방별닉네임");
    assertThat(stored.getUser().getNickname()).isEqualTo("카카오닉네임");
  }

  @Test
  @DisplayName("저장된 현재 위치가 목적지 반경 안이면 인증된 참여자를 도착 처리한다")
  void markMemberArrivedWithinArrivalRadius() throws Exception {
    MeetingMember member = saveMeetingMember("기본닉네임");
    member.updateCurrentLocation(BigDecimal.valueOf(37.5283), BigDecimal.valueOf(126.9320));
    meetingMemberRepository.flush();
    String accessToken = jwtProvider.createAccessToken(member.getUser().getId());
    Long meetingId = member.getMeeting().getId();

    mockMvc
        .perform(
            put("/api/v1/meetings/{meetingId}/members/me/arrival", meetingId)
                .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.meetingId").value(meetingId))
        .andExpect(jsonPath("$.data.arrivalTime").isNotEmpty());
  }

  @Test
  @DisplayName("약속방 닉네임을 공백으로 수정하면 입력값 검증에 실패한다")
  void updateMemberNicknameRejectsBlankValue() throws Exception {
    MeetingMember member = saveMeetingMember("기본닉네임");
    String accessToken = jwtProvider.createAccessToken(member.getUser().getId());

    mockMvc
        .perform(
            patch("/api/v1/meetings/{meetingId}/members/me/nickname", member.getMeeting().getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"   \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }

  private MeetingMember saveMeetingMember(String nickname) {
    User user = userRepository.save(new User(100L, "카카오닉네임", "https://img.kakao.com/a.jpg"));
    Meeting meeting =
        meetingRepository.save(
            new Meeting(
                user,
                "한강 피크닉",
                LocalDateTime.of(2026, 8, 20, 14, 0),
                "서울 여의도 한강공원",
                null,
                BigDecimal.valueOf(37.5283),
                BigDecimal.valueOf(126.9320),
                50,
                "ABCD1234",
                null));
    return meetingMemberRepository.save(
        new MeetingMember(meeting, user, MeetingMemberRole.HOST, nickname));
  }
}
