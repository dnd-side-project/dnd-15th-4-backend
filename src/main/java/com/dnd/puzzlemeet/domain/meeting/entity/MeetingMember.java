package com.dnd.puzzlemeet.domain.meeting.entity;

import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingMember extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "meeting_id")
  private Meeting meeting;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private MeetingMemberRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MeetingMemberStatus status;

  @Enumerated(EnumType.STRING)
  @Column(length = 10)
  private TransportType transportType;

  @Column(length = 50)
  private String transportLine;

  private LocalDateTime departedAt;

  private LocalDateTime arrivedAt;

  private Integer estimatedDurationSeconds;

  private LocalDateTime durationCalculatedAt;

  @Column(precision = 10, scale = 7)
  private BigDecimal currentLatitude;

  @Column(precision = 10, scale = 7)
  private BigDecimal currentLongitude;

  private LocalDateTime locationUpdatedAt;

  @Column(nullable = false, length = 30)
  private String nickname;

  public MeetingMember(Meeting meeting, User user, MeetingMemberRole role, String nickname) {
    this.meeting = meeting;
    this.user = user;
    this.role = role;
    this.nickname = nickname;
    this.status = MeetingMemberStatus.NOT_STARTED;
  }

  public void depart(TransportType transportType, String transportLine) {
    this.status = MeetingMemberStatus.MOVING;
    this.transportType = transportType;
    this.transportLine = transportLine;
    this.departedAt = LocalDateTime.now();
  }

  public void updateTransport(TransportType transportType, String transportLine) {
    this.transportType = transportType;
    this.transportLine = transportLine;
  }

  public void updateEstimatedDuration(int estimatedDurationSeconds) {
    this.estimatedDurationSeconds = estimatedDurationSeconds;
    this.durationCalculatedAt = LocalDateTime.now();
  }

  public void arrive() {
    this.status = MeetingMemberStatus.ARRIVED;
    this.arrivedAt = LocalDateTime.now();
  }

  public void updateCurrentLocation(BigDecimal currentLatitude, BigDecimal currentLongitude) {
    this.currentLatitude = currentLatitude;
    this.currentLongitude = currentLongitude;
    this.locationUpdatedAt = LocalDateTime.now();
  }
}
