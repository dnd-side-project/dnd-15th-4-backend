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
@Table(name = "meetings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "host_user_id")
  private User hostUser;

  @Column(nullable = false, length = 50)
  private String title;

  @Column(nullable = false)
  private LocalDateTime meetingAt;

  @Column(nullable = false, length = 100)
  private String destinationName;

  @Column(length = 200)
  private String destinationAddress;

  @Column(nullable = false, precision = 10, scale = 7)
  private BigDecimal destinationLatitude;

  @Column(nullable = false, precision = 10, scale = 7)
  private BigDecimal destinationLongitude;

  @Column(nullable = false)
  private int arrivalRadiusM;

  @Column(nullable = false, length = 20)
  private String inviteCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MeetingStatus status;

  public Meeting(
      User hostUser,
      String title,
      LocalDateTime meetingAt,
      String destinationName,
      String destinationAddress,
      BigDecimal destinationLatitude,
      BigDecimal destinationLongitude,
      int arrivalRadiusM,
      String inviteCode) {
    this.hostUser = hostUser;
    this.title = title;
    this.meetingAt = meetingAt;
    this.destinationName = destinationName;
    this.destinationAddress = destinationAddress;
    this.destinationLatitude = destinationLatitude;
    this.destinationLongitude = destinationLongitude;
    this.arrivalRadiusM = arrivalRadiusM;
    this.inviteCode = inviteCode;
    this.status = MeetingStatus.WAITING;
  }

  public void updateDetails(
      String title,
      LocalDateTime meetingAt,
      String destinationName,
      BigDecimal destinationLatitude,
      BigDecimal destinationLongitude) {
    this.title = title;
    this.meetingAt = meetingAt;
    this.destinationName = destinationName;
    this.destinationLatitude = destinationLatitude;
    this.destinationLongitude = destinationLongitude;
  }

  public void start() {
    this.status = MeetingStatus.IN_PROGRESS;
  }

  public void complete() {
    this.status = MeetingStatus.COMPLETED;
  }

  public void cancel() {
    this.status = MeetingStatus.CANCELED;
  }
}
