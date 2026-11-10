package uk.gov.laa.ccms.caab.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

class RequestRouteTest {

  @AfterEach
  void reset() {
    RequestRoute.clearKnownSegments();
  }

  @Test
  void usesTheMatchedRouteTemplateWhenThereIsOne() {
    var request = new MockHttpServletRequest("GET", "/case/300001/evidence/42/remove");
    request.setAttribute(
        HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/{caseContext}/evidence/{id}/remove");

    assertThat(RequestRoute.of(request)).isEqualTo("/{caseContext}/evidence/{id}/remove");
  }

  @Test
  void withoutARouteOnlyLiteralRouteSegmentsAreKept() {
    RequestRoute.registerKnownSegments(Set.of("application", "client", "confirm"));

    assertThat(
            RequestRoute.of(
                new MockHttpServletRequest("POST", "/application/client/Jane-Smith/confirm")))
        .isEqualTo("/application/client/*/confirm");
    assertThat(
            RequestRoute.of(
                new MockHttpServletRequest(
                    "POST", "/application/client/AB123456C;jsessionid=abc/confirm")))
        .isEqualTo("/application/client/*/confirm");
  }

  @Test
  void beforeTheRoutesAreKnownOnlyAFixedMarkerIsReturned() {
    assertThat(RequestRoute.of(new MockHttpServletRequest("GET", "/application/client/Jane-Smith")))
        .isEqualTo("[unmatched route]");
  }

  @Test
  void aVeryLongPathIsCut() {
    RequestRoute.registerKnownSegments(Set.of("a".repeat(500)));
    var request = new MockHttpServletRequest("GET", "/" + "a".repeat(500));

    assertThat(RequestRoute.of(request)).hasSize(200);
  }
}
