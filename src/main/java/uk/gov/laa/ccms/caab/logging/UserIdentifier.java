package uk.gov.laa.ccms.caab.logging;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Turns a login id into a stable log identifier that is not the login id itself. */
public final class UserIdentifier {

  public static final String MDC_KEY = "user_id";

  private static final String UNKNOWN = "unknown";
  private static final int HEX_LENGTH = 16;

  private UserIdentifier() {}

  /**
   * Returns a truncated SHA-256 of the login id, so the same user always maps to the same value.
   *
   * @param loginId the login id, which may be null or blank
   * @return the pseudonymised identifier, or "unknown" when there is no login id
   */
  public static String pseudonymise(String loginId) {
    if (loginId == null || loginId.isBlank()) {
      return UNKNOWN;
    }
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest(loginId.strip().toLowerCase().getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest).substring(0, HEX_LENGTH);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
