package uk.gov.laa.ccms.caab.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@TestConfiguration
public class TestConfig {
  @Bean
  public WebClient.Builder builder() {
    return WebClient.builder();
  }
}
