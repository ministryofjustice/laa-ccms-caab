package uk.gov.laa.ccms.caab.controller.client;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CREATE_CLIENT_ADDRESS_FLOW;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
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
import uk.gov.laa.ccms.caab.service.AddressLookupUnavailableException;
import uk.gov.laa.ccms.caab.service.AddressService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.util.UkPostcode;

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
  private static final String ADDRESS_URL = "/application/client/details/address";

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
    session.setAttribute(CREATE_CLIENT_ADDRESS_FLOW, addressFlow);
    final ClientFormDataAddressDetails currentAddressDetails =
        addressFlow.getAddressDetails() != null
            ? addressFlow.getAddressDetails()
            : clientFlowFormData.getAddressDetails() != null
                ? clientFlowFormData.getAddressDetails()
                : addressDetails;
    currentAddressDetails.setVulnerableClient(
        clientFlowFormData.getBasicDetails().getVulnerableClient());
    currentAddressDetails.setClientFlowFormAction(clientFlowFormData.getAction());

    model.addAttribute("addressDetails", currentAddressDetails);
    model.addAttribute("addressFlow", addressFlow);
    model.addAttribute("componentEnabled", true);
    model.addAttribute("selectedAddress", addressFlow.getSelectedAddress());
    model.addAttribute("changeAddressUrl", ADDRESS_URL + "/search-again");
    model.addAttribute("manualEntryUrl", ADDRESS_URL + "/manual");
    model.addAttribute("searchAgainUrl", ADDRESS_URL + "/search-again");

    return "application/client/address-client-details";
  }

  @GetMapping("/application/client/details/address/confirm")
  public String confirmClientAddressSelection(
      @SessionAttribute(name = CREATE_CLIENT_ADDRESS_FLOW, required = false) @Nullable
          final AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow,
      final Model model) {
    if (addressFlow == null
        || addressFlow.getStep() != AddressLookupFlowData.Step.SELECTED_ADDRESS
        || addressFlow.getSelectedAddress() == null) {
      return "redirect:" + ADDRESS_URL;
    }
    addConfirmationModel(addressFlow, model);
    model.addAttribute("showClientCaption", true);
    model.addAttribute("backUrl", ADDRESS_URL);
    model.addAttribute("confirmUrl", ADDRESS_URL + "/confirm");
    model.addAttribute("changeUrl", ADDRESS_URL + "/search");
    model.addAttribute("searchAgainUrl", ADDRESS_URL + "/search-again");
    model.addAttribute("manualEntryUrl", ADDRESS_URL + "/manual");
    return "application/sections/address-confirmation";
  }

  @PostMapping("/application/client/details/address/confirm")
  public String confirmClientAddressSelection(
      @SessionAttribute(name = CREATE_CLIENT_ADDRESS_FLOW, required = false) @Nullable
          final AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow) {
    if (addressFlow == null) {
      return "redirect:" + ADDRESS_URL;
    }
    addressFlow.confirmSelectedAddress();
    return addressFlow.getStep() == AddressLookupFlowData.Step.CONFIRMED_ADDRESS
        ? "redirect:" + ADDRESS_URL
        : "redirect:" + ADDRESS_URL + "/search";
  }

  @GetMapping("/application/client/details/address/search-again")
  public String searchAgain(
      @SessionAttribute(name = CREATE_CLIENT_ADDRESS_FLOW, required = false) @Nullable
          final AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow) {
    if (addressFlow == null) {
      return "redirect:" + ADDRESS_URL;
    }
    addressFlow.searchAgain();
    return "redirect:" + ADDRESS_URL;
  }

  @GetMapping("/application/client/details/address/manual")
  public String enterManualAddress(
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) final ClientFlowFormData clientFlowFormData,
      @SessionAttribute(name = CREATE_CLIENT_ADDRESS_FLOW, required = false) @Nullable
          final AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow) {
    if (addressFlow == null) {
      return "redirect:" + ADDRESS_URL;
    }
    final ClientFormDataAddressDetails blankAddress = new ClientFormDataAddressDetails();
    blankAddress.setVulnerableClient(clientFlowFormData.getBasicDetails().getVulnerableClient());
    blankAddress.setClientFlowFormAction(clientFlowFormData.getAction());
    addressFlow.enterManualEntry(blankAddress);
    return "redirect:" + ADDRESS_URL;
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

    final AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow =
        getOrCreateCreateClientAddressFlow(session);
    addressDetails.setVulnerableClient(clientFlowFormData.getBasicDetails().getVulnerableClient());
    final boolean addressExempt =
        Boolean.TRUE.equals(addressDetails.getVulnerableClient())
            || Boolean.TRUE.equals(addressDetails.getNoFixedAbode());

    if (ACTION_FIND_ADDRESS.equals(action) && addressExempt) {
      clearAddressFields(addressDetails);
      addressFlow.searchAgain();
      addressFlow.setAddressDetails(addressDetails);
      clientFlowFormData.setAddressDetails(addressDetails);
      session.setAttribute(CREATE_CLIENT_ADDRESS_FLOW, addressFlow);
      return "redirect:/application/client/details/equal-opportunities-monitoring";
    }

    if (ACTION_FIND_ADDRESS.equals(action)) {
      addressFlow.beginSearch(addressDetails.getPostcode(), addressDetails.getHouseNameNumber());
      addressFlow.setAddressDetails(addressDetails);
      session.setAttribute(CREATE_CLIENT_ADDRESS_FLOW, addressFlow);
      findAddressValidator.validate(addressDetails, bindingResult);
    } else {
      if (!addressExempt && addressFlow.getSource() == AddressLookupFlowData.Source.SEARCHED) {
        if (addressFlow.getStep() == AddressLookupFlowData.Step.SELECTED_ADDRESS
            && addressFlow.getSelectedAddress() != null) {
          return "redirect:" + ADDRESS_URL + "/confirm";
        }
        if (addressFlow.getStep() != AddressLookupFlowData.Step.CONFIRMED_ADDRESS
            || addressFlow.getSelectedAddress() == null) {
          bindingResult.reject("address.selection.invalid", "Select and confirm an address");
        } else {
          addressService.updateClientFormDataAddressDetails(
              addressDetails, addressFlow.getSelectedAddress());
        }
      } else if (addressExempt) {
        clearAddressFields(addressDetails);
        addressFlow.searchAgain();
      } else if (addressFlow.getSource() == AddressLookupFlowData.Source.MANUAL
          && addressFlow.getStep() == AddressLookupFlowData.Step.MANUAL_ENTRY) {
        clientAddressDetailsValidator.validateForAddressComponent(addressDetails, bindingResult);
      } else {
        bindingResult.reject("address.selection.invalid", "Enter an address manually");
      }
    }

    if (bindingResult.hasErrors()) {
      populateDropdowns(model);
      addressFlow.setAddressDetails(addressDetails);
      session.setAttribute(CREATE_CLIENT_ADDRESS_FLOW, addressFlow);
      addAddressModel(model, addressFlow);
      return "application/client/address-client-details";
    }

    if (ACTION_FIND_ADDRESS.equals(action)) {
      // Search for addresses
      final ResultsDisplay<AddressResultRowDisplay> clientAddressSearchResults;
      try {
        clientAddressSearchResults = addressService.getAddresses(addressDetails.getPostcode());
      } catch (AddressLookupUnavailableException exception) {
        addressFlow.markLookupUnavailable();
        session.setAttribute(CREATE_CLIENT_ADDRESS_FLOW, addressFlow);
        return "redirect:/application/client/details/address/search";
      }

      ResultsDisplay<AddressResultRowDisplay> filteredAddressResults = clientAddressSearchResults;
      if (clientAddressSearchResults.getContent() != null
          && !clientAddressSearchResults.getContent().isEmpty()) {
        filteredAddressResults =
            addressService.filterByHouseNumber(
                addressDetails.getHouseNameNumber(), clientAddressSearchResults);
      }
      addressFlow.setSearchResults(filteredAddressResults);
      session.setAttribute(CREATE_CLIENT_ADDRESS_FLOW, addressFlow);
    } else {
      if (addressExempt) {
        addressFlow.searchAgain();
        addressFlow.setAddressDetails(addressDetails);
      } else if (addressFlow.getSource() != AddressLookupFlowData.Source.SEARCHED) {
        if ("GBR".equals(addressDetails.getCountry())) {
          addressDetails.setPostcode(UkPostcode.normalize(addressDetails.getPostcode()));
        }
        addressFlow.setSource(AddressLookupFlowData.Source.MANUAL);
      }
      addressFlow.setAddressDetails(addressDetails);
      clientFlowFormData.setAddressDetails(addressDetails);
      model.addAttribute(CLIENT_FLOW_FORM_DATA, clientFlowFormData);
      session.setAttribute(CREATE_CLIENT_ADDRESS_FLOW, addressFlow);
    }

    return ACTION_FIND_ADDRESS.equals(action)
        ? "redirect:/application/client/details/address/search"
        : "redirect:/application/client/details/equal-opportunities-monitoring";
  }

  private void addConfirmationModel(
      final AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow, final Model model) {
    model.addAttribute("selectedAddress", addressFlow.getSelectedAddress());
  }

  private void addAddressModel(
      final Model model, final AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow) {
    model.addAttribute("addressFlow", addressFlow);
    model.addAttribute("componentEnabled", true);
    model.addAttribute("selectedAddress", addressFlow.getSelectedAddress());
    model.addAttribute("changeAddressUrl", ADDRESS_URL + "/search-again");
    model.addAttribute("manualEntryUrl", ADDRESS_URL + "/manual");
    model.addAttribute("searchAgainUrl", ADDRESS_URL + "/search-again");
  }

  private void populateDropdowns(Model model) {
    new DropdownBuilder(model).addDropdown("countries", lookupService.getCountries()).build();
  }

  private void clearAddressFields(final ClientFormDataAddressDetails addressDetails) {
    addressDetails.setCountry(null);
    addressDetails.setHouseNameNumber(null);
    addressDetails.setPostcode(null);
    addressDetails.setAddressLine1(null);
    addressDetails.setAddressLine2(null);
    addressDetails.setCityTown(null);
    addressDetails.setCounty(null);
    addressDetails.setAddressSearch(null);
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
