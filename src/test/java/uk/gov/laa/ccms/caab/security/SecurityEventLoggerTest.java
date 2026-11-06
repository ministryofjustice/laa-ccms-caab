package uk.gov.laa.ccms.caab.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
import uk.gov.laa.ccms.caab.bean.metric.PuiMetricService;
import uk.gov.laa.ccms.caab.logging.UserIdentifier;

class SecurityEventLoggerTest {

  private final PuiMetricService metrics = mock(PuiMetricService.class);
  private final SecurityEventLogger logger = new SecurityEventLogger(metrics);
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

  @Test
  void gainingAFunctionIsLoggedAtWarnWithTheDifference() {
    logger.privilegesReloaded(
        java.util.List.of("VIEW", "EDIT"), java.util.List.of("VIEW", "DELETE"));

    ILoggingEvent event = appender.list.getFirst();
    assertEquals("PRIVILEGES_CHANGED", keyValue(event, "event_type"));
    assertEquals("SUCCESS", keyValue(event, "outcome"));
    assertEquals(java.util.List.of("DELETE"), keyValue(event, "privileges_added"));
    assertEquals(java.util.List.of("EDIT"), keyValue(event, "privileges_removed"));
    assertEquals(ch.qos.logback.classic.Level.WARN, event.getLevel());
  }

  @Test
  void onlyLosingAFunctionIsLoggedAtInfo() {
    logger.privilegesReloaded(java.util.List.of("VIEW", "EDIT"), java.util.List.of("VIEW"));

    ILoggingEvent event = appender.list.getFirst();
    assertEquals(java.util.List.of(), keyValue(event, "privileges_added"));
    assertEquals(java.util.List.of("EDIT"), keyValue(event, "privileges_removed"));
    assertEquals(ch.qos.logback.classic.Level.INFO, event.getLevel());
  }

  @Test
  void unchangedOrReorderedFunctionsLogNothing() {
    logger.privilegesReloaded(java.util.List.of("B", "A"), java.util.List.of("A", "B"));
    logger.privilegesReloaded(null, null);

    org.junit.jupiter.api.Assertions.assertTrue(appender.list.isEmpty());
  }

  @Test
  void aMissingBeforeListCountsEverythingAsAdded() {
    logger.privilegesReloaded(null, java.util.List.of("VIEW"));

    assertEquals(java.util.List.of("VIEW"), keyValue(appender.list.getFirst(), "privileges_added"));
  }

  @Test
  void aSamlFailureIsLoggedByItsErrorCodeNotItsMessage() {
    logger.authenticationFailed(
        new org.springframework.security.saml2.provider.service.authentication
            .Saml2AuthenticationException(
            new org.springframework.security.saml2.core.Saml2Error(
                "invalid_signature", "bad signature for jane.doe@example.com")));

    ILoggingEvent event = appender.list.getFirst();
    assertEquals("AUTHENTICATION_FAILURE", keyValue(event, "event_type"));
    assertEquals("FAILURE", keyValue(event, "outcome"));
    assertEquals("invalid_signature", keyValue(event, "event_detail"));
    assertEquals(ch.qos.logback.classic.Level.WARN, event.getLevel());
    assertFalse(event.getFormattedMessage().contains("jane.doe"));
  }

  @Test
  void otherFailuresAreLoggedByExceptionClass() {
    logger.authenticationFailed(new BadCredentialsException("password was hunter2"));

    assertEquals("BadCredentialsException", keyValue(appender.list.getFirst(), "event_detail"));
  }

  @Test
  void everySecurityEventIsCountedByTypeAndOutcome() {
    logger.onAuthenticationSuccess(new AuthenticationSuccessEvent(user));
    logger.onAuthenticationFailure(
        new AuthenticationFailureBadCredentialsEvent(user, new BadCredentialsException("x")));
    logger.onAuthorizationDenied(
        new AuthorizationDeniedEvent<>(
            () -> user,
            new RequestAuthorizationContext(new MockHttpServletRequest("GET", "/x")),
            new AuthorizationDecision(false)));
    logger.logout(user);
    logger.permissionsLoaded("a", java.util.List.of("A"));
    logger.fileScanned(SecurityEventLogger.Outcome.DENIED, "1", null);
    logger.fileRejected("validation.error.invalidExtension");
    logger.inputRejected("GET", "/x", new IllegalStateException());
    logger.csrfRejected("POST", "/x", new IllegalStateException());
    logger.privilegesReloaded(java.util.List.of("A"), java.util.List.of("A", "B"));
    logger.authenticationFailed(new BadCredentialsException("x"));

    verify(metrics).incrementSecurityEvents("AUTHENTICATION_SUCCESS", "SUCCESS");
    verify(metrics, org.mockito.Mockito.times(2))
        .incrementSecurityEvents("AUTHENTICATION_FAILURE", "FAILURE");
    verify(metrics).incrementSecurityEvents("AUTHORIZATION_DENIED", "DENIED");
    verify(metrics).incrementSecurityEvents("LOGOUT", "SUCCESS");
    verify(metrics).incrementSecurityEvents("PERMISSIONS_LOADED", "SUCCESS");
    verify(metrics).incrementSecurityEvents("FILE_SCAN", "DENIED");
    verify(metrics).incrementSecurityEvents("FILE_REJECTED", "DENIED");
    verify(metrics).incrementSecurityEvents("INPUT_REJECTED", "DENIED");
    verify(metrics).incrementSecurityEvents("CSRF_REJECTED", "DENIED");
    verify(metrics).incrementSecurityEvents("PRIVILEGES_CHANGED", "SUCCESS");

    var invocations = org.mockito.Mockito.mockingDetails(metrics).getInvocations();
    java.util.Set<String> counted = pairs(invocations, "incrementSecurityEvents");
    java.util.Set<String> registered = pairs(invocations, "initSecurityEvents");
    org.assertj.core.api.Assertions.assertThat(registered).containsAll(counted);
  }

  private static java.util.Set<String> pairs(
      java.util.Collection<org.mockito.invocation.Invocation> invocations, String method) {
    return invocations.stream()
        .filter(invocation -> invocation.getMethod().getName().equals(method))
        .map(invocation -> invocation.getArgument(0) + "/" + invocation.getArgument(1))
        .collect(java.util.stream.Collectors.toSet());
  }

  @Test
  void everyKindOfEventIsRegisteredAtZeroWhenTheLoggerStarts() {
    verify(metrics).initSecurityEvents("FILE_SCAN", "DENIED");
    verify(metrics).initSecurityEvents("FILE_SCAN", "FAILURE");
    verify(metrics).initSecurityEvents("AUTHENTICATION_FAILURE", "FAILURE");
    verify(metrics).initSecurityEvents("CSRF_REJECTED", "DENIED");
    verify(metrics).initSecurityEvents("PRIVILEGES_CHANGED", "SUCCESS");
  }

  @Test
  void unchangedFunctionsAreNotCounted() {
    logger.privilegesReloaded(java.util.List.of("A"), java.util.List.of("A"));

    verify(metrics, org.mockito.Mockito.never())
        .incrementSecurityEvents(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
  }

  private static Object keyValue(ILoggingEvent event, String key) {
    return event.getKeyValuePairs().stream()
        .filter(kv -> kv.key.equals(key))
        .findFirst()
        .map(kv -> kv.value)
        .orElse(null);
  }
}
