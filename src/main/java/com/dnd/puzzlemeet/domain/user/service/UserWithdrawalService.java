package com.dnd.puzzlemeet.domain.user.service;

import com.dnd.puzzlemeet.domain.auth.repository.RefreshTokenRepository;
import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRouteRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.meeting.service.MeetingCompletionService;
import com.dnd.puzzlemeet.domain.notification.repository.PushSubscriptionRepository;
import com.dnd.puzzlemeet.domain.puzzle.entity.MemberImage;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzlePiece;
import com.dnd.puzzlemeet.domain.puzzle.repository.MemberImageRepository;
import com.dnd.puzzlemeet.domain.puzzle.repository.PuzzleCollectionRepository;
import com.dnd.puzzlemeet.domain.puzzle.repository.PuzzlePieceRepository;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.FavoriteSearchRepository;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserWithdrawalService {

  private static final String DEFAULT_MEMBER_IMAGE_URL =
      "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/puzzles/_+(9)+4.png";
  private static final List<MeetingStatus> ACTIVE_MEETING_STATUSES =
      List.of(MeetingStatus.WAITING, MeetingStatus.IN_PROGRESS);

  private final UserRepository userRepository;
  private final MeetingRepository meetingRepository;
  private final MeetingMemberRepository meetingMemberRepository;
  private final MeetingMemberRouteRepository meetingMemberRouteRepository;
  private final MemberImageRepository memberImageRepository;
  private final PuzzleCollectionRepository puzzleCollectionRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final FavoriteSearchRepository favoriteSearchRepository;
  private final PushSubscriptionRepository pushSubscriptionRepository;
  private final PuzzlePieceRepository puzzlePieceRepository;
  private final MeetingCompletionService meetingCompletionService;
  private final WithdrawalCleanupService withdrawalCleanupService;

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void withdraw(Long userId) {
    User user =
        userRepository
            .findActiveByIdForUpdate(userId)
            .orElseThrow(() -> ApiException.of(ErrorCode.USER_NOT_FOUND));

    List<Meeting> meetings =
        meetingMemberRepository.findMeetingIdsByUserId(userId).stream()
            .map(
                id ->
                    meetingRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_NOT_FOUND)))
            .toList();

    if (meetingRepository.existsByHostUserIdAndStatusIn(userId, ACTIVE_MEETING_STATUSES)) {
      throw ApiException.of(ErrorCode.USER_WITHDRAWAL_BLOCKED_BY_HOSTED_MEETING);
    }

    List<MeetingMember> meetingMembers = meetingMemberRepository.findAllByUserId(userId);
    List<String> uploadedImageUrls = anonymizeMeetingData(meetingMembers);
    meetingMembers.stream()
        .filter(member -> ACTIVE_MEETING_STATUSES.contains(member.getMeeting().getStatus()))
        .forEach(
            member ->
                puzzlePieceRepository
                    .findAllByMeetingMemberId(member.getId())
                    .forEach(PuzzlePiece::releaseMember));
    favoriteSearchRepository.deleteAllByUserId(userId);
    pushSubscriptionRepository.deleteAllByUserId(userId);
    refreshTokenRepository.deleteAllByUserId(userId);
    user.withdraw();

    meetings.forEach(
        meeting -> meetingCompletionService.completeIfAllActiveMembersArrived(meeting.getId()));
    cleanupAfterCommit(userId, uploadedImageUrls);
  }

  private void cleanupAfterCommit(Long userId, List<String> uploadedImageUrls) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            try {
              withdrawalCleanupService.unlinkKakao(userId);
            } catch (RuntimeException exception) {
              log.error(
                  "[탈퇴 정리 실패] kind=KAKAO_UNLINK, errorCode={}",
                  ErrorCode.INTERNAL_SERVER_ERROR.getCode());
            }
            withdrawalCleanupService.deleteImages(uploadedImageUrls);
          }
        });
  }

  private List<String> anonymizeMeetingData(List<MeetingMember> meetingMembers) {
    if (meetingMembers.isEmpty()) {
      return List.of();
    }

    List<Long> meetingMemberIds = meetingMembers.stream().map(MeetingMember::getId).toList();
    meetingMemberRouteRepository.deleteAllByMeetingMemberIdIn(meetingMemberIds);

    List<MemberImage> memberImages =
        memberImageRepository.findAllByMeetingMemberIdIn(meetingMemberIds);
    List<String> uploadedImageUrls =
        memberImages.stream()
            .filter(this::isUploadedMemberImage)
            .map(MemberImage::getImageUrl)
            .distinct()
            .toList();
    memberImages.forEach(image -> image.replaceWithDefaultImage(DEFAULT_MEMBER_IMAGE_URL));
    meetingMembers.forEach(MeetingMember::anonymizeForUserWithdrawal);
    if (!uploadedImageUrls.isEmpty()) {
      puzzleCollectionRepository.replaceImageUrlsWithDefault(
          uploadedImageUrls, DEFAULT_MEMBER_IMAGE_URL);
    }
    return uploadedImageUrls;
  }

  private boolean isUploadedMemberImage(MemberImage memberImage) {
    return !memberImage.isDefaultImage()
        && !DEFAULT_MEMBER_IMAGE_URL.equals(memberImage.getImageUrl());
  }
}
