package uk.gov.laa.ccms.caab.advice;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.laa.ccms.caab.client.EbsApiClientException;
import uk.gov.laa.ccms.caab.constants.SessionConstants;
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
   * @param session The HttpSession for the current request.
   * @param model The Model object to which error information will be added.
   * @return The name of the error view to be displayed.
   */
  @ExceptionHandler(value = {EbsApiClientException.class})
  public String handleDataApiClientException(EbsApiClientException e,
      HttpSession session, Model model) {
    // This exception is thrown when there's a low-level, resource-specific error,
    // such as an I/O error, Log the error details
    return generalErrorView(model, session, e);
  }

  /**
   * Handles {@link CaabApplicationException} by logging the error details and returning an
   * appropriate error response.
   *
   * @param e The CaabApplicationException that was thrown.
   * @param session The HttpSession for the current request.
   * @param model The Model object to which error information will be added.
   * @return The name of the error view to be displayed.
   */
  @ExceptionHandler(value = {CaabApplicationException.class})
  public String handleCaabApplicationException(CaabApplicationException e,
      HttpSession session, Model model) {
    return generalErrorView(model, session, e);
  }

  /**
   * Handles {@link ServletRequestBindingException} by logging the error details and returning an
   * appropriate error response.
   *
   * @param e The ServletRequestBindingException that was thrown.
   * @param session The HttpSession for the current request.
   * @param model The Model object to which error information will be added.
   * @return The name of the error view to be displayed.
   */
  @ExceptionHandler(value = {ServletRequestBindingException.class})
  public String handleServletRequestBindingException(
      ServletRequestBindingException e,
      HttpSession session,
      Model model) {
    return generalErrorView(model, session, e);
  }

  private String generalErrorView(Model model, HttpSession session, Exception e) {
    log.error("{} caught by GlobalExceptionHandler", e.getClass().getName(), e);
    model.addAttribute(SessionConstants.USER_DETAILS,
        session.getAttribute(SessionConstants.USER_DETAILS));
    model.addAttribute("error", e.getLocalizedMessage());
    model.addAttribute("errorTime", System.currentTimeMillis());
    return "error";
  }
}
