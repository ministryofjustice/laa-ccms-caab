package uk.gov.laa.ccms.caab.advice;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.laa.ccms.caab.client.InvoiceAssessmentReadException;
import uk.gov.laa.ccms.caab.constants.SessionConstants;
import uk.gov.laa.ccms.caab.security.NotAuthorisedAccessDeniedHandler;

/**
 * Controller advice class responsible for handling exceptions globally and providing appropriate
 * error responses.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler implements ApplicationEventPublisherAware {

  private final AccessDeniedHandler refusal = new NotAuthorisedAccessDeniedHandler();

  private ApplicationEventPublisher eventPublisher;

  static final String GENERIC_ERROR_MESSAGE =
      "An unexpected error occurred. Please contact support if the problem continues.";

  @Override
  public void setApplicationEventPublisher(final ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  /**
   * Refuses an action a controller found the user lacks the functions for, in the same way as the
   * route rules: logged as a denial, and the user returned to their previous page.
   *
   * @param request the refused request
   * @param response the response to send the user back on
   * @param exception the refusal
   * @throws IOException if the redirect cannot be sent
   * @throws ServletException if the refusal cannot be handled
   */
  @ExceptionHandler(AccessDeniedException.class)
  public void handleAccessDenied(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final AccessDeniedException exception)
      throws IOException, ServletException {
    if (eventPublisher != null) {
      eventPublisher.publishEvent(
          new AuthorizationDeniedEvent<>(
              SecurityContextHolder.getContext()::getAuthentication,
              request,
              new AuthorizationDecision(false)));
    }
    refusal.handle(request, response, exception);
  }

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
    if (exception instanceof InvoiceAssessmentReadException invoiceError) {
      log.error(
          "Exception caught by GlobalExceptionHandler: retained invoice read failed (HTTP {})",
          invoiceError.getHttpStatus());
    } else {
      log.error("Exception caught by GlobalExceptionHandler", exception);
    }
    model.addAttribute(
        SessionConstants.USER_DETAILS, session.getAttribute(SessionConstants.USER_DETAILS));
    model.addAttribute("error", GENERIC_ERROR_MESSAGE);
    model.addAttribute("errorTime", System.currentTimeMillis());
    return "error";
  }
}
