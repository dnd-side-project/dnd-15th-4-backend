package com.dnd.puzzlemeet.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.dnd.puzzlemeet.domain.user.dto.FavoriteSearchCreateRequest;
import com.dnd.puzzlemeet.domain.user.dto.FavoriteSearchCreateResponse;
import com.dnd.puzzlemeet.domain.user.entity.FavoriteSearch;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.FavoriteSearchRepository;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FavoriteSearchServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private FavoriteSearchRepository favoriteSearchRepository;

  private FavoriteSearchService favoriteSearchService;

  @BeforeEach
  void setUp() {
    favoriteSearchService = new FavoriteSearchService(userRepository, favoriteSearchRepository);
  }

  @Test
  @DisplayName("장소명을 저장하면 모든 종류의 연속 공백을 한 칸으로 줄이고 소문자로 중복 판정값을 만든다")
  void normalizesAllWhitespaceAndCase() {
    User user = user();
    given(userRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.of(user));
    given(favoriteSearchRepository.existsByUserIdAndNormalizedKeyword(1L, "seoul station 센터"))
        .willReturn(false);
    given(favoriteSearchRepository.countByUserId(1L)).willReturn(0L);
    given(favoriteSearchRepository.save(any(FavoriteSearch.class)))
        .willAnswer(
            invocation -> {
              FavoriteSearch favoriteSearch = invocation.getArgument(0);
              ReflectionTestUtils.setField(favoriteSearch, "id", 10L);
              return favoriteSearch;
            });

    FavoriteSearchCreateResponse response =
        favoriteSearchService.createFavoriteSearch(
            1L, new FavoriteSearchCreateRequest("\u00A0SEOUL\u2007\tStation\n\u202F센터\u3000"));

    ArgumentCaptor<FavoriteSearch> captor = ArgumentCaptor.forClass(FavoriteSearch.class);
    verify(favoriteSearchRepository).save(captor.capture());
    FavoriteSearch saved = captor.getValue();
    assertThat(saved.getKeyword()).isEqualTo("SEOUL Station 센터");
    assertThat(saved.getNormalizedKeyword()).isEqualTo("seoul station 센터");
    assertThat(response.id()).isEqualTo(10L);
    assertThat(response.keyword()).isEqualTo("SEOUL Station 센터");

    InOrder inOrder = inOrder(userRepository, favoriteSearchRepository);
    inOrder.verify(userRepository).findActiveByIdForUpdate(1L);
    inOrder
        .verify(favoriteSearchRepository)
        .existsByUserIdAndNormalizedKeyword(1L, "seoul station 센터");
    inOrder.verify(favoriteSearchRepository).countByUserId(1L);
    inOrder.verify(favoriteSearchRepository).save(any(FavoriteSearch.class));
  }

  @Test
  @DisplayName("유니코드 공백만 있는 장소명은 서비스에서도 입력값 검증 실패로 거절한다")
  void rejectsUnicodeWhitespaceOnlyKeyword() {
    given(userRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.of(user()));

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                favoriteSearchService.createFavoriteSearch(
                    1L, new FavoriteSearchCreateRequest("\u00A0\u2007\u202F\u3000")));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    verifyNoInteractions(favoriteSearchRepository);
  }

  @Test
  @DisplayName("이미 저장된 장소명이고 목록도 가득 찼으면 중복 오류를 먼저 반환한다")
  void rejectsDuplicateBeforeCheckingLimit() {
    given(userRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.of(user()));
    given(favoriteSearchRepository.existsByUserIdAndNormalizedKeyword(1L, "강남역")).willReturn(true);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                favoriteSearchService.createFavoriteSearch(
                    1L, new FavoriteSearchCreateRequest(" 강남역 ")));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FAVORITE_SEARCH_ALREADY_EXISTS);
    verify(favoriteSearchRepository, never()).countByUserId(1L);
    verify(favoriteSearchRepository, never()).save(any(FavoriteSearch.class));
  }

  @Test
  @DisplayName("서로 다른 장소명이 이미 5개이면 새 즐겨찾기 등록을 거절한다")
  void rejectsFavoriteSearchOverLimit() {
    given(userRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.of(user()));
    given(favoriteSearchRepository.existsByUserIdAndNormalizedKeyword(1L, "강남역")).willReturn(false);
    given(favoriteSearchRepository.countByUserId(1L)).willReturn(5L);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                favoriteSearchService.createFavoriteSearch(
                    1L, new FavoriteSearchCreateRequest("강남역")));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FAVORITE_SEARCH_LIMIT_EXCEEDED);
    verify(favoriteSearchRepository, never()).save(any(FavoriteSearch.class));
  }

  @Test
  @DisplayName("활성 사용자가 없으면 장소 즐겨찾기 저장을 시작하지 않는다")
  void rejectsCreationWhenActiveUserNotFound() {
    given(userRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.empty());

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                favoriteSearchService.createFavoriteSearch(
                    1L, new FavoriteSearchCreateRequest("강남역")));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    verifyNoInteractions(favoriteSearchRepository);
  }

  @Test
  @DisplayName("본인이 소유하지 않은 장소 즐겨찾기 식별자로 삭제하면 찾을 수 없음 오류가 발생한다")
  void rejectsDeletionWhenFavoriteSearchIsNotOwned() {
    given(favoriteSearchRepository.findByIdAndUserId(10L, 1L)).willReturn(Optional.empty());

    ApiException exception =
        assertThrows(ApiException.class, () -> favoriteSearchService.deleteFavoriteSearch(1L, 10L));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FAVORITE_SEARCH_NOT_FOUND);
    verify(favoriteSearchRepository, never()).delete(any(FavoriteSearch.class));
  }

  @Test
  @DisplayName("본인이 소유한 장소 즐겨찾기를 조회하면 해당 항목을 삭제한다")
  void deletesOwnedFavoriteSearch() {
    FavoriteSearch favoriteSearch = new FavoriteSearch(user(), "강남역", "강남역");
    given(favoriteSearchRepository.findByIdAndUserId(10L, 1L))
        .willReturn(Optional.of(favoriteSearch));

    favoriteSearchService.deleteFavoriteSearch(1L, 10L);

    verify(favoriteSearchRepository).delete(favoriteSearch);
  }

  private User user() {
    User user = new User(100L, "효창", "https://img.kakao.com/profile.png");
    ReflectionTestUtils.setField(user, "id", 1L);
    return user;
  }
}
