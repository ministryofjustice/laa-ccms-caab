package uk.gov.laa.ccms.caab.security;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import uk.gov.laa.ccms.caab.config.EntraProperties;
import uk.gov.laa.ccms.caab.logging.UserIdentifier;
import uk.gov.laa.ccms.caab.model.user.CcmsUserDetails;
import uk.gov.laa.ccms.caab.model.user.EntraUserMapping;
import uk.gov.laa.ccms.caab.model.user.UserViewModel;
import uk.gov.laa.ccms.caab.service.UserService;

/**
 * Resolves the CCMS username of a user EntraID has authenticated.
 *
 * <p>EntraID identifies a user by their organisational email address, which is not a CCMS username,
 * so a user who has signed in successfully is still not a user this application knows anything
 * about. Two lookups close that gap, and which one applies depends on the tenant:
 *
 * <ul>
 *   <li>Internal (caseworker) users are looked up by email in EBS, against the {@code
 *       XXCCMS_ENTRA_ID_USERS} mapping table.
 *   <li>External (provider) users authenticate through SiLAS, which puts its own identifier on the
 *       token as a custom claim. That identifier is exchanged for a CCMS username through the User
 *       Details API.
 * </ul>
 *
 * <p>This mirrors {@code RetrieveUserInfoFromPortal} in the legacy PUI, which makes the same two
 * lookups off the same two settings.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CcmsUserIdentityResolver {

  private final EntraProperties entraProperties;
  private final UserService userService;

  /**
   * Resolves the CCMS username of an authenticated EntraID user.
   *
   * @param oidcUser the user as EntraID authenticated them.
   * @return the CCMS username.
   * @throws CcmsUserResolutionException if the identity does not map to a CCMS user.
   */
  public String resolveCcmsLoginId(final OidcUser oidcUser) {
    return entraProperties.silasEnabled()
        ? externalLoginId(oidcUser)
        : internalLoginId(entraIdentity(oidcUser));
  }

  /**
   * The identity EntraID authenticated, as an email address.
   *
   * <p>EntraID only issues the {@code email} claim when the app registration asks for it as an
   * optional claim. {@code preferred_username} carries the user principal name, which for an
   * organisational account is the same address, so it stands in when {@code email} is absent.
   */
  private String entraIdentity(final OidcUser oidcUser) {
    final String email =
        Optional.ofNullable(oidcUser.getEmail())
            .filter(value -> !value.isBlank())
            .orElseGet(oidcUser::getPreferredUsername);

    if (email == null || email.isBlank()) {
      throw new CcmsUserResolutionException(
          "EntraID returned no email address or user principal name for the authenticated user - "
              + "access denied");
    }
    return email;
  }

  /** Looks an internal user up by email in the EBS mapping table. */
  private String internalLoginId(final String entraEmail) {
    final String loginId =
        lookup(
                () -> userService.getEntraUserMapping(entraEmail).blockOptional(),
                "the EBS XXCCMS_ENTRA_ID_USERS mapping table")
            .map(EntraUserMapping::getCcmsLoginId)
            .filter(value -> !value.isBlank())
            .orElseThrow(
                () ->
                    new CcmsUserResolutionException(
                        ("Unable to retrieve a CCMS user for EntraID identity [%s] from the "
                                + "XXCCMS_ENTRA_ID_USERS mapping table - access denied")
                            .formatted(entraEmail)));

    log.info(
        "Resolved EntraID identity to CCMS user [{}] from the EBS mapping table",
        UserIdentifier.pseudonymise(loginId));
    return loginId;
  }

  /**
   * Resolves an external user by exchanging the SiLAS identifier on the token for a CCMS username.
   *
   * <p>The claim carries a SiLAS uuid, not a username, so the User Details API is always called.
   * Without the claim there is nothing to exchange: the authenticated email address is not a SiLAS
   * identifier and the API has no way to match it.
   */
  private String externalLoginId(final OidcUser oidcUser) {
    final String claimName = entraProperties.customUserIdClaim();

    final String silasId =
        Optional.ofNullable(claimName)
            .filter(name -> !name.isBlank())
            .flatMap(name -> claimValue(oidcUser, name))
            .orElseThrow(
                () ->
                    new CcmsUserResolutionException(
                        ("EntraID issued no SiLAS identifier in claim [%s] for [%s] - access denied")
                            .formatted(claimName, entraIdentity(oidcUser))));

    return userDetailsApiLoginId(silasId);
  }

  /** Exchanges a SiLAS identifier for a CCMS username through the User Details API. */
  private String userDetailsApiLoginId(final String silasId) {
    final String loginId =
        lookup(() -> userService.getUserBySilasId(silasId).blockOptional(), "the User Details API")
            .map(UserViewModel::getCcmsUserDetails)
            .map(CcmsUserDetails::getUserLoginId)
            .filter(value -> !value.isBlank())
            .orElseThrow(
                () ->
                    new CcmsUserResolutionException(
                        ("Unable to retrieve a CCMS user for SiLAS identity [%s] from the "
                                + "User Details API - access denied")
                            .formatted(silasId)));

    log.info(
        "Resolved SiLAS identity to CCMS user [{}] from the User Details API",
        UserIdentifier.pseudonymise(loginId));
    return loginId;
  }

  /**
   * Runs a lookup, turning a failure to reach the service into a failed authentication.
   *
   * <p>Spring's login filter only acts on an {@code AuthenticationException}.
   */
  private <T> Optional<T> lookup(final Supplier<Optional<T>> lookup, final String source) {
    try {
      return lookup.get();
    } catch (final RuntimeException e) {
      throw new CcmsUserResolutionException(
          "Unable to reach %s to resolve the authenticated user - access denied".formatted(source),
          e);
    }
  }

  /**
   * Reads a claim as a single string.
   *
   * <p>A claim can arrive as a scalar or as a single-valued array depending on how the tenant
   * issues it, so both are accepted.
   */
  private Optional<String> claimValue(final OidcUser oidcUser, final String claimName) {
    final Object claim = oidcUser.getClaims().get(claimName);

    final Object value =
        claim instanceof Collection<?> values ? values.stream().findFirst().orElse(null) : claim;

    return Optional.ofNullable(value)
        .map(String::valueOf)
        .map(String::trim)
        .filter(trimmed -> !trimmed.isEmpty());
  }
}
