package com.dnd.puzzlemeet.domain.meeting.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dnd.puzzlemeet.TestcontainersConfiguration;
import com.dnd.puzzlemeet.domain.meeting.client.TravelRoute;
import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRole;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRoute;
import com.dnd.puzzlemeet.domain.meeting.entity.TransportType;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRouteRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.meeting.service.TransitRouteFacade;
import com.dnd.puzzlemeet.domain.puzzle.entity.MemberImage;
import com.dnd.puzzlemeet.domain.puzzle.repository.MemberImageRepository;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.security.service.JwtProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
  @Autowired private MeetingMemberRouteRepository meetingMemberRouteRepository;
  @Autowired private MemberImageRepository memberImageRepository;
  @Autowired private JwtProvider jwtProvider;
  @MockitoBean private TransitRouteFacade transitRouteFacade;

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
        .andExpect(jsonPath("$.data.nickname").value("방별닉네임"))
        .andExpect(jsonPath("$.data.nicknameSet").value(true));

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

  @Test
  @DisplayName("인증된 참여자가 출발 설정을 등록하면 이동 경로가 함께 반환된다")
  void createMemberDepartureReturnsCalculatedRoute() throws Exception {
    MeetingMember member = saveMeetingMember("기본닉네임");
    String accessToken = jwtProvider.createAccessToken(member.getUser().getId());
    Long meetingId = member.getMeeting().getId();
    mockMvc
        .perform(
            post("/api/v1/meetings/{meetingId}/members/me/departure", meetingId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(departureRequestBody()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.meetingId").value(meetingId))
        .andExpect(jsonPath("$.data.totalEstimatedTime").value(40))
        .andExpect(jsonPath("$.data.routes[1].transportType").value("SUBWAY"))
        .andExpect(jsonPath("$.data.routes[1].transportContent").value("27개 역 이동"))
        .andExpect(jsonPath("$.data.nicknameSetting.nickname").value("김땡땡"));

    MeetingMember stored = meetingMemberRepository.findById(member.getId()).orElseThrow();
    assertThat(stored.getDepartureName()).isEqualTo("서울대학교");
    assertThat(stored.getEstimatedDurationSeconds()).isEqualTo(2400);
  }

  @Test
  @DisplayName("등록한 출발 설정을 조회한다")
  void getMemberDepartureReturnsStoredSetting() throws Exception {
    MeetingMember member = saveMeetingMember("기본닉네임");
    String accessToken = jwtProvider.createAccessToken(member.getUser().getId());
    Long meetingId = member.getMeeting().getId();
    mockMvc.perform(
        post("/api/v1/meetings/{meetingId}/members/me/departure", meetingId)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(departureRequestBody()));

    mockMvc
        .perform(
            get("/api/v1/meetings/{meetingId}/members/me/departure", meetingId)
                .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.departure.placeName").value("서울대학교"))
        .andExpect(jsonPath("$.data.notificationSettings.chatBubble").value(false))
        .andExpect(jsonPath("$.data.routes.length()").value(2));
  }

  @Test
  @DisplayName("출발지 위도가 범위를 벗어나면 입력값 검증에 실패한다")
  void createMemberDepartureRejectsOutOfRangeLatitude() throws Exception {
    MeetingMember member = saveMeetingMember("기본닉네임");
    String accessToken = jwtProvider.createAccessToken(member.getUser().getId());

    mockMvc
        .perform(
            post("/api/v1/meetings/{meetingId}/members/me/departure", member.getMeeting().getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "departure": {"placeName": "서울대학교", "latitude": 137.5665, "longitude": 126.9780},
                      "notificationSettings": {"locationPermission": true, "friendArrival": true, "chatBubble": false},
                      "nicknameSetting": {"enabled": false}
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }

  @Test
  @DisplayName("참여자가 약속방을 나가면 참여자와 이동 경로, 퍼즐 이미지가 함께 삭제된다")
  void leaveMeetingDeletesMemberWithChildRows() throws Exception {
    MeetingMember host = saveMeetingMember("방장");
    memberImageRepository.save(new MemberImage(host, "https://img.kakao.com/host.png", true));
    Meeting meeting = host.getMeeting();

    User guestUser = userRepository.save(new User(200L, "참여자닉네임", "https://img.kakao.com/b.jpg"));
    MeetingMember guest =
        meetingMemberRepository.save(
            new MeetingMember(
                meeting,
                guestUser,
                MeetingMemberRole.GUEST,
                "참여자닉네임",
                "https://img.kakao.com/guest-profile.png"));
    memberImageRepository.save(new MemberImage(guest, "https://img.kakao.com/guest.png", true));
    meetingMemberRouteRepository.save(
        new MeetingMemberRoute(guest, 1, TransportType.WALK, null, null, "태릉입구역", 0, 600));
    meetingMemberRepository.flush();

    String accessToken = jwtProvider.createAccessToken(guestUser.getId());

    mockMvc
        .perform(
            delete("/api/v1/meetings/{meetingId}/members/me", meeting.getId())
                .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk());

    meetingMemberRepository.flush();
    assertThat(meetingMemberRepository.findById(guest.getId())).isEmpty();
    assertThat(
            meetingMemberRouteRepository.findAllByMeetingMemberIdOrderByRouteIndexAsc(
                guest.getId()))
        .isEmpty();
    assertThat(memberImageRepository.count()).isEqualTo(1);
    assertThat(meetingMemberRepository.findById(host.getId())).isPresent();
  }

  @Test
  @DisplayName("방장이 약속방 나가기를 요청하면 MEETING_HOST_CANNOT_LEAVE로 거절된다")
  void rejectsLeaveMeetingRequestFromHost() throws Exception {
    MeetingMember host = saveMeetingMember("방장");
    String accessToken = jwtProvider.createAccessToken(host.getUser().getId());

    mockMvc
        .perform(
            delete("/api/v1/meetings/{meetingId}/members/me", host.getMeeting().getId())
                .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MEETING_HOST_CANNOT_LEAVE"));

    assertThat(meetingMemberRepository.findById(host.getId())).isPresent();
  }

  private String departureRequestBody() {
    return """
        {
          "departure": {"placeName": "서울대학교", "latitude": 37.5665, "longitude": 126.9780},
          "notificationSettings": {"locationPermission": true, "friendArrival": true, "chatBubble": false},
          "nicknameSetting": {"enabled": true, "nickname": "김땡땡"},
          "route": {
            "totalTime": 2400,
            "steps": [
              {"type": "WALK", "time": 600, "station": {"start": null, "end": "태릉입구역"}},
              {
                "type": "SUBWAY",
                "time": 1800,
                "line": "수도권6호선",
                "station": {"start": "태릉입구역", "end": "디지털미디어시티역"},
                "stations": %s
              }
            ]
          }
        }
        """
        .formatted(stationNamesJson(27));
  }

  private String stationNamesJson(int stationCount) {
    return IntStream.rangeClosed(0, stationCount)
        .mapToObj(index -> "\"역" + index + "\"")
        .collect(Collectors.joining(", ", "[", "]"));
  }

  @Test
  @DisplayName("인증된 참여자가 출발지 좌표로 약속 장소까지의 경로를 조회한다")
  void searchMeetingRoutesReturnsSteps() throws Exception {
    MeetingMember member = saveMeetingMember("기본닉네임");
    String accessToken = jwtProvider.createAccessToken(member.getUser().getId());
    given(transitRouteFacade.findRoutes(any())).willReturn(List.of(transitRouteWithFare()));

    mockMvc
        .perform(
            post("/api/v1/meetings/{meetingId}/routes", member.getMeeting().getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"start\":{\"latitude\":37.5045,\"longitude\":127.0247}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.routes[0].totalTime").value(2400))
        .andExpect(jsonPath("$.data.routes[0].fare").value(1850))
        .andExpect(jsonPath("$.data.routes[0].transferCount").value(1))
        .andExpect(jsonPath("$.data.routes[0].pathType").value(1))
        .andExpect(jsonPath("$.data.routes[0].steps[0].type").value("SUBWAY"))
        .andExpect(jsonPath("$.data.routes[0].steps[0].line").value("수도권6호선"))
        .andExpect(jsonPath("$.data.routes[0].steps[0].station.start").value("태릉입구역"))
        .andExpect(jsonPath("$.data.routes[0].steps[0].startLocation.lat").value(37.5017))
        .andExpect(jsonPath("$.data.guide").doesNotExist());
  }

  @Test
  @DisplayName("대중교통 경로가 없으면 빈 목록과 도보 안내를 200으로 돌려준다")
  void searchMeetingRoutesReturnsWalkGuideWhenNoRouteExists() throws Exception {
    MeetingMember member = saveMeetingMember("기본닉네임");
    String accessToken = jwtProvider.createAccessToken(member.getUser().getId());
    given(transitRouteFacade.findRoutes(any())).willReturn(List.of());

    mockMvc
        .perform(
            post("/api/v1/meetings/{meetingId}/routes", member.getMeeting().getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"start\":{\"latitude\":37.5045,\"longitude\":127.0247}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.routes").isEmpty())
        .andExpect(jsonPath("$.data.guide.code").value("MEETING_MAP_TOO_CLOSE"))
        .andExpect(jsonPath("$.data.guide.travelMode").value("WALK"));
  }

  @Test
  @DisplayName("경로를 조회할 때 위도가 범위를 벗어나면 입력값 검증에 실패한다")
  void searchMeetingRoutesRejectsOutOfRangeLatitude() throws Exception {
    MeetingMember member = saveMeetingMember("기본닉네임");
    String accessToken = jwtProvider.createAccessToken(member.getUser().getId());

    mockMvc
        .perform(
            post("/api/v1/meetings/{meetingId}/routes", member.getMeeting().getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"start\":{\"latitude\":137.5045,\"longitude\":127.0247}}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }

  @Test
  @DisplayName("약속에 참여하지 않은 사용자가 경로를 조회하면 참여자 정보를 찾을 수 없다")
  void searchMeetingRoutesRejectsNonParticipant() throws Exception {
    MeetingMember member = saveMeetingMember("기본닉네임");
    User stranger = userRepository.save(new User(200L, "남", "https://img.kakao.com/b.jpg"));
    String accessToken = jwtProvider.createAccessToken(stranger.getId());

    mockMvc
        .perform(
            post("/api/v1/meetings/{meetingId}/routes", member.getMeeting().getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"start\":{\"latitude\":37.5045,\"longitude\":127.0247}}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("MEETING_MEMBER_NOT_FOUND"));
  }

  private TravelRoute transitRouteWithFare() {
    return new TravelRoute(
        2400,
        1850,
        1,
        1,
        List.of(
            new TravelRoute.Leg(
                TransportType.SUBWAY,
                "수도권6호선",
                "CD7C2F",
                1800,
                27000,
                "태릉입구역",
                "성수역",
                37.5017,
                127.0256,
                37.5446,
                127.0559,
                List.of("태릉입구역", "성수역"),
                null)));
  }

  private MeetingMember saveMeetingMember(String nickname) {
    User user = userRepository.save(new User(100L, "카카오닉네임", "https://img.kakao.com/a.jpg"));
    Meeting meeting =
        meetingRepository.save(
            new Meeting(
                user,
                "한강 피크닉",
                LocalDate.now().atTime(23, 30),
                "서울 여의도 한강공원",
                null,
                BigDecimal.valueOf(37.5283),
                BigDecimal.valueOf(126.9320),
                50,
                100,
                "ABCD1234",
                null));
    return meetingMemberRepository.save(
        new MeetingMember(
            meeting,
            user,
            MeetingMemberRole.HOST,
            nickname,
            "https://img.kakao.com/host-profile.png"));
  }
}
