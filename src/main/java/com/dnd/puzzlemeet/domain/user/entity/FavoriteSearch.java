package com.dnd.puzzlemeet.domain.user.entity;

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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
    name = "favorite_searches",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_favorite_searches_user_normalized_keyword",
            columnNames = {"user_id", "normalized_keyword"}))
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FavoriteSearch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 100)
  private String keyword;

  @Column(
      name = "normalized_keyword",
      nullable = false,
      length = 100,
      columnDefinition = "varchar(100) collate utf8mb4_bin")
  private String normalizedKeyword;

  @Column(length = 200)
  private String roadAddressName;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public FavoriteSearch(
      User user, String keyword, String normalizedKeyword, String roadAddressName) {
    this.user = user;
    this.keyword = keyword;
    this.normalizedKeyword = normalizedKeyword;
    this.roadAddressName = roadAddressName;
  }
}
