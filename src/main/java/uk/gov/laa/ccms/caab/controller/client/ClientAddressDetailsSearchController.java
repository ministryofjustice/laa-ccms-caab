package uk.gov.laa.ccms.caab.controller.client;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ADDRESS_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CREATE_CLIENT_ADDRESS_FLOW;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.AddressLookupFlowData;
import uk.gov.laa.ccms.caab.bean.AddressSearchFormData;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.AddressSearchValidator;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.service.AddressService;

/** Controller for handling address client details selection during the new application process. */
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
   * @param addressFlow the address lookup flow data.
   * @param addressSearch The address search model containing the uprn.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("/application/client/details/address/search")
  public String clientDetailsAddressSearch(
      @SessionAttribute(name = CREATE_CLIENT_ADDRESS_FLOW, required = false) @Nullable
          final AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow,
      @ModelAttribute("addressSearch") final AddressSearchFormData addressSearch,
      final Model model) {

    if (addressFlow == null
        || (addressFlow.getSearchResults() == null
            && addressFlow.getStep() != AddressLookupFlowData.Step.LOOKUP_UNAVAILABLE)
        || (addressFlow.getStep() != AddressLookupFlowData.Step.RESULTS
            && addressFlow.getStep() != AddressLookupFlowData.Step.NO_RESULTS
            && addressFlow.getStep() != AddressLookupFlowData.Step.LOOKUP_UNAVAILABLE)) {
      return "redirect:/application/client/details/address";
    }
    model.addAttribute(ADDRESS_SEARCH_RESULTS, addressFlow.getSearchResults());
    model.addAttribute("addressFlow", addressFlow);
    addSearchTermsToModel(addressFlow, model);

    return "application/client/address-client-search-results";
  }

  /**
   * Handles the client address results submission.
   *
   * @param addressFlow the address lookup flow data.
   * @param clientFlowFormData The data for create client flow.
   * @param addressSearch The address search model containing the uprn.
   * @param model The model for the view.
   * @param bindingResult Validation result.
   * @param session The session data for the endpoint.
   * @return A redirect string to the populated client address details page.
   */
  @PostMapping("/application/client/details/address/search")
  public String clientDetailsAddressSearch(
      @SessionAttribute(name = CREATE_CLIENT_ADDRESS_FLOW, required = false) @Nullable
          AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow,
      @SessionAttribute(name = CLIENT_FLOW_FORM_DATA, required = false) @Nullable
          ClientFlowFormData clientFlowFormData,
      @ModelAttribute("addressSearch") AddressSearchFormData addressSearch,
      BindingResult bindingResult,
      Model model,
      HttpSession session) {

    if (addressFlow == null
        || clientFlowFormData == null
        || addressFlow.getStep() != AddressLookupFlowData.Step.RESULTS
        || addressFlow.getSearchResults() == null) {
      return "redirect:/application/client/details/address";
    }

    // validate if an address is selected
    addressSearchValidator.validateSelection(
        addressSearch, addressFlow.getSearchResults(), bindingResult);
    if (bindingResult.hasErrors()) {
      model.addAttribute(ADDRESS_SEARCH_RESULTS, addressFlow.getSearchResults());
      model.addAttribute("addressFlow", addressFlow);
      addSearchTermsToModel(addressFlow, model);
      return "application/client/address-client-search-results";
    }

    // Add the address to the client session flow data
    final AddressResultRowDisplay selectedAddress =
        addressService.getSelectedAddress(addressSearch.getUprn(), addressFlow.getSearchResults());
    if (selectedAddress == null) {
      bindingResult.rejectValue("uprn", "invalid.selection", "Select an address from the results");
      model.addAttribute(ADDRESS_SEARCH_RESULTS, addressFlow.getSearchResults());
      model.addAttribute("addressFlow", addressFlow);
      addSearchTermsToModel(addressFlow, model);
      return "application/client/address-client-search-results";
    }
    final ClientFormDataAddressDetails addressDetails =
        addressFlow.getAddressDetails() != null
            ? addressFlow.getAddressDetails()
            : clientFlowFormData.getAddressDetails() != null
                ? clientFlowFormData.getAddressDetails()
                : new ClientFormDataAddressDetails();
    addressService.updateClientFormDataAddressDetails(addressDetails, selectedAddress);
    addressDetails.setVulnerableClient(clientFlowFormData.getBasicDetails().getVulnerableClient());
    addressDetails.setClientFlowFormAction(clientFlowFormData.getAction());
    addressFlow.setAddressDetails(addressDetails);
    clientFlowFormData.setAddressDetails(addressDetails);
    addressFlow.setSelectedAddress(selectedAddress);
    addressFlow.confirmSelectedAddress();

    session.setAttribute(CREATE_CLIENT_ADDRESS_FLOW, addressFlow);

    return "redirect:/application/client/details/address";
  }

  private void addSearchTermsToModel(
      final AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow, final Model model) {
    model.addAttribute("originalPostcode", addressFlow.getOriginalPostcode());
    model.addAttribute("originalBuildingSearch", addressFlow.getOriginalBuildingSearch());
    model.addAttribute("searchAgainUrl", "/application/client/details/address/search-again");
    model.addAttribute("manualEntryUrl", "/application/client/details/address/manual");
  }
}
