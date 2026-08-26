package com.dnd.puzzlemeet.domain.notification.service;

import com.dnd.puzzlemeet.domain.notification.repository.PushSubscriptionRepository;
import com.dnd.puzzlemeet.domain.notification.repository.PushSubscriptionTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpiredPushSubscriptionService {

  private final PushSubscriptionRepository pushSubscriptionRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean deleteIfUnchanged(PushSubscriptionTarget target) {
    return pushSubscriptionRepository.deleteExpiredIfUnchanged(
            target.getId(), target.getUserId(), target.getP256dh(), target.getAuth())
        == 1;
  }
}
