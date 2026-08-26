package com.dnd.puzzlemeet.domain.notification.event;

public record FriendArrivedEvent(Long meetingId, Long arrivedMemberId) {}
