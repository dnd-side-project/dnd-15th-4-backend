package com.dnd.puzzlemeet.domain.user.repository;

import com.dnd.puzzlemeet.domain.user.entity.FavoriteSearch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteSearchRepository extends JpaRepository<FavoriteSearch, Long> {

  boolean existsByUserIdAndNormalizedKeyword(Long userId, String normalizedKeyword);

  long countByUserId(Long userId);

  List<FavoriteSearch> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);

  Optional<FavoriteSearch> findByIdAndUserId(Long favoriteSearchId, Long userId);

  void deleteAllByUserId(Long userId);
}
