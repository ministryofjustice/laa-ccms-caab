package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_ORGANISATION;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ASSESSED_INCOME_FREQUENCY;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_EMPLOYMENT_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ORGANISATION_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_RELATIONSHIP_TO_CLIENT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.INDIVIDUAL_LIABLE_PARTY_FLOW;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ORGANISATION_LIABLE_PARTY_FLOW;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.util.WebUtils;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualLiablePartyFlow;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentOtherDetailsFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationLiablePartyFlow;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentOtherDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.controller.application.section.OpponentsSectionController.BooleanDropdownOption;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.security.ControlCharacterStrippingEditor;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.caab.service.IndividualOpponentFormService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.OpponentService;
import uk.gov.laa.ccms.caab.util.DateUtils;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller for award liable and interested-party journeys.
 *
 * <p>Individuals are added or edited over two pages. Organisations are found with a search, then
 * either an existing shared organisation is linked (with only its relationships entered) or a new
 * organisation is created. Each journey keeps its own award-scoped draft, so neither depends on an
 * in-progress application session.
 *
 * <p>Editing saves validated contacts on Next, and other details on Confirm. Back discards
 * unsubmitted other details; Cancel does not revert contacts already saved on Next.
 *
 * <p>Draft access and persistence share the session mutex so confirmation cannot save the same
 * draft twice or discard a draft started by another request. Journey tokens reject stale tabs even
 * when a replacement draft belongs to the same award and opponent.
 */
@Controller
@RequestMapping(
    "/case/outcome-and-awards/{awardPath:cost-award|financial-award|land|asset}/{awardId}"
        + "/liable-parties")
@RequiredArgsConstructor
@Slf4j
public class LiablePartyController {
  static final String ORGANISATION_SEARCH_VIEW = "application/liable-parties/organisation-search";
  static final String ORGANISATION_RESULTS_VIEW =
      "application/liable-parties/organisation-search-results";
  static final String ORGANISATION_SHARED_VIEW =
      "application/liable-parties/organisation-shared-create";
  static final String ORGANISATION_CREATE_VIEW = "application/liable-parties/organisation-create";

  static final String ORGANISATION_SEARCH_FORM = "organisationSearchCriteria";
  static final String ORGANISATION_SHARED_FORM = "liablePartySharedOrganisation";
  static final String ORGANISATION_CREATE_FORM = "liablePartyOrganisation";

  private final CaseOutcomeService caseOutcomeService;
  private final IndividualOpponentFormService individualOpponentFormService;
  private final IndividualOpponentValidator individualOpponentValidator;
  private final IndividualOpponentOtherDetailsValidator otherDetailsValidator;
  private final LookupService lookupService;
  private final OpponentService opponentService;
  private final OrganisationSearchCriteriaValidator organisationSearchCriteriaValidator;
  private final OrganisationOpponentValidator organisationOpponentValidator;

  /**
   * Adds award identifiers and journey navigation URLs to every request's model.
   *
   * @param awardPath the award type's URL path segment
   * @param awardId the selected award
   * @param opponentId the opponent being edited, or null when adding
   * @param model the model to populate
   */
  @ModelAttribute
  public void awardContext(
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      @PathVariable(value = "opponentId", required = false) final Integer opponentId,
      final Model model) {
    model.addAttribute("awardPath", awardPath);
    model.addAttribute("awardId", awardId);
    model.addAttribute("individualFormUrl", individualBasePath(awardPath, awardId, opponentId));
    final String partyListUrl =
        "/case/outcome-and-awards/" + awardPath + "/" + awardId + "/liable-parties";
    model.addAttribute("individualBackUrl", partyListUrl);
    model.addAttribute("organisationFormUrl", partyListUrl + "/organisation");
  }

  /**
   * Restricts contact form binding to editable fields and strips control characters.
   *
   * @param binder the contact form binder
   */
  @InitBinder("liablePartyIndividual")
  public void bindIndividualContactDetails(final WebDataBinder binder) {
    binder.registerCustomEditor(String.class, new ControlCharacterStrippingEditor(true));
    binder.setAllowedFields(
        "title",
        "surname",
        "firstName",
        "middleNames",
        "dateOfBirth",
        "relationshipToCase",
        "relationshipToClient",
        "nationalInsuranceNumber",
        "houseNameOrNumber",
        "addressLine1",
        "addressLine2",
        "city",
        "county",
        "country",
        "postcode",
        "telephoneHome",
        "telephoneWork",
        "telephoneMobile",
        "faxNumber",
        "emailAddress",
        "legalAided",
        "certificateNumber");
  }

