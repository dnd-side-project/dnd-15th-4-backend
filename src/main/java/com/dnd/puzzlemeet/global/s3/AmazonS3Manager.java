package com.dnd.puzzlemeet.global.s3;

import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class AmazonS3Manager {

  private final S3Template s3Template;
  private final AmazonS3Properties amazonS3Properties;

  public String uploadFile(String keyName, MultipartFile file) {
    ObjectMetadata metadata =
        ObjectMetadata.builder()
            .contentType(file.getContentType())
            .contentLength(file.getSize())
            .build();

    S3Resource resource;
    try {
      resource =
          s3Template.upload(amazonS3Properties.bucket(), keyName, file.getInputStream(), metadata);
      return resource.getURL().toString();
    } catch (IOException e) {
      log.error("[S3 업로드 실패] key={}", keyName, e);
      throw ApiException.of(ErrorCode.S3_UPLOAD_FAILED);
    }
  }

  public String generatePuzzleKeyName(UUID uuid) {
    return amazonS3Properties.puzzlePath() + '/' + uuid;
  }
}
