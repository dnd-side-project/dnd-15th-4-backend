package com.dnd.puzzlemeet.global.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AmazonS3ManagerTest {

  @Mock private S3Template s3Template;

  private AmazonS3Manager amazonS3Manager;

  @BeforeEach
  void setUp() {
    amazonS3Manager =
        new AmazonS3Manager(s3Template, new AmazonS3Properties("puzzle-meet-s3", "puzzles"));
  }

  @Test
  @DisplayName("설정된 bucket의 puzzles 하위 이미지를 삭제한다")
  void deletesManagedPuzzleImage() {
    amazonS3Manager.deletePuzzleImage(
        "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/puzzles/uploaded.png");

    verify(s3Template).deleteObject("puzzle-meet-s3", "puzzles/uploaded.png");
  }

  @Test
  @DisplayName("URL 인코딩된 S3 key를 디코딩해서 삭제한다")
  void decodesManagedPuzzleImageKey() {
    amazonS3Manager.deletePuzzleImage(
        "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/puzzles/my%20image.png");

    verify(s3Template).deleteObject("puzzle-meet-s3", "puzzles/my image.png");
  }

  @Test
  @DisplayName("puzzles 경로 밖의 이미지는 삭제하지 않는다")
  void skipsImageOutsidePuzzlePath() {
    amazonS3Manager.deletePuzzleImage(
        "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/profiles/user.png");

    verifyNoInteractions(s3Template);
  }

  @Test
  @DisplayName("다른 bucket의 이미지는 삭제하지 않는다")
  void skipsImageFromDifferentBucket() {
    amazonS3Manager.deletePuzzleImage(
        "https://other-bucket.s3.ap-northeast-2.amazonaws.com/puzzles/uploaded.png");

    verifyNoInteractions(s3Template);
  }

  @Test
  @DisplayName("상위 경로를 포함한 URL은 삭제하지 않는다")
  void skipsImageWithParentPathSegment() {
    amazonS3Manager.deletePuzzleImage(
        "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/puzzles/../private.png");

    verifyNoInteractions(s3Template);
  }

  @Test
  @DisplayName("S3 삭제 실패를 ApiException으로 변환한다")
  void mapsS3DeletionFailureToApiException() {
    String imageUrl = "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/puzzles/uploaded.png";
    willThrow(new IllegalStateException("delete failed"))
        .given(s3Template)
        .deleteObject("puzzle-meet-s3", "puzzles/uploaded.png");

    ApiException exception =
        assertThrows(ApiException.class, () -> amazonS3Manager.deletePuzzleImage(imageUrl));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.S3_DELETE_FAILED);
  }
}
