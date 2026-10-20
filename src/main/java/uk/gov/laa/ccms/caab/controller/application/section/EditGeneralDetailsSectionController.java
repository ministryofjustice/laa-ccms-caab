package uk.gov.laa.ccms.caab.controller.application.section;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.LINKED_CASES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CASE_ADDRESS_OPTION;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CASE_LINK_TYPE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ADDRESS_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CORRESPONDENCE_ADDRESS_FLOW;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_RESULT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_SUBMIT_CASE;
import static uk.gov.laa.ccms.caab.util.ApplicationUtil.requireEbsCase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.AddressLookupFlowData;
import uk.gov.laa.ccms.caab.bean.AddressSearchFormData;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.application.CaseSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.bean.validators.application.LinkedCaseValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.AddressSearchValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.CorrespondenceAddressValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.FindAddressValidator;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.config.UserRole;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.mapper.EbsApplicationMapper;
import uk.gov.laa.ccms.caab.mapper.ResultDisplayMapper;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.LinkedCaseResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.service.AddressLookupUnavailableException;
import uk.gov.laa.ccms.caab.service.AddressService;
import uk.gov.laa.ccms.caab.service.AmendmentService;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.caab.util.PaginationUtil;
import uk.gov.laa.ccms.caab.util.UkPostcode;
import uk.gov.laa.ccms.caab.util.UserRoleUtil;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller for the application's general details section. */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({CASE_SEARCH_CRITERIA, CASE_SEARCH_RESULTS})
public class EditGeneralDetailsSectionController {

  // services
  private final ApplicationService applicationService;
  private final AmendmentService amendmentService;
  private final AddressService addressService;
  private final LookupService lookupService;
  private final ProviderService providerService;

  // validators
  private final FindAddressValidator findAddressValidator;
  private final AddressSearchValidator addressSearchValidator;
  private final CorrespondenceAddressValidator correspondenceAddressValidator;
  private final LinkedCaseValidator linkedCaseValidator;
  private final CaseSearchCriteriaValidator searchCriteriaValidator;

  // Mappers
  private final EbsApplicationMapper applicationMapper;
  private final ResultDisplayMapper resultDisplayMapper;

  private static final String ACTION_FIND_ADDRESS = "find_address";
  protected static final String CURRENT_URL = "currentUrl";
  protected static final String CASE_RESULTS_PAGE = "caseResultsPage";
  private static final String ADDRESS_DETAILS_MODEL_ATTRIBUTE = "addressDetails";

  /**
   * Handles the GET request for editing an application's correspondence address.
   *
   * @param caseContext the context for the application (e.g. application or amendments)
   * @param applicationId The id of the application
   * @param model The model for the view.
   * @return The view name for the application summary page.
   */
  @GetMapping("/{caseContext}/sections/correspondence-address")
  public String correspondenceDetails(
      @PathVariable("caseContext") final CaseContext caseContext,
      @SessionAttribute(CASE) @Nullable final ApplicationDetail ebsCase,
      @SessionAttribute(APPLICATION_ID) @Nullable final String applicationId,
      final Model model,
      final HttpSession session) {

    final String addressContext = correspondenceAddressContext(caseContext, ebsCase, applicationId);
    final AddressLookupFlowData<AddressFormData> addressFlow =
        getOrCreateCorrespondenceAddressFlow(session, addressContext);

    AddressFormData addressDetails = addressFlow.getAddressDetails();
    if (addressDetails == null) {
      final AddressFormData savedAddress =
          getCorrespondenceAddressFormData(caseContext, ebsCase, applicationId);
      addressDetails = savedAddress == null ? new AddressFormData() : savedAddress;
      if (hasSavedAddress(addressDetails)) {
        addressFlow.showExistingAddress();
      }
    }

    if (addressFlow.getStep() == AddressLookupFlowData.Step.CONFIRMED_ADDRESS
        && addressFlow.getSelectedAddress() != null) {
      addressDetails = copyAddressFormData(addressFlow.getAddressDetails());
      addressService.updateAddressFormData(addressDetails, addressFlow.getSelectedAddress());
      addressDetails.setPreferredAddress(addressFlow.getAddressDetails().getPreferredAddress());
      addressDetails.setCareOf(addressFlow.getAddressDetails().getCareOf());
    }

    if (addressFlow.getAddressDetails() == null) {
      addressFlow.setAddressDetails(copyAddressFormData(addressDetails));
    }
    session.setAttribute(CORRESPONDENCE_ADDRESS_FLOW, addressFlow);
    model.addAttribute(ADDRESS_DETAILS_MODEL_ATTRIBUTE, addressDetails);
    model.addAttribute("addressFlow", addressFlow);
    model.addAttribute("selectedAddress", addressFlow.getSelectedAddress());
    model.addAttribute(
        "caseAddress",
        !StringUtils.hasText(addressDetails.getPreferredAddress())
            || "CASE".equals(addressDetails.getPreferredAddress()));
    model.addAttribute(
        "countryName", addressDetails.getCountry() == null ? "" : addressDetails.getCountry());
    addCorrespondenceAddressUrls(caseContext, model);

    populateCorrespondenceAddressDropdowns(model);

    return "application/sections/correspondence-address-details";
  }

