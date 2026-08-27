package com.dnd.puzzlemeet.domain.meeting.service;

import java.time.LocalDateTime;

public record TransitRouteQuery(
    double startLatitude,
    double startLongitude,
    double endLatitude,
    double endLongitude,
    LocalDateTime meetingAt) {}
