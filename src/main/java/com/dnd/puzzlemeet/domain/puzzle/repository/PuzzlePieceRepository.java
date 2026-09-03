package com.dnd.puzzlemeet.domain.puzzle.repository;

import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzlePiece;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PuzzlePieceRepository extends JpaRepository<PuzzlePiece, Long> {

  List<PuzzlePiece> findAllByMeetingMemberId(Long meetingMemberId);

  @Query(
      """
      select pp from PuzzlePiece pp
      left join fetch pp.meetingMember mm
      left join fetch mm.user
      where pp.puzzlePage.id in :puzzlePageIds
      order by pp.puzzlePage.id asc, pp.pieceIndex asc
      """)
  List<PuzzlePiece> findAllByPuzzlePageIdInFetchMember(
      @Param("puzzlePageIds") List<Long> puzzlePageIds);
}
