package com.dnd.puzzlemeet.health;

import com.dnd.puzzlemeet.global.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health Check", description = "서버 상태 확인 API")
@RestController
public class HealthCheckController {

  @Operation(summary = "헬스체크", description = "서버가 정상적으로 동작 중인지 확인한다.")
  @GetMapping("/health")
  public ResponseEntity<ApiResult<String>> healthCheck() {
    return ApiResult.success("OK");
  }
}
