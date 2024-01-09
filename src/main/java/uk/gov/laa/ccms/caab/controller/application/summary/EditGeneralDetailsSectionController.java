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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.AddressSearchFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.LinkedCaseValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.AddressSearchValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.CorrespondenceAddressValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.FindAddressValidator;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.LinkedCaseResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.service.AddressService;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.data.model.ProviderDetail;
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
  private final ProviderService providerService;

  private final FindAddressValidator findAddressValidator;
  private final AddressSearchValidator addressSearchValidator;
  private final CorrespondenceAddressValidator correspondenceAddressValidator;
  private final LinkedCaseValidator linkedCaseValidator;

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

    populateCorrespondenceAddressDropdowns(model);

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
      populateCorrespondenceAddressDropdowns(model);
      session.setAttribute("addressDetails", addressDetails);
      return "application/summary/correspondence-address-details";
    }

    if (ACTION_FIND_ADDRESS.equals(action)) {
      ResultsDisplay<AddressResultRowDisplay> addressSearchResults =
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
        populateCorrespondenceAddressDropdowns(model);
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

  private void populateCorrespondenceAddressDropdowns(final Model model) {
    new DropdownBuilder(model)
        .addDropdown("countries",
            lookupService.getCountries())
        .addDropdown("caseAddressOptions",
            lookupService.getCaseAddressOptions())
        .build();
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
      final ResultsDisplay<AddressResultRowDisplay> addressResultsDisplay,
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
      @SessionAttribute(ADDRESS_SEARCH_RESULTS) final ResultsDisplay<AddressResultRowDisplay>
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

  /**
   * Handles the GET request for retrieving linked cases associated with an application.
   *
   * @param applicationId the ID of the application, retrieved from the session attribute
   * @param model the Spring Model to pass attributes to the view
   * @param session the HttpSession object
   * @return the path to the linked cases summary view
   */
  @GetMapping("/application/summary/linked-cases")
  public String linkedCasesGet(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      final Model model,
      final HttpSession session) {


    final ResultsDisplay<LinkedCaseResultRowDisplay> linkedCases =
        applicationService.getLinkedCases(applicationId);

    model.addAttribute("linkedCases", linkedCases);
    session.setAttribute("linkedCases", linkedCases);

    return "application/summary/application-linked-case-summary";
  }

  /**
   * Handles the GET request for displaying the page to remove a linked case.
   *
   * @param linkedCaseId the ID of the linked case to remove
   * @param linkedCases the ResultsDisplay object containing linked case information
   * @param model the Spring Model to pass attributes to the view
   * @return the path to the linked case removal view
   */
  @GetMapping("/application/summary/linked-cases/{linked-case-id}/remove")
  public String removeLinkedCaseGet(
      @PathVariable("linked-case-id") final Integer linkedCaseId,
      @SessionAttribute("linkedCases") final ResultsDisplay<LinkedCaseResultRowDisplay> linkedCases,
      final Model model) {

    final LinkedCaseResultRowDisplay linkedCase = linkedCases.getContent() == null ? null :
        linkedCases.getContent().stream()
            .filter(lc -> linkedCaseId.equals(lc.getId()))
            .findFirst()
            .orElse(null);

    model.addAttribute("linkedCase", linkedCase);

    return "application/summary/application-linked-case-remove";
  }

  /**
   * Handles the POST request for removing a linked case.
   *
   * @param linkedCaseId the ID of the linked case to remove
   * @param applicationId the application ID from the session attribute
   * @param user the user details from the session attribute
   * @return a redirect path to the linked cases summary
   */
  @PostMapping("/application/summary/linked-cases/{linked-case-id}/remove")
  public String removeLinkedCasePost(
      @PathVariable("linked-case-id") final String linkedCaseId,
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user) {

    applicationService.removeLinkedCase(applicationId, linkedCaseId, user);

    return "redirect:/application/summary/linked-cases";
  }

  /**
   * Handles the GET request for displaying the page to confirm details of a linked case.
   *
   * @param linkedCaseId the ID of the linked case to confirm
   * @param linkedCases the ResultsDisplay object containing linked case information
   * @param model the Spring Model to pass attributes to the view
   * @return the path to the linked case confirmation view
   */
  @GetMapping("/application/summary/linked-cases/{linked-case-id}/confirm")
  public String confirmLinkedCaseGet(
      @PathVariable("linked-case-id") final Integer linkedCaseId,
      @SessionAttribute("linkedCases") final ResultsDisplay<LinkedCaseResultRowDisplay> linkedCases,
      final Model model) {

    final LinkedCaseResultRowDisplay linkedCase = linkedCases.getContent() == null ? null :
        linkedCases.getContent().stream()
            .filter(lc -> linkedCaseId.equals(lc.getId()))
            .findFirst()
            .orElse(null);

    model.addAttribute("linkedCase", linkedCase);

    populateLinkedCasesConfirmDropdowns(model);

    return "application/summary/application-linked-case-confirm";
  }

  /**
   * Handles the POST request for confirming details of a linked case.
   *
   * @param linkedCaseId the ID of the linked case to confirm
   * @param applicationId the application ID from the session attribute
   * @param user the user details from the session attribute
   * @param linkedCase the LinkedCaseResultRowDisplay object containing linked case details
   * @param bindingResult the BindingResult to capture validation errors
   * @param model the Spring Model to pass attributes to the view
   * @return a redirect path to the linked cases summary or the confirmation view on error
   */
  @PostMapping("/application/summary/linked-cases/{linked-case-id}/confirm")
  public String confirmLinkedCasePost(
      @PathVariable("linked-case-id") final String linkedCaseId,
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @ModelAttribute("linkedCase") final LinkedCaseResultRowDisplay linkedCase,
      final BindingResult bindingResult,
      final Model model) {

    linkedCaseValidator.validate(linkedCase, bindingResult);

    if (bindingResult.hasErrors()) {
      model.addAttribute("linkedCase", linkedCase);
      populateLinkedCasesConfirmDropdowns(model);
      return "application/summary/application-linked-case-confirm";
    }

    applicationService.updateLinkedCase(applicationId, linkedCaseId, linkedCase, user);

    return "redirect:/application/summary/linked-cases";
  }

  /**
   * Populates the model with dropdown options for confirming a linked case.
   *
   * @param model the Spring Model to pass attributes for dropdowns
   */
  private void populateLinkedCasesConfirmDropdowns(final Model model) {
    new DropdownBuilder(model)
        .addDropdown("caseLinkOptions",
            lookupService.getCaseLinkTypes())
        .build();
  }

  /**
   * Populates the model with dropdown options and additional attributes for linked case search.
   *
   * @param user the user details containing provider information
   * @param model the Spring Model to pass attributes for dropdowns and additional data
   */
  private void populateLinkedCasesSearchDropdowns(final UserDetail user, final Model model) {
    final ProviderDetail provider = providerService.getProvider(user.getProvider().getId()).block();
    if (provider == null) {
      throw new CaabApplicationException(
          String.format("Failed to retrieve Provider with id: %s",
              user.getProvider().getId()));
    }

    new DropdownBuilder(model)
        .addDropdown("Status",
            lookupService.getApplicationStatuses())
        .build();

    model.addAttribute("feeEarners", providerService.getAllFeeEarners(provider));
    model.addAttribute("offices", user.getProvider().getOffices());
  }

}
