package com.dnd.puzzlemeet.domain.user.repository;

import com.dnd.puzzlemeet.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByKakaoId(Long kakaoId);
}
