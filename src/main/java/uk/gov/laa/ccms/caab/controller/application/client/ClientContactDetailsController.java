package uk.gov.laa.ccms.caab.controller.application.client;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_DETAILS;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientContactDetailsValidator;
import uk.gov.laa.ccms.caab.service.DataService;

/**
 * Controller for handling contact client details selection during the new application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({
    CLIENT_DETAILS
})
@SuppressWarnings({"unchecked"})
public class ClientContactDetailsController {

  private final DataService dataService;

  private final ClientContactDetailsValidator clientContactDetailsValidator;

  /**
   * Handles the GET request for client contact details page.
   *
   * @param clientDetails The details of the client.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("application/client/details/contact")
  public String clientDetailsContact(
          @ModelAttribute(CLIENT_DETAILS) ClientDetails clientDetails,
          Model model) {
    log.info("GET /application/client/details/contact");

    return "application/client/contact-client-details";
  }

  /**
   * Handles the client contact details results submission.
   *
   * @param clientDetails The details of the client.
   * @param bindingResult Validation result.
   * @param model The model for the view.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/application/client/details/contact")
  public String clientDetailsContact(
      @ModelAttribute(CLIENT_DETAILS) ClientDetails clientDetails,
      BindingResult bindingResult,
      Model model) {
    log.info("POST /application/client/details/contact");

    clientContactDetailsValidator.validate(clientDetails, bindingResult);
    model.addAttribute(CLIENT_DETAILS, clientDetails);

    if (bindingResult.hasErrors()) {

      return "application/client/contact-client-details";
    }

    log.info("clientDetails: {}", clientDetails);
    return "redirect:/application/client/details/address";
  }

  private void populateDropdowns(Model model) {

  }
}
