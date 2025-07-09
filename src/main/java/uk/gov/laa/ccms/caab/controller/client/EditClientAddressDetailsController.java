package uk.gov.laa.ccms.caab.controller.client;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.FindAddressValidator;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.service.AddressService;
import uk.gov.laa.ccms.caab.service.LookupService;

/**
 * Controller for handling edit address client details selection during the new application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({CLIENT_FLOW_FORM_DATA})
public class EditClientAddressDetailsController {

  private final AddressService addressService;

  private final LookupService lookupService;

  private final ClientAddressDetailsValidator clientAddressDetailsValidator;

  private final FindAddressValidator findAddressValidator;

  private static final String ACTION_FIND_ADDRESS = "find_address";

  @ModelAttribute("addressDetails")
  public ClientFormDataAddressDetails getAddressDetails() {
    return new ClientFormDataAddressDetails();
  }

  /**
   * Handles the GET request for edit client address details page.
   *
   * @param clientFlowFormData The data for create client flow.
   * @param model The model for the view.
   * @return The view name for the client address details page
   */
  @GetMapping("/{caseContext}/sections/client/details/address")
  public String getEditClientDetailsAddress(
      @PathVariable("caseContext") final CaseContext caseContext,
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) final ClientFlowFormData clientFlowFormData,
      final Model model) {

    populateDropdowns(model);

    final ClientFormDataAddressDetails addressDetails = clientFlowFormData.getAddressDetails();
    addressDetails.setClientFlowFormAction(clientFlowFormData.getAction());

    model.addAttribute("addressDetails", addressDetails);

    return "application/sections/client-address-details";
  }

  /**
   * Handles the client address results submission.
   *
   * @param action The button action performed by the user.
   * @param clientFlowFormData The data for create client flow.
   * @param addressDetails The address details of the client.
   * @param bindingResult Validation result.
   * @param model The model for the view.
   * @param session The session data for the endpoint.
   * @return A redirect string to the client equal opportunities monitoring page.
   */
  @PostMapping("/{caseContext}/sections/client/details/address")
  public String postEditClientDetailsAddress(
      @PathVariable("caseContext") final CaseContext caseContext,
      @RequestParam final String action,
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) final ClientFlowFormData clientFlowFormData,
      @ModelAttribute("addressDetails") final ClientFormDataAddressDetails addressDetails,
      final BindingResult bindingResult,
      final Model model,
      final HttpSession session) {

    if (ACTION_FIND_ADDRESS.equals(action)) {
      findAddressValidator.validate(addressDetails, bindingResult);
    } else {
      clientAddressDetailsValidator.validate(addressDetails, bindingResult);
    }

    if (bindingResult.hasErrors()) {
      populateDropdowns(model);
      return "application/sections/client-address-details";
    }

    clientFlowFormData.setAddressDetails(addressDetails);
    model.addAttribute(CLIENT_FLOW_FORM_DATA, clientFlowFormData);

    if (ACTION_FIND_ADDRESS.equals(action)) {
      // Search for addresses
      ResultsDisplay<AddressResultRowDisplay> clientAddressSearchResults =
          addressService.getAddresses(clientFlowFormData.getAddressDetails().getPostcode());

      if (clientAddressSearchResults.getContent() == null) {
        bindingResult.reject(
            "address.none", "Your input for address details has not returned any results");
      } else {
        clientAddressSearchResults =
            addressService.filterByHouseNumber(
                clientFlowFormData.getAddressDetails().getHouseNameNumber(),
                clientAddressSearchResults);
        session.setAttribute(ADDRESS_SEARCH_RESULTS, clientAddressSearchResults);
      }

      if (bindingResult.hasErrors()) {
        populateDropdowns(model);
        return "application/sections/client-address-details";
      }
    }

    return ACTION_FIND_ADDRESS.equals(action)
        ? "redirect:/%s/sections/client/details/address/search"
            .formatted(caseContext.getPathValue())
        : "redirect:/%s/sections/client/details/summary".formatted(caseContext.getPathValue());
  }

  private void populateDropdowns(final Model model) {
    new DropdownBuilder(model).addDropdown("countries", lookupService.getCountries()).build();
  }
}
