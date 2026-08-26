package com.dnd.puzzlemeet.domain.notification.service;

import com.dnd.puzzlemeet.domain.notification.repository.PushSubscriptionRepository;
import com.dnd.puzzlemeet.domain.notification.repository.PushSubscriptionTarget;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushSubscriptionQueryService {

  private final PushSubscriptionRepository pushSubscriptionRepository;

  @Transactional(readOnly = true)
  public List<PushSubscriptionTarget> findTargets(Collection<Long> userIds) {
    if (userIds.isEmpty()) {
      return List.of();
    }
    return pushSubscriptionRepository.findTargetsByUserIdIn(userIds);
  }
}
