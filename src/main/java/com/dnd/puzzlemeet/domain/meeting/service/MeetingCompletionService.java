package com.dnd.puzzlemeet.domain.meeting.service;

import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberStatus;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzleCollection;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzlePage;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzlePiece;
import com.dnd.puzzlemeet.domain.puzzle.repository.PuzzleCollectionRepository;
import com.dnd.puzzlemeet.domain.puzzle.repository.PuzzlePageRepository;
import com.dnd.puzzlemeet.domain.puzzle.repository.PuzzlePieceRepository;
import com.dnd.puzzlemeet.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingCompletionService {
  private final MeetingRepository meetingRepository;
  private final MeetingMemberRepository meetingMemberRepository;
  private final PuzzlePageRepository puzzlePageRepository;
  private final PuzzlePieceRepository puzzlePieceRepository;
  private final PuzzleCollectionRepository puzzleCollectionRepository;

  @Transactional
  public void completeIfAllActiveMembersArrived(Long meetingId) {
    Meeting meeting = meetingRepository.findByIdForUpdate(meetingId).orElse(null);
    if (meeting == null
        || meeting.getStatus() != MeetingStatus.IN_PROGRESS
        || meetingMemberRepository.countActiveByMeetingId(meetingId) == 0
        || meetingMemberRepository.existsNotArrivedMember(meetingId)) {
      return;
    }
    complete(meeting);
  }

  @Transactional
  public void completeIfExpired(Long meetingId, LocalDateTime threshold) {
    Meeting meeting = meetingRepository.findByIdForUpdate(meetingId).orElse(null);
    if (meeting == null
        || meeting.getStatus() == MeetingStatus.COMPLETED
        || meeting.getStatus() == MeetingStatus.CANCELED
        || !meeting.getMeetingAt().isBefore(threshold)) {
      return;
    }
    complete(meeting);
  }

  private void complete(Meeting meeting) {
    meeting.complete();
    collectCompletedPuzzles(meeting);
  }

  private boolean isPuzzleCompleted(List<PuzzlePiece> pieces) {
    List<PuzzlePiece> assignedPieces =
        pieces.stream().filter(piece -> piece.getMeetingMember() != null).toList();
    return !assignedPieces.isEmpty()
        && assignedPieces.stream()
            .allMatch(piece -> piece.getMeetingMember().getStatus() == MeetingMemberStatus.ARRIVED);
  }

  private void collectCompletedPuzzles(Meeting meeting) {
    List<PuzzlePage> puzzlePages =
        puzzlePageRepository.findAllByMeetingIdOrderByPageNumberAsc(meeting.getId());
    if (puzzlePages.isEmpty()) {
      return;
    }

    List<Long> puzzlePageIds = puzzlePages.stream().map(PuzzlePage::getId).toList();
    Map<Long, List<PuzzlePiece>> puzzlePiecesByPageId =
        puzzlePieceRepository.findAllByPuzzlePageIdInFetchMember(puzzlePageIds).stream()
            .collect(Collectors.groupingBy(piece -> piece.getPuzzlePage().getId()));

    List<PuzzlePage> completedPages =
        puzzlePages.stream()
            .filter(
                page ->
                    isPuzzleCompleted(puzzlePiecesByPageId.getOrDefault(page.getId(), List.of())))
            .filter(page -> page.getRepresentativeMemberImage() != null)
            .toList();

    if (completedPages.isEmpty()) {
      return;
    }

    List<User> participants =
        meetingMemberRepository.findAllByMeetingIdInFetchUser(List.of(meeting.getId())).stream()
            .map(MeetingMember::getUser)
            .filter(user -> !user.isWithdrawn())
            .toList();

    List<PuzzleCollection> collections =
        completedPages.stream()
            .flatMap(
                page ->
                    participants.stream()
                        .map(
                            user ->
                                new PuzzleCollection(
                                    user, page, page.getRepresentativeMemberImage().getImageUrl())))
            .toList();

    puzzleCollectionRepository.saveAll(collections);
  }
}
