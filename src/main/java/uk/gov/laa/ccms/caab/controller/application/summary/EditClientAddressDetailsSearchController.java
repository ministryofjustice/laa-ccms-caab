package uk.gov.laa.ccms.caab.controller.application.summary;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_ADDRESS_SEARCH_RESULTS;
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
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressSearch;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressSearchValidator;
import uk.gov.laa.ccms.caab.model.ClientAddressResultsDisplay;
import uk.gov.laa.ccms.caab.service.AddressService;

/**
 * Controller for handling edit address client details selection
 * during the new application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({CLIENT_FLOW_FORM_DATA})
public class EditClientAddressDetailsSearchController {

  private final AddressService addressService;

  private final ClientAddressSearchValidator clientAddressSearchValidator;

  @ModelAttribute("addressSearch")
  public ClientFormDataAddressSearch getAddressSearch() {
    return new ClientFormDataAddressSearch();
  }

  /**
   * Handles the GET request for edit client address search page.
   *
   * @param clientAddressSearchResults the address results from ordinance survey api.
   * @param addressSearch The address search model containing the uprn.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("/application/summary/client/details/address/search")
  public String clientDetailsAddressSearch(
      @SessionAttribute(CLIENT_ADDRESS_SEARCH_RESULTS)
        ClientAddressResultsDisplay clientAddressSearchResults,
      @ModelAttribute("addressSearch") ClientFormDataAddressSearch addressSearch,
      Model model) {

    model.addAttribute(CLIENT_ADDRESS_SEARCH_RESULTS, clientAddressSearchResults);

    return "application/summary/client-address-search-results";
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
  @PostMapping("/application/summary/client/details/address/search")
  public String clientDetailsAddressSearch(
      @SessionAttribute(CLIENT_ADDRESS_SEARCH_RESULTS) ClientAddressResultsDisplay
          clientAddressSearchResults,
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) ClientFlowFormData clientFlowFormData,
      @ModelAttribute("addressSearch") ClientFormDataAddressSearch addressSearch,
      Model model,
      BindingResult bindingResult,
      HttpSession session) {

    //validate if an address is selected
    clientAddressSearchValidator.validate(addressSearch, bindingResult);
    if (bindingResult.hasErrors()) {
      model.addAttribute(CLIENT_ADDRESS_SEARCH_RESULTS, clientAddressSearchResults);
      return "application/summary/client-address-search-results";
    }

    //Add the address to the client session flow data
    addressService.addAddressToClientDetails(
        addressSearch.getUprn(),
        clientAddressSearchResults,
        clientFlowFormData.getAddressDetails());

    //Cleanup
    session.removeAttribute(CLIENT_ADDRESS_SEARCH_RESULTS);

    return "redirect:/application/summary/client/details/address";
  }

}
