package uk.gov.laa.ccms.caab.advice;

import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.Model;
import org.springframework.web.bind.ServletRequestBindingException;
import uk.gov.laa.ccms.caab.client.EbsApiClientException;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

  @MockitoBean private Model model;

  @MockitoBean private HttpSession session;

  @Autowired private GlobalExceptionHandler globalExceptionHandler;

  @Test
  void handleDataApiClientException() {
    final String errorMsg = "Test Exception";
    EbsApiClientException ebsApiClientException = new EbsApiClientException(errorMsg);

    globalExceptionHandler.handleException(model, session, ebsApiClientException);

    verify(model).addAttribute("error", errorMsg);
  }

  @Test
  void handleCaabApplicationException() {
    final String errorMsg = "Test Exception";
    CaabApplicationException caabApplicationException = new CaabApplicationException(errorMsg);

    globalExceptionHandler.handleException(model, session, caabApplicationException);

    verify(model).addAttribute("error", errorMsg);
  }

  @Test
  void handleServletRequestBindingException() {
    final String errorMsg = "Test Exception";
    ServletRequestBindingException servletRequestBindingException =
        new ServletRequestBindingException(errorMsg);

    globalExceptionHandler.handleException(model, session, servletRequestBindingException);

    verify(model).addAttribute("error", errorMsg);
  }
}