  /**
   * Restricts other-details binding to editable fields and strips control characters.
   *
   * @param binder the other-details form binder
   */
  @InitBinder("liablePartyOtherDetails")
  public void bindIndividualOtherDetails(final WebDataBinder binder) {
    binder.registerCustomEditor(String.class, new ControlCharacterStrippingEditor(true));
    binder.setAllowedFields(
        "employmentStatus",
        "employerName",
        "employerAddress",
        "courtOrderedMeansAssessment",
        "assessedIncome",
        "assessedIncomeFrequency",
        "assessedAssets",
        "assessmentDate",
        "otherInformation");
  }

  /** Displays individual contact details, resuming the existing draft when back is true. */
  @GetMapping({"/individual/add", "/individual/{opponentId}/edit"})
  public String individualContactDetails(
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      @PathVariable(value = "opponentId", required = false) final Integer opponentId,
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @RequestParam(value = "back", defaultValue = "false") final boolean back,
      @RequestParam(required = false) final String journeyToken,
      final HttpSession session,
      final Model model) {
    synchronized (WebUtils.getSessionMutex(session)) {
      final IndividualLiablePartyFlow flow;
      if (back) {
        flow =
            requireIndividualFlow(
                ebsCase, user, awardPath, awardId, opponentId, journeyToken, session);
      } else {
        requireEditPermission(ebsCase);
        if (opponentId != null) {
          flow =
              caseOutcomeService.getIndividualLiableParty(
                  ebsCase.getCaseReferenceNumber(),
                  providerId(user),
                  awardPath,
                  awardId,
                  opponentId);
        } else {
          caseOutcomeService.validatePartyAward(
              ebsCase.getCaseReferenceNumber(), providerId(user), awardPath, awardId);
          flow =
              new IndividualLiablePartyFlow(
                  ebsCase.getCaseReferenceNumber(), providerId(user), awardPath, awardId);
        }
        session.setAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW, flow);
      }
      model.addAttribute("journeyToken", flow.getJourneyToken());
      model.addAttribute("liablePartyIndividual", flow.getContactDetails());
      return individualContactPage(model);
    }
  }

  /**
   * Validates contact details and advances to other details, saving contacts only when editing.
   *
   * @param awardPath the award type's URL path segment
   * @param awardId the selected award
   * @param opponentId the opponent being edited, or null when adding
   * @param ebsCase the current case
   * @param user the current provider user
   * @param form the submitted contact details
   * @param errors the binding and validation result
   * @param journeyToken the token identifying the active draft
   * @param session the session holding the draft
   * @param model the model to populate on validation or save failure
   * @return the contact view on failure, otherwise a redirect to other details
   * @throws CaabApplicationException if the draft context is invalid or the case is read-only
   */
  @PostMapping({"/individual/add", "/individual/{opponentId}/edit"})
  public String nextIndividual(
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      @PathVariable(value = "opponentId", required = false) final Integer opponentId,
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @Validated @ModelAttribute("liablePartyIndividual") final IndividualOpponentFormData form,
      final BindingResult errors,
      @RequestParam(required = false) final String journeyToken,
      final HttpSession session,
      final Model model) {
    synchronized (WebUtils.getSessionMutex(session)) {
      final var flow =
          requireIndividualFlow(
              ebsCase, user, awardPath, awardId, opponentId, journeyToken, session);
      model.addAttribute("journeyToken", flow.getJourneyToken());
      flow.setContactDetails(form);
      flow.setContactDetailsComplete(false);
      individualOpponentFormService.prepareForValidation(form);
      individualOpponentValidator.validateLiableParty(form, errors);
      if (!errors.hasErrors() && flow.isEditing()) {
        try {
          caseOutcomeService.updateIndividualLiablePartyContactDetails(
              flow.getCaseReference(),
              flow.getProviderId(),
              awardPath,
              awardId,
              flow.getOpponentId(),
              user.getLoginId(),
              form);
        } catch (CaabApiClientException | IllegalStateException ex) {
          log.warn("Failed to update contact details for individual party {}", opponentId, ex);
          errors.reject(
              "liableParty.save.failed", "We could not save the party. Please try again.");
        }
      }
      flow.setContactDetailsComplete(!errors.hasErrors());
      session.setAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW, flow);
      if (errors.hasErrors()) {
        return individualContactPage(model);
      }
      return "redirect:"
          + individualBasePath(awardPath, awardId, opponentId)
          + "/other-details?journeyToken="
          + flow.getJourneyToken();
    }
  }

  /**
   * Displays the draft's other details after its contact details have been completed.
   *
   * @param awardPath the award type's URL path segment
   * @param awardId the selected award
   * @param opponentId the opponent being edited, or null when adding
   * @param ebsCase the current case
   * @param user the current provider user
   * @param journeyToken the token identifying the active draft
   * @param session the session holding the draft
   * @param model the model to populate
   * @return the other-details view, or a redirect to incomplete contact details
   * @throws CaabApplicationException if the draft context is invalid or the case is read-only
   */
  @GetMapping({"/individual/add/other-details", "/individual/{opponentId}/edit/other-details"})
  public String individualOtherDetails(
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      @PathVariable(value = "opponentId", required = false) final Integer opponentId,
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @RequestParam(required = false) final String journeyToken,
      final HttpSession session,
      final Model model) {
    synchronized (WebUtils.getSessionMutex(session)) {
      final var flow =
          requireIndividualFlow(
              ebsCase, user, awardPath, awardId, opponentId, journeyToken, session);
      model.addAttribute("journeyToken", flow.getJourneyToken());
      if (!flow.isContactDetailsComplete()) {
        log.warn("Individual party other details requested before contact details were completed");
        return "redirect:"
            + individualBasePath(awardPath, awardId, opponentId)
            + "?back=true&journeyToken="
            + flow.getJourneyToken();
      }
      model.addAttribute("liablePartyOtherDetails", flow.getOtherDetails());
      return individualOtherPage(model);
    }
  }

  /**
   * Confirms validated other details, or discards unsaved other details when returning Back.
   *
   * <p>Confirmation creates a new party with both forms or updates an existing party's other
   * details, then removes only the confirmed draft.
   *
   * @param awardPath the award type's URL path segment
   * @param awardId the selected award
   * @param opponentId the opponent being edited, or null when adding
   * @param ebsCase the current case
   * @param user the current provider user
   * @param form the submitted other details
   * @param errors the binding and validation result
   * @param action the confirm or back action
   * @param journeyToken the token identifying the active draft
   * @param session the session holding the draft
   * @param model the model to populate on validation, load or save failure
   * @return the other-details view on failure, or a redirect to contacts or the party list
   * @throws CaabApplicationException if the action or draft is invalid, or the case is read-only
   */
  @PostMapping({"/individual/add/other-details", "/individual/{opponentId}/edit/other-details"})
  public String confirmIndividual(
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      @PathVariable(value = "opponentId", required = false) final Integer opponentId,
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @Validated @ModelAttribute("liablePartyOtherDetails")
          final IndividualOpponentOtherDetailsFormData form,
      final BindingResult errors,
      @RequestParam(defaultValue = "confirm") final String action,
      @RequestParam(required = false) final String journeyToken,
      final HttpSession session,
      final Model model) {
    synchronized (WebUtils.getSessionMutex(session)) {
      final var flow =
          requireIndividualFlow(
              ebsCase, user, awardPath, awardId, opponentId, journeyToken, session);
      model.addAttribute("journeyToken", flow.getJourneyToken());
      if ("back".equals(action)) {
        try {
          flow.setOtherDetails(
              flow.isEditing()
                  ? caseOutcomeService
                      .getIndividualLiableParty(
                          flow.getCaseReference(),
                          flow.getProviderId(),
                          awardPath,
                          awardId,
                          flow.getOpponentId())
                      .getOtherDetails()
                  : new IndividualOpponentOtherDetailsFormData());
        } catch (CaabApiClientException | IllegalStateException ex) {
          log.warn("Failed to reload saved other details for individual party {}", opponentId, ex);
          errors.reject(
              "liableParty.load.failed",
              "We could not load the saved party details. Please try again.");
          return individualOtherPage(model);
        }
        session.setAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW, flow);
        return "redirect:"
            + individualBasePath(awardPath, awardId, opponentId)
            + "?back=true&journeyToken="
            + flow.getJourneyToken();
      }
      if (!"confirm".equals(action)) {
        throw new CaabApplicationException("Invalid individual party action");
      }
      flow.setOtherDetails(form);
      if (!flow.isContactDetailsComplete()) {
        log.warn("Rejecting individual party confirmation without completed contact details");
        session.setAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW, flow);
        return "redirect:"
            + individualBasePath(awardPath, awardId, opponentId)
            + "?back=true&journeyToken="
            + flow.getJourneyToken();
      }
      form.setAssessmentDate(DateUtils.normaliseComponentDateIfValid(form.getAssessmentDate()));
      otherDetailsValidator.validate(form, errors);
      session.setAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW, flow);
      if (errors.hasErrors()) {
        return individualOtherPage(model);
      }
      try {
        if (flow.isEditing()) {
          caseOutcomeService.updateIndividualLiablePartyOtherDetails(
              flow.getCaseReference(),
              flow.getProviderId(),
              awardPath,
              awardId,
              flow.getOpponentId(),
              user.getLoginId(),
              form);
        } else {
          caseOutcomeService.createIndividualLiableParty(
              flow.getCaseReference(),
              flow.getProviderId(),
              awardPath,
              awardId,
              user.getLoginId(),
              flow.getContactDetails(),
              form);
        }
      } catch (CaabApiClientException | IllegalStateException ex) {
        log.warn("Failed to save individual party for {} award {}", awardPath, awardId, ex);
        errors.reject("liableParty.save.failed", "We could not save the party. Please try again.");
        return individualOtherPage(model);
      }
      if (session.getAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW) == flow) {
        session.removeAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW);
      }
      return liablePartiesRedirect(awardPath, awardId);
    }
  }

  /**
   * Discards the active draft without reverting contact details already saved during an edit.
   *
   * @param awardPath the award type's URL path segment
   * @param awardId the selected award
   * @param opponentId the opponent being edited, or null when adding
   * @param ebsCase the current case
   * @param user the current provider user
   * @param journeyToken the token identifying the active draft
   * @param session the session holding the draft
   * @return a redirect to the selected award's party list
   * @throws CaabApplicationException if the draft context is invalid or the case is read-only
   */
  @GetMapping({"/individual/add/cancel", "/individual/{opponentId}/edit/cancel"})
  public String cancelIndividual(
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      @PathVariable(value = "opponentId", required = false) final Integer opponentId,
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @RequestParam(required = false) final String journeyToken,
      final HttpSession session) {
    synchronized (WebUtils.getSessionMutex(session)) {
      requireIndividualFlow(ebsCase, user, awardPath, awardId, opponentId, journeyToken, session);
      session.removeAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW);
      return liablePartiesRedirect(awardPath, awardId);
    }
  }

  /**
   * Requires edit permission and a draft matching the current case, provider, award and journey.
   *
   * @param ebsCase the current case
   * @param user the current provider user
   * @param awardPath the award type's URL path segment
   * @param awardId the selected award
   * @param opponentId the opponent being edited, or null when adding
   * @param journeyToken the token identifying the active draft
   * @param session the session holding the draft
   * @return the matching active draft
   * @throws CaabApplicationException if the draft context is invalid or the case is read-only
   */
  private IndividualLiablePartyFlow requireIndividualFlow(
      final ApplicationDetail ebsCase,
      final UserDetail user,
      final String awardPath,
      final Integer awardId,
      final Integer opponentId,
      final String journeyToken,
      final HttpSession session) {
    requireEditPermission(ebsCase);
    if (!(session.getAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW)
            instanceof IndividualLiablePartyFlow flow)
        || !Objects.equals(flow.getCaseReference(), ebsCase.getCaseReferenceNumber())
        || !Objects.equals(flow.getProviderId(), providerId(user))
        || !Objects.equals(flow.getAwardPath(), awardPath)
        || !Objects.equals(flow.getAwardId(), awardId)
        || !Objects.equals(flow.getOpponentId(), opponentId)
        || !Objects.equals(flow.getJourneyToken(), journeyToken)) {
      throw new CaabApplicationException(
          "No individual party draft exists for this case and award");
    }
    return flow;
  }

  /**
   * Requires an outcome function that permits changes to the case.
   *
   * @param ebsCase the case whose available functions are checked
   * @throws CaabApplicationException if neither outcome edit function is available
   */
  private void requireEditPermission(final ApplicationDetail ebsCase) {
    if (ebsCase.getAvailableFunctions() == null
        || !(ebsCase.getAvailableFunctions().contains(FunctionConstants.OUTCOME_WITH_DISCHARGE)
            || ebsCase.getAvailableFunctions().contains(FunctionConstants.OUTCOME_NO_DISCHARGE))) {
      throw new CaabApplicationException("The case outcome is read-only");
    }
  }

  /**
   * Populates contact dropdowns and selects the individual contact-details view.
   *
   * @param model the model to populate
   * @return the contact-details view name
   */
  private String individualContactPage(final Model model) {
    individualOpponentFormService.populateContactDropdowns(model);
    return "application/liable-parties/individual-contact-details";
  }

  /**
   * Populates employment and assessment options and selects the other-details view.
   *
   * @param model the model to populate
   * @return the other-details view name
   */
  private String individualOtherPage(final Model model) {
    model.addAttribute("meansAssessmentOptions", individualOpponentFormService.yesNoOptions());
    new DropdownBuilder(model)
        .addDropdown(
            "employmentStatuses", lookupService.getCommonValues(COMMON_VALUE_EMPLOYMENT_STATUS))
        .addDropdown(
            "incomeFrequencies",
            lookupService.getCommonValues(COMMON_VALUE_ASSESSED_INCOME_FREQUENCY))
        .build();
    return "application/liable-parties/individual-other-details";
  }

  /**
   * Builds the award-scoped add or edit URL for an individual.
   *
   * @param awardPath the award type's URL path segment
   * @param awardId the selected award
   * @param opponentId the opponent being edited, or null when adding
   * @return the individual journey's base URL
   */
  private String individualBasePath(
      final String awardPath, final Integer awardId, final Integer opponentId) {
    return "/case/outcome-and-awards/"
        + awardPath
        + "/"
        + awardId
        + "/liable-parties/individual/"
        + (opponentId == null ? "add" : opponentId + "/edit");
  }

  /**
   * Restricts search binding to the search fields.
   *
   * @param binder the search form binder
   */
  @InitBinder(ORGANISATION_SEARCH_FORM)
  public void bindSearchCriteria(final WebDataBinder binder) {
    binder.setAllowedFields("name", "type", "city", "postcode");
  }

  /**
   * Restricts shared organisation binding to the fields the user may enter; the organisation's own
   * details always come from the search, never from the request.
   *
   * @param binder the shared organisation form binder
   */
  @InitBinder(ORGANISATION_SHARED_FORM)
  public void bindSharedOrganisation(final WebDataBinder binder) {
    binder.setAllowedFields("relationshipToCase", "relationshipToClient", "otherInformation");
  }

  /**
   * Restricts new organisation binding to editable fields, excluding identity and sharing flags.
   *
   * @param binder the new organisation form binder
   */
  @InitBinder(ORGANISATION_CREATE_FORM)
  public void bindNewOrganisation(final WebDataBinder binder) {
    binder.setAllowedFields(
        "organisationName",
        "organisationType",
        "currentlyTrading",
        "relationshipToCase",
        "relationshipToClient",
        "contactNameRole",
        "houseNameOrNumber",
        "addressLine1",
        "addressLine2",
        "city",
        "county",
        "country",
        "postcode",
        "telephoneWork",
        "faxNumber",
        "emailAddress",
        "otherInformation");
  }

  /**
   * Displays the organisation search, starting a new draft unless returning to an existing one.
   *
   * @param awardPath the award type's URL path segment
   * @param awardId the selected award
   * @param ebsCase the current case
   * @param user the current provider user
   * @param back whether to resume the existing draft's search criteria
   * @param journeyToken the token identifying the draft to resume
   * @param session the session holding the draft
   * @param model the model to populate
   * @return the organisation search view
   */
  @GetMapping("/organisation/search")
  public String organisationSearch(
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @RequestParam(value = "back", defaultValue = "false") final boolean back,
      @RequestParam(required = false) final String journeyToken,
      final HttpSession session,
      final Model model) {
    synchronized (WebUtils.getSessionMutex(session)) {
      final OrganisationLiablePartyFlow flow;
      if (back) {
        flow = requireOrganisationFlow(ebsCase, user, awardPath, awardId, journeyToken, session);
      } else {
        requireEditPermission(ebsCase);
        caseOutcomeService.validatePartyAward(
            ebsCase.getCaseReferenceNumber(), providerId(user), awardPath, awardId);
        flow =
            new OrganisationLiablePartyFlow(
                ebsCase.getCaseReferenceNumber(), providerId(user), awardPath, awardId);
        session.setAttribute(ORGANISATION_LIABLE_PARTY_FLOW, flow);
      }
      model.addAttribute("journeyToken", flow.getJourneyToken());
      model.addAttribute(ORGANISATION_SEARCH_FORM, flow.getSearchCriteria());
      return organisationSearchPage(model);
    }
  }

  /**
   * Validates the search criteria and redirects to the results.
   *
   * @param awardPath the award type's URL path segment
   * @param awardId the selected award
   * @param ebsCase the current case
   * @param user the current provider user
   * @param searchCriteria the submitted search criteria
   * @param errors the binding and validation result
   * @param journeyToken the token identifying the active draft
   * @param session the session holding the draft
   * @param model the model to populate on validation failure
   * @return the search view on failure, otherwise a redirect to the results
   */
  @PostMapping("/organisation/search")
  public String submitOrganisationSearch(
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @Validated @ModelAttribute(ORGANISATION_SEARCH_FORM)
          final OrganisationSearchCriteria searchCriteria,
      final BindingResult errors,
      @RequestParam(required = false) final String journeyToken,
      final HttpSession session,
      final Model model) {
    synchronized (WebUtils.getSessionMutex(session)) {
      final var flow =
          requireOrganisationFlow(ebsCase, user, awardPath, awardId, journeyToken, session);
      model.addAttribute("journeyToken", flow.getJourneyToken());
      flow.setSearchCriteria(searchCriteria);
      flow.setSearchResults(null);
      flow.setSelectedOrganisation(null);
      organisationSearchCriteriaValidator.validate(searchCriteria, errors);
      if (errors.hasErrors()) {
        return organisationSearchPage(model);
      }
      return "redirect:"
          + organisationBasePath(awardPath, awardId)
          + "/search/results?journeyToken="
          + flow.getJourneyToken();
    }
  }

  /**
   * Runs the draft's search and displays a page of results.
   *
   * @param awardPath the award type's URL path segment
   * @param awardId the selected award
   * @param ebsCase the current case
   * @param user the current provider user
   * @param journeyToken the token identifying the active draft
   * @param page the zero-based results page
   * @param size the results page size
   * @param session the session holding the draft
   * @param model the model to populate
   * @return the results view, or the search view when there are too many results
   */
  @GetMapping("/organisation/search/results")
  public String organisationSearchResults(
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @RequestParam(required = false) final String journeyToken,
      @RequestParam(value = "page", defaultValue = "0") final int page,
      @RequestParam(value = "size", defaultValue = "10") final int size,
      final HttpSession session,
      final Model model) {
    synchronized (WebUtils.getSessionMutex(session)) {
      final var flow =
          requireOrganisationFlow(ebsCase, user, awardPath, awardId, journeyToken, session);
      model.addAttribute("journeyToken", flow.getJourneyToken());
      if (!StringUtils.hasText(flow.getSearchCriteria().getName())) {
        return "redirect:"
            + organisationBasePath(awardPath, awardId)
            + "/search?back=true&journeyToken="
            + flow.getJourneyToken();
      }
      try {
        flow.setSearchResults(
            opponentService.getOrganisations(
                flow.getSearchCriteria(), user.getLoginId(), user.getUserType(), page, size));
      } catch (TooManyResultsException ex) {
        flow.setSearchResults(null);
        final var errors =
            new BeanPropertyBindingResult(flow.getSearchCriteria(), ORGANISATION_SEARCH_FORM);
        errors.reject(
            "organisation.search.tooManyResults",
            "Your search returned too many results. Please refine your search criteria and try"
                + " again.");
        model.addAttribute(ORGANISATION_SEARCH_FORM, flow.getSearchCriteria());
        model.addAttribute(BindingResult.MODEL_KEY_PREFIX + ORGANISATION_SEARCH_FORM, errors);
        return organisationSearchPage(model);
      }
      model.addAttribute("organisationSearchResults", flow.getSearchResults());
      model.addAttribute(
          "currentUrl",
          organisationBasePath(awardPath, awardId)
              + "/search/results?journeyToken="
              + flow.getJourneyToken());
      return ORGANISATION_RESULTS_VIEW;
    }
  }

  /**
   * Loads a shared organisation offered in the draft's results and asks for its relationships.
   *
   * @param awardPath the award type's URL path segment
   * @param awardId the selected award
   * @param partyId the selected organisation's party id
   * @param ebsCase the current case
   * @param user the current provider user
   * @param journeyToken the token identifying the active draft
   * @param session the session holding the draft
   * @param model the model to populate
   * @return the shared organisation confirmation view
   * @throws CaabApplicationException if the organisation was not offered in the results
   */
  @GetMapping("/organisation/{partyId}/select")
  public String selectSharedOrganisation(
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      @PathVariable final String partyId,
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @RequestParam(required = false) final String journeyToken,
      final HttpSession session,
      final Model model) {
    synchronized (WebUtils.getSessionMutex(session)) {
      final var flow =
          requireOrganisationFlow(ebsCase, user, awardPath, awardId, journeyToken, session);
      model.addAttribute("journeyToken", flow.getJourneyToken());
      final boolean offered =
          flow.getSearchResults() != null
              && flow.getSearchResults().getContent() != null
              && flow.getSearchResults().getContent().stream()
                  .anyMatch(org -> partyId.equals(org.getPartyId()));
      if (!offered) {
        log.error("Invalid organisation partyId {} selected for award party", partyId);
        throw new CaabApplicationException("Invalid organisation partyId supplied");
      }
      final OrganisationOpponentFormData organisation =
          opponentService.getOrganisationOpponent(partyId, user.getLoginId(), user.getUserType());
      organisation.setPartyId(partyId);
      organisation.setShared(true);
      flow.setSelectedOrganisation(organisation);
      model.addAttribute(ORGANISATION_SHARED_FORM, copyOrganisation(organisation));
      return organisationSharedPage(model);
    }
  }

  /**
   * Validates the relationships for the selected shared organisation and links it to the award.
   *
   * @param awardPath the award type's URL path segment
   * @param awardId the selected award
   * @param ebsCase the current case
   * @param user the current provider user
   * @param submitted the submitted relationship details
   * @param bindingErrors the binding and baseline validation result
   * @param journeyToken the token identifying the active draft
   * @param session the session holding the draft
   * @param model the model to populate on validation or save failure
   * @return the confirmation view on failure, otherwise a redirect to the liable parties summary
   */
  @PostMapping("/organisation/select")
  public String confirmSharedOrganisation(
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @Validated @ModelAttribute(ORGANISATION_SHARED_FORM)
          final OrganisationOpponentFormData submitted,
      final BindingResult bindingErrors,
      @RequestParam(required = false) final String journeyToken,
      final HttpSession session,
      final Model model) {
    synchronized (WebUtils.getSessionMutex(session)) {
      final var flow =
          requireOrganisationFlow(ebsCase, user, awardPath, awardId, journeyToken, session);
      model.addAttribute("journeyToken", flow.getJourneyToken());
      if (flow.getSelectedOrganisation() == null) {
        return "redirect:"
            + organisationBasePath(awardPath, awardId)
            + "/search?back=true&journeyToken="
            + flow.getJourneyToken();
      }
      // Bind the user's entries onto a copy of the searched organisation, so the summary is
      // redisplayed from trusted data and the validator sees the organisation as shared.
      final OrganisationOpponentFormData organisation =
          copyOrganisation(flow.getSelectedOrganisation());
      organisation.setRelationshipToCase(submitted.getRelationshipToCase());
      organisation.setRelationshipToClient(submitted.getRelationshipToClient());
      organisation.setOtherInformation(submitted.getOtherInformation());
      final var errors = new BeanPropertyBindingResult(organisation, ORGANISATION_SHARED_FORM);
      errors.addAllErrors(bindingErrors);
      organisationOpponentValidator.validate(organisation, errors);
      if (!errors.hasErrors()
          && caseOutcomeService.hasSharedOrganisationLiableParty(
              flow.getCaseReference(),
              flow.getProviderId(),
              awardPath,
              awardId,
              organisation.getPartyId())) {
        errors.reject(
            "liableParty.organisation.duplicate",
            "This organisation has already been added to this award.");
      }
      if (!errors.hasErrors()) {
        try {
          caseOutcomeService.createOrganisationLiableParty(
              flow.getCaseReference(),
              flow.getProviderId(),
              awardPath,
              awardId,
              user.getLoginId(),
              organisation);
        } catch (CaabApiClientException | IllegalStateException ex) {
          log.warn("Failed to save shared organisation for {} award {}", awardPath, awardId, ex);
          errors.reject(
              "liableParty.save.failed", "We could not save the party. Please try again.");
        }
      }
      if (errors.hasErrors()) {
        model.addAttribute(ORGANISATION_SHARED_FORM, organisation);
        model.addAttribute(BindingResult.MODEL_KEY_PREFIX + ORGANISATION_SHARED_FORM, errors);
        return organisationSharedPage(model);
      }
      return completeOrganisationFlow(flow, session, awardPath, awardId);
    }
  }

  /**
   * Displays the form for a new organisation.
   *
   * @param awardPath the award type's URL path segment
   * @param awardId the selected award
   * @param ebsCase the current case
   * @param user the current provider user
   * @param journeyToken the token identifying the active draft
   * @param session the session holding the draft
   * @param model the model to populate
   * @return the new organisation view
   */
  @GetMapping("/organisation/create")
  public String newOrganisation(
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @RequestParam(required = false) final String journeyToken,
      final HttpSession session,
      final Model model) {
    synchronized (WebUtils.getSessionMutex(session)) {
      final var flow =
          requireOrganisationFlow(ebsCase, user, awardPath, awardId, journeyToken, session);
      model.addAttribute("journeyToken", flow.getJourneyToken());
      final var organisation = new OrganisationOpponentFormData();
      organisation.setOrganisationName(flow.getSearchCriteria().getName());
      organisation.setOrganisationType(flow.getSearchCriteria().getType());
      organisation.setCity(flow.getSearchCriteria().getCity());
      organisation.setPostcode(flow.getSearchCriteria().getPostcode());
      model.addAttribute(ORGANISATION_CREATE_FORM, organisation);
      return organisationCreatePage(model);
    }
  }

  /**
   * Validates a new organisation and links it to the award.
   *
   * @param awardPath the award type's URL path segment
   * @param awardId the selected award
   * @param ebsCase the current case
   * @param user the current provider user
   * @param organisation the submitted organisation
   * @param errors the binding and validation result
   * @param journeyToken the token identifying the active draft
   * @param session the session holding the draft
   * @param model the model to populate on validation or save failure
   * @return the new organisation view on failure, otherwise a redirect to the liable parties
   */
  @PostMapping("/organisation/create")
  public String createOrganisation(
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @Validated @ModelAttribute(ORGANISATION_CREATE_FORM)
          final OrganisationOpponentFormData organisation,
      final BindingResult errors,
      @RequestParam(required = false) final String journeyToken,
      final HttpSession session,
      final Model model) {
    synchronized (WebUtils.getSessionMutex(session)) {
      final var flow =
          requireOrganisationFlow(ebsCase, user, awardPath, awardId, journeyToken, session);
      model.addAttribute("journeyToken", flow.getJourneyToken());
      organisation.setType(OPPONENT_TYPE_ORGANISATION);
      organisation.setShared(false);
      organisationOpponentValidator.validate(organisation, errors);
      if (!errors.hasErrors()) {
        try {
          caseOutcomeService.createOrganisationLiableParty(
              flow.getCaseReference(),
              flow.getProviderId(),
              awardPath,
              awardId,
              user.getLoginId(),
              organisation);
        } catch (CaabApiClientException | IllegalStateException ex) {
          log.warn("Failed to save new organisation for {} award {}", awardPath, awardId, ex);
          errors.reject(
              "liableParty.save.failed", "We could not save the party. Please try again.");
        }
      }
      if (errors.hasErrors()) {
        return organisationCreatePage(model);
      }
      return completeOrganisationFlow(flow, session, awardPath, awardId);
    }
  }

  /**
   * Discards the active organisation draft and returns to the liable parties summary.
   *
   * @param awardPath the award type's URL path segment
   * @param awardId the selected award
   * @param ebsCase the current case
   * @param user the current provider user
   * @param journeyToken the token identifying the active draft
   * @param session the session holding the draft
   * @return a redirect to the selected award's liable parties summary
   */
  @GetMapping("/organisation/cancel")
  public String cancelOrganisation(
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @RequestParam(required = false) final String journeyToken,
      final HttpSession session) {
    synchronized (WebUtils.getSessionMutex(session)) {
      requireOrganisationFlow(ebsCase, user, awardPath, awardId, journeyToken, session);
      session.removeAttribute(ORGANISATION_LIABLE_PARTY_FLOW);
      return liablePartiesRedirect(awardPath, awardId);
    }
  }

  private String completeOrganisationFlow(
      final OrganisationLiablePartyFlow flow,
      final HttpSession session,
      final String awardPath,
      final Integer awardId) {
    if (session.getAttribute(ORGANISATION_LIABLE_PARTY_FLOW) == flow) {
      session.removeAttribute(ORGANISATION_LIABLE_PARTY_FLOW);
    }
    return liablePartiesRedirect(awardPath, awardId);
  }

  /**
   * Requires edit permission and a draft matching the current case, provider, award and journey.
   *
   * @throws CaabApplicationException if the draft context is invalid or the case is read-only
   */
  private OrganisationLiablePartyFlow requireOrganisationFlow(
      final ApplicationDetail ebsCase,
      final UserDetail user,
      final String awardPath,
      final Integer awardId,
      final String journeyToken,
      final HttpSession session) {
    requireEditPermission(ebsCase);
    if (!(session.getAttribute(ORGANISATION_LIABLE_PARTY_FLOW)
            instanceof OrganisationLiablePartyFlow flow)
        || !Objects.equals(flow.getCaseReference(), ebsCase.getCaseReferenceNumber())
        || !Objects.equals(flow.getProviderId(), providerId(user))
        || !Objects.equals(flow.getAwardPath(), awardPath)
        || !Objects.equals(flow.getAwardId(), awardId)
        || !Objects.equals(flow.getJourneyToken(), journeyToken)) {
      throw new CaabApplicationException(
          "No organisation party draft exists for this case and award");
    }
    return flow;
  }

  private static Integer providerId(final UserDetail user) {
    return user.getProvider().getId().intValue();
  }

  private static OrganisationOpponentFormData copyOrganisation(
      final OrganisationOpponentFormData source) {
    final var copy = new OrganisationOpponentFormData();
    BeanUtils.copyProperties(source, copy);
    return copy;
  }

  private String organisationSearchPage(final Model model) {
    new DropdownBuilder(model)
        .addDropdown(
            "organisationTypes", lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES))
        .build();
    return ORGANISATION_SEARCH_VIEW;
  }

  private String organisationSharedPage(final Model model) {
    model.addAttribute("relationshipsToCase", organisationRelationshipsToCase());
    new DropdownBuilder(model)
        .addDropdown(
            "relationshipsToClient",
            lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT))
        .build();
    return ORGANISATION_SHARED_VIEW;
  }

  private String organisationCreatePage(final Model model) {
    model.addAttribute("relationshipsToCase", organisationRelationshipsToCase());
    model.addAttribute(
        "currentlyTradingOptions",
        List.of(
            new BooleanDropdownOption("false", "No"), new BooleanDropdownOption("true", "Yes")));
    new DropdownBuilder(model)
        .addDropdown(
            "organisationTypes", lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES))
        .addDropdown(
            "relationshipsToClient",
            lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT))
        .addDropdown("countries", lookupService.getCountries())
        .build();
    return ORGANISATION_CREATE_VIEW;
  }

  private List<RelationshipToCaseLookupValueDetail> organisationRelationshipsToCase() {
    return lookupService
        .getOrganisationToCaseRelationships()
        .blockOptional()
        .orElseThrow(() -> new CaabApplicationException("Failed to retrieve lookup data"))
        .getContent();
  }

  private static String organisationBasePath(final String awardPath, final Integer awardId) {
    return "/case/outcome-and-awards/" + awardPath + "/" + awardId + "/liable-parties/organisation";
  }

  private static String liablePartiesRedirect(final String awardPath, final Integer awardId) {
    return "redirect:/case/outcome-and-awards/" + awardPath + "/" + awardId + "/liable-parties";
  }
}
