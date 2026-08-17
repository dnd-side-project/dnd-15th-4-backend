package com.dnd.puzzlemeet.global.s3;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.s3")
public record AmazonS3Properties(@NotBlank String bucket, @NotBlank String puzzlePath) {}
