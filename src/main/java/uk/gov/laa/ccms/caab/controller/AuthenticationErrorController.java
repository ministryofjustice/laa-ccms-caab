package uk.gov.laa.ccms.caab.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for the page shown when a user cannot be signed in.
 *
 * <p>Reached when EntraID authenticated the user but CCMS would not admit them - most often because
 * their identity is not mapped to a CCMS user. The page is deliberately reachable without
 * authentication, since by definition nobody who lands on it has any.
 */
@Controller
public class AuthenticationErrorController {

  /**
   * Displays the authentication error page.
   *
   * @return the view name.
   */
  @GetMapping("/authentication-error")
  public String authenticationError() {
    return "authentication-error";
  }
}
