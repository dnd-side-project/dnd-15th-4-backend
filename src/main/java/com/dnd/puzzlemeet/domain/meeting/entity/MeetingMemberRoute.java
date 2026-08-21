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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TransportType transportType;

  @Column(length = 50)
  private String routeName;

  @Column(length = 100)
  private String startName;

  @Column(length = 100)
  private String endName;

  @Column(nullable = false)
  private int stationCount;

  @Column(nullable = false)
  private int sectionTimeSeconds;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public MeetingMemberRoute(
      MeetingMember meetingMember,
      int routeIndex,
      TransportType transportType,
      String routeName,
      String startName,
      String endName,
      int stationCount,
      int sectionTimeSeconds) {
    this.meetingMember = meetingMember;
    this.routeIndex = routeIndex;
    this.transportType = transportType;
    this.routeName = routeName;
    this.startName = startName;
    this.endName = endName;
    this.stationCount = stationCount;
    this.sectionTimeSeconds = sectionTimeSeconds;
  }
}