  private AddressFormData copyAddressFormData(final AddressFormData source) {
    final AddressFormData copy = new AddressFormData();
    if (source == null) {
      return copy;
    }
    copy.setPreferredAddress(source.getPreferredAddress());
    copy.setCountry(source.getCountry());
    copy.setHouseNameNumber(source.getHouseNameNumber());
    copy.setPostcode(source.getPostcode());
    copy.setCareOf(source.getCareOf());
    copy.setAddressLine1(source.getAddressLine1());
    copy.setAddressLine2(source.getAddressLine2());
    copy.setCityTown(source.getCityTown());
    copy.setCounty(source.getCounty());
    return copy;
  }

  private void addCorrespondenceAddressUrls(final CaseContext caseContext, final Model model) {
    final String baseUrl =
        "/%s/sections/correspondence-address".formatted(caseContext.getPathValue());
    model.addAttribute("changeAddressUrl", baseUrl + "/search-again");
    model.addAttribute("manualEntryUrl", baseUrl + "/manual");
    model.addAttribute("searchAgainUrl", baseUrl + "/search-again");
    model.addAttribute("confirmUrl", baseUrl + "/confirm");
    model.addAttribute("changeUrl", baseUrl + "/search");
    model.addAttribute("backUrl", baseUrl);
  }

  private AddressFormData getCorrespondenceAddressFormData(
      final CaseContext caseContext,
      @Nullable final ApplicationDetail ebsCase,
      @Nullable final String applicationId) {
    if (caseContext.isAmendment()) {
      final ApplicationDetail amendmentCase = requireEbsCase(ebsCase);
      return applicationService.getCorrespondenceAddressFormData(
          amendmentCase.getCorrespondenceAddress());
    } else if (ebsCase != null && ebsCase.getCorrespondenceAddress() != null) {
      return applicationService.getCorrespondenceAddressFormData(
          ebsCase.getCorrespondenceAddress());
    }
    return applicationService.getCorrespondenceAddressFormData(applicationId);
  }

  private String correspondenceAddressContext(
      final CaseContext caseContext,
      @Nullable final ApplicationDetail ebsCase,
      @Nullable final String applicationId) {
    if (caseContext.isAmendment()) {
      return "%s:%s"
          .formatted(caseContext.getPathValue(), requireEbsCase(ebsCase).getCaseReferenceNumber());
    }
    return "%s:%s".formatted(caseContext.getPathValue(), applicationId);
  }

  private void clearCorrespondenceAddressSession(final HttpSession session) {
    session.removeAttribute(CORRESPONDENCE_ADDRESS_FLOW);
  }

  @SuppressWarnings("unchecked")
  private AddressLookupFlowData<AddressFormData> getOrCreateCorrespondenceAddressFlow(
      final HttpSession session, final String addressContext) {
    final Object sessionValue = session.getAttribute(CORRESPONDENCE_ADDRESS_FLOW);
    if (sessionValue instanceof AddressLookupFlowData<?> addressFlow
        && addressContext.equals(addressFlow.getContext())) {
      return (AddressLookupFlowData<AddressFormData>) addressFlow;
    }
    return new AddressLookupFlowData<>(addressContext);
  }

