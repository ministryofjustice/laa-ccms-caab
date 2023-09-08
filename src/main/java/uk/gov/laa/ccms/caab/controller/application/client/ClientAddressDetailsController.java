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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.service.DataService;

/**
 * Controller for handling address client details selection during the new application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({
    CLIENT_DETAILS
})
@SuppressWarnings({"unchecked"})
public class ClientAddressDetailsController {

  private final DataService dataService;

  private final ClientAddressDetailsValidator clientAddressDetailsValidator;

//  TODO Remove THIS
  @ModelAttribute(CLIENT_DETAILS)
  public ClientDetails getClientDetails() {
    return new ClientDetails();
  }

  /**
   * Handles the GET request for client address details page.
   *
   * @param clientDetails The details of the client.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("application/client/details/address")
  public String clientDetailsAddress(
          @ModelAttribute(CLIENT_DETAILS) ClientDetails clientDetails,
          Model model) {
    log.info("GET /application/client/details/address");

    return "application/client/address-client-details";
  }

  /**
   * Handles the client address results submission.
   *
   * @param clientDetails The details of the client.
   * @param bindingResult Validation result.
   * @param model The model for the view.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/application/client/details/address")
  public String clientDetailsAddress(
      @RequestParam String action,
      @ModelAttribute(CLIENT_DETAILS) ClientDetails clientDetails,
      BindingResult bindingResult,
      Model model) {
    log.info("POST /application/client/details/address");

    if ("find_address".equals(action)) {
      return "redirect:/application/client/details/address/search";

    } else if ("next".equals(action)) {
      clientAddressDetailsValidator.validate(clientDetails, bindingResult);
      model.addAttribute(CLIENT_DETAILS, clientDetails);

      if (bindingResult.hasErrors()) {
        return "application/client/address-client-details";
      }
    }

    log.info("clientDetails: {}", clientDetails);
    return "redirect:/application/client/details/equal-opportunities-monitoring";
  }
}
