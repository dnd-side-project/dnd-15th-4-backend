package com.dnd.puzzlemeet.domain.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

import com.dnd.puzzlemeet.domain.meeting.dto.ReactionMessageSendRequest;
import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRole;
import com.dnd.puzzlemeet.domain.meeting.entity.ReactionMessage;
import com.dnd.puzzlemeet.domain.meeting.entity.ReactionPreset;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.ReactionMessageRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.ReactionPresetRepository;
import com.dnd.puzzlemeet.domain.notification.event.QuickMessageSentEvent;
import com.dnd.puzzlemeet.domain.user.entity.User;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReactionMessageServiceTest {

  @Mock private MeetingRepository meetingRepository;
  @Mock private MeetingMemberRepository meetingMemberRepository;
  @Mock private ReactionPresetRepository reactionPresetRepository;
  @Mock private ReactionMessageRepository reactionMessageRepository;
  @Mock private ApplicationEventPublisher applicationEventPublisher;

  private ReactionMessageService reactionMessageService;

  @BeforeEach
  void setUp() {
    reactionMessageService =
        new ReactionMessageService(
            meetingRepository,
            meetingMemberRepository,
            reactionPresetRepository,
            reactionMessageRepository,
            applicationEventPublisher);
  }

  @Test
  @DisplayName("퀵메시지를 저장한 뒤 원문이 없는 발송 이벤트를 발행한다")
  void publishesQuickMessageEventWithoutContentAfterSave() {
    User user = new User(100L, "효창", "https://img.kakao.com/a.jpg");
    Meeting meeting = meeting(user);
    ReflectionTestUtils.setField(meeting, "id", 10L);
    MeetingMember sender =
        new MeetingMember(
            meeting, user, MeetingMemberRole.HOST, "효창", "https://img.kakao.com/host.png");
    ReflectionTestUtils.setField(sender, "id", 20L);
    ReactionPreset preset = new ReactionPreset("지금 출발");
    ReflectionTestUtils.setField(preset, "id", 30L);
    given(meetingRepository.findById(10L)).willReturn(Optional.of(meeting));
    given(meetingMemberRepository.findByMeetingIdAndUserId(10L, 100L))
        .willReturn(Optional.of(sender));
    given(reactionPresetRepository.findByIdAndIsActiveTrue(30L)).willReturn(Optional.of(preset));

    reactionMessageService.sendReactionMessage(100L, 10L, new ReactionMessageSendRequest(30L));

    InOrder ordered = inOrder(reactionMessageRepository, applicationEventPublisher);
    ordered.verify(reactionMessageRepository).save(any(ReactionMessage.class));
    ordered.verify(applicationEventPublisher).publishEvent(new QuickMessageSentEvent(10L, 20L));
    assertThat(QuickMessageSentEvent.class.isRecord()).isTrue();
    assertThat(QuickMessageSentEvent.class.getRecordComponents())
        .extracting(RecordComponent::getName)
        .containsExactly("meetingId", "senderMemberId");
  }

  private Meeting meeting(User host) {
    return new Meeting(
        host,
        "한강 피크닉",
        LocalDateTime.of(2026, 8, 25, 14, 0),
        "서울 여의도 한강공원",
        null,
        BigDecimal.valueOf(37.5283),
        BigDecimal.valueOf(126.9320),
        50,
        100,
        "ABCD1234",
        null);
  }
}
