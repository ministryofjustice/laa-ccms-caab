package uk.gov.laa.ccms.caab.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.client.EbsApiClientException;
import uk.gov.laa.ccms.caab.client.UserDetailsApiClientException;
import uk.gov.laa.ccms.caab.config.EntraProperties;
import uk.gov.laa.ccms.caab.model.user.CcmsUserDetails;
import uk.gov.laa.ccms.caab.model.user.EntraUserMapping;
import uk.gov.laa.ccms.caab.model.user.UserViewModel;
import uk.gov.laa.ccms.caab.service.UserService;

@ExtendWith(MockitoExtension.class)
@DisplayName("CCMS user identity resolver test")
class CcmsUserIdentityResolverTest {

  private static final String ENTRA_EMAIL = "a.user@justice.gov.uk";
  private static final String CCMS_LOGIN_ID = "CCMSUSER";
  private static final String SILAS_CLAIM = "CCMS_USERNAME";

  @Mock private UserService userService;

  @Mock private OidcUser oidcUser;

  private static EntraUserMapping mapping(final String ccmsLoginId) {
    final EntraUserMapping mapping = new EntraUserMapping();
    mapping.setCcmsLoginId(ccmsLoginId);
    return mapping;
  }

  private static UserViewModel silasUser(final String ccmsLoginId) {
    final CcmsUserDetails details = new CcmsUserDetails();
    details.setUserLoginId(ccmsLoginId);
    final UserViewModel user = new UserViewModel();
    user.setCcmsUserDetails(details);
    return user;
  }

  private CcmsUserIdentityResolver resolver(
      final boolean silasEnabled, final String customUserIdClaim) {
    return new CcmsUserIdentityResolver(
        new EntraProperties(silasEnabled, customUserIdClaim), userService);
  }

  @Nested
  @DisplayName("Internal users")
  class InternalUsers {

    @BeforeEach
    void setUp() {
      lenient().when(oidcUser.getEmail()).thenReturn(ENTRA_EMAIL);
    }

    @Test
    @DisplayName("Resolves the CCMS user from the EBS mapping table")
    void resolvesFromMappingTable() {
      when(userService.getEntraUserMapping(ENTRA_EMAIL))
          .thenReturn(Mono.just(mapping(CCMS_LOGIN_ID)));

      assertEquals(CCMS_LOGIN_ID, resolver(false, null).resolveCcmsLoginId(oidcUser));
    }

    @Test
    @DisplayName("Does not call the User Details API")
    void doesNotCallUserDetailsApi() {
      when(userService.getEntraUserMapping(ENTRA_EMAIL))
          .thenReturn(Mono.just(mapping(CCMS_LOGIN_ID)));

      resolver(false, SILAS_CLAIM).resolveCcmsLoginId(oidcUser);

      verify(userService).getEntraUserMapping(ENTRA_EMAIL);
    }

    @Test
    @DisplayName("Fails when the email address is not mapped")
    void failsWhenUnmapped() {
      when(userService.getEntraUserMapping(ENTRA_EMAIL)).thenReturn(Mono.empty());

      final CcmsUserResolutionException exception =
          assertThrows(
              CcmsUserResolutionException.class,
              () -> resolver(false, null).resolveCcmsLoginId(oidcUser));

      assertTrue(exception.getMessage().contains("XXCCMS_ENTRA_ID_USERS"));
      assertTrue(exception.getMessage().contains(ENTRA_EMAIL));
    }

    @Test
    @DisplayName("Fails authentication, rather than erroring, when EBS cannot be reached")
    void failsWhenEbsUnreachable() {
      when(userService.getEntraUserMapping(ENTRA_EMAIL))
          .thenThrow(new EbsApiClientException("connection refused"));

      final CcmsUserResolutionException exception =
          assertThrows(
              CcmsUserResolutionException.class,
              () -> resolver(false, null).resolveCcmsLoginId(oidcUser));

      assertTrue(exception.getMessage().contains("XXCCMS_ENTRA_ID_USERS"));
    }

    @Test
    @DisplayName("Fails authentication when the EBS call errors")
    void failsWhenEbsCallErrors() {
      when(userService.getEntraUserMapping(ENTRA_EMAIL))
          .thenReturn(Mono.error(new EbsApiClientException("boom")));

      assertThrows(
          CcmsUserResolutionException.class,
          () -> resolver(false, null).resolveCcmsLoginId(oidcUser));
    }

    @Test
    @DisplayName("Fails when the mapping holds a blank CCMS username")
    void failsWhenMappingBlank() {
      when(userService.getEntraUserMapping(ENTRA_EMAIL)).thenReturn(Mono.just(mapping("   ")));

      assertThrows(
          CcmsUserResolutionException.class,
          () -> resolver(false, null).resolveCcmsLoginId(oidcUser));
    }
  }

  @Nested
  @DisplayName("External SiLAS users")
  class ExternalUsers {

    @Test
    @DisplayName("Exchanges the custom claim for a CCMS username")
    void resolvesFromCustomClaim() {
      when(oidcUser.getClaims()).thenReturn(Map.of(SILAS_CLAIM, "silas-uuid"));
      when(oidcUser.getEmail()).thenReturn(ENTRA_EMAIL);
      when(userService.getUserBySilasId("silas-uuid"))
          .thenReturn(Mono.just(silasUser(CCMS_LOGIN_ID)));

      assertEquals(CCMS_LOGIN_ID, resolver(true, SILAS_CLAIM).resolveCcmsLoginId(oidcUser));
    }

