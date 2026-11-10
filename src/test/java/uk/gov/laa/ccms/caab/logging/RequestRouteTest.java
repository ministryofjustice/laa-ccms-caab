package uk.gov.laa.ccms.caab.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

class RequestRouteTest {

  @Test
  void usesTheMatchedRouteTemplateWhenThereIsOne() {
    var request = new MockHttpServletRequest("GET", "/case/300001/evidence/42/remove");
    request.setAttribute(
        HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/{caseContext}/evidence/{id}/remove");

    assertThat(RequestRoute.of(request)).isEqualTo("/{caseContext}/evidence/{id}/remove");
  }

  @Test
  void withoutARouteItRedactsIdentifyingSegmentsAndPathParameters() {
    var request =
        new MockHttpServletRequest(
            "POST", "/case/300001/users/jane.doe%40example.com/AB123456C;jsessionid=abc123");

    assertThat(RequestRoute.of(request)).isEqualTo("/case/*/users/*/*");
  }

  @Test
  void withoutARouteWordOnlySegmentsAreKept() {
    assertThat(RequestRoute.of(new MockHttpServletRequest("GET", "/case/overview")))
        .isEqualTo("/case/overview");
  }

  @Test
  void aVeryLongPathIsCut() {
    var request = new MockHttpServletRequest("GET", "/" + "a".repeat(500));

    assertThat(RequestRoute.of(request)).hasSize(200);
  }
}
