package com.dnd.puzzlemeet.global.config;

import com.dnd.puzzlemeet.global.client.TmapProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TmapProperties.class)
public class TmapConfig {}
