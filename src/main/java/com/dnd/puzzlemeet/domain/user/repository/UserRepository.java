package com.dnd.puzzlemeet.domain.user.repository;

import com.dnd.puzzlemeet.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByIdAndDeletedAtIsNull(Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.id = :userId and u.deletedAt is null")
  Optional<User> findActiveByIdForUpdate(@Param("userId") Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.kakaoId = :kakaoId")
  Optional<User> findByKakaoIdForUpdate(@Param("kakaoId") Long kakaoId);

  @Query("select u.kakaoId from User u where u.id = :userId")
  Optional<Long> findKakaoIdById(@Param("userId") Long userId);

  boolean existsByIdAndDeletedAtIsNull(Long userId);
}
