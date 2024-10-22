package uk.gov.laa.ccms.caab.controller.provider;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller handling provider switch requests.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ProviderController {

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

    log.info(user.toString());

    List<BaseProvider> userFirms = user.getFirms();

    model.addAttribute("userFirms", userFirms);

    log.info(userFirms.toString());

    return "provider/provider-switch.html";
  }

  /**
   * Loads the provider switch view, where the current user can select a firm to
   * act on behalf on.
   *
   * @param user    the logged-in user.
   * @return the provider switch view.
   */
  @GetMapping("/provider-switch/{provider_id}")
  public String switchProvider(
      @SessionAttribute(USER_DETAILS) UserDetail user,
      @PathVariable(value = "provider_id") Integer providerId,
      HttpSession session) {

    log.info(user.toString());

    BaseProvider newProvider = user.getFirms().stream()
        .filter(firm -> firm.getId().equals(providerId))
        .findFirst()
        .orElseThrow(() -> new CaabApplicationException("Unable to change Provider."));

    user.setProvider(newProvider);

    session.setAttribute("user", user);

    return "redirect:/";
  }

}
