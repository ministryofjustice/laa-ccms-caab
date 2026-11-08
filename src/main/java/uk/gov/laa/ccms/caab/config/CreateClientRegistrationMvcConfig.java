package uk.gov.laa.ccms.caab.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Isolates create-client identity enforcement from edit-client and correspondence journeys. */
@Configuration
public class CreateClientRegistrationMvcConfig implements WebMvcConfigurer {

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(new CreateClientRegistrationInterceptor())
        .addPathPatterns(
            "/application/client/details/**",
            "/application/client/results",
            "/application/agreement");
  }
}
