package uk.gov.laa.ccms.caab.security;

import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
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
    FILE_SCAN,
    INPUT_REJECTED,
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
