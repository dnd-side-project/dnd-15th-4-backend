package com.dnd.puzzlemeet.domain.user.entity;

import com.dnd.puzzlemeet.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

  private static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true)
  private Long kakaoId;

  @Column(length = 320)
  private String email;

  @Column(nullable = false, length = 50)
  private String nickname;

  @Column(length = 500)
  private String profileImageUrl;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(nullable = false)
  @ColumnDefault("true")
  private boolean isLocationNotificationEnabled = true;

  @Column(nullable = false)
  @ColumnDefault("true")
  private boolean isFriendArrivalNotificationEnabled = true;

  @Column(nullable = false)
  @ColumnDefault("true")
  private boolean isChatBubbleNotificationEnabled = true;

  public User(Long kakaoId, String nickname, String profileImageUrl) {
    this(kakaoId, nickname, profileImageUrl, null);
  }

  public User(Long kakaoId, String nickname, String profileImageUrl, String email) {
    this.kakaoId = kakaoId;
    this.email = email;
    this.nickname = nickname;
    this.profileImageUrl = profileImageUrl;
    this.isLocationNotificationEnabled = true;
    this.isFriendArrivalNotificationEnabled = true;
    this.isChatBubbleNotificationEnabled = true;
  }

  public void changeNickname(String nickname) {
    this.nickname = nickname;
  }

  public void updateKakaoProfile(String email, String profileImageUrl) {
    this.email = email;
    this.profileImageUrl = profileImageUrl;
  }

  public void updateNotificationSettings(
      boolean locationNotificationEnabled,
      boolean friendArrivalNotificationEnabled,
      boolean chatBubbleNotificationEnabled) {
    this.isLocationNotificationEnabled = locationNotificationEnabled;
    this.isFriendArrivalNotificationEnabled = friendArrivalNotificationEnabled;
    this.isChatBubbleNotificationEnabled = chatBubbleNotificationEnabled;
  }

  public void withdraw() {
    this.kakaoId = null;
    this.email = null;
    this.nickname = WITHDRAWN_NICKNAME;
    this.profileImageUrl = null;
    this.isLocationNotificationEnabled = false;
    this.isFriendArrivalNotificationEnabled = false;
    this.isChatBubbleNotificationEnabled = false;
    this.deletedAt = LocalDateTime.now();
  }

  public boolean isWithdrawn() {
    return deletedAt != null;
  }
}
