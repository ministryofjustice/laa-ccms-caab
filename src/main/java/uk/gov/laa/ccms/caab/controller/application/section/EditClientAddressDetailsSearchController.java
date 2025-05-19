package uk.gov.laa.ccms.caab.controller.application.section;

import static uk.gov.laa.ccms.caab.constants.ContextConstants.CONTEXT_NAME;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.AddressSearchFormData;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.validators.client.AddressSearchValidator;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
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

  private final AddressSearchValidator addressSearchValidator;

  @ModelAttribute("addressSearch")
  public AddressSearchFormData getAddressSearch() {
    return new AddressSearchFormData();
  }

  /**
   * Handles the GET request for edit client address search page.
   *
   * @param clientAddressSearchResults the address results from ordinance survey api.
   * @param addressSearch The address search model containing the uprn.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("/{" + CONTEXT_NAME + "}/sections/client/details/address/search")
  public String clientDetailsAddressSearch(
      @PathVariable(CONTEXT_NAME) final String caseContext,
      @SessionAttribute(ADDRESS_SEARCH_RESULTS)
      final ResultsDisplay<AddressResultRowDisplay> clientAddressSearchResults,
      @ModelAttribute("addressSearch") final AddressSearchFormData addressSearch,
      final Model model) {

    model.addAttribute(ADDRESS_SEARCH_RESULTS, clientAddressSearchResults);

    return "application/sections/client-address-search-results";
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
  @PostMapping("/{" + CONTEXT_NAME + "}/sections/client/details/address/search")
  public String clientDetailsAddressSearch(
      @PathVariable(CONTEXT_NAME) final String caseContext,
      @SessionAttribute(ADDRESS_SEARCH_RESULTS) final ResultsDisplay<AddressResultRowDisplay>
          clientAddressSearchResults,
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) final ClientFlowFormData clientFlowFormData,
      @ModelAttribute("addressSearch") final AddressSearchFormData addressSearch,
      final Model model,
      final BindingResult bindingResult,
      final HttpSession session) {

    //validate if an address is selected
    addressSearchValidator.validate(addressSearch, bindingResult);
    if (bindingResult.hasErrors()) {
      model.addAttribute(ADDRESS_SEARCH_RESULTS, clientAddressSearchResults);
      return "application/sections/client-address-search-results";
    }

    //Add the address to the client session flow data
    addressService.addAddressToClientDetails(
        addressSearch.getUprn(),
        clientAddressSearchResults,
        clientFlowFormData.getAddressDetails());

    //Cleanup
    session.removeAttribute(ADDRESS_SEARCH_RESULTS);

    return "redirect:/%s/sections/client/details/address".formatted(caseContext);
  }

}
