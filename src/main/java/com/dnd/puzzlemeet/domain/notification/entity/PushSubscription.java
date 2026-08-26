package com.dnd.puzzlemeet.domain.notification.entity;

import com.dnd.puzzlemeet.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(
    name = "push_subscriptions",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_push_subscriptions_endpoint_hash",
            columnNames = "endpoint_hash"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSubscription {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 2048)
  private String endpoint;

  @Column(name = "endpoint_hash", nullable = false, length = 64)
  private String endpointHash;

  @Column(nullable = false, length = 100)
  private String p256dh;

  @Column(nullable = false, length = 30)
  private String auth;

  @Column(nullable = false, updatable = false)
  private LocalDateTime registeredAt;

  @Column(nullable = false)
  private LocalDateTime refreshedAt;

  public PushSubscription(
      User user, String endpoint, String endpointHash, String p256dh, String auth) {
    this.user = user;
    this.endpoint = endpoint;
    this.endpointHash = endpointHash;
    this.p256dh = p256dh;
    this.auth = auth;
    this.registeredAt = LocalDateTime.now();
    this.refreshedAt = registeredAt;
  }
}