  /**
   * Handles the POST request for editing an application's correspondence address.
   *
   * @param caseContext the context for the application (e.g. application or amendments)
   * @param action The action performed, which button pressed. Either "find_address" or
   *     "save_address".
   * @param applicationId The id of the application
   * @param addressDetails The address details model.
   * @param user The user details.
   * @param model The model for the view.
   * @param bindingResult Validation result.
   * @param session The session data for the endpoint.
   * @return The view name for the application summary page.
   */
  @PostMapping("/{caseContext}/sections/correspondence-address")
  public String updateCorrespondenceDetails(
      @PathVariable final CaseContext caseContext,
      @RequestParam(required = false) final String action,
      @SessionAttribute(CASE) @Nullable final ApplicationDetail ebsCase,
      @SessionAttribute(APPLICATION_ID) @Nullable final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @Validated @ModelAttribute(ADDRESS_DETAILS_MODEL_ATTRIBUTE)
          final AddressFormData addressDetails,
      final BindingResult bindingResult,
      final Model model,
      final HttpSession session) {

    final String addressContext = correspondenceAddressContext(caseContext, ebsCase, applicationId);
    final AddressLookupFlowData<AddressFormData> addressFlow =
        getOrCreateCorrespondenceAddressFlow(session, addressContext);
    final boolean caseSpecific = "CASE".equals(addressDetails.getPreferredAddress());

    if (ACTION_FIND_ADDRESS.equals(action) && !caseSpecific) {
      addressFlow.searchAgain();
      bindingResult.rejectValue(
          "preferredAddress", "address.lookup.caseOnly", "Choose the case address to search");
    } else if (ACTION_FIND_ADDRESS.equals(action)) {
      addressFlow.beginSearch(addressDetails.getPostcode(), addressDetails.getHouseNameNumber());
      addressFlow.setAddressDetails(copyAddressFormData(addressDetails));
      session.setAttribute(CORRESPONDENCE_ADDRESS_FLOW, addressFlow);
      findAddressValidator.validate(addressDetails, bindingResult);
    } else if (caseSpecific && addressFlow.getSource() == AddressLookupFlowData.Source.SEARCHED) {
      if (addressFlow.getStep() == AddressLookupFlowData.Step.SELECTED_ADDRESS
          && addressFlow.getSelectedAddress() != null) {
        return "redirect:/%s/sections/correspondence-address/confirm"
            .formatted(caseContext.getPathValue());
      }
      if (addressFlow.getStep() != AddressLookupFlowData.Step.CONFIRMED_ADDRESS
          || addressFlow.getSelectedAddress() == null) {
        bindingResult.reject("address.selection.invalid", "Select and confirm an address");
      } else {
        final String preferredAddress = addressDetails.getPreferredAddress();
        final String careOf =
            addressFlow.getAddressDetails() == null
                ? null
                : addressFlow.getAddressDetails().getCareOf();
        addressService.updateAddressFormData(addressDetails, addressFlow.getSelectedAddress());
        addressDetails.setPreferredAddress(preferredAddress);
        addressDetails.setCareOf(careOf);
      }
    } else if (caseSpecific
        && addressFlow.getSource() == AddressLookupFlowData.Source.EXISTING
        && addressFlow.getAddressDetails() != null) {
      copyAddressFields(addressFlow.getAddressDetails(), addressDetails);
      addressDetails.setPreferredAddress("CASE");
      addressDetails.setCareOf(addressFlow.getAddressDetails().getCareOf());
    } else if (caseSpecific
        && addressFlow.getSource() == AddressLookupFlowData.Source.MANUAL
        && addressFlow.getStep() == AddressLookupFlowData.Step.MANUAL_ENTRY) {
      correspondenceAddressValidator.validate(addressDetails, bindingResult);
    } else if (caseSpecific) {
      bindingResult.reject("address.selection.invalid", "Enter an address manually");
    } else {
      addressFlow.searchAgain();
      correspondenceAddressValidator.validate(addressDetails, bindingResult);
    }

    if (bindingResult.hasErrors()) {
      populateCorrespondenceAddressDropdowns(model);
      model.addAttribute("addressFlow", addressFlow);
      model.addAttribute("selectedAddress", addressFlow.getSelectedAddress());
      model.addAttribute("caseAddress", caseSpecific);
      addCorrespondenceAddressUrls(caseContext, model);
      addressFlow.setAddressDetails(addressDetails);
      session.setAttribute(CORRESPONDENCE_ADDRESS_FLOW, addressFlow);
      return "application/sections/correspondence-address-details";
    }

    if (ACTION_FIND_ADDRESS.equals(action)) {
      final ResultsDisplay<AddressResultRowDisplay> addressSearchResults;
      try {
        addressSearchResults = addressService.getAddresses(addressDetails.getPostcode());
      } catch (AddressLookupUnavailableException exception) {
        addressFlow.markLookupUnavailable();
        session.setAttribute(CORRESPONDENCE_ADDRESS_FLOW, addressFlow);
        return "redirect:/%s/sections/correspondence-address/search"
            .formatted(caseContext.getPathValue());
      }

      ResultsDisplay<AddressResultRowDisplay> filteredResults = addressSearchResults;
      if (addressSearchResults.getContent() != null
          && !addressSearchResults.getContent().isEmpty()) {
        filteredResults =
            addressService.filterByHouseNumber(
                addressDetails.getHouseNameNumber(), addressSearchResults);
      }
      addressFlow.setSearchResults(filteredResults);
      session.setAttribute(CORRESPONDENCE_ADDRESS_FLOW, addressFlow);
      return "redirect:/%s/sections/correspondence-address/search"
          .formatted(caseContext.getPathValue());

    } else {
      if (caseSpecific
          && addressFlow.getSource() != AddressLookupFlowData.Source.SEARCHED
          && addressFlow.getSource() != AddressLookupFlowData.Source.EXISTING) {
        if ("GBR".equals(addressDetails.getCountry())) {
          addressDetails.setPostcode(UkPostcode.normalize(addressDetails.getPostcode()));
        }
        addressFlow.setSource(AddressLookupFlowData.Source.MANUAL);
      }
      addressFlow.setAddressDetails(addressDetails);
      session.setAttribute(CORRESPONDENCE_ADDRESS_FLOW, addressFlow);
      if (caseContext.isAmendment()) {
        final ApplicationDetail amendmentCase = requireEbsCase(ebsCase);
        if (!UserRoleUtil.hasRole(user, UserRole.SUBMIT_AMENDMENT)) {
          throw new CaabApplicationException("User not authorized to submit amendments");
        }
        // Submit the amendment, and redirect to polling endpoint
        String transactionId =
            amendmentService.submitQuickAmendmentCorrespondenceAddress(
                addressDetails, amendmentCase.getCaseReferenceNumber(), user);
        session.removeAttribute(SUBMISSION_RESULT);
        session.setAttribute(SUBMISSION_TRANSACTION_ID, transactionId);

        clearCorrespondenceAddressSession(session);
        return "redirect:/amendments/%s".formatted(SUBMISSION_SUBMIT_CASE);
      }
      applicationService.updateCorrespondenceAddress(applicationId, addressDetails, user);
      clearCorrespondenceAddressSession(session);

      return "redirect:/application/sections/linked-cases";
    }
  }

