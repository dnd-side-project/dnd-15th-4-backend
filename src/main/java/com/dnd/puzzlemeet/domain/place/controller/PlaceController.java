package com.dnd.puzzlemeet.domain.place.controller;

import com.dnd.puzzlemeet.domain.place.dto.PlaceSearchResponse;
import com.dnd.puzzlemeet.domain.place.service.PlaceService;
import com.dnd.puzzlemeet.global.annotation.ApiErrorCodeExamples;
import com.dnd.puzzlemeet.global.response.ApiResult;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "장소", description = "장소 API")
@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {

  private final PlaceService placeService;

  @Operation(summary = "장소 검색", description = "키워드로 장소를 검색한다. 검색 결과가 없으면 빈 목록을 반환한다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.INVALID_INPUT_VALUE,
    ErrorCode.MISSING_REQUEST_PARAMETER,
    ErrorCode.PLACE_SEARCH_UNAVAILABLE
  })
  @GetMapping
  public ResponseEntity<ApiResult<PlaceSearchResponse>> searchPlaces(
      @Parameter(description = "검색할 장소 키워드", example = "강남역")
          @RequestParam
          @NotBlank
          @Size(max = 100)
          String keyword,
      @Parameter(description = "조회할 페이지 번호 (0부터 시작)", example = "0")
          @RequestParam(defaultValue = "0")
          @Min(0)
          @Max(199)
          int page,
      @Parameter(description = "페이지당 조회 개수", example = "20")
          @RequestParam(defaultValue = "20")
          @Min(1)
          @Max(200)
          int size) {
    return ApiResult.success(placeService.searchPlaces(keyword, page, size));
  }
}
