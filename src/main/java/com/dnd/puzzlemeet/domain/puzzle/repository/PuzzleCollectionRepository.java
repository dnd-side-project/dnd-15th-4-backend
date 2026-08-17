package com.dnd.puzzlemeet.domain.puzzle.repository;

import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzleCollection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PuzzleCollectionRepository extends JpaRepository<PuzzleCollection, Long> {

  @Query(
      """
      select pc from PuzzleCollection pc
      join fetch pc.puzzlePage pp
      join fetch pp.meeting m
      where pc.user.id = :userId
      order by m.meetingAt desc, pp.pageNumber asc
      """)
  List<PuzzleCollection> findAllByUserIdFetchMeeting(@Param("userId") Long userId);
}
