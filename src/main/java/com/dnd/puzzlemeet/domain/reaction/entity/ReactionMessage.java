package com.dnd.puzzlemeet.domain.reaction.entity;

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
@Table(name = "reaction_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReactionMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sender_member_id")
  private MeetingMember senderMember;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reaction_preset_id")
  private ReactionPreset reactionPreset;

  @Column(nullable = false, length = 50)
  private String content;

  @Column(nullable = false)
  private LocalDateTime sentAt;

  public ReactionMessage(
      MeetingMember senderMember, ReactionPreset reactionPreset, String content) {
    this.senderMember = senderMember;
    this.reactionPreset = reactionPreset;
    this.content = content;
    this.sentAt = LocalDateTime.now();
  }
}
