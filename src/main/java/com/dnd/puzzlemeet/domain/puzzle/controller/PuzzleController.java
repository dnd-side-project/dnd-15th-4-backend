package com.dnd.puzzlemeet.domain.puzzle.controller;

import com.dnd.puzzlemeet.domain.puzzle.dto.MeetingCollectionResponse;
import com.dnd.puzzlemeet.domain.puzzle.service.PuzzleService;
import com.dnd.puzzlemeet.global.annotation.ApiErrorCodeExamples;
import com.dnd.puzzlemeet.global.response.ApiResult;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "퍼즐", description = "퍼즐 API")
@RestController
@RequestMapping("/api/v1/puzzles")
@RequiredArgsConstructor
public class PuzzleController {

  private final PuzzleService puzzleService;

  @Operation(
      summary = "내가 모은 퍼즐 조회",
      description = "로그인한 사용자가 모은 퍼즐을 약속별로 조회한다. 완성된 퍼즐이 있는 약속만 포함된다.")
  @ApiErrorCodeExamples({ErrorCode.AUTH_TOKEN_INVALID})
  @GetMapping("/me")
  public ResponseEntity<ApiResult<List<MeetingCollectionResponse>>> getMyPuzzleCollections(
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResult.success(puzzleService.getMyPuzzleCollections(principal.id()));
  }
}
