package uk.gov.laa.ccms.caab.advice;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.ui.Model;
import org.springframework.web.bind.ServletRequestBindingException;
import uk.gov.laa.ccms.caab.client.EbsApiClientException;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {

  @Mock
  private Logger loggerMock;

  @Mock
  private Model model;

  @InjectMocks
  private GlobalExceptionHandler globalExceptionHandler;

  @Test
  public void testHandleDataApiClientException() {
    final String errorMsg = "Test Exception";
    EbsApiClientException e = new EbsApiClientException(errorMsg);

    globalExceptionHandler.handleDataApiClientException(e, model);

    verify(model).addAttribute("error", errorMsg);
  }

  @Test
  public void testHandleCaabApplicationException() {
    final String errorMsg = "Test Exception";
    CaabApplicationException e = new CaabApplicationException(errorMsg);

    globalExceptionHandler.handleCaabApplicationException(e, model);

    verify(model).addAttribute("error", errorMsg);
  }

  @Test
  public void testHandleServletRequestBindingException() {
    final String errorMsg = "Test Exception";
    ServletRequestBindingException e = new ServletRequestBindingException(errorMsg);

    globalExceptionHandler.handleServletRequestBindingException(e, model);

    verify(model).addAttribute("error", errorMsg);
  }

}
