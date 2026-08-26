package com.dnd.puzzlemeet.domain.notification.event;

public record DepartureReminderClaimedEvent(Long meetingId, Long memberId, Long userId) {}
