package uk.gov.laa.ccms.caab.security;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import uk.gov.laa.ccms.caab.constants.CcmsModule;
import uk.gov.laa.ccms.caab.logging.UserIdentifier;

/** Writes authentication and authorisation outcomes as structured security events. */
@Component
@Slf4j
public class SecurityEventLogger {

  static final String EVENT_TYPE = "event_type";
  static final String OUTCOME = "outcome";

  /** The values of the {@code event_type} log field that the SOC use cases match on. */
  public enum EventType {
    AUTHENTICATION_SUCCESS,
    AUTHENTICATION_FAILURE,
    AUTHORIZATION_DENIED,
    PERMISSIONS_LOADED,
    PRIVILEGES_CHANGED,
    FILE_SCAN,
    INPUT_REJECTED,
    FILE_REJECTED,
    CSRF_REJECTED,
    LOGOUT
  }

  /** The values of the {@code outcome} log field. */
  public enum Outcome {
    SUCCESS,
    FAILURE,
    DENIED
  }

  @EventListener
  void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
    log(
        EventType.AUTHENTICATION_SUCCESS,
        event.getAuthentication(),
        Outcome.SUCCESS,
        "Authentication succeeded",
        null);
  }

  @EventListener
  void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
    log(
        EventType.AUTHENTICATION_FAILURE,
        event.getAuthentication(),
        Outcome.FAILURE,
        "Authentication failed",
        event.getException().getClass().getSimpleName());
  }

  @EventListener
  void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
    String target = null;
    if (event.getObject() instanceof RequestAuthorizationContext context) {
      target = context.getRequest().getMethod() + " " + context.getRequest().getRequestURI();
    }
    log(
        EventType.AUTHORIZATION_DENIED,
        event.getAuthentication().get(),
        Outcome.DENIED,
        "Access denied",
        target);
  }

  /**
   * Records a user logging out.
   *
   * @param authentication the user's authentication, which may be null if the session had expired
   */
  public void logout(Authentication authentication) {
    log(EventType.LOGOUT, authentication, Outcome.SUCCESS, "User logged out", null);
  }

  /**
   * Records a request the application refused as malformed or not allowed, such as an oversized
   * upload, a wrong HTTP method or a parameter of the wrong type. Only the exception class, method
   * and path are logged, as the values supplied are user input.
   *
   * @param method the HTTP method
   * @param path the request path, without its query string
   * @param exception the exception that described the rejection
   */
  public void inputRejected(String method, String path, Exception exception) {
    log.atWarn()
        .addKeyValue(EVENT_TYPE, EventType.INPUT_REJECTED.name())
        .addKeyValue(OUTCOME, Outcome.DENIED.name())
        .addKeyValue("event_detail", exception.getClass().getSimpleName())
        .addKeyValue("http_method", method)
        .addKeyValue("path", path)
        .log("Request input rejected");
  }

  /**
   * Records a change to the user's functions between two loads within one session, which happens
   * when they are edited in EBS or the legacy PUI. Nothing is logged if they are unchanged. Gaining
   * a function is logged at WARN, as it may be an escalation; only losing one is logged at INFO.
   *
   * @param before the functions the session held, which may be null
   * @param after the functions just loaded, which may be null
   */
  public void privilegesReloaded(Collection<String> before, Collection<String> after) {
    Set<String> previous = before == null ? Set.of() : new TreeSet<>(before);
    Set<String> current = after == null ? Set.of() : new TreeSet<>(after);
    List<String> added = current.stream().filter(f -> !previous.contains(f)).sorted().toList();
    List<String> removed = previous.stream().filter(f -> !current.contains(f)).sorted().toList();
    if (added.isEmpty() && removed.isEmpty()) {
      return;
    }
    log.atLevel(added.isEmpty() ? Level.INFO : Level.WARN)
        .addKeyValue(EVENT_TYPE, EventType.PRIVILEGES_CHANGED.name())
        .addKeyValue(OUTCOME, Outcome.SUCCESS.name())
        .addKeyValue("privileges_added", added)
        .addKeyValue("privileges_removed", removed)
        .log("User privileges changed within a session");
  }

  /**
   * Records an uploaded file refused by the file checks, such as a disallowed extension, a MIME
   * type or content that does not match, or a file that is too large. Only the reason code is
   * logged, as the file name and content are user supplied.
   *
   * @param reasonCode the validation error code that rejected the file
   */
  public void fileRejected(String reasonCode) {
    log.atWarn()
        .addKeyValue(EVENT_TYPE, EventType.FILE_REJECTED.name())
        .addKeyValue(OUTCOME, Outcome.DENIED.name())
        .addKeyValue("event_detail", reasonCode)
        .log("Uploaded file rejected");
  }

  /**
   * Records a state-changing request refused because its CSRF token was wrong, which is what a
   * forged cross-site request looks like. Only the exception class, method and path are logged.
   *
   * @param method the HTTP method
   * @param path the request path, without its query string
   * @param exception the CSRF exception
   */
  public void csrfRejected(String method, String path, Exception exception) {
    log.atWarn()
        .addKeyValue(EVENT_TYPE, EventType.CSRF_REJECTED.name())
        .addKeyValue(OUTCOME, Outcome.DENIED.name())
        .addKeyValue("event_detail", exception.getClass().getSimpleName())
        .addKeyValue("http_method", method)
        .addKeyValue("path", path)
        .log("Request refused: CSRF token rejected");
  }

  /**
   * Records a failed sign-in that Spring Security does not publish an event for, such as a SAML
   * response that cannot be read or verified. Only an error code or the exception class is logged,
   * as the exception message can carry the user's details.
   *
   * @param exception the authentication failure
   */
  public void authenticationFailed(AuthenticationException exception) {
    String detail =
        exception instanceof Saml2AuthenticationException saml2Exception
            ? saml2Exception.getSaml2Error().getErrorCode()
            : exception.getClass().getSimpleName();
    log(EventType.AUTHENTICATION_FAILURE, null, Outcome.FAILURE, "Authentication failed", detail);
  }

  /**
   * Records the permissions granted to a user at sign-in, so changes between sign-ins can be seen.
   *
   * @param loginId the signed-in user's login id
   * @param permissions the EBS user functions loaded for the user
   */
  public void permissionsLoaded(String loginId, Collection<String> permissions) {
    withUser(
        UserIdentifier.pseudonymise(loginId),
        () ->
            log.atInfo()
                .addKeyValue(EVENT_TYPE, EventType.PERMISSIONS_LOADED.name())
                .addKeyValue(OUTCOME, Outcome.SUCCESS.name())
                .addKeyValue("event_detail", permissions.stream().sorted().toList())
                .log("Permissions loaded at sign-in"));
  }

  /**
   * Records the outcome of scanning an uploaded file for malware. The file name is not logged, as
   * it is user supplied.
   *
   * @param outcome SUCCESS if clean, DENIED if malware was found, FAILURE if the scan errored
   * @param caseReference the related case reference, which may be null
   * @param module the source of the upload, which may be null
   */
  public void fileScanned(Outcome outcome, String caseReference, CcmsModule module) {
    Level level =
        switch (outcome) {
          case SUCCESS -> Level.INFO;
          case DENIED -> Level.WARN;
          case FAILURE -> Level.ERROR;
        };
    log.atLevel(level)
        .addKeyValue(EVENT_TYPE, EventType.FILE_SCAN.name())
        .addKeyValue(OUTCOME, outcome.name())
        .addKeyValue("case_reference", caseReference)
        .addKeyValue("ccms_module", module == null ? null : module.name())
        .log("Uploaded file scanned");
  }

  private void log(
      EventType type,
      Authentication authentication,
      Outcome outcome,
      String message,
      String detail) {
    String userId =
        UserIdentifier.pseudonymise(authentication == null ? null : authentication.getName());
    Level level = outcome == Outcome.SUCCESS ? Level.INFO : Level.WARN;
    withUser(
        userId,
        () ->
            log.atLevel(level)
                .addKeyValue(EVENT_TYPE, type.name())
                .addKeyValue(OUTCOME, outcome.name())
                .addKeyValue("event_detail", detail)
                .log(message));
  }

  private static void withUser(String userId, Runnable logging) {
    String previous = MDC.get(UserIdentifier.MDC_KEY);
    MDC.put(UserIdentifier.MDC_KEY, userId);
    try {
      logging.run();
    } finally {
      if (previous == null) {
        MDC.remove(UserIdentifier.MDC_KEY);
      } else {
        MDC.put(UserIdentifier.MDC_KEY, previous);
      }
    }
  }
}
