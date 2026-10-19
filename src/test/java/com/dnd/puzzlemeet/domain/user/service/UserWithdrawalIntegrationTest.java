package com.dnd.puzzlemeet.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.dnd.puzzlemeet.TestcontainersConfiguration;
import com.dnd.puzzlemeet.domain.auth.entity.RefreshToken;
import com.dnd.puzzlemeet.domain.auth.repository.RefreshTokenRepository;
import com.dnd.puzzlemeet.domain.auth.service.AuthService;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingJoinRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberDepartureCreateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingPreviewRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingRouteRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.ReactionMessageSendRequest;
import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRole;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberStatus;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus;
import com.dnd.puzzlemeet.domain.meeting.entity.TransportType;
import com.dnd.puzzlemeet.domain.meeting.entity.TravelMode;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.meeting.service.MeetingCompletionService;
import com.dnd.puzzlemeet.domain.meeting.service.MeetingService;
import com.dnd.puzzlemeet.domain.meeting.service.ReactionMessageService;
import com.dnd.puzzlemeet.domain.puzzle.entity.MemberImage;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzleCollection;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzlePage;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzlePiece;
import com.dnd.puzzlemeet.domain.puzzle.repository.MemberImageRepository;
import com.dnd.puzzlemeet.domain.puzzle.repository.PuzzleCollectionRepository;
import com.dnd.puzzlemeet.domain.puzzle.repository.PuzzlePageRepository;
import com.dnd.puzzlemeet.domain.puzzle.repository.PuzzlePieceRepository;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.s3.AmazonS3Manager;
import com.dnd.puzzlemeet.global.security.client.KakaoTokenClient;
import com.dnd.puzzlemeet.global.security.client.KakaoUnlinkClient;
import com.dnd.puzzlemeet.global.security.client.KakaoUserClient;
import com.dnd.puzzlemeet.global.security.client.KakaoUserResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UserWithdrawalIntegrationTest {
  private static final String DEFAULT_IMAGE =
      "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/puzzles/_+(9)+4.png";
  private static final AtomicLong KAKAO_IDS = new AtomicLong(70_000);
  @Autowired private UserWithdrawalService withdrawalService;
  @Autowired private WithdrawalCleanupService cleanupService;
  @Autowired private MeetingCompletionService completionService;
  @Autowired private MeetingService meetingService;
  @Autowired private ReactionMessageService reactionMessageService;
  @Autowired private AuthService authService;
  @Autowired private UserRepository users;
  @Autowired private MeetingRepository meetings;
  @Autowired private MeetingMemberRepository members;
  @Autowired private MemberImageRepository images;
  @Autowired private PuzzlePageRepository pages;
  @Autowired private PuzzlePieceRepository pieces;
  @Autowired private PuzzleCollectionRepository collections;
  @Autowired private RefreshTokenRepository tokens;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private JdbcTemplate jdbc;
  @MockitoBean private KakaoUnlinkClient kakaoUnlinkClient;
  @MockitoBean private AmazonS3Manager amazonS3Manager;
  @MockitoBean private KakaoTokenClient kakaoTokenClient;
  @MockitoBean private KakaoUserClient kakaoUserClient;

  @BeforeEach
  @AfterEach
  void cleanData() {
    for (String table :
        List.of(
            "reaction_messages",
            "puzzle_collections",
            "puzzle_pieces",
            "puzzle_pages",
            "member_images",
            "meeting_member_routes",
            "meeting_members",
            "meetings",
            "refresh_tokens",
            "favorite_searches",
            "push_subscriptions",
            "users")) {
      jdbc.update("delete from " + table);
    }
  }

  @Test
  @DisplayName("탈퇴 트랜잭션이 롤백되면 개인정보와 토큰이 유지되고 외부 호출도 발생하지 않는다")
  void rollsBackWithoutExternalCalls() {
    Fixture f = fixture(true);
    tokens.save(new RefreshToken(f.guest(), "a".repeat(64), LocalDateTime.now().plusDays(1)));
    tx().executeWithoutResult(
            status -> {
              withdrawalService.withdraw(f.guest().getId());
              status.setRollbackOnly();
            });
    assertThat(users.findById(f.guest().getId()).orElseThrow().isWithdrawn()).isFalse();
    assertThat(members.findById(f.guestMember().getId()).orElseThrow().getNickname())
        .isEqualTo("참여자");
    assertThat(images.findById(f.guestImage().getId()).orElseThrow().getImageUrl())
        .isEqualTo(f.guestImage().getImageUrl());
    assertThat(tokens.count()).isEqualTo(1);
    verifyNoInteractions(kakaoUnlinkClient, amazonS3Manager);
  }

  @Test
  @DisplayName("카카오와 S3 정리가 실패해도 탈퇴와 토큰 삭제가 확정되고 새 계정으로 재가입할 수 있다")
  void allowsRejoinAfterExternalCleanupFailure() {
    Fixture f = fixture(true);
    tokens.save(new RefreshToken(f.guest(), "b".repeat(64), LocalDateTime.now().plusDays(1)));
    doThrow(ApiException.of(ErrorCode.AUTH_KAKAO_UNLINK_FAILED))
        .when(kakaoUnlinkClient)
        .unlink(f.guest().getKakaoId());
    doThrow(ApiException.of(ErrorCode.S3_DELETE_FAILED))
        .when(amazonS3Manager)
        .deletePuzzleImage(f.guestImage().getImageUrl());
    withdrawalService.withdraw(f.guest().getId());
    User withdrawn = users.findById(f.guest().getId()).orElseThrow();
    assertThat(withdrawn.isWithdrawn()).isTrue();
    assertThat(withdrawn.getKakaoId()).isNull();
    assertThat(tokens.count()).isZero();
    verify(kakaoUnlinkClient).unlink(f.guest().getKakaoId());
    verify(amazonS3Manager).deletePuzzleImage(f.guestImage().getImageUrl());
    stubLogin(f.guest().getKakaoId());
    authService.loginWithKakao("code");
    assertNewAccount(f.guest());
  }

  @Test
  @DisplayName("탈퇴 커밋 후 외부 정리가 실행되지 않았어도 재가입이 가능하고 늦은 해제는 새 연결을 건드리지 않는다")
  void allowsRejoinBeforeCleanupAndSkipsLateUnlink() {
    User old = users.save(new User(KAKAO_IDS.incrementAndGet(), "사용자", null));
    tx().executeWithoutResult(
            status -> users.findActiveByIdForUpdate(old.getId()).orElseThrow().withdraw());
    stubLogin(old.getKakaoId());
    authService.loginWithKakao("code");
    cleanupService.unlinkKakao(old.getId());
    assertNewAccount(old);
    verifyNoInteractions(kakaoUnlinkClient);
  }

  @Test
  @DisplayName("탈퇴가 먼저 커밋되면 대기하던 위치 갱신과 퀵메시지가 거절되어 개인정보가 복원되지 않는다")
  void rejectsLocationAndReactionAfterWithdrawalWins() throws Exception {
    Fixture f = fixture(false);
    runOverlapping(
        () -> withdrawalService.withdraw(f.guest().getId()),
        () -> {
          ApiException error =
              assertThrows(
                  ApiException.class,
                  () ->
                      meetingService.updateCurrentLocation(
                          f.guest().getId(), f.meeting().getId(), 37.5, 127));
          assertThat(error.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        });
    assertAnonymized(f);
    ApiException error =
        assertThrows(
            ApiException.class,
            () ->
                reactionMessageService.sendReactionMessage(
                    f.guest().getId(), f.meeting().getId(), new ReactionMessageSendRequest(1L)));
    assertThat(error.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    assertThat(jdbc.queryForObject("select count(*) from reaction_messages", Long.class)).isZero();
  }

  @Test
  @DisplayName("위치 갱신이 먼저 커밋되어도 대기하던 탈퇴가 최신 위치를 지우고 익명화를 확정한다")
  void anonymizesLocationWhenLocationWins() throws Exception {
    Fixture f = fixture(false);
    runOverlapping(
        () ->
            meetingService.updateCurrentLocation(f.guest().getId(), f.meeting().getId(), 37.5, 127),
        () -> withdrawalService.withdraw(f.guest().getId()));
    assertAnonymized(f);
  }

  @Test
  @DisplayName("도착 전 참여자를 읽어 둔 위치 요청도 도착 상태와 시각을 덮어쓰지 않는다")
  void targetedLocationUpdatePreservesConcurrentArrival() throws Exception {
    Fixture f = fixture(false);
    CountDownLatch loaded = new CountDownLatch(1);
    CountDownLatch arrived = new CountDownLatch(1);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var future =
          executor.submit(
              () ->
                  tx().executeWithoutResult(
                          status -> {
                            assertThat(
                                    members
                                        .findById(f.guestMember().getId())
                                        .orElseThrow()
                                        .getStatus())
                                .isEqualTo(MeetingMemberStatus.MOVING);
                            loaded.countDown();
                            await(arrived);
                            meetingService.updateCurrentLocation(
                                f.guest().getId(), f.meeting().getId(), 37.6, 127);
                          }));
      try {
        await(loaded);
        meetingService.markMemberArrived(f.guest().getId(), f.meeting().getId());
      } finally {
        arrived.countDown();
      }
      future.get(10, TimeUnit.SECONDS);
    }
    MeetingMember saved = members.findById(f.guestMember().getId()).orElseThrow();
    assertThat(saved.getStatus()).isEqualTo(MeetingMemberStatus.ARRIVED);
    assertThat(saved.getArrivedAt()).isNotNull();
    assertThat(saved.getCurrentLatitude()).isEqualByComparingTo("37.6");
  }

  @Test
  @DisplayName("탈퇴와 도착 순서가 바뀌어도 익명 기록을 유지하고 활성 사용자에게 퍼즐을 한 번만 수집한다")
  void completesAndCollectsOnceWithConcurrentArrivalAndWithdrawal() throws Exception {
    for (boolean withdrawalFirst : List.of(true, false)) {
      cleanData();
      Fixture f = fixture(true);
      Runnable withdrawal = () -> withdrawalService.withdraw(f.guest().getId());
      Runnable arrival =
          () -> meetingService.markMemberArrived(f.host().getId(), f.meeting().getId());
      runOverlapping(
          withdrawalFirst ? withdrawal : arrival, withdrawalFirst ? arrival : withdrawal);
      assertThat(meetings.findById(f.meeting().getId()).orElseThrow().getStatus())
          .isEqualTo(MeetingStatus.COMPLETED);
      assertThat(members.count()).isEqualTo(2);
      assertThat(members.countActiveByMeetingId(f.meeting().getId())).isEqualTo(1);
      assertThat(pieces.findAllByMeetingMemberId(f.guestMember().getId())).isEmpty();
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from puzzle_pieces where meeting_member_id is null and is_system_filled = true",
                  Long.class))
          .isEqualTo(1);
      assertThat(collections.findAll())
          .singleElement()
          .satisfies(
              collection -> {
                assertThat(collection.getUser().getId()).isEqualTo(f.host().getId());
                assertThat(collection.getImageUrl()).isEqualTo(DEFAULT_IMAGE);
              });
      completionService.completeIfAllActiveMembersArrived(f.meeting().getId());
      completionService.completeIfExpired(f.meeting().getId(), LocalDateTime.now().plusDays(1));
      assertThat(collections.count()).isEqualTo(1);
    }
  }

  @Test
  @DisplayName("상세 목록 초대에는 익명 참여자가 남고 정원과 새 퍼즐 배정에서는 제외된다")
  void keepsAnonymousDisplayButExcludesCapacityAndNewAssignment() {
    Fixture f = fixture(false);
    withdrawalService.withdraw(f.guest().getId());
    var detail = meetingService.getMeetingDetail(f.host().getId(), f.meeting().getId());
    assertThat(detail.currentParticipantCount()).isEqualTo(1);
    assertThat(detail.participants()).extracting("name").containsExactly("방장", "탈퇴한 사용자");
    assertThat(
            meetingService.getMeetings(f.host().getId(), null).getFirst().currentParticipantCount())
        .isEqualTo(1);
    assertThat(
            meetingService
                .previewMeeting(new MeetingPreviewRequest(f.meeting().getInviteCode()))
                .currentMemberCount())
        .isEqualTo(1);
    User joining = users.save(new User(KAKAO_IDS.incrementAndGet(), "새 참여자", null));
    given(amazonS3Manager.uploadFile(any(), any())).willReturn(DEFAULT_IMAGE);
    meetingService.joinMeeting(
        joining.getId(),
        new MeetingJoinRequest(f.meeting().getInviteCode(), null, false, false),
        null);
    assertThat(members.countActiveByMeetingId(f.meeting().getId())).isEqualTo(2);
    meetingService.createDeparture(
        f.host().getId(),
        f.meeting().getId(),
        new MeetingMemberDepartureCreateRequest(
            new MeetingMemberDepartureCreateRequest.Departure("출발지", 37.5, 127),
            new MeetingMemberDepartureCreateRequest.NotificationSettings(false, false, false),
            new MeetingMemberDepartureCreateRequest.NicknameSetting(false, null),
            new MeetingRouteRequest(
                60,
                List.of(
                    new MeetingRouteRequest.Step(TransportType.WALK, 60, null, null, List.of()))),
            TravelMode.WALK));
    assertThat(pieces.findAllByMeetingMemberId(f.guestMember().getId())).isEmpty();
    assertThat(pieces.findAll()).hasSize(4);
  }

  @Test
  @DisplayName("완료된 약속은 탈퇴 후에도 담당 조각과 성공 실패 및 수집 기록을 유지하고 이미지만 교체한다")
  void preservesCompletedMeetingHistory() {
    Fixture f = fixture(true);
    tx().executeWithoutResult(
            status -> {
              meetings.findByIdForUpdate(f.meeting().getId()).orElseThrow().complete();
              members.findById(f.guestMember().getId()).orElseThrow().arrive();
              PuzzlePage page =
                  pages.findAllByMeetingIdOrderByPageNumberAsc(f.meeting().getId()).getFirst();
              collections.save(new PuzzleCollection(f.host(), page, f.guestImage().getImageUrl()));
            });
    Long collectionId = collections.findAll().getFirst().getId();
    withdrawalService.withdraw(f.guest().getId());
    assertThat(meetings.findById(f.meeting().getId()).orElseThrow().getStatus())
        .isEqualTo(MeetingStatus.COMPLETED);
    assertThat(members.findById(f.guestMember().getId()).orElseThrow().getStatus())
        .isEqualTo(MeetingMemberStatus.ARRIVED);
    assertThat(pieces.findAllByMeetingMemberId(f.guestMember().getId())).hasSize(1);
    assertThat(collections.findAll())
        .singleElement()
        .satisfies(
            collection -> {
              assertThat(collection.getId()).isEqualTo(collectionId);
              assertThat(collection.getImageUrl()).isEqualTo(DEFAULT_IMAGE);
            });
    assertThat(members.findById(f.hostMember().getId()).orElseThrow().getStatus())
        .isEqualTo(MeetingMemberStatus.NOT_STARTED);
  }

  @Test
  @DisplayName("여러 약속에서 같은 업로드 사진을 사용해도 기본 이미지를 제외하고 한 번만 삭제한다")
  void deduplicatesImageCleanup() {
    Fixture f = fixture(false);
    Meeting other = meetings.save(meeting(f.host()));
    MeetingMember otherMember =
        members.save(
            new MeetingMember(other, f.guest(), MeetingMemberRole.GUEST, "참여자", "default"));
    images.save(new MemberImage(otherMember, f.guestImage().getImageUrl(), false));
    withdrawalService.withdraw(f.guest().getId());
    verify(amazonS3Manager).deletePuzzleImage(f.guestImage().getImageUrl());
    org.mockito.Mockito.verifyNoMoreInteractions(amazonS3Manager);
  }

  @Test
  @DisplayName("카카오 해제가 진행 중이면 재가입은 사용자 잠금을 기다린 뒤 새 계정으로 완료된다")
  void serializesRejoinBehindOngoingUnlink() throws Exception {
    User old = users.save(new User(KAKAO_IDS.incrementAndGet(), "사용자", null));
    tx().executeWithoutResult(
            status -> users.findActiveByIdForUpdate(old.getId()).orElseThrow().withdraw());
    stubLogin(old.getKakaoId());
    CountDownLatch unlinkStarted = new CountDownLatch(1);
    CountDownLatch finishUnlink = new CountDownLatch(1);
    CountDownLatch loginStarted = new CountDownLatch(1);
    given(kakaoUnlinkClient.unlink(old.getKakaoId()))
        .willAnswer(
            invocation -> {
              unlinkStarted.countDown();
              await(finishUnlink);
              return old.getKakaoId();
            });
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var cleanup = executor.submit(() -> cleanupService.unlinkKakao(old.getId()));
      try {
        await(unlinkStarted);
        var login =
            executor.submit(
                () -> {
                  loginStarted.countDown();
                  return authService.loginWithKakao("code");
                });
        try {
          await(loginStarted);
          assertThrows(TimeoutException.class, () -> login.get(200, TimeUnit.MILLISECONDS));
        } finally {
          finishUnlink.countDown();
        }
        cleanup.get(10, TimeUnit.SECONDS);
        login.get(10, TimeUnit.SECONDS);
      } finally {
        finishUnlink.countDown();
      }
    }
    assertNewAccount(old);
    verify(kakaoUnlinkClient).unlink(old.getKakaoId());
  }

  private void stubLogin(Long kakaoId) {
    given(kakaoTokenClient.exchangeAuthorizationCode("code")).willReturn("access");
    given(kakaoUserClient.getUserInfo("access"))
        .willReturn(
            new KakaoUserResponse(
                kakaoId,
                new KakaoUserResponse.KakaoAccount(
                    new KakaoUserResponse.KakaoAccount.Profile("새 사용자", null),
                    false,
                    true,
                    true,
                    "new@example.com")));
  }

  private void assertNewAccount(User old) {
    User registered =
        tx().execute(status -> users.findByKakaoIdForUpdate(old.getKakaoId()).orElseThrow());
    assertThat(registered.getId()).isNotEqualTo(old.getId());
    assertThat(registered.isWithdrawn()).isFalse();
    assertThat(members.findAllByUserId(registered.getId())).isEmpty();
    assertThat(collections.findAllByUserIdFetchMeeting(registered.getId())).isEmpty();
    assertThat(tokens.findAll())
        .singleElement()
        .satisfies(token -> assertThat(token.getUser().getId()).isEqualTo(registered.getId()));
  }

  private void assertAnonymized(Fixture f) {
    MeetingMember member = members.findById(f.guestMember().getId()).orElseThrow();
    assertThat(member.getNickname()).isEqualTo("탈퇴한 사용자");
    assertThat(member.getCurrentLatitude()).isNull();
    assertThat(member.getCurrentLongitude()).isNull();
    assertThat(member.getLocationUpdatedAt()).isNull();
  }

  private void runOverlapping(Runnable firstAction, Runnable secondAction) throws Exception {
    CountDownLatch changed = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    CountDownLatch started = new CountDownLatch(1);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var first =
          executor.submit(
              () ->
                  tx().executeWithoutResult(
                          status -> {
                            firstAction.run();
                            changed.countDown();
                            await(release);
                          }));
      try {
        await(changed);
        var second =
            executor.submit(
                () -> {
                  started.countDown();
                  secondAction.run();
                });
        try {
          await(started);
          assertThrows(TimeoutException.class, () -> second.get(200, TimeUnit.MILLISECONDS));
        } finally {
          release.countDown();
        }
        first.get(10, TimeUnit.SECONDS);
        second.get(10, TimeUnit.SECONDS);
      } finally {
        release.countDown();
      }
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(exception);
    }
  }

  private TransactionTemplate tx() {
    TransactionTemplate tx = new TransactionTemplate(transactionManager);
    tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    return tx;
  }

  private Fixture fixture(boolean assigned) {
    User host = users.save(new User(KAKAO_IDS.incrementAndGet(), "방장", null));
    User guest =
        users.save(new User(KAKAO_IDS.incrementAndGet(), "참여자", "private", "guest@example.com"));
    Meeting meeting = meetings.save(meeting(host));
    MeetingMember hostMember =
        new MeetingMember(meeting, host, MeetingMemberRole.HOST, "방장", "default");
    hostMember.updateCurrentLocation(BigDecimal.valueOf(37.5), BigDecimal.valueOf(127));
    hostMember = members.save(hostMember);
    MeetingMember guestMember =
        new MeetingMember(meeting, guest, MeetingMemberRole.GUEST, "참여자", "default");
    guestMember.depart();
    guestMember.updateCurrentLocation(BigDecimal.valueOf(37.5), BigDecimal.valueOf(127));
    guestMember = members.save(guestMember);
    images.save(new MemberImage(hostMember, DEFAULT_IMAGE, true));
    MemberImage guestImage =
        images.save(
            new MemberImage(
                guestMember,
                "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/puzzles/Photo.png",
                false));
    if (assigned) {
      PuzzlePage page = new PuzzlePage(meeting, 1);
      page.selectRepresentativeImage(guestImage);
      page = pages.save(page);
      pieces.save(new PuzzlePiece(page, hostMember, (byte) 1));
      pieces.save(new PuzzlePiece(page, guestMember, (byte) 2));
    }
    return new Fixture(host, guest, meeting, hostMember, guestMember, guestImage);
  }

  private Meeting meeting(User host) {
    Meeting meeting =
        new Meeting(
            host,
            "테스트 약속",
            LocalDate.now().atTime(23, 59),
            "목적지",
            null,
            BigDecimal.valueOf(37.5),
            BigDecimal.valueOf(127),
            500,
            2,
            UUID.randomUUID().toString().substring(0, 8),
            null);
    meeting.start();
    return meeting;
  }

  private record Fixture(
      User host,
      User guest,
      Meeting meeting,
      MeetingMember hostMember,
      MeetingMember guestMember,
      MemberImage guestImage) {}
}
