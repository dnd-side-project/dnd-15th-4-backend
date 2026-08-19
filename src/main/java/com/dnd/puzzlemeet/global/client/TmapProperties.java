package com.dnd.puzzlemeet.global.client;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "tmap")
public record TmapProperties(
    @NotBlank String appKey, @NotBlank String transitRouteUri, @NotBlank String poiSearchUri) {}
