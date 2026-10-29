package uk.gov.laa.ccms.caab.security;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;

@DisplayName("Submitted case client interceptor")
class SubmittedCaseClientInterceptorTest {

  private final SubmittedCaseClientInterceptor interceptor = new SubmittedCaseClientInterceptor();

  private MockHttpSession session;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
    request = new MockHttpServletRequest("GET", "/application/sections/client/details/summary");
    request.setSession(session);
    response = new MockHttpServletResponse();
  }

  @Test
  @DisplayName("A draft application's client is allowed")
  void allowsDraftApplication() throws Exception {
    session.setAttribute(ACTIVE_CASE, ActiveCase.builder().caseReferenceNumber("DRAFT").build());

    assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
  }

  @Test
  @DisplayName("A draft opened after a submitted case is still allowed")
  void allowsDraftAfterSubmittedCase() throws Exception {
    session.setAttribute(CASE, new ApplicationDetail().caseReferenceNumber("SUBMITTED"));
    session.setAttribute(ACTIVE_CASE, ActiveCase.builder().caseReferenceNumber("DRAFT").build());

    assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
  }

  @Test
  @DisplayName("The open submitted case's client is refused")
  void refusesSubmittedCase() throws Exception {
    session.setAttribute(CASE, new ApplicationDetail().caseReferenceNumber("SUBMITTED"));
    session.setAttribute(
        ACTIVE_CASE, ActiveCase.builder().caseReferenceNumber("SUBMITTED").build());

    assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
    assertThat(response.getRedirectedUrl()).isEqualTo("/home");
    assertThat(RefusedActionSession.consumeNotAuthorised(session)).isTrue();
  }

  @Test
  @DisplayName("A submitted case with no active case yet is refused")
  void refusesSubmittedCaseWithoutActiveCase() throws Exception {
    session.setAttribute(CASE, new ApplicationDetail().caseReferenceNumber("SUBMITTED"));

    assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
  }
}
