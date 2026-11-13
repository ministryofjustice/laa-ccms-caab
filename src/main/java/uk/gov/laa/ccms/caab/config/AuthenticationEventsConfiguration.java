package uk.gov.laa.ccms.caab.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;

/**
 * Publishes authentication outcomes as Spring events so that they can be logged.
 *
 * <p>Kept out of {@link SecurityConfiguration}: the authentication manager needs this while the
 * filter chain is still being built, and the integration tests replace that class with a mock.
 */
@Configuration(proxyBeanMethods = false)
public class AuthenticationEventsConfiguration {

  @Bean
  static AuthenticationEventPublisher authenticationEventPublisher(
      ApplicationEventPublisher eventPublisher) {
    return new DefaultAuthenticationEventPublisher(eventPublisher);
  }
}
