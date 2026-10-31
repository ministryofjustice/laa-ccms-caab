package uk.gov.laa.ccms.caab.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import uk.gov.laa.ccms.caab.logging.UserIdentifier;

class SecurityEventLoggerTest {

  private final SecurityEventLogger logger = new SecurityEventLogger();
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private final Logger target = (Logger) LoggerFactory.getLogger(SecurityEventLogger.class);
  private final TestingAuthenticationToken user =
      new TestingAuthenticationToken("jane.doe@example.com", "secret");

  @BeforeEach
  void attach() {
    appender.start();
    target.addAppender(appender);
  }

  @AfterEach
  void detach() {
    target.detachAppender(appender);
  }

  @Test
  void authenticationSuccessIsLoggedWithPseudonymisedUser() {
    logger.onAuthenticationSuccess(new AuthenticationSuccessEvent(user));

    ILoggingEvent event = appender.list.getFirst();
    assertEquals("AUTHENTICATION_SUCCESS", keyValue(event, "event_type"));
    assertEquals(
        UserIdentifier.pseudonymise("jane.doe@example.com"),
        event.getMDCPropertyMap().get("user_id"));
    assertFalse(event.getFormattedMessage().contains("jane.doe"));
  }

  @Test
  void authenticationFailureLogsExceptionTypeNotMessage() {
    logger.onAuthenticationFailure(
        new AuthenticationFailureBadCredentialsEvent(
            user, new BadCredentialsException("password was hunter2")));

    ILoggingEvent event = appender.list.getFirst();
    assertEquals("AUTHENTICATION_FAILURE", keyValue(event, "event_type"));
    assertEquals("BadCredentialsException", keyValue(event, "event_detail"));
  }

  @Test
  void authorizationDeniedLogsMethodAndPathWithoutQueryString() {
    var request = new MockHttpServletRequest("GET", "/case/billing/poa");
    request.setQueryString("ref=SENSITIVE");
    logger.onAuthorizationDenied(
        new AuthorizationDeniedEvent<>(
            () -> user,
            new RequestAuthorizationContext(request),
            new AuthorizationDecision(false)));

    ILoggingEvent event = appender.list.getFirst();
    assertEquals("AUTHORIZATION_DENIED", keyValue(event, "event_type"));
    assertEquals("GET /case/billing/poa", keyValue(event, "event_detail"));
  }

  @Test
  void logoutWithoutAuthenticationIsStillLogged() {
    logger.logout(null);

    ILoggingEvent event = appender.list.getFirst();
    assertEquals("LOGOUT", keyValue(event, "event_type"));
    assertTrue(event.getMDCPropertyMap().containsKey("user_id"));
  }

  @Test
  void permissionsLoadedLogsSortedFunctionCodes() {
    logger.permissionsLoaded("jane.doe@example.com", java.util.List.of("B_FUNC", "A_FUNC"));

    ILoggingEvent event = appender.list.getFirst();
    assertEquals("PERMISSIONS_LOADED", keyValue(event, "event_type"));
    assertEquals("SUCCESS", keyValue(event, "outcome"));
    assertEquals(java.util.List.of("A_FUNC", "B_FUNC"), keyValue(event, "event_detail"));
  }

  @Test
  void fileScannedLogsOutcomeAtMatchingLevelWithoutFileName() {
    logger.fileScanned(
        SecurityEventLogger.Outcome.DENIED,
        "300001",
        uk.gov.laa.ccms.caab.constants.CcmsModule.APPLICATION);

    ILoggingEvent event = appender.list.getFirst();
    assertEquals("FILE_SCAN", keyValue(event, "event_type"));
    assertEquals("DENIED", keyValue(event, "outcome"));
    assertEquals("300001", keyValue(event, "case_reference"));
    assertEquals("APPLICATION", keyValue(event, "ccms_module"));
    assertEquals(ch.qos.logback.classic.Level.WARN, event.getLevel());
  }

  @Test
  void inputRejectedLogsClassMethodAndPathOnly() {
    logger.inputRejected(
        "POST",
        "/application/sections",
        new org.springframework.web.multipart.MaxUploadSizeExceededException(1L));

    ILoggingEvent event = appender.list.getFirst();
    assertEquals("INPUT_REJECTED", keyValue(event, "event_type"));
    assertEquals("DENIED", keyValue(event, "outcome"));
    assertEquals("MaxUploadSizeExceededException", keyValue(event, "event_detail"));
    assertEquals("POST", keyValue(event, "http_method"));
    assertEquals("/application/sections", keyValue(event, "path"));
    assertEquals(ch.qos.logback.classic.Level.WARN, event.getLevel());
  }

  @Test
  void failuresAndDenialsLogAtWarnAndSuccessesAtInfo() {
    logger.onAuthenticationSuccess(new AuthenticationSuccessEvent(user));
    logger.onAuthenticationFailure(
        new AuthenticationFailureBadCredentialsEvent(user, new BadCredentialsException("x")));
    logger.logout(user);

    assertEquals(ch.qos.logback.classic.Level.INFO, appender.list.get(0).getLevel());
    assertEquals(ch.qos.logback.classic.Level.WARN, appender.list.get(1).getLevel());
    assertEquals(ch.qos.logback.classic.Level.INFO, appender.list.get(2).getLevel());
  }

  @Test
  void eventsRestoreTheUserAlreadyInTheLoggingContext() {
    org.slf4j.MDC.put("user_id", "request-user");
    try {
      logger.logout(user);
      logger.permissionsLoaded("someone.else@example.com", java.util.List.of("A"));

      assertEquals("request-user", org.slf4j.MDC.get("user_id"));
    } finally {
      org.slf4j.MDC.remove("user_id");
    }
  }

  @Test
  void eventsLeaveNoUserBehindWhenNoneWasSet() {
    logger.logout(user);

    org.junit.jupiter.api.Assertions.assertNull(org.slf4j.MDC.get("user_id"));
  }

  @Test
  void csrfRejectedLogsClassMethodAndPathOnly() {
    logger.csrfRejected(
        "POST",
        "/application/sections",
        new org.springframework.security.web.csrf.InvalidCsrfTokenException(
            new org.springframework.security.web.csrf.DefaultCsrfToken("h", "p", "secret"),
            "actual-secret"));

    ILoggingEvent event = appender.list.getFirst();
    assertEquals("CSRF_REJECTED", keyValue(event, "event_type"));
    assertEquals("DENIED", keyValue(event, "outcome"));
    assertEquals("InvalidCsrfTokenException", keyValue(event, "event_detail"));
    assertEquals("POST", keyValue(event, "http_method"));
    assertEquals("/application/sections", keyValue(event, "path"));
    assertFalse(event.getFormattedMessage().contains("secret"));
    assertEquals(ch.qos.logback.classic.Level.WARN, event.getLevel());
  }

  @Test
  void fileRejectedLogsOnlyTheReasonCode() {
    logger.fileRejected("validation.error.invalidMagicBytes");

    ILoggingEvent event = appender.list.getFirst();
    assertEquals("FILE_REJECTED", keyValue(event, "event_type"));
    assertEquals("DENIED", keyValue(event, "outcome"));
    assertEquals("validation.error.invalidMagicBytes", keyValue(event, "event_detail"));
    assertEquals(ch.qos.logback.classic.Level.WARN, event.getLevel());
  }

  private static Object keyValue(ILoggingEvent event, String key) {
    return event.getKeyValuePairs().stream()
        .filter(kv -> kv.key.equals(key))
        .findFirst()
        .map(kv -> kv.value)
        .orElse(null);
  }
}
