package uk.gov.laa.ccms.caab.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.server.PathContainer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.config.UserRole;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;

@DisplayName("Draft application only interceptor")
class DraftApplicationOnlyInterceptorTest {

  private final DraftApplicationOnlyInterceptor interceptor = new DraftApplicationOnlyInterceptor();

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

  private static ActiveCase amendment() {
    return ActiveCase.builder().caseReferenceNumber("AMENDED").amendment(true).build();
  }

  private static void loggedInWith(String... functions) {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("user", null, functions));
  }

  @AfterEach
  void clearLogin() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName(
      "An amendment is let through for a user who holds VC, as its validation corrections"
          + " use these pages")
  void allowsAmendmentWithViewCaseDetails() throws Exception {
    session.setAttribute(ACTIVE_CASE, amendment());
    loggedInWith(UserRole.VIEW_CASE_DETAILS.getCode(), UserRole.VIEW_CLIENT_DETAILS.getCode());

    assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
  }

  @Test
  @DisplayName("An amendment is refused for a user without VC")
  void refusesAmendmentWithoutViewCaseDetails() throws Exception {
    session.setAttribute(ACTIVE_CASE, amendment());
    loggedInWith(UserRole.AMEND_CASE.getCode());

    assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("An amendment's client details also need CD, as on the amendment pages")
  void amendmentClientDetailsNeedClientDetailsFunction() throws Exception {
    session.setAttribute(ACTIVE_CASE, amendment());
    loggedInWith(UserRole.VIEW_CASE_DETAILS.getCode(), UserRole.AMEND_CASE.getCode());

    assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
        .isInstanceOf(AccessDeniedException.class);

    loggedInWith(UserRole.VIEW_CASE_DETAILS.getCode(), UserRole.VIEW_CLIENT_DETAILS.getCode());
    assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/application/validate",
        "/application/submit/summary",
        "/application/declaration",
        "/application/abandon",
        "/application/abandon/confirmed",
        "/application/sections/application-type",
        "/application/case-costs",
        "/application/proceedings/1/remove",
        "/application/prior-authorities/1/remove"
      })
  @DisplayName("An amendment can use no application page beyond its corrections")
  void amendmentCannotUseApplicationSubmission(String path) {
    session.setAttribute(ACTIVE_CASE, amendment());
    loggedInWith(
        UserRole.VIEW_CASE_DETAILS.getCode(),
        UserRole.SUBMIT_APPLICATION.getCode(),
        UserRole.VIEW_CLIENT_DETAILS.getCode());
    MockHttpServletRequest submission = new MockHttpServletRequest("POST", path);
    submission.setSession(session);

    assertThatThrownBy(() -> interceptor.preHandle(submission, response, new Object()))
        .isInstanceOf(AccessDeniedException.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/application/sections/provider-details",
        "/application/sections/correspondence-address"
      })
  @DisplayName("An amendment's validation corrections are still allowed")
  void amendmentCorrectionsAreAllowed(String path) throws Exception {
    session.setAttribute(ACTIVE_CASE, amendment());
    loggedInWith(UserRole.VIEW_CASE_DETAILS.getCode());
    MockHttpServletRequest correction = new MockHttpServletRequest("POST", path);
    correction.setSession(session);

    assertThat(interceptor.preHandle(correction, response, new Object())).isTrue();
  }

  @Test
  @DisplayName("A draft can still be submitted")
  void draftCanUseApplicationSubmission() throws Exception {
    session.setAttribute(ACTIVE_CASE, ActiveCase.builder().draftApplication(true).build());
    MockHttpServletRequest submission =
        new MockHttpServletRequest("POST", "/application/declaration");
    submission.setSession(session);

    assertThat(interceptor.preHandle(submission, response, new Object())).isTrue();
  }

  @Test
  @DisplayName("A percent-encoded client segment still needs CD")
  void encodedClientDetailsNeedClientDetailsFunction() {
    session.setAttribute(ACTIVE_CASE, amendment());
    loggedInWith(UserRole.VIEW_CASE_DETAILS.getCode());
    MockHttpServletRequest encoded =
        new MockHttpServletRequest("GET", "/application/sections/cl%69ent/details/summary");
    encoded.setSession(session);

    assertThatThrownBy(() -> interceptor.preHandle(encoded, response, new Object()))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("An amendment's other corrections need only VC")
  void amendmentProviderDetailsNeedOnlyViewCaseDetails() throws Exception {
    session.setAttribute(ACTIVE_CASE, amendment());
    loggedInWith(UserRole.VIEW_CASE_DETAILS.getCode());
    MockHttpServletRequest providerDetails =
        new MockHttpServletRequest("POST", "/application/sections/provider-details");
    providerDetails.setSession(session);

    assertThat(interceptor.preHandle(providerDetails, response, new Object())).isTrue();
  }

  @Test
  @DisplayName(
      "With no active case at all, nothing shows a draft is being edited, so it is refused")
  void refusesWithoutActiveCase() {
    assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("A refusal reaches GlobalExceptionHandler and returns the user with the message")
  void refusalIsHandledByTheExceptionAdvice() throws Exception {
    session.setAttribute(CASE, new ApplicationDetail().caseReferenceNumber("SUBMITTED"));
    MockMvc mockMvc = guardedMockMvc();

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/application/sections/client/details/summary")
                    .session(session))
            .andReturn();

    assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/home");
    assertThat(RefusedActionSession.consumeNotAuthorised(session)).isTrue();
  }

  @Test
  @DisplayName("A retained amendment cannot change case costs through the application pages")
  void retainedAmendmentCannotChangeCaseCosts() throws Exception {
    session.setAttribute(ACTIVE_CASE, submitted());

    MvcResult result =
        guardedMockMvc()
            .perform(MockMvcRequestBuilders.post("/application/case-costs").session(session))
            .andReturn();

    assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/home");
  }

  @Test
  @DisplayName("A draft can change its case costs, and the sections page itself is never guarded")
  void draftCanChangeCaseCosts() throws Exception {
    session.setAttribute(ACTIVE_CASE, submitted());
    MockMvc mockMvc = guardedMockMvc();

    assertThat(
            mockMvc
                .perform(MockMvcRequestBuilders.get("/application/sections").session(session))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(200);

    session.setAttribute(ACTIVE_CASE, draft());
    assertThat(
            mockMvc
                .perform(MockMvcRequestBuilders.post("/application/case-costs").session(session))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(200);
  }

  /**
   * Routes under /application that are deliberately not guarded, with why. Every other route there,
   * including each shared /{caseContext} route, must be guarded.
   */
  private static final Map<String, String> EXEMPT_ROUTES =
      Map.ofEntries(
          Map.entry("/application/new", "starts a new application; there is no draft yet"),
          Map.entry("/application/application-type", "new application flow"),
          Map.entry("/application/delegated-functions", "new application flow"),
          Map.entry("/application/office", "new application flow"),
          Map.entry("/application/category-of-law", "new application flow"),
          Map.entry("/application/agreement", "new application flow"),
          Map.entry("/application/agreement/print", "new application flow"),
          Map.entry("/application/copy-case/**", "new application flow"),
          Map.entry("/application/client/**", "new application flow: choosing the client"),
          Map.entry("/application/client-create", "new application flow"),
          Map.entry("/application/client-create/confirmed", "creates the new draft"),
          Map.entry("/application/sections", "builds the draft's active case"),
          Map.entry(
              "/application/counsel/**",
              "counsel picker for the cost limit quick edit, which runs on a submitted case"),
          Map.entry(
              "/application/summary/print",
              "prints a summary already built by a permitted page, for amendments too"),
          Map.entry(
              "/application/assessments",
              "billing and POA run here against a submitted case, with their own checks"),
          Map.entry(
              "/application/assessments/confirm/**",
              "the OPA interview returns here, including from billing and POA"),
          Map.entry(
              "/application/client-update/**",
              "client update status, after the active case is removed"),
          Map.entry("/application/submit-case/**", "submission status, after the draft is sent"),
          Map.entry("/application/*/confirmed", "submission status, after the draft is sent"),
          Map.entry("/application/*/failed", "submission status, after the draft is sent"),
          Map.entry("/application/search/**", "case search"),
          Map.entry("/application/search", "case search"),
          Map.entry("/application/*/view", "opens a case or draft from search"),
          Map.entry("/application/notification-attachments/confirmed", "notifications"),
          Map.entry("/application/submit-case-provider-request/confirmed", "provider requests"),
          Map.entry("/application/submit-general-provider-request/confirmed", "provider requests"));

  @Test
  @DisplayName("Every application route is guarded or exempt for a stated reason")
  void everyApplicationRouteIsGuardedOrExempt() throws Exception {
    PathPatternParser parser = new PathPatternParser();
    List<PathPattern> guarded =
        DraftApplicationOnlyInterceptor.GUARDED_PATHS.stream().map(parser::parse).toList();
    List<PathPattern> exempt = EXEMPT_ROUTES.keySet().stream().map(parser::parse).toList();

    List<String> uncovered = new ArrayList<>();
    for (String route : applicationRoutes()) {
      PathContainer path = PathContainer.parsePath(route);
      boolean isGuarded = guarded.stream().anyMatch(pattern -> pattern.matches(path));
      boolean isExempt = exempt.stream().anyMatch(pattern -> pattern.matches(path));
      if (!isGuarded && !isExempt) {
        uncovered.add(route);
      }
    }

    assertThat(uncovered)
        .as("Add these to DraftApplicationOnlyInterceptor.GUARDED_PATHS or to EXEMPT_ROUTES")
        .isEmpty();
  }

  /** Each controller route under /application, with sample values for its path variables. */
  private static Set<String> applicationRoutes() throws ClassNotFoundException {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
    Set<String> routes = new TreeSet<>();
    for (BeanDefinition candidate : scanner.findCandidateComponents("uk.gov.laa.ccms.caab")) {
      Class<?> controller =
          ((AbstractBeanDefinition) candidate)
              .resolveBeanClass(DraftApplicationOnlyInterceptorTest.class.getClassLoader());
      if (controller
          .getProtectionDomain()
          .getCodeSource()
          .getLocation()
          .getPath()
          .contains("/test/")) {
        continue;
      }
      for (Method method : controller.getDeclaredMethods()) {
        RequestMapping mapping =
            AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (mapping == null) {
          continue;
        }
        for (String path : mapping.path()) {
          String route = path.replace("{caseContext}", "application").replaceAll("\\{[^}]*}", "x");
          if (route.startsWith("/application/") || route.equals("/application")) {
            routes.add(route);
          }
        }
      }
    }
    return routes;
  }

  private MockMvc guardedMockMvc() {
    return MockMvcBuilders.standaloneSetup(new ApplicationPages())
        .addMappedInterceptors(
            DraftApplicationOnlyInterceptor.GUARDED_PATHS.toArray(String[]::new), interceptor)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Controller
  static class ApplicationPages {
    @GetMapping("/application/sections/client/details/summary")
    @ResponseBody
    String summary() {
      return "client summary";
    }

    @GetMapping("/application/sections")
    @ResponseBody
    String sections() {
      return "sections";
    }

    @PostMapping("/application/case-costs")
    @ResponseBody
    String caseCosts() {
      return "case costs";
    }
  }
}