  private void populateCorrespondenceAddressDropdowns(final Model model) {
    new DropdownBuilder(model)
        .addDropdown("countries", lookupService.getCountries())
        .addDropdown(
            "caseAddressOptions", lookupService.getCommonValues(COMMON_VALUE_CASE_ADDRESS_OPTION))
        .build();
  }

  private boolean hasSavedAddress(final AddressFormData addressDetails) {
    return StringUtils.hasText(addressDetails.getCountry())
        || StringUtils.hasText(addressDetails.getPostcode())
        || StringUtils.hasText(addressDetails.getHouseNameNumber())
        || StringUtils.hasText(addressDetails.getAddressLine1())
        || StringUtils.hasText(addressDetails.getAddressLine2())
        || StringUtils.hasText(addressDetails.getCityTown())
        || StringUtils.hasText(addressDetails.getCounty());
  }

  /**
   * Handles the GET request for edit correspondence address search page.
   *
   * @param caseContext the context for the application (e.g. application or amendments)
   * @param addressFlow the correspondence address lookup flow data.
   * @param ebsCase the EBS case details.
   * @param applicationId the application ID.
   * @param addressSearch The address search model containing the uprn.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("/{caseContext}/sections/correspondence-address/search")
  public String correspondenceAddressGet(
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(name = CORRESPONDENCE_ADDRESS_FLOW, required = false)
          final AddressLookupFlowData<AddressFormData> addressFlow,
      @SessionAttribute(CASE) @Nullable final ApplicationDetail ebsCase,
      @SessionAttribute(APPLICATION_ID) @Nullable final String applicationId,
      @ModelAttribute("addressSearch") final AddressSearchFormData addressSearch,
      final HttpSession session,
      final Model model) {

    if (!isCurrentCorrespondenceAddressFlow(caseContext, ebsCase, applicationId, addressFlow)
        || (addressFlow.getSearchResults() == null
            && addressFlow.getStep() != AddressLookupFlowData.Step.LOOKUP_UNAVAILABLE)
        || (addressFlow.getStep() != AddressLookupFlowData.Step.RESULTS
            && addressFlow.getStep() != AddressLookupFlowData.Step.NO_RESULTS
            && addressFlow.getStep() != AddressLookupFlowData.Step.LOOKUP_UNAVAILABLE)) {
      clearCorrespondenceAddressSession(session);
      return "redirect:/%s/sections/correspondence-address".formatted(caseContext.getPathValue());
    }

    model.addAttribute(ADDRESS_SEARCH_RESULTS, addressFlow.getSearchResults());
    model.addAttribute("addressFlow", addressFlow);
    addAddressSearchTermsToModel(addressFlow, model);
    model.addAttribute(
        "formAction",
        "%s/sections/correspondence-address/search".formatted(caseContext.getPathValue()));
    addCorrespondenceAddressUrls(caseContext, model);

    return "application/sections/address-search-results";
  }

  /**
   * Handles the correspondence address results submission.
   *
   * @param caseContext the context for the application (e.g. application or amendments)
   * @param addressFlow the correspondence address lookup flow data.
   * @param ebsCase the EBS case details.
   * @param applicationId the application ID.
   * @param addressSearch The address search model containing the uprn.
   * @param model The model for the view.
   * @param bindingResult Validation result.
   * @param session The session data for the endpoint.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/{caseContext}/sections/correspondence-address/search")
  public String correspondenceAddressSearchPost(
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(name = CORRESPONDENCE_ADDRESS_FLOW, required = false)
          final AddressLookupFlowData<AddressFormData> addressFlow,
      @SessionAttribute(CASE) @Nullable final ApplicationDetail ebsCase,
      @SessionAttribute(APPLICATION_ID) @Nullable final String applicationId,
      @ModelAttribute("addressSearch") final AddressSearchFormData addressSearch,
      final Model model,
      final BindingResult bindingResult,
      final HttpSession session) {

    if (!isCurrentCorrespondenceAddressFlow(caseContext, ebsCase, applicationId, addressFlow)
        || addressFlow.getSearchResults() == null
        || addressFlow.getStep() != AddressLookupFlowData.Step.RESULTS
        || addressFlow.getAddressDetails() == null) {
      clearCorrespondenceAddressSession(session);
      return "redirect:/%s/sections/correspondence-address".formatted(caseContext.getPathValue());
    }

    // validate if an address is selected
    addressSearchValidator.validateSelection(
        addressSearch, addressFlow.getSearchResults(), bindingResult);
    if (bindingResult.hasErrors()) {
      model.addAttribute(ADDRESS_SEARCH_RESULTS, addressFlow.getSearchResults());
      model.addAttribute("addressFlow", addressFlow);
      model.addAttribute(
          "formAction",
          "%s/sections/correspondence-address/search".formatted(caseContext.getPathValue()));
      addAddressSearchTermsToModel(addressFlow, model);
      addCorrespondenceAddressUrls(caseContext, model);
      return "application/sections/address-search-results";
    }

    final AddressResultRowDisplay selectedAddress =
        addressService.getSelectedAddress(addressSearch.getUprn(), addressFlow.getSearchResults());
    if (selectedAddress == null) {
      bindingResult.rejectValue("uprn", "invalid.selection", "Select an address from the results");
      model.addAttribute(ADDRESS_SEARCH_RESULTS, addressFlow.getSearchResults());
      model.addAttribute("addressFlow", addressFlow);
      model.addAttribute(
          "formAction",
          "%s/sections/correspondence-address/search".formatted(caseContext.getPathValue()));
      addAddressSearchTermsToModel(addressFlow, model);
      addCorrespondenceAddressUrls(caseContext, model);
      return "application/sections/address-search-results";
    }
    addressFlow.setSelectedAddress(selectedAddress);
    session.setAttribute(CORRESPONDENCE_ADDRESS_FLOW, addressFlow);

    return "redirect:/%s/sections/correspondence-address/confirm"
        .formatted(caseContext.getPathValue());
  }

  private void addAddressSearchTermsToModel(
      final AddressLookupFlowData<AddressFormData> addressFlow, final Model model) {
    model.addAttribute("originalPostcode", addressFlow.getOriginalPostcode());
    model.addAttribute("originalBuildingSearch", addressFlow.getOriginalBuildingSearch());
  }

  @GetMapping("/{caseContext}/sections/correspondence-address/confirm")
  public String correspondenceAddressConfirmGet(
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(name = CORRESPONDENCE_ADDRESS_FLOW, required = false) @Nullable
          final AddressLookupFlowData<AddressFormData> addressFlow,
      @SessionAttribute(CASE) @Nullable final ApplicationDetail ebsCase,
      @SessionAttribute(APPLICATION_ID) @Nullable final String applicationId,
      final Model model) {
    if (!isCurrentCorrespondenceAddressFlow(caseContext, ebsCase, applicationId, addressFlow)
        || addressFlow.getStep() != AddressLookupFlowData.Step.SELECTED_ADDRESS
        || addressFlow.getSelectedAddress() == null) {
      return "redirect:/%s/sections/correspondence-address".formatted(caseContext.getPathValue());
    }
    model.addAttribute("selectedAddress", addressFlow.getSelectedAddress());
    model.addAttribute("showClientCaption", false);
    addCorrespondenceAddressUrls(caseContext, model);
    return "application/sections/address-confirmation";
  }

  @PostMapping("/{caseContext}/sections/correspondence-address/confirm")
  public String correspondenceAddressConfirmPost(
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(name = CORRESPONDENCE_ADDRESS_FLOW, required = false) @Nullable
          final AddressLookupFlowData<AddressFormData> addressFlow,
      @SessionAttribute(CASE) @Nullable final ApplicationDetail ebsCase,
      @SessionAttribute(APPLICATION_ID) @Nullable final String applicationId) {
    if (isCurrentCorrespondenceAddressFlow(caseContext, ebsCase, applicationId, addressFlow)) {
      addressFlow.confirmSelectedAddress();
      if (addressFlow.getStep() == AddressLookupFlowData.Step.CONFIRMED_ADDRESS) {
        return "redirect:/%s/sections/correspondence-address".formatted(caseContext.getPathValue());
      }
    }
    return "redirect:/%s/sections/correspondence-address/search"
        .formatted(caseContext.getPathValue());
  }

  @GetMapping("/{caseContext}/sections/correspondence-address/search-again")
  public String correspondenceAddressSearchAgain(
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(name = CORRESPONDENCE_ADDRESS_FLOW, required = false) @Nullable
          final AddressLookupFlowData<AddressFormData> addressFlow,
      @SessionAttribute(CASE) @Nullable final ApplicationDetail ebsCase,
      @SessionAttribute(APPLICATION_ID) @Nullable final String applicationId) {
    if (!isCurrentCorrespondenceAddressFlow(caseContext, ebsCase, applicationId, addressFlow)) {
      return "redirect:/%s/sections/correspondence-address".formatted(caseContext.getPathValue());
    }
    if (!StringUtils.hasText(addressFlow.getOriginalPostcode())) {
      final AddressFormData searchDraft = new AddressFormData();
      if (addressFlow.getAddressDetails() != null) {
        searchDraft.setPreferredAddress(addressFlow.getAddressDetails().getPreferredAddress());
        searchDraft.setCareOf(addressFlow.getAddressDetails().getCareOf());
      }
      addressFlow.setAddressDetails(searchDraft);
    }
    addressFlow.searchAgain();
    return "redirect:/%s/sections/correspondence-address".formatted(caseContext.getPathValue());
  }

  @GetMapping("/{caseContext}/sections/correspondence-address/manual")
  public String correspondenceAddressManual(
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(name = CORRESPONDENCE_ADDRESS_FLOW, required = false) @Nullable
          final AddressLookupFlowData<AddressFormData> addressFlow,
      @SessionAttribute(CASE) @Nullable final ApplicationDetail ebsCase,
      @SessionAttribute(APPLICATION_ID) @Nullable final String applicationId) {
    if (!isCurrentCorrespondenceAddressFlow(caseContext, ebsCase, applicationId, addressFlow)) {
      return "redirect:/%s/sections/correspondence-address".formatted(caseContext.getPathValue());
    }
    final AddressFormData blankAddress = new AddressFormData();
    blankAddress.setPreferredAddress("CASE");
    if (addressFlow.getAddressDetails() != null) {
      blankAddress.setCareOf(addressFlow.getAddressDetails().getCareOf());
    }
    addressFlow.enterManualEntry(blankAddress);
    return "redirect:/%s/sections/correspondence-address".formatted(caseContext.getPathValue());
  }

  private void copyAddressFields(final AddressFormData source, final AddressFormData target) {
    target.setCountry(source.getCountry());
    target.setHouseNameNumber(source.getHouseNameNumber());
    target.setPostcode(source.getPostcode());
    target.setAddressLine1(source.getAddressLine1());
    target.setAddressLine2(source.getAddressLine2());
    target.setCityTown(source.getCityTown());
    target.setCounty(source.getCounty());
  }

  private boolean isCurrentCorrespondenceAddressFlow(
      final CaseContext caseContext,
      @Nullable final ApplicationDetail ebsCase,
      @Nullable final String applicationId,
      @Nullable final AddressLookupFlowData<AddressFormData> addressFlow) {
    return addressFlow != null
        && correspondenceAddressContext(caseContext, ebsCase, applicationId)
            .equals(addressFlow.getContext());
  }

  /**
   * Handles the GET request for retrieving linked cases associated with an application.
   *
   * @param applicationId the ID of the application, retrieved from the session attribute
   * @param model the Spring Model to pass attributes to the view
   * @param session the HttpSession object
   * @return the path to the linked cases summary view
   */
  @GetMapping("/{caseContext}/sections/linked-cases")
  public String linkedCasesGet(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @PathVariable final CaseContext caseContext,
      final Model model,
      final HttpSession session) {
    log.info("Getting linked-cases for context {}", caseContext);
    final ResultsDisplay<LinkedCaseResultRowDisplay> linkedCases =
        applicationService.getLinkedCases(applicationId);

    model.addAttribute(LINKED_CASES, linkedCases);
    session.setAttribute(LINKED_CASES, linkedCases);
    model.addAttribute(CASE_SEARCH_CRITERIA, new CaseSearchCriteria());

    return "application/sections/application-linked-case-summary";
  }

