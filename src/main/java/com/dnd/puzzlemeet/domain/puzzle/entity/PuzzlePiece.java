package com.dnd.puzzlemeet.domain.puzzle.entity;

import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "puzzle_pieces")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PuzzlePiece {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "puzzle_page_id")
  private PuzzlePage puzzlePage;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "meeting_member_id")
  private MeetingMember meetingMember;

  @Column(nullable = false)
  private Byte pieceIndex;

  @Column(nullable = false)
  private boolean isRevealed;

  private LocalDateTime revealedAt;

  @Column(nullable = false)
  private boolean isSystemFilled;

  public PuzzlePiece(PuzzlePage puzzlePage, MeetingMember meetingMember, Byte pieceIndex) {
    this.puzzlePage = puzzlePage;
    this.meetingMember = meetingMember;
    this.pieceIndex = pieceIndex;
    this.isRevealed = false;
    this.isSystemFilled = meetingMember == null;
  }

  public void reveal() {
    this.isRevealed = true;
    this.revealedAt = LocalDateTime.now();
  }
}
