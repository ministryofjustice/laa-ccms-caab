package uk.gov.laa.ccms.caab.config;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider.ResponseToken;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.SimpleRedirectInvalidSessionStrategy;
import uk.gov.laa.ccms.caab.service.UserService;

/**
 * Configuration class for customizing Spring Security settings.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {

  @Value("${portal.logoutUrl}")
  private String logoutUrl;

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

    OpenSaml4AuthenticationProvider authenticationProvider = new OpenSaml4AuthenticationProvider();
    authenticationProvider.setResponseAuthenticationConverter(groupsConverter());

    return http
      .authorizeHttpRequests(authorize -> authorize
          .requestMatchers(HttpMethod.GET,
              "/actuator/prometheus",
              "/actuator/health/**",
              "/actuator/info",
              "/actuator/metrics").permitAll()// Ensure Actuator endpoints are excluded
          .requestMatchers(HttpMethod.GET,
              "/provider-requests/*")
            .hasAuthority(UserRole.CREATE_PROVIDER_REQUEST.getCode())
          .requestMatchers(HttpMethod.GET,
              "/application/office",
              "/application/category-of-law",
              "/application/application-type",
              "/application/delegated-functions",
              "/application/copy-case/search",
              "/application/client/search",
              "/application/client/*/confirm")
            .hasAuthority(UserRole.CREATE_APPLICATION.getCode())
          .requestMatchers(HttpMethod.GET,
              "/application/search",
              "/application/search/results")
            .hasAuthority(UserRole.VIEW_CASES_AND_APPLICATIONS.getCode())
          .requestMatchers(HttpMethod.GET,
              "/notifications/search",
              "/notifications/search-results")
            .hasAuthority(UserRole.VIEW_NOTIFICATIONS.getCode())
          .requestMatchers(HttpMethod.GET,
              "/application/proceedings/add/matter-type")
            .hasAuthority(UserRole.ADD_PROCEEDING.getCode())
          .requestMatchers(
              "/notifications/*/attachments/*")
            .hasAuthority(UserRole.VIEW_NOTIFICATION_ATTACHMENT.getCode())
          .requestMatchers(
              "/application/proceedings/*/remove")
            .hasAuthority(UserRole.DELETE_PROCEEDING.getCode())
          .requestMatchers(
              "/application/proceedings/*/summary")
            .hasAuthority(UserRole.VIEW_PROCEEDING.getCode())
          .requestMatchers(HttpMethod.GET,
              "/notifications/*/provide-documents-or-evidence")
            .hasAuthority(UserRole.UPLOAD_EVIDENCE.getCode())
          .requestMatchers(HttpMethod.POST,
              "/notifications/*/provide-documents-or-evidence")
            .hasAuthority(UserRole.SUBMIT_DOCUMENT_UPLOAD.getCode())
          .requestMatchers(HttpMethod.POST,
              "/application/sections/client/details/summary")
            .hasAuthority(UserRole.SUBMIT_UPDATE_CLIENT.getCode())
          .requestMatchers(HttpMethod.POST,
              "/application/sections")
          .hasAuthority(UserRole.SUBMIT_APPLICATION.getCode())
          .requestMatchers("/case/overview").hasAuthority(UserRole.VIEW_CASE_DETAILS.getCode())
          .anyRequest().authenticated())
        .sessionManagement(sessionManagement -> sessionManagement
          .invalidSessionStrategy(new SimpleRedirectInvalidSessionStrategy("/home")))
      .logout(logout -> logout
          .addLogoutHandler((req, res, auth) -> {
            try {
              res.sendRedirect(logoutUrl);
            } catch (IOException e) {
              throw new RuntimeException("Failed to redirect to Identity Provider.", e);
            }
          })
      )
      .saml2Login(saml2 -> saml2
              .authenticationManager(new ProviderManager(authenticationProvider)))
      .build();
  }

  /**
   * Creates a custom converter for processing SAML response tokens.
   *
   * @return A Converter for processing SAML response tokens into authenticated principals.
   */
  private Converter<ResponseToken, Saml2Authentication> groupsConverter() {
    Converter<ResponseToken, Saml2Authentication> delegate =
            OpenSaml4AuthenticationProvider.createDefaultResponseAuthenticationConverter();
    return responseToken -> {
      Saml2Authentication authentication = delegate.convert(responseToken);
      Saml2AuthenticatedPrincipal principal =
              (Saml2AuthenticatedPrincipal) authentication.getPrincipal();
      List<String> groups = principal.getAttribute("groups");
      Set<GrantedAuthority> authorities = new HashSet<>();
      if (groups != null) {
        groups.stream().map(SimpleGrantedAuthority::new).forEach(authorities::add);
      } else {
        authorities.addAll(authentication.getAuthorities());
      }
      authorities.addAll(getUserFunctions(principal));
      return new Saml2Authentication(principal, authentication.getSaml2Response(), authorities);
    };
  }

  private Collection<? extends GrantedAuthority> getUserFunctions(
      Saml2AuthenticatedPrincipal principal) {
    return userService.getUser(principal.getName()).blockOptional()
        .orElseThrow(() -> new RuntimeException("Failed to retrieve user functions."))
        .getFunctions()
        .stream()
        .map(SimpleGrantedAuthority::new)
        .toList();
  }
}