  /**
   * Handles the GET request for displaying the page to remove a linked case.
   *
   * @param linkedCaseId the ID of the linked case to remove
   * @param linkedCases the ResultsDisplay object containing linked case information
   * @param model the Spring Model to pass attributes to the view
   * @return the path to the linked case removal view
   */
  @GetMapping("/{caseContext}/sections/linked-cases/{linked-case-id}/remove")
  public String removeLinkedCaseGet(
      @PathVariable("linked-case-id") final Integer linkedCaseId,
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(LINKED_CASES) final ResultsDisplay<LinkedCaseResultRowDisplay> linkedCases,
      final Model model) {

    log.info(
        "Getting linked-case to remove with id: {} caseContext: {}", linkedCaseId, caseContext);
    final LinkedCaseResultRowDisplay linkedCase =
        linkedCases.getContent() == null
            ? null
            : linkedCases.getContent().stream()
                .filter(lc -> linkedCaseId.equals(lc.getId()))
                .findFirst()
                .orElse(null);

    model.addAttribute("caseContext", caseContext);
    model.addAttribute("linkedCase", linkedCase);

    return "application/sections/application-linked-case-remove";
  }

  /**
   * Handles the POST request for removing a linked case.
   *
   * @param linkedCaseId the ID of the linked case to remove
   * @param user the user details from the session attribute
   * @return a redirect path to the linked cases summary
   */
  @PostMapping("/{caseContext}/sections/linked-cases/{linked-case-id}/remove")
  public String removeLinkedCasePost(
      @PathVariable("linked-case-id") final String linkedCaseId,
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(USER_DETAILS) final UserDetail user) {

    log.info(
        "Removing linked-case to remove with id: {} caseContext: {}", linkedCaseId, caseContext);
    applicationService.removeLinkedCase(linkedCaseId, user);

    return "redirect:/%s/sections/linked-cases".formatted(caseContext.getPathValue());
  }

