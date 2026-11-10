package uk.gov.laa.ccms.caab.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import java.net.URI;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import uk.gov.laa.ccms.caab.security.SecurityEventLogger;

class UploadSizeErrorFilterTest {

  private final SecurityEventLogger securityEventLogger = mock(SecurityEventLogger.class);

  private final UploadSizeErrorFilter filter = new UploadSizeErrorFilter(securityEventLogger);

  @Test
  void redirectsOversizedMultipartRequestToItsForm() throws Exception {
    MockHttpServletRequest request = request("/civil/case-provider-requests/documents");
    request.setQueryString("caseReferenceNumber=123");
    MockHttpServletResponse response =
        new MockHttpServletResponse() {
          @Override
          public String encodeRedirectURL(String url) {
            return url + ";jsessionid=synthetic";
          }
        };
    FilterChain chain =
        (req, res) -> {
          throw new InvalidParameterException(
              new SizeLimitExceededException("too large", 13_000_000, 12_582_912));
        };

    filter.doFilter(request, response, chain);

    assertTrue(
        response
            .getRedirectedUrl()
            .matches(
                "/civil/case-provider-requests/documents\\?caseReferenceNumber=123"
                    + "&uploadSizeErrorToken=[0-9a-f-]{36}"));
    MockHttpServletRequest redirected = redirectedRequest(response, request);
    assertEquals(
        Boolean.TRUE,
        new SessionFlashMapManager()
            .retrieveAndUpdate(redirected, new MockHttpServletResponse())
            .get(UploadSizeErrorFilter.ERROR_ATTRIBUTE));
  }

  @Test
  void redirectsNotificationUploadWithRequiredSendBy() throws Exception {
    MockHttpServletRequest request = request("/civil/notifications/234/attachments/upload");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        request,
        response,
        (req, res) -> {
          throw new InvalidParameterException(
              new FileSizeLimitExceededException("too large", 8_388_609, 8_388_608));
        });

