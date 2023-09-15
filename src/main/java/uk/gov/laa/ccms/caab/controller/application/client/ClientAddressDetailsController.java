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
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsFindAddressValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;

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

  private final CommonLookupService commonLookupService;

  private final ClientAddressDetailsValidator clientAddressDetailsValidator;

  private final ClientAddressDetailsFindAddressValidator clientAddressDetailsFindAddressValidator;

  private static final String ACTION_FIND_ADDRESS = "find_address";

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
          Model model,
          BindingResult bindingResult) {
    log.info("GET /application/client/details/address");

    populateDropdowns(model);

    //when accessed via the redirect from /application/client/details/address/search
    if (clientDetails.isNoAddressLookup()) {
      clientAddressDetailsFindAddressValidator.validate(clientDetails, bindingResult);
      clientDetails.setNoAddressLookup(false);
      model.addAttribute(CLIENT_DETAILS, clientDetails);
    }

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

    model.addAttribute(CLIENT_DETAILS, clientDetails);

    if (ACTION_FIND_ADDRESS.equals(action)) {
      clientAddressDetailsFindAddressValidator.validate(clientDetails, bindingResult);
    } else {
      clientAddressDetailsValidator.validate(clientDetails, bindingResult);
    }

    if (bindingResult.hasErrors()) {
      populateDropdowns(model);
      return "application/client/address-client-details";
    }

    return ACTION_FIND_ADDRESS.equals(action)
        ? "redirect:/application/client/details/address/search"
        : "redirect:/application/client/details/equal-opportunities-monitoring";
  }

  private void populateDropdowns(Model model) {
    Mono<CommonLookupDetail> countriesMono = commonLookupService.getCountries();
    model.addAttribute("countries", countriesMono.block().getContent());
  }
}