  /**
   * Handles the GET request for displaying the page to confirm details of a linked case.
   *
   * @param linkedCaseId the ID of the linked case to confirm
   * @param linkedCases the ResultsDisplay object containing linked case information
   * @param model the Spring Model to pass attributes to the view
   * @return the path to the linked case confirmation view
   */
  @GetMapping("/{caseContext}/sections/linked-cases/{linked-case-id}/confirm")
  public String confirmLinkedCaseGet(
      @PathVariable("linked-case-id") final Integer linkedCaseId,
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(LINKED_CASES) final ResultsDisplay<LinkedCaseResultRowDisplay> linkedCases,
      final Model model) {

    log.info(
        "Getting linked-case to confirm with id: {} caseContext: {}", linkedCaseId, caseContext);
    final LinkedCaseResultRowDisplay linkedCase =
        linkedCases.getContent() == null
            ? null
            : linkedCases.getContent().stream()
                .filter(lc -> linkedCaseId.equals(lc.getId()))
                .findFirst()
                .orElse(null);

    model.addAttribute("caseContext", caseContext);
    model.addAttribute("currentLinkedCase", linkedCase);
    model.addAttribute("linkedCaseId", linkedCaseId);

    populateLinkedCasesConfirmDropdowns(model);

    return "application/sections/application-linked-case-confirm";
  }

