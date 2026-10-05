package uk.gov.laa.ccms.caab.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import uk.gov.laa.ccms.caab.security.NotAuthorisedAccessDeniedHandler;
import uk.gov.laa.ccms.caab.security.RefusedActionSession;

/**
 * Runs requests through the user-function rules of {@link SecurityConfiguration}. No controllers
 * are registered, so a request the rules let through ends in a 404, and one they refuse is sent
 * back to the referring page.
 */
@SpringJUnitWebConfig(UserFunctionAuthorizationTest.Config.class)
@DisplayName("User function authorisation rules")
class UserFunctionAuthorizationTest {

  private static final String PREVIOUS_PAGE = "/previous-page?tab=1";

  private static final String REFERER = "http://localhost" + PREVIOUS_PAGE;

  @Configuration
  @EnableWebMvc
  @EnableWebSecurity
  static class Config {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
      return http.authorizeHttpRequests(
              authorize -> {
                SecurityConfiguration.authorizeUserFunctions(authorize);
                authorize.anyRequest().authenticated();
              })
          .exceptionHandling(
              exceptions -> exceptions.accessDeniedHandler(new NotAuthorisedAccessDeniedHandler()))
          .build();
    }
  }

  @Autowired private WebApplicationContext context;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  static Stream<Arguments> protectedRoutes() {
    return Stream.of(
        route("GET", "/general-provider-requests/types", UserRole.CREATE_GENERAL_REQUEST),
        route("POST", "/general-provider-requests/details", UserRole.CREATE_GENERAL_REQUEST),
        route(
            "POST",
            "/case-provider-requests/details",
            UserRole.SUBMIT_CASE_QUERY,
            UserRole.VIEW_CASE_DETAILS),
        route(
            "POST",
            "/application/submit-case-provider-request/confirmed",
            UserRole.SUBMIT_CASE_QUERY,
            UserRole.VIEW_CASE_DETAILS),
        route("POST", "/application/client/details/summary", UserRole.SUBMIT_REGISTER_CLIENT),
        route("GET", "/application/client/details/summary", UserRole.CREATE_APPLICATION),
        route(
            "POST",
            "/application/submit-general-provider-request/confirmed",
            UserRole.CREATE_GENERAL_REQUEST),
        route("GET", "/application/new", UserRole.CREATE_APPLICATION),
        route("POST", "/application/office", UserRole.CREATE_APPLICATION),
        route("GET", "/application/application-type", UserRole.CREATE_APPLICATION),
        route("POST", "/application/copy-case/search", UserRole.CREATE_APPLICATION),
        route("POST", "/application/client/confirmed", UserRole.CREATE_APPLICATION),
        route("POST", "/application/client-create/confirmed", UserRole.CREATE_APPLICATION),
        route("GET", "/application/agreement", UserRole.CREATE_APPLICATION),
        route("GET", "/application/search", UserRole.VIEW_CASES_AND_APPLICATIONS),
        route("POST", "/application/search", UserRole.VIEW_CASES_AND_APPLICATIONS),
        route("GET", "/application/search/results", UserRole.VIEW_CASES_AND_APPLICATIONS),
        route("GET", "/application/proceedings/add/matter-type", UserRole.ADD_PROCEEDING),
        route("GET", "/amendments/proceedings/add/matter-type", UserRole.ADD_PROCEEDING),
        route("POST", "/amendments/proceedings/add/confirm", UserRole.ADD_PROCEEDING),
        route("GET", "/application/proceedings/12/remove", UserRole.DELETE_PROCEEDING),
        route("POST", "/amendments/proceedings/12/remove", UserRole.DELETE_PROCEEDING),
        route("GET", "/application/proceedings/12/summary", UserRole.VIEW_PROCEEDING),
        route("GET", "/amendments/proceedings/12/summary", UserRole.VIEW_PROCEEDING),
        route("POST", "/amendments/proceedings/edit/confirm", UserRole.VIEW_PROCEEDING),
        route(
            "GET",
            "/case/details/proceeding/0",
            UserRole.VIEW_PROCEEDING,
            UserRole.VIEW_CASE_DETAILS),
        route(
            "POST", "/application/sections/client/details/summary", UserRole.SUBMIT_UPDATE_CLIENT),
        route(
            "POST",
            "/amendments/sections/client/details/summary",
            UserRole.SUBMIT_UPDATE_CLIENT,
            UserRole.VIEW_CLIENT_DETAILS),
        route("POST", "/application/sections", UserRole.SUBMIT_APPLICATION),
        route("GET", "/application/validate", UserRole.SUBMIT_APPLICATION),
        route("POST", "/application/submit/summary", UserRole.SUBMIT_APPLICATION),
        route("POST", "/application/declaration", UserRole.SUBMIT_APPLICATION),
        route("POST", "/amendments/summary", UserRole.SUBMIT_AMENDMENT),
        route("GET", "/amendments/validate", UserRole.SUBMIT_AMENDMENT),
        route("POST", "/amendments/submit/summary", UserRole.SUBMIT_AMENDMENT),
        route("GET", "/amendments/new", UserRole.AMEND_CASE),
        route("GET", "/amendments/create", UserRole.AMEND_CASE),
        route("GET", "/amendments/summary", UserRole.AMEND_CASE),
        route("GET", "/amendments/sections/client/details/summary", UserRole.VIEW_CLIENT_DETAILS),
        route("POST", "/amendments/sections/client/details/basic", UserRole.VIEW_CLIENT_DETAILS),
        route("POST", "/amendments/client-update/confirmed", UserRole.VIEW_CLIENT_DETAILS),
        route(
            "GET",
            "/notifications/7/attachments/3/retrieve",
            UserRole.VIEW_NOTIFICATION_ATTACHMENT),
        route(
            "POST",
            "/notifications/7/provide-documents-or-evidence",
            UserRole.SUBMIT_DOCUMENT_UPLOAD,
            UserRole.UPLOAD_EVIDENCE),
        route("GET", "/notifications/7/provide-documents-or-evidence", UserRole.UPLOAD_EVIDENCE),
        route("POST", "/notifications/7/attachments/upload", UserRole.UPLOAD_EVIDENCE),
        route("GET", "/notifications/7/attachments/3/remove", UserRole.UPLOAD_EVIDENCE),
        route("GET", "/notifications/7/attachments/3/retrieveDraft", UserRole.UPLOAD_EVIDENCE),
        route("POST", "/application/notification-attachments/confirmed", UserRole.UPLOAD_EVIDENCE),
        route("POST", "/notifications/search", UserRole.VIEW_NOTIFICATIONS),
        route("POST", "/notifications/7", UserRole.SUBMIT_NOTIFICATION),
        route("GET", "/notifications/7", UserRole.VIEW_NOTIFICATIONS),
        route("GET", "/notifications/search-results", UserRole.VIEW_NOTIFICATIONS),
        route("GET", "/notifications", UserRole.VIEW_NOTIFICATIONS),
        route(
            "POST",
            "/case/billing/poa/submit",
            UserRole.SUBMIT_PAYMENT_ON_ACCOUNT,
            UserRole.VIEW_CASE_DETAILS),
        route(
            "POST",
            "/case/billing/poa/declaration",
            UserRole.SUBMIT_PAYMENT_ON_ACCOUNT,
            UserRole.VIEW_CASE_DETAILS),
        route(
            "GET",
            "/case/billing/poa/summary",
            UserRole.CREATE_PAYMENT_ON_ACCOUNT,
            UserRole.VIEW_CASE_DETAILS),
        route(
            "POST", "/case/billing/bill/submit", UserRole.SUBMIT_BILL, UserRole.VIEW_CASE_DETAILS),
        route(
            "POST", "/case/billing/bill/remove", UserRole.DELETE_BILL, UserRole.VIEW_CASE_DETAILS),
        route("GET", "/case/billing/bill/copy", UserRole.CREATE_BILL, UserRole.VIEW_CASE_DETAILS),
        route("GET", "/case/billing/bill", UserRole.CREATE_BILL, UserRole.VIEW_CASE_DETAILS),
        route(
            "POST",
            "/case/billing/undertaking",
            UserRole.ENTER_UNDERTAKING,
            UserRole.VIEW_CASE_DETAILS),
        route("GET", "/case/billing", UserRole.VIEW_CASE_BILL, UserRole.VIEW_CASE_DETAILS),
        route(
            "POST",
            "/case/outcome-and-awards/proceeding/0/outcome/clear",
            UserRole.CLEAR_OUTCOME,
            UserRole.VIEW_CASE_DETAILS),
        route(
            "POST",
            "/case/outcome-and-awards/proceeding/0/outcome",
            UserRole.UPDATE_PROCEEDING_OUTCOME,
            UserRole.VIEW_CASE_DETAILS),
        route(
            "POST",
            "/case/outcome-and-awards/proceeding/0/outcome/court-search",
            UserRole.UPDATE_PROCEEDING_OUTCOME,
            UserRole.VIEW_CASE_DETAILS),
        route(
            "GET", "/case/outcome-and-awards", UserRole.RECORD_OUTCOME, UserRole.VIEW_CASE_DETAILS),
        route(
            "GET",
            "/case/outcome-and-awards",
            UserRole.REQUEST_CASE_DISCHARGE,
            UserRole.VIEW_CASE_DETAILS),
        route(
            "POST",
            "/case/outcome-and-awards/asset",
            UserRole.RECORD_OUTCOME,
            UserRole.VIEW_CASE_DETAILS),
        route("GET", "/case/overview", UserRole.VIEW_CASE_DETAILS),
        route("GET", "/application/proceedings/scope-limitations/confirm", UserRole.ADD_PROCEEDING),
        route(
            "POST", "/amendments/proceedings/scope-limitations/1/remove", UserRole.VIEW_PROCEEDING),
        route("GET", "/application/2/view", UserRole.VIEW_CASES_AND_APPLICATIONS),
        route("GET", "/application/2/view", UserRole.VIEW_CASE_DETAILS),
        route("GET", "/case/details", UserRole.VIEW_CASE_DETAILS));
  }

  /** A session in which the referring page was shown by a plain page load. */
  private static MockHttpSession sessionWithRenderedPages(String... pages) {
    MockHttpSession session = new MockHttpSession();
    for (String page : pages) {
      RefusedActionSession.recordRenderedPage(session, page);
    }
    return session;
  }

  private static ResultMatcher notAuthorisedPending() {
    return result ->
        assertThat(RefusedActionSession.consumeNotAuthorised(result.getRequest().getSession()))
            .isTrue();
  }

  /**
   * A protected route: the action's own function, and any further functions its flow needs, such as
   * VC for a case action.
   */
  private static Arguments route(
      String method, String path, UserRole role, UserRole... flowFunctions) {
    return Arguments.of(HttpMethod.valueOf(method), path, role, List.of(flowFunctions));
  }

  @ParameterizedTest(name = "{0} {1} is refused without {2}")
  @MethodSource("protectedRoutes")
  void refusedWithoutFunction(
      HttpMethod method, String path, UserRole role, List<UserRole> flowFunctions)
      throws Exception {
    mockMvc
        .perform(
            request(method, path)
                .session(sessionWithRenderedPages(PREVIOUS_PAGE))
                .header(HttpHeaders.REFERER, REFERER)
                .with(user("user").authorities(() -> "OTHER"))
                .with(csrf()))
        .andExpect(redirectedUrl(PREVIOUS_PAGE))
        .andExpect(notAuthorisedPending());
  }

  @ParameterizedTest(name = "{0} {1} is allowed with {2} and {3}")
  @MethodSource("protectedRoutes")
  void allowedWithFunction(
      HttpMethod method, String path, UserRole role, List<UserRole> flowFunctions)
      throws Exception {
    List<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority(role.getCode()));
    flowFunctions.forEach(
        flowFunction -> authorities.add(new SimpleGrantedAuthority(flowFunction.getCode())));

    mockMvc
        .perform(request(method, path).with(user("user").authorities(authorities)).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @ParameterizedTest(name = "{0} {1} is refused with only {2}, without {3}")
  @MethodSource("routesWithFlowFunctions")
  void refusedWithoutFlowFunction(
      HttpMethod method, String path, UserRole role, List<UserRole> flowFunctions)
      throws Exception {
    mockMvc
        .perform(request(method, path).with(user("user").authorities(role::getCode)).with(csrf()))
        .andExpect(redirectedUrl("/home"));
  }

  static Stream<Arguments> routesWithFlowFunctions() {
    return protectedRoutes().filter(arguments -> !((List<?>) arguments.get()[3]).isEmpty());
  }

  @Test
  @DisplayName("Routes outside the rules only need the user to be logged in")
  void unrestrictedRouteNeedsOnlyAuthentication() throws Exception {
    mockMvc
        .perform(request(HttpMethod.GET, "/home").with(user("user").authorities(() -> "OTHER")))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("A referer not shown by a plain page load is replaced by the last page that was")
  void refererNotRenderedReturnsToLastRenderedPage() throws Exception {
    mockMvc
        .perform(
            request(HttpMethod.GET, "/case/overview")
                .session(sessionWithRenderedPages("/earlier-page", "/last-page?x=1"))
                .header(HttpHeaders.REFERER, "http://localhost/form-posted-page")
                .with(user("user").authorities(() -> "OTHER")))
        .andExpect(redirectedUrl("/last-page?x=1"))
        .andExpect(notAuthorisedPending());
  }

  @Test
  @DisplayName("A refused action without a referer returns to the last page shown")
  void refusedWithoutRefererReturnsToLastRenderedPage() throws Exception {
    mockMvc
        .perform(
            request(HttpMethod.GET, "/case/overview")
                .session(sessionWithRenderedPages("/last-page"))
                .with(user("user").authorities(() -> "OTHER")))
        .andExpect(redirectedUrl("/last-page"));
  }

  @Test
  @DisplayName("With no page to return to, a refused action returns the user home")
  void refusedWithNoPageToReturnToGoesHome() throws Exception {
    mockMvc
        .perform(
            request(HttpMethod.GET, "/case/overview")
                .header(HttpHeaders.REFERER, "https://elsewhere.example/page")
                .with(user("user").authorities(() -> "OTHER")))
        .andExpect(redirectedUrl("/home"))
        .andExpect(notAuthorisedPending());
  }

  @Test
  @DisplayName("The refused page itself is never the page returned to")
  void refusedPageIsNotReturnedTo() throws Exception {
    mockMvc
        .perform(
            request(HttpMethod.GET, "/case/overview")
                .session(sessionWithRenderedPages("/last-page", "/case/overview"))
                .header(HttpHeaders.REFERER, "http://localhost/case/overview")
                .with(user("user").authorities(() -> "OTHER")))
        .andExpect(redirectedUrl("/home"));
  }

  @Test
  @DisplayName("A refused action never redirects to a page that could be read as another host")
  void refusedNeverRedirectsOffSite() throws Exception {
    mockMvc
        .perform(
            request(HttpMethod.GET, "/case/overview")
                .session(sessionWithRenderedPages("//evil.example/page"))
                .header(HttpHeaders.REFERER, "http://localhost//evil.example/page")
                .with(user("user").authorities(() -> "OTHER")))
        .andExpect(redirectedUrl("/home"));
  }

  @Test
  @DisplayName("A refused form post returns to the page it was posted from at the same path")
  void refusedFormPostReturnsToSamePathPage() throws Exception {
    mockMvc
        .perform(
            request(HttpMethod.POST, "/application/sections")
                .session(sessionWithRenderedPages("/earlier-page", "/application/sections"))
                .header(HttpHeaders.REFERER, "http://localhost/application/sections")
                .with(user("user").authorities(() -> "OTHER"))
                .with(csrf()))
        .andExpect(redirectedUrl("/application/sections"))
        .andExpect(notAuthorisedPending());
  }

  @Test
  @DisplayName("A refused background request gets a 403 rather than a redirect")
  void refusedBackgroundRequestIsForbidden() throws Exception {
    mockMvc
        .perform(
            request(HttpMethod.GET, "/notifications/search-options/prefetch")
                .header("Sec-Fetch-Mode", "cors")
                .header(HttpHeaders.ACCEPT, "application/json")
                .with(user("user").authorities(() -> "OTHER")))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("A CSRF failure keeps the 403 response")
  void csrfFailureIsForbidden() throws Exception {
    mockMvc
        .perform(
            request(HttpMethod.POST, "/notifications/7")
                .with(user("user").authorities(UserRole.SUBMIT_NOTIFICATION::getCode)))
        .andExpect(status().isForbidden());
  }
}
