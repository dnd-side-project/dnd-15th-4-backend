package com.dnd.puzzlemeet.global.s3;

import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
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

  public void deletePuzzleImage(String imageUrl) {
    Optional<String> puzzleKey = extractPuzzleKey(imageUrl);
    if (puzzleKey.isEmpty()) {
      log.warn("[S3 퍼즐 이미지 삭제 건너뜀] 관리 범위 밖의 URL입니다. url={}", imageUrl);
      return;
    }

    try {
      s3Template.deleteObject(amazonS3Properties.bucket(), puzzleKey.get());
    } catch (RuntimeException e) {
      log.error("[S3 퍼즐 이미지 삭제 실패] key={}", puzzleKey.get(), e);
      throw ApiException.of(ErrorCode.S3_DELETE_FAILED);
    }
  }

  private Optional<String> extractPuzzleKey(String imageUrl) {
    if (imageUrl == null || imageUrl.isBlank()) {
      return Optional.empty();
    }

    try {
      URI uri = URI.create(imageUrl);
      if (!"https".equalsIgnoreCase(uri.getScheme()) || !isConfiguredBucketHost(uri.getHost())) {
        return Optional.empty();
      }

      String path = uri.getPath();
      if (path == null || !path.startsWith("/")) {
        return Optional.empty();
      }

      String key = path.substring(1);
      String puzzlePrefix = normalizedPuzzlePath() + '/';
      if (!key.startsWith(puzzlePrefix) || containsUnsafePathSegment(key)) {
        return Optional.empty();
      }

      String relativeKey = key.substring(puzzlePrefix.length());
      return relativeKey.isBlank() ? Optional.empty() : Optional.of(key);
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private boolean isConfiguredBucketHost(String host) {
    if (host == null) {
      return false;
    }

    String normalizedHost = host.toLowerCase(Locale.ROOT);
    String bucketPrefix = amazonS3Properties.bucket().toLowerCase(Locale.ROOT) + ".s3";
    return normalizedHost.equals(bucketPrefix + ".amazonaws.com")
        || (normalizedHost.startsWith(bucketPrefix + ".")
            && normalizedHost.endsWith(".amazonaws.com"));
  }

  private String normalizedPuzzlePath() {
    return amazonS3Properties.puzzlePath().replaceAll("^/+|/+$", "");
  }

  private boolean containsUnsafePathSegment(String key) {
    return Arrays.stream(key.split("/", -1))
        .anyMatch(segment -> segment.isBlank() || segment.equals(".") || segment.equals(".."));
  }
}
