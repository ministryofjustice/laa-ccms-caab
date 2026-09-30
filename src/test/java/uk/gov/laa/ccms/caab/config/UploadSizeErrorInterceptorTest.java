package uk.gov.laa.ccms.caab.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestDocumentUploadValidator;

class UploadSizeErrorInterceptorTest {

  private final UploadSizeErrorInterceptor interceptor =
      new UploadSizeErrorInterceptor(
          new ProviderRequestDocumentUploadValidator(
              List.of("pdf"), "8MB", List.of("application/pdf")));
  private final SessionFlashMapManager flashMapManager = new SessionFlashMapManager();

  @ParameterizedTest
  @CsvSource({
    "/general-provider-requests/documents,generalProviderRequestEvidenceUploadForm",
    "/case-provider-requests/documents,caseProviderRequestEvidenceUploadForm",
    "/general-provider-requests/details,providerRequestDetails",
    "/case-provider-requests/details,providerRequestDetails",
    "/case/outcome-and-awards/document/upload,outcomeAndAwardsDocumentUploadForm",
    "/application/evidence/add,evidenceUploadForm",
    "/amendments/evidence/add,evidenceUploadForm",
    "/notifications/234/attachments/upload,attachmentUploadFormData"
  })
  void showsSizeErrorOnMatchingUploadFormOnlyOnce(String path, String formName) throws IOException {
    MockHttpSession session = new MockHttpSession();
    String redirect = failedUpload(path, null, session);
    MockHttpServletRequest request = redirectedRequest(redirect, session);
    ModelAndView view = view(formName);

    assertEquals(Boolean.TRUE, consumeFlash(request).get(UploadSizeErrorFilter.ERROR_ATTRIBUTE));
    interceptor.postHandle(request, new MockHttpServletResponse(), new Object(), view);

    BindingResult errors = errors(view, formName);
    assertEquals("validation.error.maxFileSize", errors.getFieldError("file").getCode());
    assertEquals("8MB", errors.getFieldError("file").getArguments()[0]);
    assertNull(
        flashMapManager.retrieveAndUpdate(
            redirectedRequest(redirect, session), new MockHttpServletResponse()));
  }

  @Test
  void unrelatedGetDoesNotConsumeError() throws IOException {
    MockHttpSession session = new MockHttpSession();
    String redirect = failedUpload("/general-provider-requests/documents", null, session);
    MockHttpServletRequest unrelated = redirectedRequest("/civil/home", session);
    ModelAndView view = new ModelAndView("home");

    assertNull(consumeFlash(unrelated));
    interceptor.postHandle(unrelated, new MockHttpServletResponse(), new Object(), view);

    assertFalse(view.getModel().containsKey(BindingResult.MODEL_KEY_PREFIX + "home"));
    assertEquals(
        Boolean.TRUE,
        consumeFlash(redirectedRequest(redirect, session))
            .get(UploadSizeErrorFilter.ERROR_ATTRIBUTE));
  }

  @Test
  void concurrentDraftsOnSameRouteKeepTheirOwnErrors() throws IOException {
    String path = "/notifications/234/attachments/upload";
    MockHttpSession session = new MockHttpSession();
    String first = failedUpload(path, "sendBy=ELECTRONIC&attachmentId=567", session);
    String second = failedUpload(path, "sendBy=ELECTRONIC&attachmentId=890", session);
    String formName = "attachmentUploadFormData";

    MockHttpServletRequest ordinary =
        redirectedRequest("/civil" + path + "?attachmentId=567", session);
    assertNull(consumeFlash(ordinary));
    ModelAndView ordinaryView = view(formName);
    interceptor.postHandle(ordinary, new MockHttpServletResponse(), new Object(), ordinaryView);
    assertFalse(errors(ordinaryView, formName).hasErrors());

    assertError(second, session, formName);
    assertError(first, session, formName);
    assertNoError(second, session, formName);
    assertNoError(first, session, formName);
  }

  @Test
  void repeatedFailuresForTheSameDraftEachShowOneError() throws IOException {
    MockHttpSession session = new MockHttpSession();
    String path = "/notifications/234/attachments/upload";
    String query = "sendBy=ELECTRONIC&attachmentId=567";
    String first = failedUpload(path, query, session);
    String second = failedUpload(path, query, session);
    assertNotEquals(first, second);

    String wrongToken =
        first.replaceFirst("uploadSizeErrorToken=[0-9a-f-]{36}", "uploadSizeErrorToken=wrong");
    assertNoError(wrongToken, session, "attachmentUploadFormData");
    assertError(second, session, "attachmentUploadFormData");
    assertError(first, session, "attachmentUploadFormData");
  }

  @Test
  void concurrentUploadRoutesKeepTheirOwnErrors() throws IOException {
    MockHttpSession session = new MockHttpSession();
    String first = failedUpload("/general-provider-requests/documents", null, session);
    String second = failedUpload("/application/evidence/add", null, session);

    assertError(second, session, "evidenceUploadForm");
    assertError(first, session, "generalProviderRequestEvidenceUploadForm");
    assertNoError(first, session, "generalProviderRequestEvidenceUploadForm");
    assertNoError(second, session, "evidenceUploadForm");
  }

  private void assertError(String redirect, MockHttpSession session, String formName) {
    MockHttpServletRequest request = redirectedRequest(redirect, session);
    ModelAndView view = view(formName);
    consumeFlash(request);
    interceptor.postHandle(request, new MockHttpServletResponse(), new Object(), view);
    assertEquals(
        "validation.error.maxFileSize", errors(view, formName).getFieldError("file").getCode());
  }

  private void assertNoError(String redirect, MockHttpSession session, String formName) {
    MockHttpServletRequest request = redirectedRequest(redirect, session);
    ModelAndView view = view(formName);
    assertNull(consumeFlash(request));
    interceptor.postHandle(request, new MockHttpServletResponse(), new Object(), view);
    assertFalse(errors(view, formName).hasErrors());
  }

  private FlashMap consumeFlash(MockHttpServletRequest request) {
    FlashMap flash = flashMapManager.retrieveAndUpdate(request, new MockHttpServletResponse());
    if (flash != null) {
      request.setAttribute(DispatcherServlet.INPUT_FLASH_MAP_ATTRIBUTE, flash);
    }
    return flash;
  }

  private static BindingResult errors(ModelAndView view, String formName) {
    return (BindingResult) view.getModel().get(BindingResult.MODEL_KEY_PREFIX + formName);
  }

  private static ModelAndView view(String formName) {
    ModelAndView view = new ModelAndView("requests/provider-request-doc-upload");
    EvidenceUploadFormData form = new EvidenceUploadFormData();
    view.addObject(formName, form);
    view.addObject(
        BindingResult.MODEL_KEY_PREFIX + formName,
        new org.springframework.validation.BeanPropertyBindingResult(form, formName));
    return view;
  }

  private static String failedUpload(String path, String query, MockHttpSession session)
      throws IOException {
    MockHttpServletRequest post = new MockHttpServletRequest("POST", "/civil" + path);
    post.setContextPath("/civil");
    post.setQueryString(query);
    post.setSession(session);
    MockHttpServletResponse response = new MockHttpServletResponse();
    UploadSizeErrorFilter.redirectToUploadForm(post, response);
    return response.getRedirectedUrl();
  }

  private static MockHttpServletRequest redirectedRequest(String url, MockHttpSession session) {
    URI uri = URI.create(url);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", uri.getPath());
    request.setContextPath("/civil");
    request.setQueryString(uri.getRawQuery());
    request.setSession(session);
    return request;
  }
}