    assertTrue(
        response
            .getRedirectedUrl()
            .matches(
                "/civil/notifications/234/attachments/upload\\?sendBy=ELECTRONIC"
                    + "&uploadSizeErrorToken=[0-9a-f-]{36}"));
  }

  @Test
  void preservesDraftIdOnOversizedNotificationReplacement() throws Exception {
    MockHttpServletRequest request = request("/civil/notifications/234/attachments/upload");
    request.setQueryString("sendBy=ELECTRONIC&attachmentId=567");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        request,
        response,
        (req, res) -> {
          throw new InvalidParameterException(
              new FileSizeLimitExceededException("too large", 8_388_609, 8_388_608));
        });

    assertTrue(
        response
            .getRedirectedUrl()
            .matches(
                "/civil/notifications/234/attachments/upload\\?sendBy=ELECTRONIC"
                    + "&attachmentId=567&uploadSizeErrorToken=[0-9a-f-]{36}"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/general-provider-requests/details",
        "/case-provider-requests/details",
        "/case/outcome-and-awards/document/upload",
        "/application/evidence/add",
        "/amendments/evidence/add"
      })
  void redirectsOtherUploadForms(String path) throws Exception {
    MockHttpServletRequest request = request("/civil" + path);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        request,
        response,
        (req, res) -> {
          throw new InvalidParameterException(
              new FileSizeLimitExceededException("too large", 8_388_609, 8_388_608));
        });

    assertTrue(
        response
            .getRedirectedUrl()
            .matches("/civil" + path + "\\?uploadSizeErrorToken=[0-9a-f-]{36}"));
  }

  @Test
  void doesNotHandleUnrelatedParameterErrors() {
    MockHttpServletRequest request = request("/civil/general-provider-requests/documents");
    MockHttpServletResponse response = new MockHttpServletResponse();
    InvalidParameterException exception = new InvalidParameterException("invalid parameter");

    assertThrows(
        InvalidParameterException.class,
        () ->
            filter.doFilter(
                request,
                response,
                (req, res) -> {
                  throw exception;
                }));
    assertNull(request.getSession(false));
  }

  @Test
  void doesNotHandleSizeErrorOutsideUploadRoutes() {
    MockHttpServletRequest request = request("/civil/home");
    InvalidParameterException exception =
        new InvalidParameterException(
            new SizeLimitExceededException("too large", 13_000_000, 12_582_912));

    assertThrows(
        InvalidParameterException.class,
        () ->
            filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (req, res) -> {
                  throw exception;
                }));
    assertNull(request.getSession(false));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/civil//outside.example/case-provider-requests/documents",
        "/civil/notifications//outside.example/attachments/upload",
        "/civil/notifications/234/attachments/upload/https://outside.example"
      })
  void rejectsUploadPathsThatCouldLeaveTheApplication(String uri) {
    MockHttpServletRequest request = request(uri);
    InvalidParameterException exception =
        new InvalidParameterException(
            new SizeLimitExceededException("too large", 13_000_000, 12_582_912));
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThrows(
        InvalidParameterException.class,
        () ->
            filter.doFilter(
                request,
                response,
                (req, res) -> {
                  throw exception;
                }));
    assertNull(response.getRedirectedUrl());
    assertNull(request.getSession(false));
  }

  @Test
  void remoteUrlInQueryCannotChangeRedirectHost() throws Exception {
    MockHttpServletRequest request = request("/civil/notifications/234/attachments/upload");
    request.setQueryString("sendBy=ELECTRONIC&next=https://outside.example/path");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        request,
        response,
        (req, res) -> {
          throw new InvalidParameterException(
              new FileSizeLimitExceededException("too large", 8_388_609, 8_388_608));
        });

    URI redirect = URI.create(response.getRedirectedUrl());
    assertNull(redirect.getHost());
    assertEquals("/civil/notifications/234/attachments/upload", redirect.getPath());
    assertTrue(redirect.getRawQuery().contains("next=https://outside.example/path"));
    assertEquals(
        Boolean.TRUE,
        new SessionFlashMapManager()
            .retrieveAndUpdate(redirectedRequest(response, request), new MockHttpServletResponse())
            .get(UploadSizeErrorFilter.ERROR_ATTRIBUTE));
  }

  private static MockHttpServletRequest request(String uri) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
    request.setContextPath("/civil");
    return request;
  }

  private static MockHttpServletRequest redirectedRequest(
      MockHttpServletResponse response, MockHttpServletRequest original) {
    URI redirect = URI.create(response.getRedirectedUrl());
    MockHttpServletRequest request = new MockHttpServletRequest("GET", redirect.getPath());
    request.setContextPath(original.getContextPath());
    request.setQueryString(redirect.getRawQuery());
    request.setSession(original.getSession());
    return request;
  }

  @Test
  void logsTheSizeFailureWithoutTheQueryString() throws Exception {
    MockHttpServletRequest request = request("/civil/case-provider-requests/documents");
    request.setQueryString("caseReferenceNumber=123");
    FilterChain chain =
        (req, res) -> {
          throw new InvalidParameterException(
              new SizeLimitExceededException("too large", 13_000_000, 12_582_912));
        };

    uk.gov.laa.ccms.caab.logging.RequestRoute.registerKnownSegments(
        java.util.Set.of("case-provider-requests", "documents"));
    try {
      filter.doFilter(request, new MockHttpServletResponse(), chain);
    } finally {
      uk.gov.laa.ccms.caab.logging.RequestRoute.clearKnownSegments();
    }

    verify(securityEventLogger)
        .inputRejected(
            eq("POST"),
            eq("/civil/case-provider-requests/documents"),
            any(SizeLimitExceededException.class));
  }

  @Test
  void theRejectionIsLoggedInsideTheRequestCorrelation() throws Exception {
    String[] requestId = new String[1];
    org.mockito.Mockito.doAnswer(
            invocation -> {
              requestId[0] = org.slf4j.MDC.get("request_id");
              return null;
            })
        .when(securityEventLogger)
        .inputRejected(any(), any(), any());
    int correlationOrder = org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 1;
    int uploadOrder =
        org.springframework.core.annotation.AnnotationUtils.findAnnotation(
                UploadSizeErrorFilter.class, org.springframework.core.annotation.Order.class)
            .value();
    org.junit.jupiter.api.Assertions.assertTrue(uploadOrder > correlationOrder);
    var filters =
        new jakarta.servlet.Filter[] {
          new uk.gov.laa.ccms.caab.logging.SessionCorrelationFilter(), filter
        };
    jakarta.servlet.Servlet servlet = mock(jakarta.servlet.Servlet.class);
    org.mockito.Mockito.doThrow(
            new InvalidParameterException(
                new SizeLimitExceededException("too large", 13_000_000, 12_582_912)))
        .when(servlet)
        .service(any(), any());

    new org.springframework.mock.web.MockFilterChain(servlet, filters)
        .doFilter(
            request("/civil/case-provider-requests/documents"), new MockHttpServletResponse());

    org.junit.jupiter.api.Assertions.assertNotNull(requestId[0]);
  }

  @Test
  void theRejectionIsLoggedWithTheSignedInUserFromTheSession() throws Exception {
    String[] userId = new String[1];
    org.mockito.Mockito.doAnswer(
            invocation -> {
              userId[0] = org.slf4j.MDC.get("user_id");
              return null;
            })
        .when(securityEventLogger)
        .inputRejected(any(), any(), any());
    MockHttpServletRequest request = request("/civil/case-provider-requests/documents");
    var session = new org.springframework.mock.web.MockHttpSession();
    var context =
        new org.springframework.security.core.context.SecurityContextImpl(
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated("jane.doe@example.com", "n/a", java.util.List.of()));
    session.setAttribute(
        org.springframework.security.web.context.HttpSessionSecurityContextRepository
            .SPRING_SECURITY_CONTEXT_KEY,
        context);
    request.setSession(session);
    FilterChain chain =
        (req, res) -> {
          throw new InvalidParameterException(
              new SizeLimitExceededException("too large", 13_000_000, 12_582_912));
        };

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertEquals(
        uk.gov.laa.ccms.caab.logging.UserIdentifier.pseudonymise("jane.doe@example.com"),
        userId[0]);
    assertNull(org.slf4j.MDC.get("user_id"));
  }
}
