package com.dnd.puzzlemeet.domain.meeting.service;

import com.dnd.puzzlemeet.domain.meeting.client.TmapRoute;
import com.dnd.puzzlemeet.domain.meeting.client.TmapRouteClient;
import com.dnd.puzzlemeet.domain.meeting.client.TmapTransitRoute;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingCreateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingCreateResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingJoinRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingJoinResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingListResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberArrivalResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberDepartureCreateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberDepartureResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberDepartureUpdateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberNicknameUpdateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberNicknameUpdateResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingPreviewRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingPreviewResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingRouteSearchResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingUpdateRequest;
import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRole;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRoute;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberStatus;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus;
import com.dnd.puzzlemeet.domain.meeting.entity.TransportType;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRouteRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.puzzle.entity.MemberImage;
import com.dnd.puzzlemeet.domain.puzzle.repository.MemberImageRepository;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.s3.AmazonS3Manager;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {

  private static final int ARRIVAL_RADIUS_M = 50;
  private static final double EARTH_RADIUS_M = 6_371_000;
  private static final double WALKING_SPEED_METERS_PER_SECOND = 4_000.0 / 3_600;
  private static final int ROUTE_RESEARCH_THRESHOLD_SECONDS = 3_600;
  private static final double SECONDS_PER_MINUTE = 60.0;
  private static final int INVITE_CODE_LENGTH = 8;
  private static final String INVITE_CODE_CHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final String DEFAULT_MEMBER_IMAGE_URL =
      "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/puzzles/_+(9)+4.png";

  private static final SecureRandom RANDOM = new SecureRandom();

  private static final Comparator<Meeting> MEETING_LIST_ORDER =
      (m1, m2) -> {
        boolean completed1 = m1.getStatus() == MeetingStatus.COMPLETED;
        boolean completed2 = m2.getStatus() == MeetingStatus.COMPLETED;
        if (completed1 != completed2) {
          return completed1 ? 1 : -1;
        }
        return completed1
            ? m2.getMeetingAt().compareTo(m1.getMeetingAt())
            : m1.getMeetingAt().compareTo(m2.getMeetingAt());
      };

  private final MeetingRepository meetingRepository;
  private final MeetingMemberRepository meetingMemberRepository;
  private final MeetingMemberRouteRepository meetingMemberRouteRepository;
  private final MemberImageRepository memberImageRepository;
  private final UserRepository userRepository;
  private final AmazonS3Manager amazonS3Manager;
  private final TmapRouteClient tmapRouteClient;

  @Transactional
  public MeetingCreateResponse createMeeting(
      Long userId, MeetingCreateRequest request, MultipartFile image) {
    User host =
        userRepository
            .findById(userId)
            .orElseThrow(() -> ApiException.of(ErrorCode.USER_NOT_FOUND));

    Meeting meeting =
        new Meeting(
            host,
            request.title(),
            request.dateTime(),
            request.destination(),
            null,
            BigDecimal.valueOf(request.latitude()),
            BigDecimal.valueOf(request.longitude()),
            ARRIVAL_RADIUS_M,
            generateInviteCode(),
            request.memo());
    meetingRepository.save(meeting);

    registerMember(meeting, host, MeetingMemberRole.HOST, request.nickname(), image);

    return MeetingCreateResponse.from(meeting);
  }

  @Transactional(readOnly = true)
  public MeetingPreviewResponse previewMeeting(MeetingPreviewRequest request) {
    Meeting meeting =
        meetingRepository
            .findByInviteCode(request.inviteCode())
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_INVITE_CODE_INVALID));

    if (meeting.getStatus() != MeetingStatus.WAITING) {
      throw ApiException.of(ErrorCode.MEETING_INVITE_CODE_INVALID);
    }

    List<MeetingMember> members =
        meetingMemberRepository.findAllByMeetingIdInFetchUser(List.of(meeting.getId()));

    return MeetingPreviewResponse.from(meeting, members);
  }

  @Transactional
  public MeetingJoinResponse joinMeeting(
      Long userId, MeetingJoinRequest request, MultipartFile image) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> ApiException.of(ErrorCode.USER_NOT_FOUND));

    Meeting meeting =
        meetingRepository
            .findByInviteCode(request.inviteCode())
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_INVITE_CODE_INVALID));

    if (meeting.getStatus() != MeetingStatus.WAITING) {
      throw ApiException.of(ErrorCode.MEETING_NOT_JOINABLE);
    }

    if (meetingMemberRepository.existsByMeetingIdAndUserId(meeting.getId(), userId)) {
      throw ApiException.of(ErrorCode.MEETING_MEMBER_ALREADY_JOINED);
    }

    registerMember(meeting, user, MeetingMemberRole.GUEST, request.nickname(), image);

    return MeetingJoinResponse.from(meeting);
  }

  @Transactional
  public MeetingMemberNicknameUpdateResponse updateMemberNickname(
      Long userId, Long meetingId, MeetingMemberNicknameUpdateRequest request) {
    MeetingMember member = getActiveMeetingMember(userId, meetingId);
    member.changeNickname(request.nickname());
    return MeetingMemberNicknameUpdateResponse.from(member);
  }

  @Transactional
  public MeetingMemberArrivalResponse markMemberArrived(Long userId, Long meetingId) {
    MeetingMember member = getActiveMeetingMember(userId, meetingId);
    if (member.getStatus() == MeetingMemberStatus.ARRIVED) {
      return MeetingMemberArrivalResponse.from(member);
    }

    if (member.getCurrentLatitude() == null || member.getCurrentLongitude() == null) {
      throw ApiException.of(ErrorCode.MEETING_ARRIVAL_LOCATION_INVALID);
    }

    Meeting meeting = member.getMeeting();
    if (!isWithinArrivalRadius(
        member.getCurrentLatitude(),
        member.getCurrentLongitude(),
        meeting.getDestinationLatitude(),
        meeting.getDestinationLongitude(),
        meeting.getArrivalRadiusM())) {
      throw ApiException.of(ErrorCode.MEETING_ARRIVAL_LOCATION_INVALID);
    }

    member.arrive();
    return MeetingMemberArrivalResponse.from(member);
  }

  @Transactional(readOnly = true)
  public List<MeetingListResponse> getMeetings(Long userId, MeetingStatus status) {
    List<Meeting> meetings =
        meetingRepository.findAllByParticipantUserIdAndStatus(userId, status).stream()
            .sorted(MEETING_LIST_ORDER)
            .toList();

    if (meetings.isEmpty()) {
      return List.of();
    }

    List<Long> meetingIds = meetings.stream().map(Meeting::getId).toList();
    Map<Long, List<MeetingMember>> membersByMeetingId =
        meetingMemberRepository.findAllByMeetingIdInFetchUser(meetingIds).stream()
            .collect(Collectors.groupingBy(mm -> mm.getMeeting().getId()));

    return meetings.stream()
        .map(meeting -> MeetingListResponse.from(meeting, membersByMeetingId.get(meeting.getId())))
        .toList();
  }

  @Transactional
  public void updateMeeting(Long userId, Long meetingId, MeetingUpdateRequest request) {
    Meeting meeting =
        meetingRepository
            .findById(meetingId)
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_NOT_FOUND));

    if (!meeting.getHostUser().getId().equals(userId)) {
      throw ApiException.of(ErrorCode.AUTH_FORBIDDEN);
    }

    if (meeting.getStatus() != MeetingStatus.WAITING) {
      throw ApiException.of(ErrorCode.MEETING_NOT_WAITING);
    }

    if ((request.latitude() == null) != (request.longitude() == null)) {
      throw ApiException.of(ErrorCode.INVALID_INPUT_VALUE);
    }

    String title = request.title() != null ? request.title() : meeting.getTitle();
    LocalDateTime meetingAt =
        request.dateTime() != null ? request.dateTime() : meeting.getMeetingAt();
    String destination =
        request.destination() != null ? request.destination() : meeting.getDestinationName();
    BigDecimal latitude =
        request.latitude() != null
            ? BigDecimal.valueOf(request.latitude())
            : meeting.getDestinationLatitude();
    BigDecimal longitude =
        request.longitude() != null
            ? BigDecimal.valueOf(request.longitude())
            : meeting.getDestinationLongitude();
    String memo = request.memo() != null ? request.memo() : meeting.getMemo();

    meeting.updateDetails(title, meetingAt, destination, latitude, longitude, memo);
  }

  @Transactional
  public void cancelMeeting(Long userId, Long meetingId) {
    Meeting meeting =
        meetingRepository
            .findById(meetingId)
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_NOT_FOUND));

    if (!meeting.getHostUser().getId().equals(userId)) {
      throw ApiException.of(ErrorCode.AUTH_FORBIDDEN);
    }

    if (meeting.getStatus() != MeetingStatus.WAITING) {
      throw ApiException.of(ErrorCode.MEETING_NOT_WAITING);
    }

    meeting.cancel();
  }

  @Transactional
  public void leaveMeeting(Long userId, Long meetingId) {
    MeetingMember member = getActiveMeetingMember(userId, meetingId);
    if (member.getRole() == MeetingMemberRole.HOST) {
      throw ApiException.of(ErrorCode.MEETING_HOST_CANNOT_LEAVE);
    }

    meetingMemberRouteRepository.deleteAllByMeetingMemberId(member.getId());
    memberImageRepository.deleteAllByMeetingMemberId(member.getId());
    meetingMemberRepository.delete(member);
  }

  @Transactional
  public MeetingMemberDepartureResponse createDeparture(
      Long userId,
      Long meetingId,
      String placeName,
      double latitude,
      double longitude,
      MeetingMemberDepartureCreateRequest.NotificationSettings notificationSettings,
      MeetingMemberDepartureCreateRequest.NicknameSetting nicknameSetting) {
    MeetingMember member = getActiveMeetingMember(userId, meetingId);
    if (member.getDepartureName() != null) {
      throw ApiException.of(ErrorCode.MEETING_DEPARTURE_ALREADY_SET);
    }

    applyNicknameSetting(member, nicknameSetting.enabled(), nicknameSetting.nickname());
    member.updateNotificationSettings(
        notificationSettings.locationPermission(),
        notificationSettings.friendArrival(),
        notificationSettings.chatBubble());

    return applyDeparture(member, placeName, latitude, longitude);
  }

  @Transactional(readOnly = true)
  public MeetingMemberDepartureResponse getDeparture(Long userId, Long meetingId) {
    MeetingMember member = getActiveMeetingMember(userId, meetingId);
    if (member.getDepartureName() == null) {
      throw ApiException.of(ErrorCode.MEETING_DEPARTURE_NOT_FOUND);
    }
    return MeetingMemberDepartureResponse.of(member, findRoutes(member));
  }

  @Transactional
  public MeetingMemberDepartureResponse updateDeparture(
      Long userId,
      Long meetingId,
      String placeName,
      Double latitude,
      Double longitude,
      MeetingMemberDepartureUpdateRequest.NotificationSettings notificationSettings,
      MeetingMemberDepartureUpdateRequest.NicknameSetting nicknameSetting) {
    MeetingMember member = getActiveMeetingMember(userId, meetingId);
    if (member.getDepartureName() == null) {
      throw ApiException.of(ErrorCode.MEETING_DEPARTURE_NOT_FOUND);
    }

    if (nicknameSetting != null) {
      applyNicknameSetting(member, nicknameSetting.enabled(), nicknameSetting.nickname());
    }
    if (notificationSettings != null) {
      member.updateNotificationSettings(
          notificationSettings.locationPermission(),
          notificationSettings.friendArrival(),
          notificationSettings.chatBubble());
    }
    if (placeName != null) {
      return applyDeparture(member, placeName, latitude, longitude);
    }

    return MeetingMemberDepartureResponse.of(member, findRoutes(member));
  }

  @Transactional(readOnly = true)
  public MeetingRouteSearchResponse searchRoutes(
      Long userId, Long meetingId, double latitude, double longitude) {
    MeetingMember member = getActiveMeetingMember(userId, meetingId);
    return MeetingRouteSearchResponse.from(
        resolveTransitRoutes(member.getMeeting(), latitude, longitude));
  }

  private List<TmapTransitRoute> resolveTransitRoutes(
      Meeting meeting, double latitude, double longitude) {
    double destinationLatitude = meeting.getDestinationLatitude().doubleValue();
    double destinationLongitude = meeting.getDestinationLongitude().doubleValue();
    LocalDateTime firstDepartAt = firstQueryDepartAt(meeting);

    List<TmapTransitRoute> routes =
        tmapRouteClient.findTransitRoutes(
            latitude, longitude, destinationLatitude, destinationLongitude, firstDepartAt);
    if (routes.isEmpty()) {
      return List.of(walkingTransitRoute(meeting, latitude, longitude));
    }

    int estimatedTimeSeconds = routes.getFirst().totalTimeSeconds();
    if (!needsReQuery(firstDepartAt, estimatedTimeSeconds)) {
      return routes;
    }

    List<TmapTransitRoute> reQueried =
        tmapRouteClient.findTransitRoutes(
            latitude,
            longitude,
            destinationLatitude,
            destinationLongitude,
            reQueryDepartAt(meeting, estimatedTimeSeconds));
    return reQueried.isEmpty() ? routes : reQueried;
  }

  private TmapTransitRoute walkingTransitRoute(Meeting meeting, double latitude, double longitude) {
    double distanceM =
        distanceMeters(
            BigDecimal.valueOf(latitude),
            BigDecimal.valueOf(longitude),
            meeting.getDestinationLatitude(),
            meeting.getDestinationLongitude());
    int totalTimeSeconds = (int) Math.round(distanceM / WALKING_SPEED_METERS_PER_SECOND);
    return new TmapTransitRoute(
        totalTimeSeconds,
        0,
        0,
        null,
        List.of(
            new TmapTransitRoute.Leg(
                TransportType.WALK,
                null,
                null,
                totalTimeSeconds,
                (int) Math.round(distanceM),
                null,
                meeting.getDestinationName(),
                latitude,
                longitude,
                meeting.getDestinationLatitude().doubleValue(),
                meeting.getDestinationLongitude().doubleValue(),
                List.of())));
  }

  private MeetingMemberDepartureResponse applyDeparture(
      MeetingMember member, String placeName, double latitude, double longitude) {
    Meeting meeting = member.getMeeting();
    TmapRoute route = resolveRoute(meeting, latitude, longitude);

    member.updateDeparture(placeName, BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude));
    member.updateEstimatedDuration(route.totalTimeSeconds());
    TmapRoute.Leg mainLeg = mainTransportLeg(route);
    member.updateTransport(mainLeg.transportType(), mainLeg.routeName());

    meetingMemberRouteRepository.deleteAllByMeetingMemberId(member.getId());
    return MeetingMemberDepartureResponse.of(member, saveRoutes(member, placeName, route));
  }

  private TmapRoute resolveRoute(Meeting meeting, double latitude, double longitude) {
    double destinationLatitude = meeting.getDestinationLatitude().doubleValue();
    double destinationLongitude = meeting.getDestinationLongitude().doubleValue();
    LocalDateTime firstDepartAt = firstQueryDepartAt(meeting);

    TmapRoute estimatedRoute =
        tmapRouteClient
            .findTransitRoute(
                latitude, longitude, destinationLatitude, destinationLongitude, firstDepartAt)
            .orElse(null);
    if (estimatedRoute == null) {
      return walkingRoute(meeting, latitude, longitude);
    }
    if (!needsReQuery(firstDepartAt, estimatedRoute.totalTimeSeconds())) {
      return estimatedRoute;
    }

    return tmapRouteClient
        .findTransitRoute(
            latitude,
            longitude,
            destinationLatitude,
            destinationLongitude,
            reQueryDepartAt(meeting, estimatedRoute.totalTimeSeconds()))
        .orElse(estimatedRoute);
  }

  private LocalDateTime firstQueryDepartAt(Meeting meeting) {
    LocalDateTime meetingAt = meeting.getMeetingAt();
    return meetingAt.isAfter(LocalDateTime.now()) ? meetingAt : null;
  }

  private boolean needsReQuery(LocalDateTime firstDepartAt, int estimatedTimeSeconds) {
    return firstDepartAt != null && estimatedTimeSeconds >= ROUTE_RESEARCH_THRESHOLD_SECONDS;
  }

  private LocalDateTime reQueryDepartAt(Meeting meeting, int estimatedTimeSeconds) {
    LocalDateTime departAt = meeting.getMeetingAt().minusSeconds(estimatedTimeSeconds);
    return departAt.isAfter(LocalDateTime.now()) ? departAt : null;
  }

  private void applyNicknameSetting(MeetingMember member, boolean enabled, String nickname) {
    if (!enabled) {
      member.resetNicknameToDefault(member.getUser().getNickname());
      return;
    }
    if (nickname == null || nickname.isBlank()) {
      throw ApiException.of(ErrorCode.INVALID_INPUT_VALUE);
    }
    member.changeNickname(nickname);
  }

  private TmapRoute walkingRoute(Meeting meeting, double latitude, double longitude) {
    double distanceM =
        distanceMeters(
            BigDecimal.valueOf(latitude),
            BigDecimal.valueOf(longitude),
            meeting.getDestinationLatitude(),
            meeting.getDestinationLongitude());
    int totalTimeSeconds = (int) Math.round(distanceM / WALKING_SPEED_METERS_PER_SECOND);
    return new TmapRoute(
        totalTimeSeconds,
        List.of(
            new TmapRoute.Leg(
                TransportType.WALK,
                null,
                null,
                meeting.getDestinationName(),
                totalTimeSeconds,
                0)));
  }

  private List<MeetingMemberRoute> saveRoutes(
      MeetingMember member, String departurePlaceName, TmapRoute route) {
    List<TmapRoute.Leg> legs = route.legs();
    List<MeetingMemberRoute> routes = new ArrayList<>(legs.size());
    for (int index = 0; index < legs.size(); index++) {
      TmapRoute.Leg leg = legs.get(index);
      routes.add(
          new MeetingMemberRoute(
              member,
              index + 1,
              routeContent(leg, index == 0, departurePlaceName),
              leg.transportType(),
              transportContent(leg),
              toMinutes(leg.sectionTimeSeconds())));
    }
    return meetingMemberRouteRepository.saveAll(routes);
  }

  private List<MeetingMemberRoute> findRoutes(MeetingMember member) {
    return meetingMemberRouteRepository.findAllByMeetingMemberIdOrderByRouteIndexAsc(
        member.getId());
  }

  private String routeContent(TmapRoute.Leg leg, boolean first, String departurePlaceName) {
    if (leg.transportType() == TransportType.WALK) {
      if (first) {
        return departurePlaceName;
      }
      return leg.startName() != null ? leg.startName() : leg.endName();
    }
    return leg.startName() + " " + leg.routeName() + " 승차";
  }

  private String transportContent(TmapRoute.Leg leg) {
    return switch (leg.transportType()) {
      case WALK -> "도보";
      case SUBWAY -> leg.stationCount() + "개 역 이동";
      case BUS -> leg.stationCount() + "개 정류장 이동";
      case ETC -> leg.routeName() != null ? leg.routeName() : "이동";
    };
  }

  private TmapRoute.Leg mainTransportLeg(TmapRoute route) {
    return route.legs().stream()
        .filter(leg -> leg.transportType() != TransportType.WALK)
        .findFirst()
        .orElse(route.legs().getFirst());
  }

  private int toMinutes(int seconds) {
    return (int) Math.round(seconds / SECONDS_PER_MINUTE);
  }

  private void registerMember(
      Meeting meeting, User user, MeetingMemberRole role, String nickname, MultipartFile image) {
    String resolvedNickname = nickname != null ? nickname : user.getNickname();
    MeetingMember member = new MeetingMember(meeting, user, role, resolvedNickname);
    meetingMemberRepository.save(member);

    boolean hasImage = image != null && !image.isEmpty();
    String imageUrl = hasImage ? uploadMemberImage(image) : DEFAULT_MEMBER_IMAGE_URL;
    memberImageRepository.save(new MemberImage(member, imageUrl, !hasImage));
  }

  private MeetingMember getActiveMeetingMember(Long userId, Long meetingId) {
    Meeting meeting =
        meetingRepository
            .findById(meetingId)
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_NOT_FOUND));
    if (meeting.getStatus() != MeetingStatus.WAITING
        && meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
      throw ApiException.of(ErrorCode.MEETING_MEMBER_NOT_ACTIVE);
    }

    return meetingMemberRepository
        .findByMeetingIdAndUserId(meetingId, userId)
        .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_MEMBER_NOT_FOUND));
  }

  private boolean isWithinArrivalRadius(
      BigDecimal currentLatitude,
      BigDecimal currentLongitude,
      BigDecimal destinationLatitude,
      BigDecimal destinationLongitude,
      int arrivalRadiusM) {
    return distanceMeters(
            currentLatitude, currentLongitude, destinationLatitude, destinationLongitude)
        <= arrivalRadiusM;
  }

  private double distanceMeters(
      BigDecimal fromLatitude,
      BigDecimal fromLongitude,
      BigDecimal toLatitude,
      BigDecimal toLongitude) {
    double latitudeDifference =
        Math.toRadians(toLatitude.doubleValue() - fromLatitude.doubleValue());
    double longitudeDifference =
        Math.toRadians(toLongitude.doubleValue() - fromLongitude.doubleValue());
    double fromLatitudeRadians = Math.toRadians(fromLatitude.doubleValue());
    double toLatitudeRadians = Math.toRadians(toLatitude.doubleValue());

    double haversine =
        Math.pow(Math.sin(latitudeDifference / 2), 2)
            + Math.cos(fromLatitudeRadians)
                * Math.cos(toLatitudeRadians)
                * Math.pow(Math.sin(longitudeDifference / 2), 2);
    double centralAngle = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    return EARTH_RADIUS_M * centralAngle;
  }

  private String uploadMemberImage(MultipartFile image) {
    String keyName = amazonS3Manager.generatePuzzleKeyName(UUID.randomUUID());
    return amazonS3Manager.uploadFile(keyName, image);
  }

  private String generateInviteCode() {
    String code;
    do {
      code = randomInviteCode();
    } while (meetingRepository.existsByInviteCode(code));
    return code;
  }

  private String randomInviteCode() {
    StringBuilder code = new StringBuilder(INVITE_CODE_LENGTH);
    for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
      code.append(INVITE_CODE_CHARS.charAt(RANDOM.nextInt(INVITE_CODE_CHARS.length())));
    }
    return code.toString();
  }
}
