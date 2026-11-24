package uk.gov.laa.ccms.caab.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AllAuthoritiesAuthorizationManager;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.session.SimpleRedirectInvalidSessionStrategy;
import uk.gov.laa.ccms.caab.logging.UserContextFilter;
import uk.gov.laa.ccms.caab.security.AbsoluteSessionTimeoutFilter;
import uk.gov.laa.ccms.caab.security.CcmsOidcUser;
import uk.gov.laa.ccms.caab.security.CcmsUserIdentityResolver;
import uk.gov.laa.ccms.caab.security.CcmsUserResolutionException;
import uk.gov.laa.ccms.caab.security.CspNonceFilter;
import uk.gov.laa.ccms.caab.security.CsrfLoggingAccessDeniedHandler;
import uk.gov.laa.ccms.caab.security.IdentityProviderLogoutHandler;
import uk.gov.laa.ccms.caab.security.NotAuthorisedAccessDeniedHandler;
import uk.gov.laa.ccms.caab.security.SecurityEventLogger;
import uk.gov.laa.ccms.caab.security.SessionStartRecordingSuccessHandler;
import uk.gov.laa.ccms.caab.service.UserService;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Configuration class for customizing Spring Security settings. */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SecurityConfiguration {

  /** Where a user is sent when EntraID authenticates them but CCMS will not admit them. */
  static final String AUTHENTICATION_ERROR_PATH = "/authentication-error";

  @Value("${portal.logoutUrl}")
  private String logoutUrl;

  @Value("${laa.ccms.session.absolute-timeout:12h}")
  private Duration absoluteSessionTimeout;

  @Value("${csp.report-enabled:true}")
  private boolean cspReportEnabled;

  @Value("${csp.report-only:false}")
  private boolean cspReportOnly;

  @Value("${csp.upgrade-insecure-requests:true}")
  private boolean cspUpgradeInsecureRequests;

  @Value("${laa.ccms.oracle-web-determination-server.url:}")
  private String owdUrl;

  private final UserService userService;

  private final CcmsUserIdentityResolver ccmsUserIdentityResolver;
  private final SecurityEventLogger securityEventLogger;

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

    return http.authorizeHttpRequests(
            authorize -> {
              authorizePublicRoutes(authorize);
              authorizeUserFunctions(authorize);
              authorizeAuthenticatedRoutes(authorize);
              authorize.anyRequest().denyAll();
            })
        .csrf(
            csrf ->
                csrf.ignoringRequestMatchers("/csp/report")
                    .withObjectPostProcessor(
                        new ObjectPostProcessor<CsrfFilter>() {
                          @Override
                          public <O extends CsrfFilter> O postProcess(O filter) {
                            filter.setAccessDeniedHandler(
                                new CsrfLoggingAccessDeniedHandler(securityEventLogger));
                            return filter;
                          }
                        }))
        .exceptionHandling(
            exceptions ->
                exceptions.accessDeniedHandler(
                    new NotAuthorisedAccessDeniedHandler(
                        new CsrfLoggingAccessDeniedHandler(securityEventLogger))))
        .headers(
            headers ->
                headers
                    .frameOptions(frameOptions -> frameOptions.sameOrigin())
                    .httpStrictTransportSecurity(
                        hsts -> hsts.maxAgeInSeconds(31536000).includeSubDomains(true))
                    .referrerPolicy(
                        referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .permissionsPolicyHeader(
                        permissions ->
                            permissions.policy(
                                "camera=(), microphone=(), geolocation=(), payment=(), usb=()")))
        .addFilterBefore(
            new CspNonceFilter(cspReportEnabled, cspReportOnly, cspUpgradeInsecureRequests, owdUrl),
            BasicAuthenticationFilter.class)
        .addFilterBefore(
            new AbsoluteSessionTimeoutFilter(absoluteSessionTimeout, "/home"),
            AuthorizationFilter.class)
        .addFilterAfter(new UserContextFilter(), SecurityContextHolderFilter.class)
        .sessionManagement(
            sessionManagement ->
                sessionManagement.invalidSessionStrategy(
                    new SimpleRedirectInvalidSessionStrategy("/home")))
        .logout(
            logout ->
                logout.addLogoutHandler(
                    new IdentityProviderLogoutHandler(logoutUrl, securityEventLogger)))
        .oauth2Login(
            oauth2 ->
                oauth2
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService()))
                    .successHandler(new SessionStartRecordingSuccessHandler())
                    .failureHandler(authenticationFailureHandler()))
        .build();
  }

  /** Permits static assets, probes, container error dispatches and deliberate public endpoints. */
  static void authorizePublicRoutes(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
          authorize) {
    authorize
        .dispatcherTypeMatchers(DispatcherType.ERROR)
        .permitAll()
        .requestMatchers(
            "/assets/**",
            "/ccms/**",
            "/govuk-dialect/**",
            "/favicon.ico",
            AUTHENTICATION_ERROR_PATH)
        .permitAll()
        .requestMatchers(
            HttpMethod.GET, "/actuator/prometheus", "/actuator/health", "/actuator/health/**")
        .permitAll() // Ensure Actuator endpoints are excluded
        .requestMatchers(HttpMethod.POST, "/csp/report")
        .permitAll();
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
            "/application/submit-general-provider-request/confirmed",
            "/application/submit-general-provider-request/failed")
        .hasAuthority(UserRole.CREATE_GENERAL_REQUEST.getCode())
        // A case query is raised from an open case.
        .requestMatchers(
            "/case-provider-requests/**",
            "/application/submit-case-provider-request/confirmed",
            "/application/submit-case-provider-request/failed")
        .access(caseAction(UserRole.SUBMIT_CASE_QUERY))
        // Registering a client is a step in creating an application.
        .requestMatchers(HttpMethod.POST, "/application/client/details/summary")
        .access(allFunctions(UserRole.CREATE_APPLICATION, UserRole.SUBMIT_REGISTER_CLIENT))
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
        .requestMatchers("/application/proceedings/add/**")
        .hasAuthority(UserRole.ADD_PROCEEDING.getCode())
        .requestMatchers("/application/proceedings/*/remove")
        .hasAuthority(UserRole.DELETE_PROCEEDING.getCode())
        .requestMatchers("/application/proceedings/*/summary", "/application/proceedings/edit/**")
        .hasAuthority(UserRole.VIEW_PROCEEDING.getCode())
        // Scope limitations are edited inside both the add and the edit proceeding flows, which
        // share these URLs.
        .requestMatchers("/application/proceedings/scope-limitations/**")
        .access(anyFunction(UserRole.ADD_PROCEEDING, UserRole.VIEW_PROCEEDING))
        .requestMatchers(HttpMethod.POST, "/application/sections/client/details/summary")
        .hasAuthority(UserRole.SUBMIT_UPDATE_CLIENT.getCode())
        .requestMatchers(HttpMethod.POST, "/application/sections")
        .hasAuthority(UserRole.SUBMIT_APPLICATION.getCode())
        .requestMatchers(
            "/application/validate", "/application/submit/summary", "/application/declaration")
        .hasAuthority(UserRole.SUBMIT_APPLICATION.getCode())
        // An amendment is made to an open case, so every amendment route needs VC.
        .requestMatchers("/amendments/proceedings/add/**")
        .access(caseAction(UserRole.ADD_PROCEEDING))
        .requestMatchers("/amendments/proceedings/*/remove")
        .access(caseAction(UserRole.DELETE_PROCEEDING))
        .requestMatchers("/amendments/proceedings/*/summary", "/amendments/proceedings/edit/**")
        .access(caseAction(UserRole.VIEW_PROCEEDING))
        .requestMatchers("/amendments/proceedings/scope-limitations/**")
        .access(
            AuthorizationManagers.allOf(
                caseAction(), anyFunction(UserRole.ADD_PROCEEDING, UserRole.VIEW_PROCEEDING)))
        // Submitting an amended client is the end of the amend client flow, which needs CD.
        .requestMatchers(HttpMethod.POST, "/amendments/sections/client/details/summary")
        .access(caseAction(UserRole.VIEW_CLIENT_DETAILS, UserRole.SUBMIT_UPDATE_CLIENT))
        .requestMatchers(HttpMethod.POST, "/amendments/summary")
        .access(caseAction(UserRole.SUBMIT_AMENDMENT))
        .requestMatchers("/amendments/validate", "/amendments/submit/summary")
        .access(caseAction(UserRole.SUBMIT_AMENDMENT))
        .requestMatchers(
            "/amendments/new",
            "/amendments/application-type",
            "/amendments/delegated-functions",
            "/amendments/create",
            "/amendments/edit-delegated-functions",
            "/amendments/summary")
        .access(caseAction(UserRole.AMEND_CASE))
        .requestMatchers("/amendments/sections/client/details/**", "/amendments/client-update/**")
        .access(caseAction(UserRole.VIEW_CLIENT_DETAILS))
        // The cost limit and means reassessment quick edits are also made to an open case.
        .requestMatchers(
            "/amendments/**",
            "/allocate-cost-limit",
            "/allocate-cost-limit/**",
            "/means-reassessment",
            "/means-reassessment/**")
        .access(caseAction())
        // Every notification page is reached by opening the notification, which needs NOT.
        .requestMatchers(HttpMethod.POST, "/notifications/*/attachments/*/remove")
        .access(notificationAction(UserRole.UPLOAD_EVIDENCE))
        .requestMatchers("/notifications/*/attachments/*/remove")
        .denyAll()
        .requestMatchers(
            "/notifications/*/attachments/*/retrieve", "/notifications/*/attachments/*/download")
        .access(notificationAction(UserRole.VIEW_NOTIFICATION_ATTACHMENT))
        // Submitting documents is the end of the provide documents flow, which needs EVID.
        .requestMatchers(HttpMethod.POST, "/notifications/*/provide-documents-or-evidence")
        .access(notificationAction(UserRole.UPLOAD_EVIDENCE, UserRole.SUBMIT_DOCUMENT_UPLOAD))
        .requestMatchers(
            "/notifications/*/provide-documents-or-evidence",
            "/notifications/*/attachments/**",
            "/application/notification-attachments/confirmed",
            "/application/notification-attachments/failed")
        .access(notificationAction(UserRole.UPLOAD_EVIDENCE))
        // Searching a case's notifications is an action on the open case.
        .requestMatchers("/notifications/case-search")
        .access(notificationAction(UserRole.VIEW_CASE_DETAILS))
        .requestMatchers(HttpMethod.POST, "/notifications/search")
        .access(notificationAction())
        .requestMatchers(HttpMethod.POST, "/notifications/*")
        .access(notificationAction(UserRole.SUBMIT_NOTIFICATION))
        .requestMatchers("/notifications", "/notifications/**")
        .access(notificationAction())
        // Opening a case puts it in the session: a draft goes to its sections, a submitted case
        // to the case overview. Users reach it from case search or a notification's case link.
        .requestMatchers("/application/*/view")
        .access(anyFunction(UserRole.VIEW_CASES_AND_APPLICATIONS, UserRole.VIEW_CASE_DETAILS))
        // Every case page is reached by opening the case, which the legacy PUI checks, so each
        // case action needs VC as well as its own function.
        .requestMatchers("/case/details/proceeding/*")
        .access(caseAction(UserRole.VIEW_PROCEEDING))
        // Billing actions are reached from the case's billing page, which needs CB, and a
        // payment on account or bill is submitted from inside the flow that creates it.
        .requestMatchers(
            "/case/billing/poa/submit",
            "/case/billing/poa/declaration",
            "/case/billing/poa/confirmation")
        .access(
            caseAction(
                UserRole.VIEW_CASE_BILL,
                UserRole.CREATE_PAYMENT_ON_ACCOUNT,
                UserRole.SUBMIT_PAYMENT_ON_ACCOUNT))
        // Creating and deleting a payment on account both need the POA function, as
        // they do in the legacy PUI.
        .requestMatchers("/case/billing/poa", "/case/billing/poa/**")
        .access(caseAction(UserRole.VIEW_CASE_BILL, UserRole.CREATE_PAYMENT_ON_ACCOUNT))
        // Submitting and deleting a bill are separate permissions from creating one in
        // the legacy PUI, which gates them on the subbill and delete-bill functions.
        // The bill rules are exact paths, not a prefix, so every route that submits
        // a bill has to be listed - "/case/billing/bill" alone does not cover them.
        .requestMatchers(
            "/case/billing/bill/submit",
            "/case/billing/bill/declaration",
            "/case/billing/bill/confirmation")
        .access(caseAction(UserRole.VIEW_CASE_BILL, UserRole.CREATE_BILL, UserRole.SUBMIT_BILL))
        .requestMatchers("/case/billing/bill/remove")
        .access(caseAction(UserRole.VIEW_CASE_BILL, UserRole.DELETE_BILL))
        // Copying creates a bill, and the summary reports on the one being created, so
        // both take the create permission.
        .requestMatchers("/case/billing/bill/copy", "/case/billing/bill/summary")
        .access(caseAction(UserRole.VIEW_CASE_BILL, UserRole.CREATE_BILL))
        .requestMatchers("/case/billing/bill")
        .access(caseAction(UserRole.VIEW_CASE_BILL, UserRole.CREATE_BILL))
        .requestMatchers("/case/billing/undertaking")
        .access(caseAction(UserRole.VIEW_CASE_BILL, UserRole.ENTER_UNDERTAKING))
        .requestMatchers("/case/billing")
        .access(caseAction(UserRole.VIEW_CASE_BILL))
        .requestMatchers("/case/outcome-and-awards/submit")
        .access(outcomeAction(UserRole.SUBMIT_CASE_OUTCOME))
        // This discharge page is part of the submission journey; legacy additionally
        // requires POD, case eligibility and completed proceeding outcomes.
        .requestMatchers("/case/outcome-and-awards/discharge")
        .access(caseAction(UserRole.SUBMIT_CASE_OUTCOME, UserRole.REQUEST_CASE_DISCHARGE))
        .requestMatchers("/case/outcome-and-awards/proceeding/*/outcome/clear")
        .access(outcomeAction(UserRole.CLEAR_OUTCOME))
        .requestMatchers("/case/outcome-and-awards/proceeding/*/outcome/**")
        .access(outcomeAction(UserRole.UPDATE_PROCEEDING_OUTCOME))
        .requestMatchers("/case/outcome-and-awards", "/case/outcome-and-awards/**")
        .access(outcomeAction())
        .requestMatchers("/case/**")
        .access(caseAction());
  }

  /**
   * Explicitly allows routes intended for any signed-in user (Home, provider switch, shared
   * court/counsel pickers and session-bound draft application pages) before the final {@code
   * denyAll()} catch-all.
   */
  static void authorizeAuthenticatedRoutes(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
          authorize) {
    authorize.requestMatchers(SecurityConfiguration::isContextRootGetOrHead).authenticated();
    allowGetAndHead(
        authorize,
        "/",
        "/home",
        "/provider-switch",
        "/submissions/alreadySubmitted",
        "/counsel/results",
        "/counsel/search",
        "/counsel/select",
        "/court/results",
        "/court/search",
        "/court/select",
        "/application/counsel/confirm",
        "/application/submit-case",
        "/application/submit-case/confirmed",
        "/application/submit-case/failed",
        "/application/submit-case/undertaking",
        "/application/submit-case/undertaking/confirmed",
        "/application/submit-case/undertaking/failed",
        "/application/client-update",
        "/application/client-update/confirmed",
        "/application/client-update/failed",
        "/application/abandon",
        "/application/assessments",
        "/application/assessments/confirm",
        "/application/assessments/confirm/caseSummary.do",
        "/application/assessments/*/remove",
        "/application/case-costs",
        "/application/evidence/add",
        "/application/evidence/*/remove",
        "/application/evidence/*/view",
        "/application/prior-authorities/add/details",
        "/application/prior-authorities/add/type",
        "/application/prior-authorities/edit/details",
        "/application/prior-authorities/*/confirm",
        "/application/prior-authorities/*/remove",
        "/application/proceedings-and-costs",
        "/application/proceedings/*/make-lead",
        "/application/sections",
        "/application/sections/application-type",
        "/application/sections/client/details/address",
        "/application/sections/client/details/address/search",
        "/application/sections/client/details/basic",
        "/application/sections/client/details/contact",
        "/application/sections/client/details/deceased",
        "/application/sections/client/details/equal-opportunities-monitoring",
        "/application/sections/client/details/summary",
        "/application/sections/correspondence-address",
        "/application/sections/correspondence-address/search",
        "/application/sections/evidence",
        "/application/sections/linked-cases",
        "/application/sections/linked-cases/search",
        "/application/sections/linked-cases/search/results",
        "/application/sections/linked-cases/*/add",
        "/application/sections/linked-cases/*/confirm",
        "/application/sections/linked-cases/*/remove",
        "/application/sections/opponents",
        "/application/sections/opponents/individual/create",
        "/application/sections/opponents/organisation/create",
        "/application/sections/opponents/organisation/search",
        "/application/sections/opponents/organisation/search/results",
        "/application/sections/opponents/organisation/*/select",
        "/application/sections/opponents/*/edit",
        "/application/sections/opponents/*/remove",
        "/application/sections/provider-details",
        "/application/sections/summary",
        "/application/summary/print");
    authorize
        .requestMatchers(
            HttpMethod.POST,
            "/provider-switch",
            "/counsel/search",
            "/court/search",
            "/application/counsel/confirm",
            "/application/submit-case/confirmed",
            "/application/submit-case/failed",
            "/application/submit-case/undertaking/confirmed",
            "/application/submit-case/undertaking/failed",
            "/application/client-update/confirmed",
            "/application/client-update/failed",
            "/application/abandon/confirmed",
            "/application/assessments/*/remove",
            "/application/case-costs",
            "/application/evidence/add",
            "/application/prior-authorities/add/details",
            "/application/prior-authorities/add/type",
            "/application/prior-authorities/edit/details",
            "/application/prior-authorities/edit/type",
            "/application/prior-authorities/*/remove",
            "/application/sections/application-type",
            "/application/sections/client/details/address",
            "/application/sections/client/details/address/search",
            "/application/sections/client/details/basic",
            "/application/sections/client/details/contact",
            "/application/sections/client/details/deceased",
            "/application/sections/client/details/equal-opportunities-monitoring",
            "/application/sections/correspondence-address",
            "/application/sections/correspondence-address/search",
            "/application/sections/linked-cases/add",
            "/application/sections/linked-cases/search",
            "/application/sections/linked-cases/*/confirm",
            "/application/sections/linked-cases/*/remove",
            "/application/sections/opponents/individual/create",
            "/application/sections/opponents/organisation/create",
            "/application/sections/opponents/organisation/search",
            "/application/sections/opponents/organisation/shared/create",
            "/application/sections/opponents/*/edit",
            "/application/sections/opponents/*/remove",
            "/application/sections/provider-details")
        .authenticated();
  }

  private static boolean isContextRootGetOrHead(HttpServletRequest request) {
    return (HttpMethod.GET.matches(request.getMethod())
            || HttpMethod.HEAD.matches(request.getMethod()))
        && request.getServletPath().isEmpty()
        && request.getPathInfo() == null;
  }

  private static void allowGetAndHead(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
          authorize,
      String... patterns) {
    authorize.requestMatchers(HttpMethod.GET, patterns).authenticated();
    authorize.requestMatchers(HttpMethod.HEAD, patterns).authenticated();
  }

  /** Requires every one of the given functions. */
  private static AuthorizationManager<RequestAuthorizationContext> allFunctions(UserRole... roles) {
    return AllAuthoritiesAuthorizationManager.hasAllAuthorities(
        Arrays.stream(roles).map(UserRole::getCode).toList());
  }

  /** Requires at least one of the given functions. */
  private static AuthorizationManager<RequestAuthorizationContext> anyFunction(UserRole... roles) {
    return AuthorityAuthorizationManager.hasAnyAuthority(
        Arrays.stream(roles).map(UserRole::getCode).toArray(String[]::new));
  }

  /** Requires VC, to be in a case, and every one of the given functions. */
  private static AuthorizationManager<RequestAuthorizationContext> caseAction(UserRole... roles) {
    return allFunctions(withFunction(UserRole.VIEW_CASE_DETAILS, roles));
  }

  /** Requires NOT, to be in a notification, and every one of the given functions. */
  private static AuthorizationManager<RequestAuthorizationContext> notificationAction(
      UserRole... roles) {
    return allFunctions(withFunction(UserRole.VIEW_NOTIFICATIONS, roles));
  }

  /**
   * Requires a case action that is part of recording an outcome, which the legacy PUI allows with
   * either outcome function.
   */
  private static AuthorizationManager<RequestAuthorizationContext> outcomeAction(
      UserRole... roles) {
    return AuthorizationManagers.allOf(
        caseAction(roles), anyFunction(UserRole.RECORD_OUTCOME, UserRole.REQUEST_CASE_DISCHARGE));
  }

  private static UserRole[] withFunction(UserRole first, UserRole... rest) {
    return Stream.concat(Stream.of(first), Arrays.stream(rest)).toArray(UserRole[]::new);
  }

  /**
   * Creates a custom {@link OAuth2UserService} for processing the OIDC ID token / userinfo response
   * returned by EntraID.
   *
   * <p>EntraID authenticates a user as an email address, which is not a CCMS username, so the
   * identity it returns is first resolved to the CCMS user behind it. Everything the application
   * does afterwards - the functions that gate each route, the user details on the model and
   * session, every EBS and SOA call made on the user's behalf - is done as that CCMS user.
   *
   * @return An OAuth2UserService for loading an authenticated OidcUser.
   */
  @Bean
  OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
    OidcUserService delegate = new OidcUserService();
    return userRequest -> {
      OidcUser oidcUser = delegate.loadUser(userRequest);

      String ccmsLoginId = ccmsUserIdentityResolver.resolveCcmsLoginId(oidcUser);

      List<String> groups = oidcUser.getClaimAsStringList("groups");
      Set<GrantedAuthority> authorities = new HashSet<>();
      if (groups != null) {
        groups.stream().map(SimpleGrantedAuthority::new).forEach(authorities::add);
      } else {
        authorities.addAll(oidcUser.getAuthorities());
      }

      Collection<? extends GrantedAuthority> userFunctions = getUserFunctions(ccmsLoginId);
      authorities.addAll(userFunctions);
      securityEventLogger.permissionsLoaded(
          ccmsLoginId, userFunctions.stream().map(GrantedAuthority::getAuthority).toList());

      return new CcmsOidcUser(oidcUser, ccmsLoginId, authorities);
    };
  }

  /**
   * Sends a user EntraID authenticated but CCMS would not admit to the authentication error page,
   * leaving them unauthenticated.
   *
   * @return An AuthenticationFailureHandler that reports the failure and denies access.
   */
  @Bean
  AuthenticationFailureHandler authenticationFailureHandler() {
    return (request, response, exception) -> {
      log.error("EntraID authentication failed: {}", exception.getClass().getSimpleName());
      securityEventLogger.authenticationFailed(exception);
      response.sendRedirect(request.getContextPath() + AUTHENTICATION_ERROR_PATH);
    };
  }

  /**
   * The functions the CCMS user holds, which gate what they can reach in the application.
   *
   * @param loginId the CCMS username the EntraID identity resolved to.
   * @return the user's functions as granted authorities.
   */
  private Collection<? extends GrantedAuthority> getUserFunctions(String loginId) {
    final Optional<UserDetail> user;
    try {
      user = userService.getUserByLoginId(loginId).blockOptional();
    } catch (RuntimeException e) {
      // Spring's login filter only acts on an AuthenticationException.
      throw new CcmsUserResolutionException(
          "Unable to reach EBS to retrieve CCMS user [%s] - access denied".formatted(loginId), e);
    }

    List<String> functions =
        user.map(details -> Optional.ofNullable(details.getFunctions()).orElseGet(List::of))
            .orElseThrow(
                () ->
                    new CcmsUserResolutionException(
                        "Unable to retrieve CCMS user [%s] from EBS - access denied"
                            .formatted(loginId)));

    return functions.stream().map(SimpleGrantedAuthority::new).toList();
  }
}
