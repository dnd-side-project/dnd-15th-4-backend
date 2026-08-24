package com.dnd.puzzlemeet.domain.user.controller;

import com.dnd.puzzlemeet.domain.user.dto.FavoriteSearchCreateRequest;
import com.dnd.puzzlemeet.domain.user.dto.FavoriteSearchCreateResponse;
import com.dnd.puzzlemeet.domain.user.dto.FavoriteSearchListResponse;
import com.dnd.puzzlemeet.domain.user.service.FavoriteSearchService;
import com.dnd.puzzlemeet.global.annotation.ApiErrorCodeExample;
import com.dnd.puzzlemeet.global.annotation.ApiErrorCodeExamples;
import com.dnd.puzzlemeet.global.response.ApiResult;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.response.SuccessCode;
import com.dnd.puzzlemeet.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "장소 즐겨찾기", description = "사용자 장소 즐겨찾기 API")
@RestController
@RequestMapping("/api/v1/users/me/favorite-searches")
@RequiredArgsConstructor
public class FavoriteSearchController {

  private final FavoriteSearchService favoriteSearchService;

  @Operation(summary = "장소 즐겨찾기 등록", description = "장소명을 본인의 즐겨찾기에 등록한다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.INVALID_INPUT_VALUE,
    ErrorCode.USER_NOT_FOUND,
    ErrorCode.FAVORITE_SEARCH_ALREADY_EXISTS,
    ErrorCode.FAVORITE_SEARCH_LIMIT_EXCEEDED
  })
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping
  public ResponseEntity<ApiResult<FavoriteSearchCreateResponse>> createFavoriteSearch(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody FavoriteSearchCreateRequest request) {
    return ApiResult.success(
        SuccessCode.CREATED, favoriteSearchService.createFavoriteSearch(principal.id(), request));
  }

  @Operation(summary = "장소 즐겨찾기 목록 조회", description = "본인의 장소 즐겨찾기를 최근 등록한 순서로 조회한다.")
  @ApiErrorCodeExample(ErrorCode.AUTH_TOKEN_INVALID)
  @GetMapping
  public ResponseEntity<ApiResult<List<FavoriteSearchListResponse>>> getFavoriteSearches(
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResult.success(favoriteSearchService.getFavoriteSearches(principal.id()));
  }

  @Operation(summary = "장소 즐겨찾기 삭제", description = "본인이 등록한 장소 즐겨찾기를 삭제한다.")
  @ApiErrorCodeExamples({ErrorCode.AUTH_TOKEN_INVALID, ErrorCode.FAVORITE_SEARCH_NOT_FOUND})
  @DeleteMapping("/{favoriteSearchId}")
  public ResponseEntity<ApiResult<Void>> deleteFavoriteSearch(
      @AuthenticationPrincipal UserPrincipal principal,
      @Parameter(description = "삭제할 장소 즐겨찾기 식별자", example = "1") @PathVariable
          Long favoriteSearchId) {
    favoriteSearchService.deleteFavoriteSearch(principal.id(), favoriteSearchId);
    return ApiResult.success(null);
  }
}
