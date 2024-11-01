package uk.gov.laa.ccms.caab.controller.provider;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.bean.provider.ProviderFirmFormData;
import uk.gov.laa.ccms.caab.bean.validators.provider.ProviderFirmValidator;
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
  private final ProviderFirmValidator providerFirmValidator;

  /**
   * Loads the provider switch view, where the current user can select a firm to
   * act on behalf on.
   *
   * @param user                    the logged-in user.
   * @param providerFirmFormData    form data to store the selected provider.
   * @param model                   model to store attributes for the view.
   * @return the provider switch view.
   */
  @GetMapping("/provider-switch")
  public String switchProvider(
      @SessionAttribute(USER_DETAILS) UserDetail user,
      @ModelAttribute("providerFirmFormData") ProviderFirmFormData providerFirmFormData,
      Model model) {

    providerFirmFormData.setProviderFirmId(user.getProvider().getId());

    populateProviderFirmDropdown(model, user);

    model.addAttribute("providerFirmFormData", providerFirmFormData);

    return "provider/provider-switch";
  }

  /**
   * Updates the current provider via the session.
   *
   * @param user                    the logged-in user.
   * @param providerFirmFormData    form data to store the selected provider.
   * @param bindingResult           validation result.
   * @param model                   model to store attributes for the view.
   * @param session                 the current session.
   * @return a redirect to the home page.
   */
  @PostMapping("/provider-switch")
  public String switchProvider(
      @SessionAttribute(USER_DETAILS) UserDetail user,
      @ModelAttribute("providerFirmFormData") ProviderFirmFormData providerFirmFormData,
      BindingResult bindingResult,
      Model model,
      HttpSession session) {

    providerFirmValidator.validate(providerFirmFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      populateProviderFirmDropdown(model, user);
      return "provider/provider-switch";
    }

    BaseProvider newProvider = user.getFirms().stream()
        .filter(firm -> firm.getId().equals(providerFirmFormData.getProviderFirmId()))
        .findFirst()
        .orElseThrow(() -> new CaabApplicationException("Unable to change Provider."));

    userService.updateUserOptions(newProvider.getId(), user.getLoginId(), user.getUserType())
        .block();

    user.setProvider(newProvider);

    session.setAttribute("user", user);

    return "redirect:/home";
  }

  /**
   * Populate the view model with a list of provider firms.
   *
   * @param model     model to store attributes for the view.
   * @param user      the logged-in user.
   */
  private void populateProviderFirmDropdown(Model model, UserDetail user) {
    // Sort by primary firm first, then by name
    List<BaseProvider> providerFirms = user.getFirms().stream()
        .sorted(Comparator.comparing(BaseProvider::getName))
        .sorted(Comparator.comparing(BaseProvider::getIsPrimary).reversed())
        .toList();
    model.addAttribute("providerFirms", providerFirms);
  }

}
