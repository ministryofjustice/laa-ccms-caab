package uk.gov.laa.ccms.caab.controller.application.summary;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ADDRESS_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.AddressSearchFormData;
import uk.gov.laa.ccms.caab.bean.validators.client.AddressSearchValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.CorrespondenceAddressValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.FindAddressValidator;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.model.AddressResultsDisplay;
import uk.gov.laa.ccms.caab.service.AddressService;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller for the application's general details section.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class EditGeneralDetailsSectionController {

  private final ApplicationService applicationService;
  private final AddressService addressService;
  private final LookupService lookupService;

  private final FindAddressValidator findAddressValidator;
  private final AddressSearchValidator addressSearchValidator;
  private final CorrespondenceAddressValidator correspondenceAddressValidator;

  private static final String ACTION_FIND_ADDRESS = "find_address";

  /**
   * Handles the GET request for editing an application's correspondence address.
   *
   * @param applicationId The id of the application
   * @param model The model for the view.
   * @return The view name for the application summary page.
   */
  @GetMapping("/application/summary/correspondence-address")
  public String correspondenceDetails(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      final Model model,
      final HttpSession session) {

    if (session.getAttribute("addressDetails") == null) {
      final AddressFormData addressDetails =
          applicationService.getCorrespondenceAddressFormData(applicationId);
      model.addAttribute("addressDetails", addressDetails);
      session.setAttribute("addressDetails", addressDetails);
    } else {
      final AddressFormData addressDetails =
          (AddressFormData) session.getAttribute("addressDetails");
      model.addAttribute("addressDetails", addressDetails);
    }

    populateDropdowns(model);

    return "application/summary/correspondence-address-details";
  }

  /**
   * Handles the POST request for editing an application's correspondence address.
   *
   * @param action The action performed, which button pressed.
   *               Either "find_address" or "save_address".
   * @param applicationId The id of the application
   * @param addressDetails The address details model.
   * @param user The user details.
   * @param model The model for the view.
   * @param bindingResult Validation result.
   * @param session The session data for the endpoint.
   * @return The view name for the application summary page.
   */
  @PostMapping("/application/summary/correspondence-address")
  public String updateCorrespondenceDetails(
      @RequestParam final String action,
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @ModelAttribute("addressDetails") final AddressFormData addressDetails,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final Model model,
      final BindingResult bindingResult,
      final HttpSession session) {

    if (ACTION_FIND_ADDRESS.equals(action)) {
      findAddressValidator.validate(addressDetails, bindingResult);
    } else {
      correspondenceAddressValidator.validate(addressDetails, bindingResult);
    }

    if (bindingResult.hasErrors()) {
      populateDropdowns(model);
      session.setAttribute("addressDetails", addressDetails);
      return "application/summary/correspondence-address-details";
    }

    if (ACTION_FIND_ADDRESS.equals(action)) {
      AddressResultsDisplay addressSearchResults =
          addressService.getAddresses(addressDetails.getPostcode());

      if (addressSearchResults.getContent() == null) {
        bindingResult.reject(
            "address.none",
            "Your input for address details has not returned any results.");
      } else {
        addressSearchResults = addressService.filterByHouseNumber(
            addressDetails.getHouseNameNumber(),
            addressSearchResults);
        session.setAttribute(ADDRESS_SEARCH_RESULTS, addressSearchResults);
      }

      if (bindingResult.hasErrors()) {
        populateDropdowns(model);
        session.setAttribute("addressDetails", addressDetails);
        return "application/summary/correspondence-address-details";
      }

      session.setAttribute("addressDetails", addressDetails);
      return "redirect:/application/summary/correspondence-address/search";

    } else {
      applicationService.updateCorrespondenceAddress(applicationId, addressDetails, user);
      session.removeAttribute("addressDetails");

      return "redirect:/application/summary/linked-cases";
    }
  }

  /**
   * Handles the GET request for edit correspondence address search page.
   *
   * @param addressResultsDisplay the address results from ordinance survey api.
   * @param addressSearch The address search model containing the uprn.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("/application/summary/correspondence-address/search")
  public String correspondenceAddressGet(
      @SessionAttribute(ADDRESS_SEARCH_RESULTS)
      final AddressResultsDisplay addressResultsDisplay,
      @ModelAttribute("addressSearch") final AddressSearchFormData addressSearch,
      final Model model) {

    model.addAttribute(ADDRESS_SEARCH_RESULTS, addressResultsDisplay);
    model.addAttribute("formAction",
        "application/summary/correspondence-address/search");
    model.addAttribute("backLink",
        "/application/summary/correspondence-address");

    return "application/summary/address-search-results";
  }

  /**
   * Handles the correspondence address results submission.
   *
   * @param addressResultsDisplay the address results from ordinance survey api.
   * @param addressSearch The address search model containing the uprn.
   * @param model The model for the view.
   * @param bindingResult Validation result.
   * @param session The session data for the endpoint.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/application/summary/correspondence-address/search")
  public String correspondenceAddressSearchPost(
      @SessionAttribute(ADDRESS_SEARCH_RESULTS) final AddressResultsDisplay
          addressResultsDisplay,
      @SessionAttribute("addressDetails") final AddressFormData addressDetails,
      @ModelAttribute("addressSearch") final AddressSearchFormData addressSearch,
      final Model model,
      final BindingResult bindingResult,
      final HttpSession session) {

    //validate if an address is selected
    addressSearchValidator.validate(addressSearch, bindingResult);
    if (bindingResult.hasErrors()) {
      model.addAttribute(ADDRESS_SEARCH_RESULTS, addressResultsDisplay);
      model.addAttribute("formAction",
          "application/summary/correspondence-address/search");
      model.addAttribute("backLink",
          "/application/summary/correspondence-address");
      return "application/summary/address-search-results";
    }

    //Cleanup
    session.removeAttribute(ADDRESS_SEARCH_RESULTS);

    addressService.filterAndUpdateAddressFormData(
            addressSearch.getUprn(),
            addressResultsDisplay, addressDetails);

    return "redirect:/application/summary/correspondence-address";
  }

  private void populateDropdowns(final Model model) {
    new DropdownBuilder(model)
        .addDropdown("countries",
            lookupService.getCountries())
        .addDropdown("caseAddressOptions",
            lookupService.getCaseAddressOptions())
        .build();
  }
}
