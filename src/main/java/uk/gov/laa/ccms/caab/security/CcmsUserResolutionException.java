package uk.gov.laa.ccms.caab.security;

import java.io.Serial;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * Raised when an EntraID authenticated identity cannot be matched to a CCMS user.
 *
 * <p>The user has proved who they are to EntraID, but they are not a CCMS user, so there is nothing
 * to grant them access to. It extends {@link OAuth2AuthenticationException} so that the login
 * filter treats it as a failed authentication and hands it to the configured failure handler.
 */
public class CcmsUserResolutionException extends OAuth2AuthenticationException {

  @Serial private static final long serialVersionUID = 1L;

  /** The error code reported against a failure to resolve a CCMS user. */
  public static final String ERROR_CODE = "ccms_user_not_found";

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message.
   */
  public CcmsUserResolutionException(final String message) {
    super(new OAuth2Error(ERROR_CODE, message, null), message);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message.
   * @param cause the cause of the exception.
   */
  public CcmsUserResolutionException(final String message, final Throwable cause) {
    super(new OAuth2Error(ERROR_CODE, message, null), message, cause);
  }
}
