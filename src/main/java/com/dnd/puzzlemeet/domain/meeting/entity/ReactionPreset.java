package com.dnd.puzzlemeet.domain.meeting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reaction_presets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReactionPreset {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 50)
  private String content;

  @Column(nullable = false)
  private boolean isActive;

  public ReactionPreset(String content) {
    this.content = content;
    this.isActive = true;
  }

  public void activate() {
    this.isActive = true;
  }

  public void deactivate() {
    this.isActive = false;
  }
}
