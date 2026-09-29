package uk.gov.laa.ccms.caab.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestDocumentUploadValidator;

class UploadSizeErrorInterceptorTest {

  private final UploadSizeErrorInterceptor interceptor =
      new UploadSizeErrorInterceptor(
          new ProviderRequestDocumentUploadValidator(
              List.of("pdf"), "8MB", List.of("application/pdf")));

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
  void showsSizeErrorOnMatchingUploadFormAndClearsMarker(String path, String formName) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/civil" + path);
    request.setContextPath("/civil");
    request.getSession().setAttribute(UploadSizeErrorFilter.SESSION_ATTRIBUTE, path);
    ModelAndView view = new ModelAndView("requests/provider-request-doc-upload");
    view.addObject(formName, new EvidenceUploadFormData());

    interceptor.postHandle(request, new MockHttpServletResponse(), new Object(), view);

    BindingResult errors =
        (BindingResult) view.getModel().get(BindingResult.MODEL_KEY_PREFIX + formName);
    assertEquals("validation.error.maxFileSize", errors.getFieldError("file").getCode());
    assertEquals("8MB", errors.getFieldError("file").getArguments()[0]);
    assertNull(request.getSession().getAttribute(UploadSizeErrorFilter.SESSION_ATTRIBUTE));
  }

  @Test
  void doesNotShowStaleErrorOnOtherPages() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/civil/home");
    request.setContextPath("/civil");
    request
        .getSession()
        .setAttribute(
            UploadSizeErrorFilter.SESSION_ATTRIBUTE, "/general-provider-requests/documents");
    ModelAndView view = new ModelAndView("home");

    interceptor.postHandle(request, new MockHttpServletResponse(), new Object(), view);

    assertTrue(view.getModel().isEmpty());
    assertEquals(
        "/general-provider-requests/documents",
        request.getSession().getAttribute(UploadSizeErrorFilter.SESSION_ATTRIBUTE));
  }
}
