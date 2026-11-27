package uk.gov.laa.ccms.caab.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import uk.gov.laa.ccms.caab.controller.HomeController;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.security.SecurityEventLogger;
import uk.gov.laa.ccms.caab.security.UserFunctionAuthorities;
import uk.gov.laa.ccms.caab.service.UserService;
import uk.gov.laa.ccms.caab.util.ApplicationEditorState;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller advice class responsible for adding the OIDC authenticated principal and user details
 * to the model.
 */
@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class OidcPrincipalControllerAdvice {

  private final UserService userService;

  private final SecurityEventLogger securityEventLogger;

  /**
   * Adds the OIDC authenticated principal and user details to the model.
   *
   * @param authentication The authenticated principal representing the authenticated user.
   * @param model The Model object to which attributes will be added.
   * @param session The HttpSession to store and retrieve user details.
   */
  @ModelAttribute
  public void addOidcPrincipalToModel(
      Authentication authentication, Model model, HttpSession session, HttpServletRequest request) {

    if (authentication != null
        && authentication.getPrincipal() instanceof OidcUser oidcUser
        && !requestIsForController(
            request, uk.gov.laa.ccms.caab.controller.CspReportController.class)) {

      String loginId = authentication.getName();

      UserDetail user = new UserDetail();
      user.setLoginId(loginId);
      UserDetail previous = (UserDetail) session.getAttribute("user");

      // When the home page is requested, user details must be retrieved from the database to
      // ensure the correct provider is displayed, as it can be updated outside the control of
      // this service (via legacy PUI or EBS). See https://dsdmoj.atlassian.net/browse/CCMSPUI-949.
      if (session.getAttribute("user") != null
          && !requestIsForController(request, HomeController.class)) {
        user = (UserDetail) session.getAttribute("user");

        if (!user.getLoginId().equals(loginId)) {
          final Integer userId = user.getUserId();
          user = loadUser(() -> userService.getUser(userId).block(), loginId);
        }

      } else {
        user = loadUser(() -> userService.getUserByLoginId(loginId).block(), loginId);
        if (previous != null && user != null) {
          UserFunctionAuthorities.replace(authentication, previous, user, session);
          securityEventLogger.privilegesReloaded(previous.getFunctions(), user.getFunctions());
          // Switched elsewhere (legacy PUI or EBS): nothing open under the old provider carries
          // over.
          if (!Objects.equals(providerId(previous), providerId(user))) {
            ApplicationEditorState.clearOpenApplication(session);
          }
        }
      }

      if (user == null) {
        endLogin(session);
        throw new CaabApplicationException("Unable to retrieve user details for " + loginId);
      }

      model.addAttribute("user", user);
      model.addAttribute("userAttributes", oidcUser.getClaims());

      session.setAttribute("user", user);
    }
  }

  private static Integer providerId(UserDetail user) {
    return user.getProvider() == null ? null : user.getProvider().getId();
  }

  /** Loads the user, treating a failed load like a missing user so that it fails closed. */
  private static UserDetail loadUser(Supplier<UserDetail> load, String loginId) {
    try {
      return load.get();
    } catch (RuntimeException e) {
      log.error("Unable to load user {}", loginId, e);
      return null;
    }
  }

  /**
   * Drops the logged-in authentication, so its functions cannot be used without a user to check
   * them against, and the next request logs the user in again.
   */
  private static void endLogin(HttpSession session) {
    SecurityContextHolder.clearContext();
    // The whole session goes, so the next login cannot inherit the open case or application.
    session.invalidate();
  }

  /**
   * Determines whether the target of the given {@code request} matches the provided controller
   * class.
   *
   * @param request the request to be inspected.
   * @param targetClass the controller class to test against.
   * @return true if the target controller class matches, false otherwise.
   */
  private boolean requestIsForController(HttpServletRequest request, Class<?> targetClass) {
    Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
    return handler instanceof HandlerMethod handlerMethod
        && handlerMethod.getBeanType().equals(targetClass);
  }
}
