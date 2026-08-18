package com.dnd.puzzlemeet.domain.meeting.config;

import com.dnd.puzzlemeet.domain.meeting.client.TmapProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TmapProperties.class)
public class TmapConfig {}
