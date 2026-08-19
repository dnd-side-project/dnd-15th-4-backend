package com.dnd.puzzlemeet.domain.puzzle.repository;

import com.dnd.puzzlemeet.domain.puzzle.entity.MemberImage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberImageRepository extends JpaRepository<MemberImage, Long> {

  Optional<MemberImage> findByMeetingMemberId(Long meetingMemberId);

  void deleteAllByMeetingMemberId(Long meetingMemberId);
}
