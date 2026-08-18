package com.dnd.puzzlemeet.domain.meeting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "meeting_member_routes")
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingMemberRoute {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "meeting_member_id")
  private MeetingMember meetingMember;

  @Column(nullable = false)
  private int routeIndex;

  @Column(nullable = false, length = 200)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TransportType transportType;

  @Column(nullable = false, length = 100)
  private String transportContent;

  @Column(nullable = false)
  private int estimatedTimeMinutes;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public MeetingMemberRoute(
      MeetingMember meetingMember,
      int routeIndex,
      String content,
      TransportType transportType,
      String transportContent,
      int estimatedTimeMinutes) {
    this.meetingMember = meetingMember;
    this.routeIndex = routeIndex;
    this.content = content;
    this.transportType = transportType;
    this.transportContent = transportContent;
    this.estimatedTimeMinutes = estimatedTimeMinutes;
  }
}
