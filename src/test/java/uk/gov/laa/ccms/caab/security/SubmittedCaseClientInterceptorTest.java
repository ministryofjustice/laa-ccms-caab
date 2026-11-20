package uk.gov.laa.ccms.caab.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
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

  private static ActiveCase draft() {
    return ActiveCase.builder().caseReferenceNumber("DRAFT").draftApplication(true).build();
  }

  private static ActiveCase submitted() {
    return ActiveCase.builder().caseReferenceNumber("SUBMITTED").build();
  }

  @Test
  @DisplayName("A draft application's client is allowed")
  void allowsDraftApplication() throws Exception {
    session.setAttribute(ACTIVE_CASE, draft());

    assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
  }

  @Test
  @DisplayName("A draft opened after a submitted case is still allowed")
  void allowsDraftAfterSubmittedCase() throws Exception {
    session.setAttribute(CASE, new ApplicationDetail().caseReferenceNumber("SUBMITTED"));
    session.setAttribute(ACTIVE_CASE, draft());

    assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
  }

  @Test
  @DisplayName("The open submitted case's client is refused")
  void refusesSubmittedCase() throws Exception {
    session.setAttribute(CASE, new ApplicationDetail().caseReferenceNumber("SUBMITTED"));
    session.setAttribute(ACTIVE_CASE, submitted());

    assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("A submitted active case is refused after the submitted case leaves the session")
  void refusesSubmittedActiveCaseWithoutCase() throws Exception {
    session.setAttribute(ACTIVE_CASE, submitted());

    assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("A submitted case with no active case yet is refused")
  void refusesSubmittedCaseWithoutActiveCase() throws Exception {
    session.setAttribute(CASE, new ApplicationDetail().caseReferenceNumber("SUBMITTED"));

    assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("A refusal reaches GlobalExceptionHandler and returns the user with the message")
  void refusalIsHandledByTheExceptionAdvice() throws Exception {
    session.setAttribute(CASE, new ApplicationDetail().caseReferenceNumber("SUBMITTED"));
    MockMvc mockMvc =
        MockMvcBuilders.standaloneSetup(new ClientPage())
            .addInterceptors(interceptor)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/application/sections/client/details/summary")
                    .session(session))
            .andReturn();

    assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/home");
    assertThat(RefusedActionSession.consumeNotAuthorised(session)).isTrue();
  }

  @Controller
  static class ClientPage {
    @GetMapping("/application/sections/client/details/summary")
    String summary() {
      return "client-summary";
    }
  }
}
