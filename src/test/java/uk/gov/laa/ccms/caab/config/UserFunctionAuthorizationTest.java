package uk.gov.laa.ccms.caab.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import uk.gov.laa.ccms.caab.security.NotAuthorisedAccessDeniedHandler;

/**
 * Runs requests through the user-function rules of {@link SecurityConfiguration}. No controllers
 * are registered, so a request the rules let through ends in a 404, and one they refuse is sent
 * back to the referring page.
 */
@SpringJUnitWebConfig(UserFunctionAuthorizationTest.Config.class)
@DisplayName("User function authorisation rules")
class UserFunctionAuthorizationTest {

  private static final String FLASH_MAPS_SESSION_ATTRIBUTE =
      SessionFlashMapManager.class.getName() + ".FLASH_MAPS";

  private static final String REFERER = "http://localhost/previous-page?tab=1";

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
        route("POST", "/case-provider-requests/details", UserRole.SUBMIT_CASE_QUERY),
        route(
            "POST",
            "/application/submit-case-provider-request/confirmed",
            UserRole.SUBMIT_CASE_QUERY),
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
        route("GET", "/case/details/proceeding/0", UserRole.VIEW_PROCEEDING),
        route(
            "POST", "/application/sections/client/details/summary", UserRole.SUBMIT_UPDATE_CLIENT),
        route("POST", "/amendments/sections/client/details/summary", UserRole.SUBMIT_UPDATE_CLIENT),
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
            UserRole.SUBMIT_DOCUMENT_UPLOAD),
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
        route("POST", "/case/billing/poa/submit", UserRole.SUBMIT_PAYMENT_ON_ACCOUNT),
        route("POST", "/case/billing/poa/declaration", UserRole.SUBMIT_PAYMENT_ON_ACCOUNT),
        route("GET", "/case/billing/poa/summary", UserRole.CREATE_PAYMENT_ON_ACCOUNT),
        route("POST", "/case/billing/bill/submit", UserRole.SUBMIT_BILL),
        route("POST", "/case/billing/bill/remove", UserRole.DELETE_BILL),
        route("GET", "/case/billing/bill/copy", UserRole.CREATE_BILL),
        route("GET", "/case/billing/bill", UserRole.CREATE_BILL),
        route("POST", "/case/billing/undertaking", UserRole.ENTER_UNDERTAKING),
        route("GET", "/case/billing", UserRole.VIEW_CASE_BILL),
        route(
            "POST", "/case/outcome-and-awards/proceeding/0/outcome/clear", UserRole.CLEAR_OUTCOME),
        route(
            "POST",
            "/case/outcome-and-awards/proceeding/0/outcome",
            UserRole.UPDATE_PROCEEDING_OUTCOME),
        route(
            "POST",
            "/case/outcome-and-awards/proceeding/0/outcome/court-search",
            UserRole.UPDATE_PROCEEDING_OUTCOME),
        route("GET", "/case/outcome-and-awards", UserRole.RECORD_OUTCOME),
        route("GET", "/case/outcome-and-awards", UserRole.REQUEST_CASE_DISCHARGE),
        route("POST", "/case/outcome-and-awards/asset", UserRole.RECORD_OUTCOME),
        route("GET", "/case/overview", UserRole.VIEW_CASE_DETAILS),
        route("GET", "/case/details", UserRole.VIEW_CASE_DETAILS));
  }

  /** The handler saves its flash map to the session, outside the dispatcher MockMvc reads. */
  @SuppressWarnings("unchecked")
  private static ResultMatcher notAuthorisedFlashed() {
    return result -> {
      List<FlashMap> flashMaps =
          (List<FlashMap>)
              result.getRequest().getSession().getAttribute(FLASH_MAPS_SESSION_ATTRIBUTE);
      assertThat(flashMaps).hasSize(1);
      assertThat(
              flashMaps.getFirst().get(NotAuthorisedAccessDeniedHandler.NOT_AUTHORISED_ATTRIBUTE))
          .isEqualTo(true);
    };
  }

  private static Arguments route(String method, String path, UserRole role) {
    return Arguments.of(HttpMethod.valueOf(method), path, role);
  }

  @ParameterizedTest(name = "{0} {1} is refused without {2}")
  @MethodSource("protectedRoutes")
  void refusedWithoutFunction(HttpMethod method, String path, UserRole role) throws Exception {
    mockMvc
        .perform(
            request(method, path)
                .header(HttpHeaders.REFERER, REFERER)
                .with(user("user").authorities(() -> "OTHER"))
                .with(csrf()))
        .andExpect(redirectedUrl("/previous-page?tab=1"))
        .andExpect(notAuthorisedFlashed());
  }

  @ParameterizedTest(name = "{0} {1} is allowed with {2}")
  @MethodSource("protectedRoutes")
  void allowedWithFunction(HttpMethod method, String path, UserRole role) throws Exception {
    mockMvc
        .perform(request(method, path).with(user("user").authorities(role::getCode)).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Routes outside the rules only need the user to be logged in")
  void unrestrictedRouteNeedsOnlyAuthentication() throws Exception {
    mockMvc
        .perform(request(HttpMethod.GET, "/home").with(user("user").authorities(() -> "OTHER")))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("A refused action without a usable referer returns the user home")
  void refusedWithoutRefererGoesHome() throws Exception {
    mockMvc
        .perform(
            request(HttpMethod.GET, "/case/overview")
                .header(HttpHeaders.REFERER, "https://elsewhere.example/page")
                .with(user("user").authorities(() -> "OTHER")))
        .andExpect(redirectedUrl("/home"))
        .andExpect(notAuthorisedFlashed());
  }

  @Test
  @DisplayName("A refused action whose referer is the refused page returns the user home")
  void refusedFromSamePageGoesHome() throws Exception {
    mockMvc
        .perform(
            request(HttpMethod.GET, "/case/overview")
                .header(HttpHeaders.REFERER, "http://localhost/case/overview")
                .with(user("user").authorities(() -> "OTHER")))
        .andExpect(redirectedUrl("/home"));
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
