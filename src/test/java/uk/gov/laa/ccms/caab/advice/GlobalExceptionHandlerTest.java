package uk.gov.laa.ccms.caab.advice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.ui.Model;
import uk.gov.laa.ccms.caab.service.DataServiceException;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {

  @Mock
  private Logger loggerMock;

  @Mock
  private Model model;

  @InjectMocks
  private GlobalExceptionHandler globalExceptionHandler;

  @Test
  public void testHandleDataServiceException() {
    final String errorMsg = "Test Exception";
    DataServiceException e = new DataServiceException(errorMsg);

    globalExceptionHandler.handleDataServiceException(e, model);

    verify(model).addAttribute("error", errorMsg);
  }
}
