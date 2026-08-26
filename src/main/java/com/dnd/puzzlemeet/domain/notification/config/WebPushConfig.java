package com.dnd.puzzlemeet.domain.notification.config;

import jakarta.annotation.PostConstruct;
import java.security.GeneralSecurityException;
import java.security.Security;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WebPushProperties.class)
public class WebPushConfig {

  private static final int CONNECT_TIMEOUT_MILLIS = 2_000;
  private static final int CONNECTION_REQUEST_TIMEOUT_MILLIS = 1_000;
  private static final int RESPONSE_TIMEOUT_MILLIS = 5_000;
  private static final int MAX_CONNECTIONS = 4;

  @PostConstruct
  void registerBouncyCastleProvider() {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
      log.info("[WebPush 설정] BouncyCastle provider 등록 완료");
    }
  }

  @Bean
  PushService pushService(WebPushProperties properties) throws GeneralSecurityException {
    WebPushProperties.Vapid vapid = properties.vapid();
    PushService pushService =
        new PushService(vapid.publicKey(), vapid.privateKey(), vapid.subject());
    if (!Utils.verifyKeyPair(pushService.getPrivateKey(), pushService.getPublicKey())) {
      throw new IllegalStateException("WebPush VAPID 공개키와 개인키가 일치하지 않습니다.");
    }
    return pushService;
  }

  @Bean(destroyMethod = "close")
  CloseableHttpAsyncClient webPushHttpClient() {
    RequestConfig requestConfig =
        RequestConfig.custom()
            .setConnectTimeout(CONNECT_TIMEOUT_MILLIS)
            .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MILLIS)
            .setSocketTimeout(RESPONSE_TIMEOUT_MILLIS)
            .setRedirectsEnabled(false)
            .build();
    CloseableHttpAsyncClient client =
        HttpAsyncClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setMaxConnTotal(MAX_CONNECTIONS)
            .setMaxConnPerRoute(MAX_CONNECTIONS)
            .build();
    client.start();
    return client;
  }
}
