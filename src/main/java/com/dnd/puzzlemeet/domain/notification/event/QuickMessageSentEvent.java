package com.dnd.puzzlemeet.domain.notification.event;

public record QuickMessageSentEvent(Long meetingId, Long senderMemberId) {}
