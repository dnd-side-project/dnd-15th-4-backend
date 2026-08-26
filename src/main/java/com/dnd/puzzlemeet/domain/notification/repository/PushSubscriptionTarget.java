package com.dnd.puzzlemeet.domain.notification.repository;

public interface PushSubscriptionTarget {

  Long getId();

  Long getUserId();

  String getEndpoint();

  String getP256dh();

  String getAuth();
}
