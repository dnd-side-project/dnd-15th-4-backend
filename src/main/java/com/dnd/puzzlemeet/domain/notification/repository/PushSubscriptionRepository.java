package com.dnd.puzzlemeet.domain.notification.repository;

import com.dnd.puzzlemeet.domain.notification.entity.PushSubscription;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

  @Modifying
  @Query(
      value =
          """
          insert into push_subscriptions (
            user_id, endpoint, endpoint_hash, p256dh, auth, registered_at, refreshed_at
          )
          select incoming.user_id, incoming.endpoint, incoming.endpoint_hash,
                 incoming.p256dh, incoming.auth,
                 incoming.registered_at, incoming.refreshed_at
          from (
            select u.id as user_id,
                   :endpoint as endpoint,
                   :endpointHash as endpoint_hash,
                   :p256dh as p256dh,
                   :auth as auth,
                   current_timestamp(6) as registered_at,
                   current_timestamp(6) as refreshed_at
            from users u
            where u.id = :userId
              and u.deleted_at is null
          ) as incoming
          on duplicate key update
            user_id = incoming.user_id,
            endpoint = incoming.endpoint,
            p256dh = incoming.p256dh,
            auth = incoming.auth,
            refreshed_at = incoming.refreshed_at
          """,
      nativeQuery = true)
  int upsertForActiveUser(
      @Param("userId") Long userId,
      @Param("endpoint") String endpoint,
      @Param("endpointHash") String endpointHash,
      @Param("p256dh") String p256dh,
      @Param("auth") String auth);

  @Modifying
  @Query(
      """
      delete from PushSubscription ps
      where ps.user.id = :userId
        and ps.endpointHash = :endpointHash
      """)
  int deleteByUserIdAndEndpointHash(
      @Param("userId") Long userId, @Param("endpointHash") String endpointHash);

  @Query(
      """
      select ps.id as id,
             ps.user.id as userId,
             ps.endpoint as endpoint,
             ps.p256dh as p256dh,
             ps.auth as auth
      from PushSubscription ps
      where ps.user.id in :userIds
        and ps.user.deletedAt is null
      order by ps.id asc
      """)
  List<PushSubscriptionTarget> findTargetsByUserIdIn(@Param("userIds") Collection<Long> userIds);

  @Modifying
  @Query(
      value =
          """
          delete from push_subscriptions
          where id = :subscriptionId
            and user_id = :userId
            and binary p256dh = binary :p256dh
            and binary auth = binary :auth
          """,
      nativeQuery = true)
  int deleteExpiredIfUnchanged(
      @Param("subscriptionId") Long subscriptionId,
      @Param("userId") Long userId,
      @Param("p256dh") String p256dh,
      @Param("auth") String auth);

  void deleteAllByUserId(Long userId);
}
