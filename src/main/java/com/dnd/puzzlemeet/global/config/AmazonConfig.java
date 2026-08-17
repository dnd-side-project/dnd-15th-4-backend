package com.dnd.puzzlemeet.global.config;

import com.dnd.puzzlemeet.global.s3.AmazonS3Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AmazonS3Properties.class)
public class AmazonConfig {}
