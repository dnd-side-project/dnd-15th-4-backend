package com.dnd.puzzlemeet.domain.user.service;

import com.dnd.puzzlemeet.domain.auth.repository.RefreshTokenRepository;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRouteRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.notification.repository.PushSubscriptionRepository;
import com.dnd.puzzlemeet.domain.puzzle.entity.MemberImage;
import com.dnd.puzzlemeet.domain.puzzle.repository.MemberImageRepository;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.FavoriteSearchRepository;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.s3.AmazonS3Manager;
import com.dnd.puzzlemeet.global.security.client.KakaoUnlinkClient;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final RefreshTokenRepository refreshTokenRepository;
  private final FavoriteSearchRepository favoriteSearchRepository;
  private final PushSubscriptionRepository pushSubscriptionRepository;
  private final KakaoUnlinkClient kakaoUnlinkClient;
  private final AmazonS3Manager amazonS3Manager;
  private final EntityManager entityManager;

  @Transactional
  public void withdraw(Long userId) {
    User user =
        userRepository
            .findActiveByIdForUpdate(userId)
            .orElseThrow(() -> ApiException.of(ErrorCode.USER_NOT_FOUND));

    if (meetingRepository.existsByHostUserIdAndStatusIn(userId, ACTIVE_MEETING_STATUSES)) {
      throw ApiException.of(ErrorCode.USER_WITHDRAWAL_BLOCKED_BY_HOSTED_MEETING);
    }

    Long kakaoId = user.getKakaoId();

    List<MeetingMember> meetingMembers = meetingMemberRepository.findAllByUserId(userId);
    List<String> uploadedImageUrls = anonymizeMeetingData(meetingMembers);
    favoriteSearchRepository.deleteAllByUserId(userId);
    pushSubscriptionRepository.deleteAllByUserId(userId);
    refreshTokenRepository.deleteAllByUserId(userId);
    user.withdraw();

    entityManager.flush();

    kakaoUnlinkClient.unlink(kakaoId);
    uploadedImageUrls.forEach(amazonS3Manager::deletePuzzleImage);
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
            .toList();
    memberImages.forEach(image -> image.replaceWithDefaultImage(DEFAULT_MEMBER_IMAGE_URL));
    meetingMembers.forEach(MeetingMember::anonymizeForUserWithdrawal);
    return uploadedImageUrls;
  }

  private boolean isUploadedMemberImage(MemberImage memberImage) {
    return !DEFAULT_MEMBER_IMAGE_URL.equals(memberImage.getImageUrl());
  }
}
