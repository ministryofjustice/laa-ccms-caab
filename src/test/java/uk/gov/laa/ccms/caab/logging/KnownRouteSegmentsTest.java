package uk.gov.laa.ccms.caab.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = KnownRouteSegmentsTest.Config.class)
class KnownRouteSegmentsTest {

  @Autowired private WebApplicationContext context;

  @BeforeEach
  void register() {
    context.getBean(KnownRouteSegments.class).onRefresh(new ContextRefreshedEvent(context));
  }

  @AfterEach
  void reset() {
    RequestRoute.clearKnownSegments();
  }

  @Test
  void keepsOnlySegmentsThatAreLiteralPartsOfARealRoute() {
    assertThat(RequestRoute.of(new MockHttpServletRequest("GET", "/case/alex-synthetic/overview")))
        .isEqualTo("/case/*/overview");
    assertThat(
            RequestRoute.of(
                new MockHttpServletRequest("POST", "/client/alex.synthetic%40example.test")))
        .isEqualTo("/*/*");
  }

  @Test
  void theContextPathIsKept() {
    var request = new MockHttpServletRequest("GET", "/civil/case/300001/overview");
    request.setContextPath("/civil");

    assertThat(RequestRoute.of(request)).isEqualTo("/civil/case/*/overview");
  }

  @Configuration
  @EnableWebMvc
  static class Config {

    @Bean
    KnownRouteSegments knownRouteSegments() {
      return new KnownRouteSegments();
    }

    @Bean
    ProbeController probeController() {
      return new ProbeController();
    }
  }

  @Controller
  static class ProbeController {

    @GetMapping("/case/{reference}/overview")
    @ResponseBody
    String overview(@org.springframework.web.bind.annotation.PathVariable String reference) {
      return reference;
    }
  }
}
