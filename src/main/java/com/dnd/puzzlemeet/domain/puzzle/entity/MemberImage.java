package com.dnd.puzzlemeet.domain.puzzle.entity;

import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "member_images")
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberImage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "meeting_member_id")
  private MeetingMember meetingMember;

  @Column(nullable = false, length = 500)
  private String imageUrl;

  @Column(nullable = false)
  private boolean isDefaultImage;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public MemberImage(MeetingMember meetingMember, String imageUrl, boolean isDefaultImage) {
    this.meetingMember = meetingMember;
    this.imageUrl = imageUrl;
    this.isDefaultImage = isDefaultImage;
  }

  public void changeImage(String imageUrl, boolean isDefaultImage) {
    this.imageUrl = imageUrl;
    this.isDefaultImage = isDefaultImage;
  }

  public void replaceWithDefaultImage(String defaultImageUrl) {
    this.imageUrl = defaultImageUrl;
    this.isDefaultImage = true;
  }
}
