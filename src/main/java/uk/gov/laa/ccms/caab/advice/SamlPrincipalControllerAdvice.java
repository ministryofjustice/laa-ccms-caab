package uk.gov.laa.ccms.caab.advice;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import uk.gov.laa.ccms.caab.service.UserService;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller advice class responsible for adding the SAML authenticated principal and user details
 * to the model.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class SamlPrincipalControllerAdvice {

  private final UserService userService;

  /**
   * Adds the SAML authenticated principal and user details to the model.
   *
   * @param principal The SAML2 authenticated principal representing the authenticated user.
   * @param model The Model object to which attributes will be added.
   * @param session The HttpSession to store and retrieve user details.
   */
  @ModelAttribute
  public void addSamlPrincipalToModel(
      @AuthenticationPrincipal Saml2AuthenticatedPrincipal principal,
      Model model,
      HttpSession session) {

    if (principal != null) {

      UserDetail user = new UserDetail();
      user.setLoginId(principal.getName());

      if (session.getAttribute("user") != null) {
        user = (UserDetail) session.getAttribute("user");

        if (!user.getLoginId().equals(principal.getName())) {
          user = userService.getUser(user.getLoginId()).block();
        }

      } else {
        user = userService.getUser(user.getLoginId()).block();
      }

      model.addAttribute("user", user);
      model.addAttribute("userAttributes", principal.getAttributes());

      session.setAttribute("user", user);
    }
  }
}
