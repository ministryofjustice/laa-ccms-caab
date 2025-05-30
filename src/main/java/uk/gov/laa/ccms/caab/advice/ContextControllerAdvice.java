package uk.gov.laa.ccms.caab.advice;

import static uk.gov.laa.ccms.caab.constants.ContextConstants.AMENDMENTS;
import static uk.gov.laa.ccms.caab.constants.ContextConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.ContextConstants.CONTEXT_NAME;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.HandlerMapping;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

/**
 * Controller advice class that performs a check when the "caseContext" path variable is used to
 * distinguish between a new application and an amendment. An exception is thrown if
 * an unrecognized context is passed.
 *
 * @author Jamie Briggs
 */
@Component
@ControllerAdvice
public class ContextControllerAdvice {

  /**
   * Validates the path variable "caseContext".
   *
   * @param request the HttpServletRequest request object.
   */
  @ModelAttribute
  public void validateContextPathVariable(HttpServletRequest request) {

    final Map<String, String> uriVariables =
        (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

    if (uriVariables != null && uriVariables.containsKey(CONTEXT_NAME)) {
      String context = uriVariables.get(CONTEXT_NAME);
      if (!APPLICATION.equalsIgnoreCase(context)
          && !AMENDMENTS.equalsIgnoreCase(context)) {
        throw new CaabApplicationException("Unknown context passed: " + context);
      }
    }
  }


}
