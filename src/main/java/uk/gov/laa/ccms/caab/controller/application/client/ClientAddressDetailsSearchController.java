package uk.gov.laa.ccms.caab.controller.application.client;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ADDRESS_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;

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
import uk.gov.laa.ccms.caab.bean.AddressSearchFormData;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.validators.client.AddressSearchValidator;
import uk.gov.laa.ccms.caab.model.AddressResultsDisplay;
import uk.gov.laa.ccms.caab.service.AddressService;

/**
 * Controller for handling address client details selection during the new application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({CLIENT_FLOW_FORM_DATA})
public class ClientAddressDetailsSearchController {

  private final AddressService addressService;

  private final AddressSearchValidator addressSearchValidator;

  @ModelAttribute("addressSearch")
  public AddressSearchFormData getAddressSearch() {
    return new AddressSearchFormData();
  }

  /**
   * Handles the GET request for client address details page.
   *
   * @param clientAddressSearchResults the address results from ordinance survey api.
   * @param addressSearch The address search model containing the uprn.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("/application/client/details/address/search")
  public String clientDetailsAddressSearch(
      @SessionAttribute(ADDRESS_SEARCH_RESULTS)
      AddressResultsDisplay clientAddressSearchResults,
      @ModelAttribute("addressSearch") AddressSearchFormData addressSearch,
      Model model) {

    model.addAttribute(ADDRESS_SEARCH_RESULTS, clientAddressSearchResults);

    return "application/client/address-client-search-results";
  }

  /**
   * Handles the client address results submission.
   *
   * @param clientAddressSearchResults the address results from ordinance survey api.
   * @param clientFlowFormData The data for create client flow.
   * @param addressSearch The address search model containing the uprn.
   * @param model The model for the view.
   * @param bindingResult Validation result.
   * @param session The session data for the endpoint.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/application/client/details/address/search")
  public String clientDetailsAddressSearch(
      @SessionAttribute(ADDRESS_SEARCH_RESULTS) AddressResultsDisplay
          clientAddressSearchResults,
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) ClientFlowFormData clientFlowFormData,
      @ModelAttribute("addressSearch") AddressSearchFormData addressSearch,
      Model model,
      BindingResult bindingResult,
      HttpSession session) {

    //validate if an address is selected
    addressSearchValidator.validate(addressSearch, bindingResult);
    if (bindingResult.hasErrors()) {
      model.addAttribute(ADDRESS_SEARCH_RESULTS, clientAddressSearchResults);
      return "application/client/address-client-search-results";
    }

    //Add the address to the client session flow data
    addressService.addAddressToClientDetails(
        addressSearch.getUprn(),
        clientAddressSearchResults,
        clientFlowFormData.getAddressDetails());

    //Cleanup
    session.removeAttribute(ADDRESS_SEARCH_RESULTS);

    return "redirect:/application/client/details/address";
  }

}