  /**
   * Handles the POST request for confirming details of a linked case.
   *
   * @param linkedCaseId the ID of the linked case to confirm
   * @param user the user details from the session attribute
   * @param linkedCase the LinkedCaseResultRowDisplay object containing linked case details
   * @param bindingResult the BindingResult to capture validation errors
   * @param model the Spring Model to pass attributes to the view
   * @return a redirect path to the linked cases summary or the confirmation view on error
   */
  @PostMapping("/{caseContext}/sections/linked-cases/{linked-case-id}/confirm")
  public String confirmLinkedCasePost(
      @PathVariable("linked-case-id") final String linkedCaseId,
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @ModelAttribute("currentLinkedCase") final LinkedCaseResultRowDisplay linkedCase,
      final BindingResult bindingResult,
      final Model model) {

    log.info("Confirming linked-case with id: {} caseContext: {}", linkedCaseId, caseContext);
    linkedCaseValidator.validate(linkedCase, bindingResult);

    if (bindingResult.hasErrors()) {
      model.addAttribute("currentLinkedCase", linkedCase);
      model.addAttribute("linkedCaseId", linkedCaseId);
      populateLinkedCasesConfirmDropdowns(model);
      return "application/sections/application-linked-case-confirm";
    }

    applicationService.updateLinkedCase(linkedCaseId, linkedCase, user);

    return "redirect:/%s/sections/linked-cases".formatted(caseContext.getPathValue());
  }

  /**
   * Populates the model with dropdown options for confirming a linked case.
   *
   * @param model the Spring Model to pass attributes for dropdowns
   */
  private void populateLinkedCasesConfirmDropdowns(final Model model) {
    new DropdownBuilder(model)
        .addDropdown("caseLinkOptions", lookupService.getCommonValues(COMMON_VALUE_CASE_LINK_TYPE))
        .build();
  }

  /**
   * Handles the GET request for searching linked cases.
   *
   * @param userDetails The details of the logged-in user.
   * @param model The UI model to add attributes to.
   * @return String The view name for linked cases search.
   */
  @GetMapping("/{caseContext}/sections/linked-cases/search")
  public String linkedCasesSearchGet(
      @SessionAttribute(USER_DETAILS) final UserDetail userDetails,
      @PathVariable final CaseContext caseContext,
      final Model model) {

    populateLinkedCasesSearchDropdowns(userDetails, model);

    return "application/sections/application-linked-case-search";
  }

  /**
   * Handles the POST request for searching linked cases, validates the search criteria, and
   * processes search results.
   *
   * @param activeCase Active case details from session attribute.
   * @param caseSearchCriteria Search criteria model attribute.
   * @param user User details from session attribute.
   * @param currentLinkedCases The current Linked cases for the application.
   * @param redirectAttributes Redirect attributes.
   * @param bindingResult Binding result for validation.
   * @param model Spring MVC model.
   * @return The view name for redirection or linked case search view.
   */
  @PostMapping("/{caseContext}/sections/linked-cases/search")
  public String linkedCasesSearchPost(
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(LINKED_CASES)
          final ResultsDisplay<LinkedCaseResultRowDisplay> currentLinkedCases,
      final RedirectAttributes redirectAttributes,
      @Validated @ModelAttribute(CASE_SEARCH_CRITERIA) final CaseSearchCriteria caseSearchCriteria,
      final BindingResult bindingResult,
      final Model model) {

    log.info("Searching for linked-case with id: {} caseContext: {}", caseContext, caseContext);
    searchCriteriaValidator.validate(caseSearchCriteria, bindingResult);
    if (bindingResult.hasErrors()) {
      populateLinkedCasesSearchDropdowns(user, model);
      return "application/sections/application-linked-case-search";
    }

    final List<BaseApplicationDetail> searchResults;

    try {
      searchResults = applicationService.getCases(caseSearchCriteria, user);

      // filter out current linked cases and where application id is the same as the current
      // application
      searchResults.removeIf(
          result ->
              currentLinkedCases.getContent().stream()
                      .anyMatch(
                          lc -> lc.getLscCaseReference().equals(result.getCaseReferenceNumber()))
                  || result.getCaseReferenceNumber().equals(activeCase.getCaseReferenceNumber()));

      if (searchResults.isEmpty()) {
        return "application/sections/application-linked-case-search-no-results";
      }
    } catch (final TooManyResultsException e) {
      return "application/sections/application-linked-case-search-too-many-results";
    }

    redirectAttributes.addFlashAttribute(CASE_SEARCH_RESULTS, searchResults);

    return "redirect:/%s/sections/linked-cases/search/results"
        .formatted(caseContext.getPathValue());
  }

