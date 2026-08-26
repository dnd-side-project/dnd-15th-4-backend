package com.dnd.puzzlemeet.domain.meeting.service;

import com.dnd.puzzlemeet.domain.meeting.dto.ReactionMessageSendRequest;
import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.ReactionMessage;
import com.dnd.puzzlemeet.domain.meeting.entity.ReactionPreset;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.ReactionMessageRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.ReactionPresetRepository;
import com.dnd.puzzlemeet.domain.notification.event.QuickMessageSentEvent;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReactionMessageService {

  private final MeetingRepository meetingRepository;
  private final MeetingMemberRepository meetingMemberRepository;
  private final ReactionPresetRepository reactionPresetRepository;
  private final ReactionMessageRepository reactionMessageRepository;
  private final ApplicationEventPublisher applicationEventPublisher;

  @Transactional
  public void sendReactionMessage(Long userId, Long meetingId, ReactionMessageSendRequest request) {
    Meeting meeting =
        meetingRepository
            .findById(meetingId)
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_NOT_FOUND));

    MeetingMember senderMember =
        meetingMemberRepository
            .findByMeetingIdAndUserId(meeting.getId(), userId)
            .orElseThrow(() -> ApiException.of(ErrorCode.AUTH_FORBIDDEN));

    ReactionPreset preset =
        reactionPresetRepository
            .findByIdAndIsActiveTrue(request.presetId())
            .orElseThrow(() -> ApiException.of(ErrorCode.REACTION_PRESET_NOT_FOUND));

    ReactionMessage message = new ReactionMessage(senderMember, preset, preset.getContent());
    reactionMessageRepository.save(message);
    applicationEventPublisher.publishEvent(
        new QuickMessageSentEvent(meeting.getId(), senderMember.getId()));
  }
}
