package uk.gov.laa.ccms.caab.advice;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.laa.ccms.caab.constants.SessionConstants;

/**
 * Controller advice class responsible for handling exceptions globally and providing appropriate
 * error responses.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * Handles all exceptions globally and renders the default error page.
   *
   * @param model the Model object used to pass attributes to the view
   * @param session the HttpSession object from which session attributes are retrieved
   * @param exception the exception that was caught
   * @return the name of the error view to be rendered
   */
  @ExceptionHandler(Exception.class)
  public String handleException(Model model, HttpSession session, Exception exception) {
    log.error("Exception caught by GlobalExceptionHandler: {}", exception.getMessage(), exception);
    model.addAttribute(
        SessionConstants.USER_DETAILS, session.getAttribute(SessionConstants.USER_DETAILS));
    model.addAttribute("error", exception.getLocalizedMessage());
    model.addAttribute("errorTime", System.currentTimeMillis());
    return "error";
  }
}
