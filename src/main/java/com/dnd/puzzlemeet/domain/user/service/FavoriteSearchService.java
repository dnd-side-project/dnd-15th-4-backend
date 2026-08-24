package com.dnd.puzzlemeet.domain.user.service;

import com.dnd.puzzlemeet.domain.user.dto.FavoriteSearchCreateRequest;
import com.dnd.puzzlemeet.domain.user.dto.FavoriteSearchCreateResponse;
import com.dnd.puzzlemeet.domain.user.dto.FavoriteSearchListResponse;
import com.dnd.puzzlemeet.domain.user.entity.FavoriteSearch;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.FavoriteSearchRepository;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteSearchService {

  private static final int MAX_FAVORITE_SEARCH_COUNT = 5;
  private static final int MAX_KEYWORD_LENGTH = 100;

  private final UserRepository userRepository;
  private final FavoriteSearchRepository favoriteSearchRepository;

  @Transactional
  public FavoriteSearchCreateResponse createFavoriteSearch(
      Long userId, FavoriteSearchCreateRequest request) {
    User user =
        userRepository
            .findActiveByIdForUpdate(userId)
            .orElseThrow(() -> ApiException.of(ErrorCode.USER_NOT_FOUND));

    String keyword = normalizeWhitespace(request.keyword());
    String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
    if (keyword.isEmpty()
        || normalizedKeyword.codePointCount(0, normalizedKeyword.length()) > MAX_KEYWORD_LENGTH) {
      throw ApiException.of(ErrorCode.INVALID_INPUT_VALUE);
    }

    if (favoriteSearchRepository.existsByUserIdAndNormalizedKeyword(userId, normalizedKeyword)) {
      throw ApiException.of(ErrorCode.FAVORITE_SEARCH_ALREADY_EXISTS);
    }

    if (favoriteSearchRepository.countByUserId(userId) >= MAX_FAVORITE_SEARCH_COUNT) {
      throw ApiException.of(ErrorCode.FAVORITE_SEARCH_LIMIT_EXCEEDED);
    }

    FavoriteSearch favoriteSearch =
        favoriteSearchRepository.save(new FavoriteSearch(user, keyword, normalizedKeyword));
    return FavoriteSearchCreateResponse.from(favoriteSearch);
  }

  @Transactional(readOnly = true)
  public List<FavoriteSearchListResponse> getFavoriteSearches(Long userId) {
    return favoriteSearchRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
        .map(FavoriteSearchListResponse::from)
        .toList();
  }

  @Transactional
  public void deleteFavoriteSearch(Long userId, Long favoriteSearchId) {
    FavoriteSearch favoriteSearch =
        favoriteSearchRepository
            .findByIdAndUserId(favoriteSearchId, userId)
            .orElseThrow(() -> ApiException.of(ErrorCode.FAVORITE_SEARCH_NOT_FOUND));
    favoriteSearchRepository.delete(favoriteSearch);
  }

  private String normalizeWhitespace(String keyword) {
    StringBuilder normalized = new StringBuilder(keyword.length());
    boolean pendingSpace = false;

    for (int offset = 0; offset < keyword.length(); ) {
      int codePoint = keyword.codePointAt(offset);
      offset += Character.charCount(codePoint);

      if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
        pendingSpace = normalized.length() > 0;
        continue;
      }

      if (pendingSpace) {
        normalized.append(' ');
        pendingSpace = false;
      }
      normalized.appendCodePoint(codePoint);
    }

    return normalized.toString();
  }
}
