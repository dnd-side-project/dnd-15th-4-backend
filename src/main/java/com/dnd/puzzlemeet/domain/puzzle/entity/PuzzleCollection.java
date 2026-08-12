package com.dnd.puzzlemeet.domain.puzzle.entity;

import com.dnd.puzzlemeet.domain.user.entity.User;
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
@Table(name = "puzzle_collections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PuzzleCollection {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "puzzle_page_id")
  private PuzzlePage puzzlePage;

  @Column(nullable = false, length = 500)
  private String imageUrl;

  @Column(nullable = false)
  private LocalDateTime collectedAt;

  public PuzzleCollection(User user, PuzzlePage puzzlePage, String imageUrl) {
    this.user = user;
    this.puzzlePage = puzzlePage;
    this.imageUrl = imageUrl;
    this.collectedAt = LocalDateTime.now();
  }
}
