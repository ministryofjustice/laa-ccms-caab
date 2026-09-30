package uk.gov.laa.ccms.caab.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class UploadSizeErrorFilterTest {

  private final UploadSizeErrorFilter filter = new UploadSizeErrorFilter();

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
}
