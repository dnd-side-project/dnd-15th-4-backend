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

  @Column(nullable = false, length = 50)
  private String nickname;

  @Column(length = 500)
  private String profileImageUrl;

  public User(Long kakaoId, String nickname, String profileImageUrl) {
    this.kakaoId = kakaoId;
    this.nickname = nickname;
    this.profileImageUrl = profileImageUrl;
  }

  public void changeNickname(String nickname) {
    this.nickname = nickname;
  }

  public void updateProfileImage(String profileImageUrl) {
    this.profileImageUrl = profileImageUrl;
  }
}
