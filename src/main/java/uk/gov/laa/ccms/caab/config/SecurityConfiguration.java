package uk.gov.laa.ccms.caab.config;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.session.SimpleRedirectInvalidSessionStrategy;
import uk.gov.laa.ccms.caab.security.CcmsOidcUser;
import uk.gov.laa.ccms.caab.security.CcmsUserIdentityResolver;
import uk.gov.laa.ccms.caab.security.CcmsUserResolutionException;
import uk.gov.laa.ccms.caab.security.CspNonceFilter;
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
            authorize ->
                authorize
                    .requestMatchers(
                        "/assets/**", "/ccms/**", "/favicon.ico", AUTHENTICATION_ERROR_PATH)
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/actuator/prometheus",
                        "/actuator/health/**",
                        "/actuator/info",
                        "/actuator/metrics")
                    .permitAll() // Ensure Actuator endpoints are excluded
                    .requestMatchers(HttpMethod.POST, "/csp/report")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET, "/general-provider-requests/*", "/case-provider-requests/*")
                    .hasAuthority(UserRole.CREATE_PROVIDER_REQUEST.getCode())
                    .requestMatchers(
                        HttpMethod.GET,
                        "/application/office",
                        "/application/category-of-law",
                        "/application/application-type",
                        "/application/delegated-functions",
                        "/application/copy-case/search",
                        "/application/client/search",
                        "/application/client/*/confirm")
                    .hasAuthority(UserRole.CREATE_APPLICATION.getCode())
                    .requestMatchers(
                        HttpMethod.GET, "/application/search", "/application/search/results")
                    .hasAuthority(UserRole.VIEW_CASES_AND_APPLICATIONS.getCode())
                    .requestMatchers(
                        HttpMethod.GET,
                        "/notifications/search",
                        "/notifications/search-results",
                        "/notifications/search-options/prefetch")
                    .hasAuthority(UserRole.VIEW_NOTIFICATIONS.getCode())
                    .requestMatchers(HttpMethod.GET, "/application/proceedings/add/matter-type")
                    .hasAuthority(UserRole.ADD_PROCEEDING.getCode())
                    .requestMatchers("/notifications/*/attachments/*")
                    .hasAuthority(UserRole.VIEW_NOTIFICATION_ATTACHMENT.getCode())
                    .requestMatchers("/application/proceedings/*/remove")
                    .hasAuthority(UserRole.DELETE_PROCEEDING.getCode())
                    .requestMatchers("/application/proceedings/*/summary")
                    .hasAuthority(UserRole.VIEW_PROCEEDING.getCode())
                    .requestMatchers(
                        HttpMethod.GET, "/notifications/*/provide-documents-or-evidence")
                    .hasAuthority(UserRole.UPLOAD_EVIDENCE.getCode())
                    .requestMatchers(
                        HttpMethod.POST, "/notifications/*/provide-documents-or-evidence")
                    .hasAuthority(UserRole.SUBMIT_DOCUMENT_UPLOAD.getCode())
                    .requestMatchers(
                        HttpMethod.POST, "/application/sections/client/details/summary")
                    .hasAuthority(UserRole.SUBMIT_UPDATE_CLIENT.getCode())
                    .requestMatchers(HttpMethod.POST, "/application/sections")
                    .hasAuthority(UserRole.SUBMIT_APPLICATION.getCode())
                    .requestMatchers("/case/overview")
                    .hasAuthority(UserRole.VIEW_CASE_DETAILS.getCode())
                    .requestMatchers(HttpMethod.GET, "/case/details/costs/allocation")
                    .hasAuthority(UserRole.VIEW_CASE_DETAILS.getCode())
                    // Creating and deleting a payment on account both need the POA function, as
                    // they do in the legacy PUI.
                    .requestMatchers("/case/billing/poa", "/case/billing/poa/**")
                    .hasAuthority(UserRole.CREATE_PAYMENT_ON_ACCOUNT.getCode())
                    // Submitting and deleting a bill are separate permissions from creating one in
                    // the legacy PUI, which gates them on the subbill and delete-bill functions.
                    // The bill rules are exact paths, not a prefix, so every route that submits
                    // a bill has to be listed - "/case/billing/bill" alone does not cover them.
                    .requestMatchers(
                        "/case/billing/bill/submit",
                        "/case/billing/bill/declaration",
                        "/case/billing/bill/confirmation")
                    .hasAuthority(UserRole.SUBMIT_BILL.getCode())
                    .requestMatchers("/case/billing/bill/remove")
                    .hasAuthority(UserRole.DELETE_BILL.getCode())
                    // Copying creates a bill, and the summary reports on the one being created, so
                    // both take the create permission.
                    .requestMatchers("/case/billing/bill/copy", "/case/billing/bill/summary")
                    .hasAuthority(UserRole.CREATE_BILL.getCode())
                    .requestMatchers("/case/billing/bill")
                    .hasAuthority(UserRole.CREATE_BILL.getCode())
                    .anyRequest()
                    .authenticated())
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
        .oauth2Login(
            oauth2 ->
                oauth2
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService()))
                    .failureHandler(authenticationFailureHandler()))
        .build();
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

      authorities.addAll(getUserFunctions(ccmsLoginId));

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
      log.error("EntraID authentication failed: {}", exception.getMessage(), exception);
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
