package uk.gov.laa.ccms.caab.config;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider.ResponseToken;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration class for customizing Spring Security settings.
 */
@Configuration
public class SecurityConfiguration {

  /**
   * Configures Spring Security filters and settings.
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
                    .anyRequest().authenticated())
            .csrf(AbstractHttpConfigurer::disable)
            .saml2Login(saml2 -> saml2
                    .authenticationManager(new ProviderManager(authenticationProvider)))
            .saml2Logout(withDefaults())
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
    return (responseToken) -> {
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
      return new Saml2Authentication(principal, authentication.getSaml2Response(), authorities);
    };
  }
}
