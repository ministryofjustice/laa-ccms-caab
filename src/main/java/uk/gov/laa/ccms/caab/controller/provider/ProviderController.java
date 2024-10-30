package uk.gov.laa.ccms.caab.controller.provider;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.service.UserService;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller handling provider switch requests.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ProviderController {

  private final UserService userService;

  /**
   * Loads the provider switch view, where the current user can select a firm to
   * act on behalf on.
   *
   * @param user    the logged-in user.
   * @param model   model to store attributes for the view.
   * @return the provider switch view.
   */
  @GetMapping("/provider-switch")
  public String switchProvider(
      @SessionAttribute(USER_DETAILS) UserDetail user,
      Model model) {

    // Sort by primary firm first, then by name
    List<BaseProvider> userFirms = user.getFirms().stream()
        .sorted(Comparator.comparing(BaseProvider::getName))
        .sorted(Comparator.comparing(BaseProvider::getIsPrimary).reversed())
        .toList();

    model.addAttribute("userFirms", userFirms);

    return "provider/provider-switch";
  }

  /**
   * Updates the current provider via the session.
   *
   * @param user            the logged-in user.
   * @param providerId      the ID of the selected provider to switch to.
   * @param session         the current session.
   * @return a redirect to the home page.
   */
  @GetMapping("/provider-switch/{provider_id}")
  public String switchProvider(
      @SessionAttribute(USER_DETAILS) UserDetail user,
      @PathVariable(value = "provider_id") Integer providerId,
      HttpSession session) {

    BaseProvider newProvider = user.getFirms().stream()
        .filter(firm -> firm.getId().equals(providerId))
        .findFirst()
        .orElseThrow(() -> new CaabApplicationException("Unable to change Provider."));

//    userService.updateUserOptions(newProvider.getId(), user.getLoginId(), user.getUserType())
//        .block();

    user.setProvider(newProvider);

    log.info(user.toString());

    session.setAttribute("user", user);

    return "redirect:/home";
  }

}
