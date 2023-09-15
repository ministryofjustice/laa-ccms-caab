package uk.gov.laa.ccms.caab.controller.application.client;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_ADDRESS_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_DETAILS;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressSearchValidator;
import uk.gov.laa.ccms.caab.model.ClientAddressResultsDisplay;
import uk.gov.laa.ccms.caab.service.AddressService;

/**
 * Controller for handling address client details selection during the new application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({
    CLIENT_DETAILS
})
public class ClientAddressDetailsSearchController {

  private final AddressService addressService;

  private final ClientAddressSearchValidator clientAddressSearchValidator;

  /**
   * Handles the GET request for client address details page.
   *
   * @param clientDetails The details of the client.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("application/client/details/address/search")
  public String clientDetailsAddressSearch(
      @ModelAttribute(CLIENT_DETAILS) ClientDetails clientDetails,
      Model model, HttpSession session) {
    log.info("GET /application/client/details/address/search");

    ClientAddressResultsDisplay clientAddressSearchResults =
        addressService.getAddresses(clientDetails.getPostcode());

    //Check for no address data return from ordinance survey
    if (clientAddressSearchResults.getContent() == null) {
      clientDetails.setUprn(null);
      clientDetails.setNoAddressLookup(true);
      model.addAttribute(CLIENT_DETAILS, clientDetails);

      return "redirect:/application/client/details/address";
    }

    clientAddressSearchResults = addressService.filterByHouseNumber(
        clientDetails.getHouseNameNumber(), clientAddressSearchResults);

    session.setAttribute(CLIENT_ADDRESS_SEARCH_RESULTS, clientAddressSearchResults);
    model.addAttribute(CLIENT_ADDRESS_SEARCH_RESULTS, clientAddressSearchResults);

    return "application/client/address-client-search-results";
  }

  /**
   * Handles the client address results submission.
   *
   * @param clientDetails The details of the client.
   * @param model The model for the view.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/application/client/details/address/search")
  public String clientDetailsAddressSearch(
      @SessionAttribute(CLIENT_ADDRESS_SEARCH_RESULTS) ClientAddressResultsDisplay
          clientAddressSearchResults,
      @ModelAttribute(CLIENT_DETAILS) ClientDetails clientDetails,
      Model model,
      BindingResult bindingResult,
      HttpSession session) {
    log.info("POST /application/client/details/address/search");

    clientAddressSearchValidator.validate(clientDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      model.addAttribute(CLIENT_ADDRESS_SEARCH_RESULTS, clientAddressSearchResults);
      return "application/client/address-client-search-results";
    }

    addressService.addAddressToClientDetails(
        clientDetails.getUprn(), clientAddressSearchResults, clientDetails);

    //Cleanup
    session.removeAttribute(CLIENT_ADDRESS_SEARCH_RESULTS);
    clientDetails.setUprn(null);

    model.addAttribute(CLIENT_DETAILS, clientDetails);

    return "redirect:/application/client/details/address";
  }

}