    @Test
    @DisplayName("Reads a claim issued as a single valued list")
    void resolvesFromListValuedClaim() {
      when(oidcUser.getClaims()).thenReturn(Map.of(SILAS_CLAIM, List.of("silas-uuid")));
      when(oidcUser.getEmail()).thenReturn(ENTRA_EMAIL);
      when(userService.getUserBySilasId("silas-uuid"))
          .thenReturn(Mono.just(silasUser(CCMS_LOGIN_ID)));

      assertEquals(CCMS_LOGIN_ID, resolver(true, SILAS_CLAIM).resolveCcmsLoginId(oidcUser));
    }

    @Test
    @DisplayName("Falls back to the authenticated identity when no claim is configured")
    void fallsBackWhenClaimNotConfigured() {
      when(oidcUser.getEmail()).thenReturn(ENTRA_EMAIL);
      when(userService.getUserBySilasId(ENTRA_EMAIL))
          .thenReturn(Mono.just(silasUser(CCMS_LOGIN_ID)));

      assertEquals(CCMS_LOGIN_ID, resolver(true, "  ").resolveCcmsLoginId(oidcUser));
    }

    @Test
    @DisplayName("Falls back to the authenticated identity when the claim is absent")
    void fallsBackWhenClaimAbsent() {
      when(oidcUser.getClaims()).thenReturn(Map.of("something-else", "value"));
      when(oidcUser.getEmail()).thenReturn(ENTRA_EMAIL);
      when(userService.getUserBySilasId(ENTRA_EMAIL))
          .thenReturn(Mono.just(silasUser(CCMS_LOGIN_ID)));

      assertEquals(CCMS_LOGIN_ID, resolver(true, SILAS_CLAIM).resolveCcmsLoginId(oidcUser));
    }

    @Test
    @DisplayName("Fails when the SiLAS identity is not known")
    void failsWhenUnknown() {
      when(oidcUser.getClaims()).thenReturn(Map.of(SILAS_CLAIM, "silas-uuid"));
      when(oidcUser.getEmail()).thenReturn(ENTRA_EMAIL);
      when(userService.getUserBySilasId("silas-uuid")).thenReturn(Mono.empty());

      final CcmsUserResolutionException exception =
          assertThrows(
              CcmsUserResolutionException.class,
              () -> resolver(true, SILAS_CLAIM).resolveCcmsLoginId(oidcUser));

      assertTrue(exception.getMessage().contains("silas-uuid"));
      assertTrue(exception.getMessage().contains("User Details API"));
    }

    @Test
    @DisplayName("Fails authentication when the User Details API cannot be reached")
    void failsWhenUserDetailsApiUnreachable() {
      when(oidcUser.getClaims()).thenReturn(Map.of(SILAS_CLAIM, "silas-uuid"));
      when(oidcUser.getEmail()).thenReturn(ENTRA_EMAIL);
      when(userService.getUserBySilasId("silas-uuid"))
          .thenReturn(Mono.error(new UserDetailsApiClientException("boom")));

      final CcmsUserResolutionException exception =
          assertThrows(
              CcmsUserResolutionException.class,
              () -> resolver(true, SILAS_CLAIM).resolveCcmsLoginId(oidcUser));

      assertTrue(exception.getMessage().contains("User Details API"));
    }

    @Test
    @DisplayName("Fails when the API returns no CCMS user details")
    void failsWhenNoUserDetails() {
      when(oidcUser.getClaims()).thenReturn(Map.of(SILAS_CLAIM, "silas-uuid"));
      when(oidcUser.getEmail()).thenReturn(ENTRA_EMAIL);
      when(userService.getUserBySilasId("silas-uuid")).thenReturn(Mono.just(new UserViewModel()));

      assertThrows(
          CcmsUserResolutionException.class,
          () -> resolver(true, SILAS_CLAIM).resolveCcmsLoginId(oidcUser));
    }
  }

  @Nested
  @DisplayName("EntraID identity")
  class EntraIdentity {

    @Test
    @DisplayName("Falls back to the user principal name when no email claim is issued")
    void fallsBackToPreferredUsername() {
      when(oidcUser.getEmail()).thenReturn(null);
      when(oidcUser.getPreferredUsername()).thenReturn(ENTRA_EMAIL);
      when(userService.getEntraUserMapping(ENTRA_EMAIL))
          .thenReturn(Mono.just(mapping(CCMS_LOGIN_ID)));

      assertEquals(CCMS_LOGIN_ID, resolver(false, null).resolveCcmsLoginId(oidcUser));
    }

    @Test
    @DisplayName("Fails when EntraID identifies the user by neither")
    void failsWhenNoIdentity() {
      when(oidcUser.getEmail()).thenReturn("  ");
      when(oidcUser.getPreferredUsername()).thenReturn(null);

      assertThrows(
          CcmsUserResolutionException.class,
          () -> resolver(false, null).resolveCcmsLoginId(oidcUser));

      verifyNoInteractions(userService);
    }
  }
}
