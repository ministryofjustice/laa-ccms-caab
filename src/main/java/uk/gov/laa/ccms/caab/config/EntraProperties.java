package uk.gov.laa.ccms.caab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration controlling how an EntraID authenticated identity is mapped onto a CCMS user.
 *
 * <p>EntraID identifies a user by their organisational email address, which is not a CCMS username.
 * There are two ways to get from one to the other, and which applies depends on the tenant the
 * application is deployed against:
 *
 * <ul>
 *   <li>Internal (caseworker) users are mapped by email through the {@code XXCCMS_ENTRA_ID_USERS}
 *       table in EBS.
 *   <li>External (provider) users authenticate through SiLAS, which supplies its own identifier as
 *       a custom claim on the token. That identifier is exchanged for a CCMS username through the
 *       User Details API.
 * </ul>
 *
 * <p>The legacy PUI makes the same distinction, driven by the same two settings.
 *
 * @param silasEnabled whether users are external SiLAS identities rather than internal ones.
 * @param customUserIdClaim the token claim holding the SiLAS identifier. Only used when {@code
 *     silasEnabled} is true; when it is unset, or absent from the token, the authenticated identity
 *     is used as-is.
 */
@ConfigurationProperties(prefix = "laa.ccms.entra")
public record EntraProperties(boolean silasEnabled, String customUserIdClaim) {}
