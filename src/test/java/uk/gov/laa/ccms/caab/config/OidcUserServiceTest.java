package uk.gov.laa.ccms.caab.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.security.CcmsOidcUser;
import uk.gov.laa.ccms.caab.security.CcmsUserIdentityResolver;
import uk.gov.laa.ccms.caab.security.CcmsUserResolutionException;
import uk.gov.laa.ccms.caab.security.SecurityEventLogger;
import uk.gov.laa.ccms.caab.service.UserService;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * The service that turns an EntraID identity into a signed-in CCMS user.
 *
 * <p>The resolver and the principal are covered on their own; this covers the step that joins them
 * - that the EBS functions are loaded for the resolved CCMS login id, and that a user EBS does not
 * return is refused rather than admitted without functions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OIDC user service test")
class OidcUserServiceTest {

  private static final String CCMS_LOGIN_ID = "A-CCMS-USER";

  @Mock private UserService userService;
  @Mock private CcmsUserIdentityResolver ccmsUserIdentityResolver;
  @Mock private SecurityEventLogger securityEventLogger;

  private OAuth2UserService<OidcUserRequest, OidcUser> service() {
    return new SecurityConfiguration(userService, ccmsUserIdentityResolver, securityEventLogger)
        .oidcUserService();
  }

  /**
   * Only the {@code openid} scope, so the delegate builds the user from the id token rather than
   * calling a userinfo endpoint this test would have to stand up.
   */
  private static OidcUserRequest request() {
    ClientRegistration registration =
        ClientRegistration.withRegistrationId("silas-identity")
            .clientId("a-client")
            .clientSecret("a-secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost/civil/login/oauth2/code/silas-identity")
            .authorizationUri("http://an-idp/authorize")
            .tokenUri("http://an-idp/token")
            .scope("openid")
            .build();
    OidcIdToken idToken =
        new OidcIdToken(
            "an-id-token",
            Instant.now(),
            Instant.now().plusSeconds(300),
            Map.of(
                IdTokenClaimNames.SUB,
                "a-subject",
                IdTokenClaimNames.ISS,
                "http://an-idp",
                "email",
                "a.user@justice.gov.uk"));
    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "an-access-token",
            Instant.now(),
            Instant.now().plusSeconds(300));
    return new OidcUserRequest(
        registration, accessToken, idToken, Map.of(OidcParameterNames.ID_TOKEN, "an-id-token"));
  }

  @Test
  @DisplayName("Loads the EBS functions of the resolved CCMS user, not of the EntraID identity")
  void loadsFunctionsForTheResolvedCcmsUser() {
    when(ccmsUserIdentityResolver.resolveCcmsLoginId(any())).thenReturn(CCMS_LOGIN_ID);
    when(userService.getUserByLoginId(CCMS_LOGIN_ID))
        .thenReturn(Mono.just(new UserDetail().functions(List.of("VC", "NOT"))));

    OidcUser loaded = service().loadUser(request());

    // The EBS lookup is keyed by the resolved login id: the mock would not have matched otherwise.
    verify(userService).getUserByLoginId(CCMS_LOGIN_ID);
    CcmsOidcUser ccmsUser = assertInstanceOf(CcmsOidcUser.class, loaded);
    // getName is the CCMS login id, so everything downstream acts as that user.
    assertEquals(CCMS_LOGIN_ID, ccmsUser.getName());
    assertTrue(
        loaded.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList()
            .containsAll(List.of("VC", "NOT")));
    verify(securityEventLogger).permissionsLoaded(CCMS_LOGIN_ID, List.of("VC", "NOT"));
  }

  @Test
  @DisplayName("Refuses a user EBS does not return rather than admitting them with no functions")
  void refusesAUserEbsDoesNotReturn() {
    when(ccmsUserIdentityResolver.resolveCcmsLoginId(any())).thenReturn(CCMS_LOGIN_ID);
    when(userService.getUserByLoginId(CCMS_LOGIN_ID)).thenReturn(Mono.empty());

    CcmsUserResolutionException refused =
        assertThrows(CcmsUserResolutionException.class, () -> service().loadUser(request()));

    assertTrue(refused.getMessage().contains("Unable to retrieve CCMS user"));
  }

  @Test
  @DisplayName("Refuses the sign-in when EBS cannot be reached")
  void refusesWhenEbsCannotBeReached() {
    when(ccmsUserIdentityResolver.resolveCcmsLoginId(any())).thenReturn(CCMS_LOGIN_ID);
    when(userService.getUserByLoginId(CCMS_LOGIN_ID))
        .thenReturn(Mono.error(new RuntimeException("EBS unavailable")));

    CcmsUserResolutionException refused =
        assertThrows(CcmsUserResolutionException.class, () -> service().loadUser(request()));

    assertTrue(refused.getMessage().contains("Unable to reach EBS"));
  }

  @Test
  @DisplayName("Refuses the sign-in when the identity cannot be resolved to a CCMS user")
  void refusesAnUnresolvableIdentity() {
    when(ccmsUserIdentityResolver.resolveCcmsLoginId(any()))
        .thenThrow(new CcmsUserResolutionException("no mapping - access denied"));

    assertThrows(CcmsUserResolutionException.class, () -> service().loadUser(request()));
  }
}
