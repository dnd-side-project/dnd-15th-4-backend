package com.dnd.puzzlemeet.domain.meeting.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dnd.puzzlemeet.TestcontainersConfiguration;
import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRole;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.ReactionMessageRepository;
import com.dnd.puzzlemeet.domain.notification.client.WebPushClient;
import com.dnd.puzzlemeet.domain.notification.client.WebPushSendResult;
import com.dnd.puzzlemeet.domain.notification.dto.PushNotificationPayload;
import com.dnd.puzzlemeet.domain.notification.entity.NotificationType;
import com.dnd.puzzlemeet.domain.notification.repository.PushSubscriptionRepository;
import com.dnd.puzzlemeet.domain.notification.repository.PushSubscriptionTarget;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzlePage;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzlePiece;
import com.dnd.puzzlemeet.domain.puzzle.repository.PuzzlePageRepository;
import com.dnd.puzzlemeet.domain.puzzle.repository.PuzzlePieceRepository;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.security.service.JwtProvider;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ReactionMessageIntegrationTest {

  private static final String PUBLIC_KEY =
      "BCP0phORUOCEcxm7gu2GCS5Naf7I2fW-o0JRQoJYEj30MSKlqo9WbBRygbxx6mlLciR2loUhdd63WXCeP6aq7IY";

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtProvider jwtProvider;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private MeetingRepository meetingRepository;
  @Autowired private MeetingMemberRepository meetingMemberRepository;
  @Autowired private ReactionMessageRepository reactionMessageRepository;
  @Autowired private PuzzlePageRepository puzzlePageRepository;
  @Autowired private PuzzlePieceRepository puzzlePieceRepository;
  @Autowired private PushSubscriptionRepository pushSubscriptionRepository;
  @MockitoBean private WebPushClient webPushClient;

  private User sender;
  private User recipient;
  private Meeting meeting;
  private MeetingMember senderMember;
  private MeetingMember recipientMember;
  private long presetId;

  @BeforeEach
  void setUp() throws Exception {
    deleteTestData();
    // 사용자 ID와 참여자 ID가 달라도 메시지의 발신자와 퍼즐 참여자를 연결해야 한다.
    userRepository.save(new User(31_000L, "다른 사용자", "https://img.example/other.png"));
    sender = userRepository.save(new User(31_001L, "발신자", "https://img.example/sender.png"));
    recipient = userRepository.save(new User(31_002L, "수신자", "https://img.example/recipient.png"));
    Meeting newMeeting =
        new Meeting(
            sender,
            "퀵메시지 검증 약속",
            LocalDate.now().atTime(23, 59),
            "약속 장소",
            null,
            BigDecimal.valueOf(37.5283),
            BigDecimal.valueOf(126.9320),
            4,
            100,
            "MSGTEST1",
            null);
    newMeeting.start();
    meeting = meetingRepository.save(newMeeting);
    senderMember = saveMember(sender, MeetingMemberRole.HOST);
    recipientMember = saveMember(recipient, MeetingMemberRole.GUEST);
    PuzzlePage page = puzzlePageRepository.save(new PuzzlePage(meeting, 1));
    puzzlePieceRepository.save(new PuzzlePiece(page, senderMember, (byte) 1));
    puzzlePieceRepository.save(new PuzzlePiece(page, recipientMember, (byte) 2));
    presetId = getDeparturePresetId();
    given(webPushClient.send(any(), any())).willReturn(WebPushSendResult.success(201));
  }

  @AfterEach
  void tearDown() {
    deleteTestData();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @DisplayName("발신자의 위치 유무와 무관하게 다른 참여자가 메시지를 조회하고 푸시 발송 대상이 된다")
  void sharesMessageAndSendsPushRegardlessOfSenderLocation(boolean hasLocation) throws Exception {
    if (hasLocation) {
      senderMember.updateCurrentLocation(BigDecimal.valueOf(37.5), BigDecimal.valueOf(126.9));
      meetingMemberRepository.save(senderMember);
    }
    registerSubscription(sender);
    registerSubscription(recipient);

    sendMessage();

    ResultActions response = assertRecipientCanReadMessage();
    if (hasLocation) {
      response.andExpect(jsonPath("$.data.puzzleGroups[0].members[0].latitude").value(37.5));
    } else {
      response.andExpect(jsonPath("$.data.puzzleGroups[0].members[0].latitude").isEmpty());
    }

    ArgumentCaptor<PushSubscriptionTarget> target =
        ArgumentCaptor.forClass(PushSubscriptionTarget.class);
    ArgumentCaptor<PushNotificationPayload> payload =
        ArgumentCaptor.forClass(PushNotificationPayload.class);
    verify(webPushClient, timeout(5_000).times(1)).send(target.capture(), payload.capture());
    assertThat(target.getValue().getUserId()).isEqualTo(recipient.getId());
    assertThat(payload.getValue())
        .isEqualTo(
            new PushNotificationPayload(
                NotificationType.QUICK_MESSAGE, "PuzzleMeet", "새 퀵메시지가 도착했어요.", meeting.getId()));
  }

  @Test
  @DisplayName("수신자의 푸시 구독이 없어도 다른 참여자의 메시지는 진행 조회 응답에 포함된다")
  void sharesMessageWithoutPushSubscription() throws Exception {
    sendMessage();

    assertRecipientCanReadMessage();
    verify(webPushClient, after(700).never()).send(any(), any());
  }

  @Test
  @DisplayName("말풍선 알림을 끈 수신자도 메시지는 조회하지만 푸시는 발송하지 않는다")
  void sharesMessageWithoutPushWhenNotificationDisabled() throws Exception {
    registerSubscription(recipient);
    recipientMember.updateNotificationSettings(true, true, false);
    meetingMemberRepository.save(recipientMember);

    sendMessage();

    assertRecipientCanReadMessage();
    verify(webPushClient, after(700).never()).send(any(), any());
  }

  @Test
  @DisplayName("프리셋 보정은 누락된 문구만 채우고 기존 ID와 비활성 설정을 유지한다")
  void seedsOnlyMissingPresetsWithoutChangingExistingOnes() {
    transactionTemplate.executeWithoutResult(
        status -> {
          Long existingId =
              jdbcTemplate.queryForObject(
                  "select id from reaction_presets where content = '배고파'", Long.class);
          jdbcTemplate.update(
              "update reaction_presets set is_active = b'0' where id = ?", existingId);
          jdbcTemplate.update("delete from reaction_presets where content = '가는중'");
          jdbcTemplate.update(
              "insert into reaction_presets (content, is_active) values ('거의 다 왔어요', b'1')");

          applyPresetMigration();
          applyPresetMigration();

          assertThat(
                  jdbcTemplate.queryForList("select content from reaction_presets", String.class))
              .containsExactlyInAnyOrder("배고파", "인증해", "가는중", "지금 출발", "뭐해?", "거의 다 왔어요");
          assertThat(
                  jdbcTemplate.queryForObject(
                      "select id from reaction_presets where content = '배고파'", Long.class))
              .isEqualTo(existingId);
          assertThat(
                  jdbcTemplate.queryForObject(
                      "select is_active from reaction_presets where id = ?",
                      Boolean.class,
                      existingId))
              .isFalse();
          assertThat(
                  jdbcTemplate.queryForObject(
                      "select is_active from reaction_presets where content = '가는중'",
                      Boolean.class))
              .isTrue();
          status.setRollbackOnly();
        });
  }

  private void applyPresetMigration() {
    jdbcTemplate.execute(
        (ConnectionCallback<Void>)
            connection -> {
              ScriptUtils.executeSqlScript(
                  connection, new ClassPathResource("db/migration/V3__seed_reaction_presets.sql"));
              return null;
            });
  }

  private MeetingMember saveMember(User user, MeetingMemberRole role) {
    return meetingMemberRepository.save(
        new MeetingMember(meeting, user, role, user.getNickname(), user.getProfileImageUrl()));
  }

  private void registerSubscription(User user) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/notifications/push-subscriptions")
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "endpoint": "https://push.services.mozilla.com/wpush/v2/test-%s",
                      "keys": {"p256dh": "%s", "auth": "AAAAAAAAAAAAAAAAAAAAAA"}
                    }
                    """
                        .formatted(user.getId(), PUBLIC_KEY)))
        .andExpect(status().isOk());
  }

  private void sendMessage() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/meetings/{meetingId}/reaction-messages", meeting.getId())
                .header("Authorization", bearerToken(sender))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"presetId\":%s}".formatted(presetId)))
        .andExpect(status().isCreated());
  }

  private long getDeparturePresetId() throws Exception {
    String response =
        mockMvc
            .perform(
                get("/api/v1/meetings/reaction-presets")
                    .header("Authorization", bearerToken(sender)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(5))
            .andExpect(
                jsonPath("$.data[*].content").value(contains("배고파", "인증해", "가는중", "지금 출발", "뭐해?")))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    for (JsonNode preset : objectMapper.readTree(response).get("data")) {
      if ("지금 출발".equals(preset.get("content").stringValue())) {
        long id = preset.get("id").longValue();
        assertThat(id).isPositive();
        return id;
      }
    }
    throw new AssertionError("기본 프리셋에 '지금 출발'이 없습니다.");
  }

  private ResultActions assertRecipientCanReadMessage() throws Exception {
    assertThat(sender.getId()).isNotEqualTo(senderMember.getId());
    return mockMvc
        .perform(
            get("/api/v1/meetings/{meetingId}/in-progress", meeting.getId())
                .header("Authorization", bearerToken(recipient)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.quickMessages.length()").value(1))
        .andExpect(jsonPath("$.data.quickMessages[0].id").isNumber())
        .andExpect(jsonPath("$.data.quickMessages[0].content").value("지금 출발"))
        .andExpect(jsonPath("$.data.quickMessages[0].senderId").value(sender.getId()))
        .andExpect(jsonPath("$.data.puzzleGroups[0].members[0].userId").value(sender.getId()));
  }

  private String bearerToken(User user) {
    return "Bearer " + jwtProvider.createAccessToken(user.getId());
  }

  private void deleteTestData() {
    pushSubscriptionRepository.deleteAllInBatch();
    reactionMessageRepository.deleteAllInBatch();
    puzzlePieceRepository.deleteAllInBatch();
    puzzlePageRepository.deleteAllInBatch();
    meetingMemberRepository.deleteAllInBatch();
    meetingRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
  }
}
