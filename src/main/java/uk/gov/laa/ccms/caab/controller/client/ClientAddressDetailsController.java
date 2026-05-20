package uk.gov.laa.ccms.caab.controller.client;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CREATE_CLIENT_ADDRESS_FLOW;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.AddressLookupFlowData;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.FindAddressValidator;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.service.AddressService;
import uk.gov.laa.ccms.caab.service.LookupService;

/** Controller for handling address client details selection during the new application process. */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({CLIENT_FLOW_FORM_DATA})
@SuppressWarnings({"unchecked"})
public class ClientAddressDetailsController {

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
   * Handles the GET request for client address details page.
   *
   * @param clientFlowFormData The data for create client flow.
   * @param addressDetails The address details of the client.
   * @param model The model for the view.
   * @return The view name for the client address details page
   */
  @GetMapping("/application/client/details/address")
  public String clientDetailsAddress(
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) final ClientFlowFormData clientFlowFormData,
      @ModelAttribute("addressDetails") final ClientFormDataAddressDetails addressDetails,
      final Model model,
      final HttpSession session) {

    populateDropdowns(model);

    final AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow =
        getOrCreateCreateClientAddressFlow(session);
    final AddressResultRowDisplay selectedAddress = addressFlow.getSelectedAddress();
    final ClientFormDataAddressDetails currentAddressDetails =
        selectedAddress != null && clientFlowFormData.getAddressDetails() != null
            ? clientFlowFormData.getAddressDetails()
            : addressDetails;
    currentAddressDetails.setVulnerableClient(
        clientFlowFormData.getBasicDetails().getVulnerableClient());
    currentAddressDetails.setClientFlowFormAction(clientFlowFormData.getAction());

    if (selectedAddress != null) {
      addressService.updateClientFormDataAddressDetails(currentAddressDetails, selectedAddress);
      clientFlowFormData.setAddressDetails(currentAddressDetails);
      addressFlow.setSelectedAddress(null);
      session.setAttribute(CREATE_CLIENT_ADDRESS_FLOW, addressFlow);
    }

    if (clientFlowFormData.getAddressDetails() != null) {
      model.addAttribute("addressDetails", clientFlowFormData.getAddressDetails());
    }

    return "application/client/address-client-details";
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
  @PostMapping("/application/client/details/address")
  public String clientDetailsAddress(
      @RequestParam(required = false) String action,
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) ClientFlowFormData clientFlowFormData,
      @Validated @ModelAttribute("addressDetails") ClientFormDataAddressDetails addressDetails,
      BindingResult bindingResult,
      Model model,
      HttpSession session) {

    if (ACTION_FIND_ADDRESS.equals(action)) {
      findAddressValidator.validate(addressDetails, bindingResult);
    } else {
      clientAddressDetailsValidator.validate(addressDetails, bindingResult);
    }

    if (bindingResult.hasErrors()) {
      populateDropdowns(model);
      return "application/client/address-client-details";
    }

    clientFlowFormData.setAddressDetails(addressDetails);
    model.addAttribute(CLIENT_FLOW_FORM_DATA, clientFlowFormData);

    if (ACTION_FIND_ADDRESS.equals(action)) {
      // Search for addresses
      ResultsDisplay<AddressResultRowDisplay> clientAddressSearchResults =
          addressService.getAddresses(clientFlowFormData.getAddressDetails().getPostcode());

      if (clientAddressSearchResults.getContent() == null
          || clientAddressSearchResults.getContent().isEmpty()) {
        bindingResult.reject(
            "address.none", "Your input for address details has not returned any results");
      } else {
        clientAddressSearchResults =
            addressService.filterByHouseNumber(
                clientFlowFormData.getAddressDetails().getHouseNameNumber(),
                clientAddressSearchResults);
        final AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow =
            getOrCreateCreateClientAddressFlow(session);
        addressFlow.setSearchResults(clientAddressSearchResults);
        session.setAttribute(CREATE_CLIENT_ADDRESS_FLOW, addressFlow);
      }

      if (bindingResult.hasErrors()) {
        populateDropdowns(model);
        return "application/client/address-client-details";
      }
    }

    return ACTION_FIND_ADDRESS.equals(action)
        ? "redirect:/application/client/details/address/search"
        : "redirect:/application/client/details/equal-opportunities-monitoring";
  }

  private void populateDropdowns(Model model) {
    new DropdownBuilder(model).addDropdown("countries", lookupService.getCountries()).build();
  }

  @SuppressWarnings("unchecked")
  private AddressLookupFlowData<ClientFormDataAddressDetails> getOrCreateCreateClientAddressFlow(
      final HttpSession session) {
    final Object sessionValue = session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW);
    if (sessionValue instanceof AddressLookupFlowData<?> addressFlow) {
      return (AddressLookupFlowData<ClientFormDataAddressDetails>) addressFlow;
    }
    return new AddressLookupFlowData<>("create-client");
  }
}
