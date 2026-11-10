package uk.gov.laa.ccms.caab.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

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

  /**
   * Runs logging with the user who is signed in to the request's session, for code that runs
   * outside the security filter chain and so has no user in the logging context.
   *
   * @param request the request, whose session may hold a security context
   * @param logging the logging to run
   */
  public static void withSessionUser(HttpServletRequest request, Runnable logging) {
    String userId = sessionUser(request);
    if (userId == null) {
      logging.run();
      return;
    }
    String previous = MDC.get(MDC_KEY);
    MDC.put(MDC_KEY, userId);
    try {
      logging.run();
    } finally {
      if (previous == null) {
        MDC.remove(MDC_KEY);
      } else {
        MDC.put(MDC_KEY, previous);
      }
    }
  }

  private static String sessionUser(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null
        && session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
            instanceof SecurityContext context) {
      Authentication authentication = context.getAuthentication();
      if (authentication != null
          && authentication.isAuthenticated()
          && !(authentication instanceof AnonymousAuthenticationToken)) {
        return pseudonymise(authentication.getName());
      }
    }
    return null;
  }
}
