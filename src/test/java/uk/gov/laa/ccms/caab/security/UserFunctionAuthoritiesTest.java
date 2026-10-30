package uk.gov.laa.ccms.caab.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import uk.gov.laa.ccms.data.model.UserDetail;

@DisplayName("User function authorities")
class UserFunctionAuthoritiesTest {

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  private static Saml2AssertionAuthentication authentication(String... authorities) {
    return new Saml2AssertionAuthentication(
        "user",
        mock(Saml2ResponseAssertionAccessor.class),
        Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList(),
        "idp");
  }

  private static List<String> currentAuthorities() {
    return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList();
  }

  @Test
  @DisplayName("The reloaded functions replace the previous ones, keeping the SAML groups")
  void replacesFunctionsAndKeepsGroups() {
    MockHttpSession session = new MockHttpSession();

    UserFunctionAuthorities.replace(
        authentication("group1", "CA", "NOT"),
        new UserDetail().functions(List.of("CA", "NOT")),
        new UserDetail().functions(List.of("NOT", "VC")),
        session);

    assertThat(currentAuthorities()).containsExactlyInAnyOrder("group1", "NOT", "VC");
    assertThat(
            (SecurityContext)
                session.getAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
        .isSameAs(SecurityContextHolder.getContext());
  }

  @Test
  @DisplayName("A function revoked before the previous user was loaded is still removed")
  void removesFunctionMissingFromPreviousUser() {
    UserFunctionAuthorities.replace(
        authentication("group1", "CA", "NOT"),
        new UserDetail().functions(List.of("NOT")),
        new UserDetail().functions(List.of("NOT")),
        new MockHttpSession());

    assertThat(currentAuthorities()).containsExactlyInAnyOrder("group1", "NOT");
  }

  @Test
  @DisplayName("A login that is not SAML is left alone")
  void ignoresOtherAuthentication() {
    UserFunctionAuthorities.replace(
        null, new UserDetail(), new UserDetail().functions(List.of("CA")), new MockHttpSession());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
