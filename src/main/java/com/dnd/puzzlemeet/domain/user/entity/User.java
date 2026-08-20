package com.dnd.puzzlemeet.domain.user.entity;

import com.dnd.puzzlemeet.global.entity.BaseTimeEntity;
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
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private Long kakaoId;

  @Column(length = 320)
  private String email;

  @Column(nullable = false, length = 50)
  private String nickname;

  @Column(length = 500)
  private String profileImageUrl;

  public User(Long kakaoId, String nickname, String profileImageUrl) {
    this(kakaoId, nickname, profileImageUrl, null);
  }

  public User(Long kakaoId, String nickname, String profileImageUrl, String email) {
    this.kakaoId = kakaoId;
    this.email = email;
    this.nickname = nickname;
    this.profileImageUrl = profileImageUrl;
  }

  public void changeNickname(String nickname) {
    this.nickname = nickname;
  }

  public void updateKakaoProfile(String email, String profileImageUrl) {
    this.email = email;
    this.profileImageUrl = profileImageUrl;
  }
}
