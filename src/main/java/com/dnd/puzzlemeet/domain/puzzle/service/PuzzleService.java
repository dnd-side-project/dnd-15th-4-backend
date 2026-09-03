package com.dnd.puzzlemeet.domain.puzzle.service;

import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberStatus;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.puzzle.dto.MeetingCollectionResponse;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzleCollection;
import com.dnd.puzzlemeet.domain.puzzle.repository.PuzzleCollectionRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PuzzleService {

  private final PuzzleCollectionRepository puzzleCollectionRepository;
  private final MeetingMemberRepository meetingMemberRepository;

  @Transactional(readOnly = true)
  public List<MeetingCollectionResponse> getMyPuzzleCollections(Long userId) {
    List<PuzzleCollection> collections =
        puzzleCollectionRepository.findAllByUserIdFetchMeeting(userId);
    if (collections.isEmpty()) {
      return List.of();
    }

    Map<Long, List<PuzzleCollection>> collectionsByMeetingId =
        collections.stream()
            .collect(
                Collectors.groupingBy(
                    collection -> collection.getPuzzlePage().getMeeting().getId(),
                    LinkedHashMap::new,
                    Collectors.toList()));

    return collectionsByMeetingId.values().stream().map(this::toMeetingCollectionResponse).toList();
  }

  private MeetingCollectionResponse toMeetingCollectionResponse(
      List<PuzzleCollection> collections) {
    Meeting meeting = collections.get(0).getPuzzlePage().getMeeting();
    List<String> puzzleImageUrls = collections.stream().map(PuzzleCollection::getImageUrl).toList();

    List<MeetingMember> members =
        meetingMemberRepository.findAllByMeetingIdInFetchUser(List.of(meeting.getId()));
    List<MeetingCollectionResponse.RankingEntry> rankings =
        members.stream()
            .filter(member -> !member.getUser().isWithdrawn())
            .map(member -> toCollectionRankingEntry(member, meeting.getMeetingAt()))
            .sorted(
                Comparator.comparing(MeetingCollectionResponse.RankingEntry::late)
                    .thenComparing(
                        MeetingCollectionResponse.RankingEntry::arrivedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

    return new MeetingCollectionResponse(
        meeting.getId(),
        meeting.getTitle(),
        meeting.getMeetingAt(),
        meeting.getDestinationName(),
        puzzleImageUrls,
        rankings);
  }

  private MeetingCollectionResponse.RankingEntry toCollectionRankingEntry(
      MeetingMember member, LocalDateTime meetingAt) {
    boolean arrived = member.getStatus() == MeetingMemberStatus.ARRIVED;
    LocalDateTime arrivedAt = member.getArrivedAt();
    boolean late = !arrived || arrivedAt.isAfter(meetingAt);
    Long earlyArrivalMinutes = late ? null : Duration.between(arrivedAt, meetingAt).toMinutes();

    return new MeetingCollectionResponse.RankingEntry(
        member.getUser().getId(),
        member.getNickname(),
        member.getProfileImageUrl(),
        arrived,
        arrivedAt,
        earlyArrivalMinutes,
        late);
  }
}
