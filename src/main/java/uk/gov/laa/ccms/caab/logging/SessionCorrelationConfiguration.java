package uk.gov.laa.ccms.caab.logging;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Registers correlation before security for both initial requests and redispatches. */
@Configuration(proxyBeanMethods = false)
public class SessionCorrelationConfiguration {

  @Bean
  FilterRegistrationBean<SessionCorrelationFilter> sessionCorrelationFilterRegistration(
      SessionCorrelationFilter filter) {
    FilterRegistrationBean<SessionCorrelationFilter> registration =
        new FilterRegistrationBean<>(filter);
    registration.setName("sessionCorrelationFilter");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
    registration.setDispatcherTypes(
        DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
    registration.setAsyncSupported(true);
    return registration;
  }
}
