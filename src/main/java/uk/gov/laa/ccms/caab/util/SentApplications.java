package uk.gov.laa.ccms.caab.util;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.util.WebUtils;

/**
 * The applications this session has sent, with each one's submission transaction. Kept apart from
 * the case-scoped session, which leaving a case or switching provider clears, and from the shared
 * submission transaction, which any later submission overwrites, so a sent application stays locked
 * until its submission is known to have failed.
 */
public final class SentApplications {

  private static final String ATTRIBUTE = SentApplications.class.getName();

  private SentApplications() {}

  public static void record(HttpSession session, Integer applicationId, String transactionId) {
    synchronized (WebUtils.getSessionMutex(session)) {
      Map<Integer, String> sent = new HashMap<>(sent(session));
      sent.put(applicationId, transactionId);
      session.setAttribute(ATTRIBUTE, sent);
    }
  }

  public static Optional<String> transactionFor(HttpSession session, Integer applicationId) {
    if (applicationId == null) {
      return Optional.empty();
    }
    synchronized (WebUtils.getSessionMutex(session)) {
      return Optional.ofNullable(sent(session).get(applicationId)).filter(id -> !id.isBlank());
    }
  }

  public static void forget(HttpSession session, Integer applicationId) {
    synchronized (WebUtils.getSessionMutex(session)) {
      Map<Integer, String> sent = new HashMap<>(sent(session));
      if (sent.remove(applicationId) != null) {
        session.setAttribute(ATTRIBUTE, sent);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<Integer, String> sent(HttpSession session) {
    return session.getAttribute(ATTRIBUTE) instanceof Map<?, ?> map
        ? (Map<Integer, String>) map
        : Map.of();
  }
}
