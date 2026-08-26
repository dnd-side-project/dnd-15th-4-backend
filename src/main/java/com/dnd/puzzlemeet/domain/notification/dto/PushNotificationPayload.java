package com.dnd.puzzlemeet.domain.notification.dto;

import com.dnd.puzzlemeet.domain.notification.entity.NotificationType;

public record PushNotificationPayload(
    NotificationType type, String title, String body, Long meetingId) {}
