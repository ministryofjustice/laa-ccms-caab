package uk.gov.laa.ccms.caab.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import uk.gov.laa.ccms.caab.security.CspNonceFilter;

/** Controller advice to make the CSP nonce available to all templates. */
@ControllerAdvice
public class CspAdvice {

  /**
   * Adds the CSP nonce from the request attribute to the model.
   *
   * @param request the current HTTP request.
   * @return the CSP nonce, or null if not present.
   */
  @ModelAttribute("cspNonce")
  public String getCspNonce(HttpServletRequest request) {
    return (String) request.getAttribute(CspNonceFilter.CSP_NONCE_ATTRIBUTE);
  }
}
