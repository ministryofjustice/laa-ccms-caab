package uk.gov.laa.ccms.caab.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;

/** Publishes authorization decisions as Spring events so that denials can be logged. */
@Configuration(proxyBeanMethods = false)
public class AuthorizationEventsConfiguration {

  @Bean
  AuthorizationEventPublisher authorizationEventPublisher(
      ApplicationEventPublisher eventPublisher) {
    return new SpringAuthorizationEventPublisher(eventPublisher);
  }
}
