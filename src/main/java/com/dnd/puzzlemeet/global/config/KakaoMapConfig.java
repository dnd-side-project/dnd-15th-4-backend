package com.dnd.puzzlemeet.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KakaoMapProperties.class)
public class KakaoMapConfig {}
