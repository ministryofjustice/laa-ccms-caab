package uk.gov.laa.ccms.caab.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.laa.ccms.caab.client.EbsApiClientException;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

/**
 * Controller advice class responsible for handling exceptions globally and providing appropriate
 * error responses.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * Handles {@link EbsApiClientException} by logging the error details and returning an appropriate
   * error response.
   *
   * @param e The EbsApiClientException that was thrown.
   * @param model The Model object to which error information will be added.
   * @return The name of the error view to be displayed.
   */
  @ExceptionHandler(value = {EbsApiClientException.class})
  public String handleDataApiClientException(EbsApiClientException e, Model model) {
    // This exception is thrown when there's a low-level, resource-specific error,
    // such as an I/O error, Log the error details
    log.error("EbsApiClientException caught by GlobalExceptionHandler", e);
    // return an appropriate response
    model.addAttribute("error", e.getLocalizedMessage());
    model.addAttribute("errorTime", System.currentTimeMillis());

    return "error";
  }

  /**
   * Handles {@link CaabApplicationException} by logging the error details and returning an
   * appropriate error response.
   *
   * @param e The CaabApplicationException that was thrown.
   * @param model The Model object to which error information will be added.
   * @return The name of the error view to be displayed.
   */
  @ExceptionHandler(value = {CaabApplicationException.class})
  public String handleCaabApplicationException(CaabApplicationException e, Model model) {
    // Generic handler
    log.error("CaabApplicationException caught by GlobalExceptionHandler", e);
    // return an appropriate response
    model.addAttribute("error", e.getLocalizedMessage());
    model.addAttribute("errorTime", System.currentTimeMillis());

    return "error";
  }
}
