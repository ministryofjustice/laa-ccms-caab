package uk.gov.laa.ccms.caab.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import uk.gov.laa.ccms.caab.controller.HomeController;
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
   * @param authentication The authenticated principal representing the authenticated user.
   * @param model The Model object to which attributes will be added.
   * @param session The HttpSession to store and retrieve user details.
   */
  @ModelAttribute
  public void addSamlPrincipalToModel(
      Authentication authentication, Model model, HttpSession session, HttpServletRequest request) {

    if (authentication instanceof Saml2AssertionAuthentication saml2Authentication
        && !requestIsForController(
            request, uk.gov.laa.ccms.caab.controller.CspReportController.class)) {

      String loginId = saml2Authentication.getName();

      UserDetail user = new UserDetail();
      user.setLoginId(loginId);

      // When the home page is requested, user details must be retrieved from the database to
      // ensure the correct provider is displayed, as it can be updated outside the control of
      // this service (via legacy PUI or EBS). See https://dsdmoj.atlassian.net/browse/CCMSPUI-949.
      if (session.getAttribute("user") != null
          && !requestIsForController(request, HomeController.class)) {
        user = (UserDetail) session.getAttribute("user");

        if (!user.getLoginId().equals(loginId)) {
          user = userService.getUser(user.getUserId()).block();
        }

      } else {
        UserDetail previousUser = (UserDetail) session.getAttribute("user");
        user = userService.getUserByLoginId(loginId).block();
        if (previousUser != null && user != null) {
          refreshUserFunctions(saml2Authentication, previousUser, user, session);
        }
      }

      model.addAttribute("user", user);
      model.addAttribute("userAttributes", saml2Authentication.getCredentials().getAttributes());

      session.setAttribute("user", user);
    }
  }

  /**
   * Replaces the user's functions in the logged-in authentication with those just reloaded, as the
   * legacy PUI reloads them on entering the home page, which includes after a provider switch.
   * Authorities that are not functions, such as the SAML groups, are kept.
   */
  private static void refreshUserFunctions(
      Saml2AssertionAuthentication authentication,
      UserDetail previousUser,
      UserDetail reloadedUser,
      HttpSession session) {
    List<String> previousFunctions = functionsOf(previousUser);
    List<String> reloadedFunctions = functionsOf(reloadedUser);

    Set<GrantedAuthority> authorities = new LinkedHashSet<>();
    authentication.getAuthorities().stream()
        .filter(authority -> !previousFunctions.contains(authority.getAuthority()))
        .forEach(authorities::add);
    reloadedFunctions.stream().map(SimpleGrantedAuthority::new).forEach(authorities::add);

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new Saml2AssertionAuthentication(
            authentication.getPrincipal(),
            authentication.getCredentials(),
            authorities,
            authentication.getRelyingPartyRegistrationId()));
    SecurityContextHolder.setContext(context);
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
  }

  private static List<String> functionsOf(UserDetail user) {
    return Optional.ofNullable(user.getFunctions()).orElse(List.of());
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