  /**
   * Displays the search results for linked cases with pagination.
   *
   * @param page Requested page number.
   * @param size Size of the page.
   * @param caseSearchResults List of search results.
   * @param request HTTP servlet request.
   * @param model Spring MVC model.
   * @param session HTTP session.
   * @return The view name for search results.
   */
  @GetMapping("/{caseContext}/sections/linked-cases/search/results")
  public String linkedCasesSearchResults(
      @RequestParam(value = "page", defaultValue = "0") final int page,
      @RequestParam(value = "size", defaultValue = "10") final int size,
      @PathVariable final CaseContext caseContext,
      @ModelAttribute(CASE_SEARCH_RESULTS) final List<BaseApplicationDetail> caseSearchResults,
      final HttpServletRequest request,
      final Model model,
      final HttpSession session) {

    log.info("Displaying linked cases search results for caseContext: {}", caseContext);
    // Paginate the results list, and convert to the Page wrapper object for display
    final ApplicationDetails linkedCaseSearchResults =
        applicationMapper.toApplicationDetails(
            PaginationUtil.paginateList(Pageable.ofSize(size).withPage(page), caseSearchResults));

    model.addAttribute(CURRENT_URL, request.getRequestURL().toString());

    model.addAttribute(CASE_RESULTS_PAGE, linkedCaseSearchResults);
    session.setAttribute(CASE_RESULTS_PAGE, linkedCaseSearchResults);
    return "application/sections/application-linked-case-search-results";
  }

  /**
   * Handles the GET request to initiate the addition of a linked case based on selected case
   * reference ID.
   *
   * @param caseReferenceId The case reference ID path variable.
   * @param linkedCaseSearchResults the results from the linked case search.=.
   * @param model Spring MVC model.
   * @return The view name for adding a linked case.
   */
  @GetMapping("/{caseContext}/sections/linked-cases/{case-reference-id}/add")
  public String addLinkedCaseGet(
      @PathVariable("case-reference-id") final String caseReferenceId,
      @PathVariable final CaseContext caseContext,
      @SessionAttribute(CASE_RESULTS_PAGE) final ApplicationDetails linkedCaseSearchResults,
      final Model model) {

    log.info(
        "Adding linked case with case reference: {} for caseContext: {}",
        caseReferenceId,
        caseContext);
    final BaseApplicationDetail baseApplication =
        linkedCaseSearchResults.getContent() == null
            ? null
            : linkedCaseSearchResults.getContent().stream()
                .filter(lc -> caseReferenceId.equals(lc.getCaseReferenceNumber()))
                .findFirst()
                .orElseThrow(
                    () ->
                        new CaabApplicationException(
                            "Unable to add linked case with case reference: %s"
                                .formatted(caseReferenceId)));

    // map base application to linked case
    final LinkedCaseResultRowDisplay linkedCase =
        resultDisplayMapper.toLinkedCaseResultRowDisplay(baseApplication);

    linkedCase.setRelationToCase(null);

    model.addAttribute("caseContext", caseContext);
    model.addAttribute("currentLinkedCase", linkedCase);
    populateLinkedCasesConfirmDropdowns(model);

    return "application/sections/application-linked-case-add";
  }

  /**
   * Processes the POST request for adding a linked case, validating the provided linked case
   * details.
   *
   * @param applicationId Application ID from session attribute.
   * @param user User details from session attribute.
   * @param linkedCase Linked case model attribute.
   * @param bindingResult Binding result for validation.
   * @param model Spring MVC model.
   * @return The redirection view name upon successful addition, or the view name to re-add upon
   *     validation errors.
   */
  @PostMapping("/{caseContext}/sections/linked-cases/add")
  public String addLinkedCasePost(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @PathVariable final CaseContext caseContext,
      @ModelAttribute("currentLinkedCase") final LinkedCaseResultRowDisplay linkedCase,
      final BindingResult bindingResult,
      final Model model) {

    log.info(
        "Adding linked case with case reference: {} for caseContext: {}",
        applicationId,
        caseContext);
    linkedCaseValidator.validate(linkedCase, bindingResult);

    if (bindingResult.hasErrors()) {
      model.addAttribute("caseContext", caseContext);
      model.addAttribute("currentLinkedCase", linkedCase);
      populateLinkedCasesConfirmDropdowns(model);
      return "application/sections/application-linked-case-add";
    }

    applicationService.addLinkedCase(applicationId, linkedCase, user);

    return "redirect:/%s/sections/linked-cases".formatted(caseContext.getPathValue());
  }

  /**
   * Populates the model with dropdown options and additional attributes for linked case search.
   *
   * @param user the user details containing provider information
   * @param model the Spring Model to pass attributes for dropdowns and additional data
   */
  private void populateLinkedCasesSearchDropdowns(final UserDetail user, final Model model) {
    final Tuple2<ProviderDetail, CaseStatusLookupDetail> combinedResults =
        Optional.ofNullable(
                Mono.zip(
                        providerService.getProvider(user.getProvider().getId()),
                        lookupService.getCaseStatusValues())
                    .block())
            .orElseThrow(() -> new CaabApplicationException("Failed to retrieve lookup data"));

    final ProviderDetail providerDetail = combinedResults.getT1();
    final CaseStatusLookupDetail caseStatusLookupDetail = combinedResults.getT2();

    model.addAttribute("feeEarners", providerService.getAllFeeEarners(providerDetail));
    model.addAttribute("offices", user.getProvider().getOffices());
    model.addAttribute("statuses", caseStatusLookupDetail.getContent());

    new DropdownBuilder(model)
        .addDropdown("Status", lookupService.getCommonValues(COMMON_VALUE_APPLICATION_STATUS))
        .build();

    model.addAttribute("feeEarners", providerService.getAllFeeEarners(providerDetail));
    model.addAttribute("offices", user.getProvider().getOffices());
  }
}
