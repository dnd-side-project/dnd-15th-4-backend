package com.dnd.puzzlemeet.domain.puzzle.repository;

import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzlePage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PuzzlePageRepository extends JpaRepository<PuzzlePage, Long> {

  List<PuzzlePage> findAllByMeetingIdOrderByPageNumberAsc(Long meetingId);

  List<PuzzlePage> findAllByRepresentativeMemberImageId(Long memberImageId);

  boolean existsByMeetingId(Long meetingId);
}
