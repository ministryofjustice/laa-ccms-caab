package uk.gov.laa.ccms.caab.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authorization.AllAuthoritiesAuthorizationManager;
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
        // A case query is raised from an open case.
        .requestMatchers(
            "/case-provider-requests/**", "/application/submit-case-provider-request/confirmed")
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
        .requestMatchers("/notifications/*/attachments/*/retrieve")
        .access(notificationAction(UserRole.VIEW_NOTIFICATION_ATTACHMENT))
        // Submitting documents is the end of the provide documents flow, which needs EVID.
        .requestMatchers(HttpMethod.POST, "/notifications/*/provide-documents-or-evidence")
        .access(notificationAction(UserRole.UPLOAD_EVIDENCE, UserRole.SUBMIT_DOCUMENT_UPLOAD))
        .requestMatchers(
            "/notifications/*/provide-documents-or-evidence",
            "/notifications/*/attachments/**",
            "/application/notification-attachments/confirmed")
        .access(notificationAction(UserRole.UPLOAD_EVIDENCE))
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
        .requestMatchers("/case/outcome-and-awards/proceeding/*/outcome/clear")
        .access(outcomeAction(UserRole.CLEAR_OUTCOME))
        .requestMatchers("/case/outcome-and-awards/proceeding/*/outcome/**")
        .access(outcomeAction(UserRole.UPDATE_PROCEEDING_OUTCOME))
        .requestMatchers("/case/outcome-and-awards", "/case/outcome-and-awards/**")
        .access(outcomeAction())
        .requestMatchers("/case/**")
        .access(caseAction());
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
