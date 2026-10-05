package uk.gov.laa.ccms.caab.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider.ResponseAuthenticationConverter;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider.ResponseToken;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.session.SimpleRedirectInvalidSessionStrategy;
import uk.gov.laa.ccms.caab.security.CspNonceFilter;
import uk.gov.laa.ccms.caab.security.NotAuthorisedAccessDeniedHandler;
import uk.gov.laa.ccms.caab.service.UserService;

/** Configuration class for customizing Spring Security settings. */
@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {

  @Value("${portal.logoutUrl}")
  private String logoutUrl;

  @Value("${csp.report-enabled:true}")
  private boolean cspReportEnabled;

  @Value("${csp.report-only:false}")
  private boolean cspReportOnly;

  @Value("${csp.upgrade-insecure-requests:true}")
  private boolean cspUpgradeInsecureRequests;

  @Value("${laa.ccms.oracle-web-determination-server.url:}")
  private String owdUrl;

  private static final String CASE_CONTEXT = "/{caseContext:application|amendments}";

  private final UserService userService;

  /**
   * Configures Spring Security filters and settings, along with endpoint restrictions based on
   * granted user authorities (actions).
   *
   * @param http The HttpSecurity instance to be configured.
   * @return A SecurityFilterChain instance with configured security settings.
   * @throws Exception If an error occurs during configuration.
   */
  @Bean
  SecurityFilterChain configure(HttpSecurity http) throws Exception {

    OpenSaml5AuthenticationProvider authenticationProvider = new OpenSaml5AuthenticationProvider();
    authenticationProvider.setResponseAuthenticationConverter(groupsConverter());

    return http.authorizeHttpRequests(
            authorize -> {
              authorize
                  .requestMatchers("/assets/**", "/ccms/**", "/govuk-dialect/**", "/favicon.ico")
                  .permitAll()
                  .requestMatchers(
                      HttpMethod.GET,
                      "/actuator/prometheus",
                      "/actuator/health/**",
                      "/actuator/info",
                      "/actuator/metrics")
                  .permitAll() // Ensure Actuator endpoints are excluded
                  .requestMatchers(HttpMethod.POST, "/csp/report")
                  .permitAll();
              authorizeUserFunctions(authorize);
              authorize.anyRequest().authenticated();
            })
        .exceptionHandling(
            exceptions -> exceptions.accessDeniedHandler(new NotAuthorisedAccessDeniedHandler()))
        .csrf(csrf -> csrf.ignoringRequestMatchers("/csp/report"))
        .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
        .addFilterBefore(
            new CspNonceFilter(cspReportEnabled, cspReportOnly, cspUpgradeInsecureRequests, owdUrl),
            BasicAuthenticationFilter.class)
        .sessionManagement(
            sessionManagement ->
                sessionManagement.invalidSessionStrategy(
                    new SimpleRedirectInvalidSessionStrategy("/home")))
        .logout(
            logout ->
                logout.addLogoutHandler(
                    (req, res, auth) -> {
                      try {
                        res.sendRedirect(logoutUrl);
                      } catch (IOException e) {
                        throw new RuntimeException("Failed to redirect to Identity Provider.", e);
                      }
                    }))
        .saml2Login(
            saml2 -> saml2.authenticationManager(new ProviderManager(authenticationProvider)))
        .build();
  }

  /**
   * Restricts each action to the users who hold its function, matching the actions the legacy PUI
   * checks. The first matching rule applies, so specific paths come before the prefixes they sit
   * under.
   *
   * @param authorize the request authorisation registry to add the rules to.
   */
  static void authorizeUserFunctions(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
          authorize) {
    authorize
        .requestMatchers(
            "/general-provider-requests/**",
            "/application/submit-general-provider-request/confirmed")
        .hasAuthority(UserRole.CREATE_GENERAL_REQUEST.getCode())
        .requestMatchers(
            "/case-provider-requests/**", "/application/submit-case-provider-request/confirmed")
        .hasAuthority(UserRole.SUBMIT_CASE_QUERY.getCode())
        .requestMatchers(HttpMethod.POST, "/application/client/details/summary")
        .hasAuthority(UserRole.SUBMIT_REGISTER_CLIENT.getCode())
        .requestMatchers(
            "/application/new",
            "/application/office",
            "/application/category-of-law",
            "/application/application-type",
            "/application/delegated-functions",
            "/application/copy-case/**",
            "/application/client/**",
            "/application/client-create/**",
            "/application/agreement/**")
        .hasAuthority(UserRole.CREATE_APPLICATION.getCode())
        .requestMatchers("/application/search", "/application/search/**")
        .hasAuthority(UserRole.VIEW_CASES_AND_APPLICATIONS.getCode())
        .requestMatchers(CASE_CONTEXT + "/proceedings/add/**")
        .hasAuthority(UserRole.ADD_PROCEEDING.getCode())
        .requestMatchers(CASE_CONTEXT + "/proceedings/*/remove")
        .hasAuthority(UserRole.DELETE_PROCEEDING.getCode())
        .requestMatchers(
            CASE_CONTEXT + "/proceedings/*/summary", CASE_CONTEXT + "/proceedings/edit/**")
        .hasAuthority(UserRole.VIEW_PROCEEDING.getCode())
        .requestMatchers(HttpMethod.POST, CASE_CONTEXT + "/sections/client/details/summary")
        .hasAuthority(UserRole.SUBMIT_UPDATE_CLIENT.getCode())
        .requestMatchers(HttpMethod.POST, "/application/sections")
        .hasAuthority(UserRole.SUBMIT_APPLICATION.getCode())
        .requestMatchers(
            "/application/validate", "/application/submit/summary", "/application/declaration")
        .hasAuthority(UserRole.SUBMIT_APPLICATION.getCode())
        .requestMatchers(HttpMethod.POST, "/amendments/summary")
        .hasAuthority(UserRole.SUBMIT_AMENDMENT.getCode())
        .requestMatchers("/amendments/validate", "/amendments/submit/summary")
        .hasAuthority(UserRole.SUBMIT_AMENDMENT.getCode())
        .requestMatchers(
            "/amendments/new",
            "/amendments/application-type",
            "/amendments/delegated-functions",
            "/amendments/create",
            "/amendments/edit-delegated-functions",
            "/amendments/summary")
        .hasAuthority(UserRole.AMEND_CASE.getCode())
        .requestMatchers("/amendments/sections/client/details/**", "/amendments/client-update/**")
        .hasAuthority(UserRole.VIEW_CLIENT_DETAILS.getCode())
        .requestMatchers("/notifications/*/attachments/*/retrieve")
        .hasAuthority(UserRole.VIEW_NOTIFICATION_ATTACHMENT.getCode())
        .requestMatchers(HttpMethod.POST, "/notifications/*/provide-documents-or-evidence")
        .hasAuthority(UserRole.SUBMIT_DOCUMENT_UPLOAD.getCode())
        .requestMatchers(
            "/notifications/*/provide-documents-or-evidence",
            "/notifications/*/attachments/**",
            "/application/notification-attachments/confirmed")
        .hasAuthority(UserRole.UPLOAD_EVIDENCE.getCode())
        .requestMatchers(HttpMethod.POST, "/notifications/search")
        .hasAuthority(UserRole.VIEW_NOTIFICATIONS.getCode())
        .requestMatchers(HttpMethod.POST, "/notifications/*")
        .hasAuthority(UserRole.SUBMIT_NOTIFICATION.getCode())
        .requestMatchers("/notifications", "/notifications/**")
        .hasAuthority(UserRole.VIEW_NOTIFICATIONS.getCode())
        // Opening a case puts it in the session: a draft goes to its sections, a submitted case
        // to the case overview. Users reach it from case search or a notification's case link.
        .requestMatchers("/application/*/view")
        .hasAnyAuthority(
            UserRole.VIEW_CASES_AND_APPLICATIONS.getCode(), UserRole.VIEW_CASE_DETAILS.getCode())
        // Every case page is reached by opening the case, which the legacy PUI checks, so each
        // case action needs VC as well as its own function.
        .requestMatchers("/case/details/proceeding/*")
        .access(caseAction(UserRole.VIEW_PROCEEDING))
        .requestMatchers(
            "/case/billing/poa/submit",
            "/case/billing/poa/declaration",
            "/case/billing/poa/confirmation")
        .access(caseAction(UserRole.SUBMIT_PAYMENT_ON_ACCOUNT))
        // Creating and deleting a payment on account both need the POA function, as
        // they do in the legacy PUI.
        .requestMatchers("/case/billing/poa", "/case/billing/poa/**")
        .access(caseAction(UserRole.CREATE_PAYMENT_ON_ACCOUNT))
        // Submitting and deleting a bill are separate permissions from creating one in
        // the legacy PUI, which gates them on the subbill and delete-bill functions.
        // The bill rules are exact paths, not a prefix, so every route that submits
        // a bill has to be listed - "/case/billing/bill" alone does not cover them.
        .requestMatchers(
            "/case/billing/bill/submit",
            "/case/billing/bill/declaration",
            "/case/billing/bill/confirmation")
        .access(caseAction(UserRole.SUBMIT_BILL))
        .requestMatchers("/case/billing/bill/remove")
        .access(caseAction(UserRole.DELETE_BILL))
        // Copying creates a bill, and the summary reports on the one being created, so
        // both take the create permission.
        .requestMatchers("/case/billing/bill/copy", "/case/billing/bill/summary")
        .access(caseAction(UserRole.CREATE_BILL))
        .requestMatchers("/case/billing/bill")
        .access(caseAction(UserRole.CREATE_BILL))
        .requestMatchers("/case/billing/undertaking")
        .access(caseAction(UserRole.ENTER_UNDERTAKING))
        .requestMatchers("/case/billing")
        .access(caseAction(UserRole.VIEW_CASE_BILL))
        .requestMatchers("/case/outcome-and-awards/proceeding/*/outcome/clear")
        .access(caseAction(UserRole.CLEAR_OUTCOME))
        .requestMatchers("/case/outcome-and-awards/proceeding/*/outcome/**")
        .access(caseAction(UserRole.UPDATE_PROCEEDING_OUTCOME))
        // The legacy PUI lets either outcome function record an outcome.
        .requestMatchers("/case/outcome-and-awards", "/case/outcome-and-awards/**")
        .access(caseAction(UserRole.RECORD_OUTCOME, UserRole.REQUEST_CASE_DISCHARGE))
        .requestMatchers("/case/**")
        .hasAuthority(UserRole.VIEW_CASE_DETAILS.getCode());
  }

  /** Requires VC, to be in a case, and any one of the action's functions. */
  private static AuthorizationManager<RequestAuthorizationContext> caseAction(
      UserRole... actionRoles) {
    return AuthorizationManagers.allOf(
        AuthorityAuthorizationManager.hasAuthority(UserRole.VIEW_CASE_DETAILS.getCode()),
        AuthorityAuthorizationManager.hasAnyAuthority(
            Arrays.stream(actionRoles).map(UserRole::getCode).toArray(String[]::new)));
  }

  /**
   * Creates a custom converter for processing SAML response tokens.
   *
   * @return A Converter for processing SAML response tokens into authenticated principals.
   */
  private Converter<ResponseToken, Saml2Authentication> groupsConverter() {
    ResponseAuthenticationConverter delegate = new ResponseAuthenticationConverter();
    return responseToken -> {
      Saml2AssertionAuthentication authentication =
          (Saml2AssertionAuthentication) delegate.convert(responseToken);

      Saml2ResponseAssertionAccessor accessor = authentication.getCredentials();

      Map<String, List<Object>> attributes = accessor.getAttributes();

      List<Object> groups = attributes.get("groups");
      Set<GrantedAuthority> authorities = new HashSet<>();
      if (groups != null) {
        groups.stream()
            .map(Object::toString)
            .map(SimpleGrantedAuthority::new)
            .forEach(authorities::add);
      } else {
        authorities.addAll(authentication.getAuthorities());
      }

      // authentication.getPrincipal() is going to be email address here....
      String principal = authentication.getName();
      authorities.addAll(getUserFunctions(principal));
      return new Saml2AssertionAuthentication(
          principal,
          authentication.getCredentials(),
          authorities,
          authentication.getRelyingPartyRegistrationId());
    };
  }

  private Collection<? extends GrantedAuthority> getUserFunctions(String loginId) {
    return userService
        .getUserByLoginId(loginId)
        .blockOptional()
        .orElseThrow(() -> new RuntimeException("Failed to retrieve user functions."))
        .getFunctions()
        .stream()
        .map(SimpleGrantedAuthority::new)
        .toList();
  }
}
