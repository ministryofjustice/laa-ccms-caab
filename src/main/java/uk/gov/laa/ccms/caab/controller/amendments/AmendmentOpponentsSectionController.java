package uk.gov.laa.ccms.caab.controller.amendments;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_OPPONENT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.opponent.AbstractOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentValidator;
import uk.gov.laa.ccms.caab.constants.CommonValueConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OpponentSectionDisplay;
import uk.gov.laa.ccms.caab.service.AmendmentService;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller for managing opponents and other parties during the amendment process. */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({CURRENT_OPPONENT})
public class AmendmentOpponentsSectionController {

  private final ApplicationService applicationService;
  private final AmendmentService amendmentService;
  private final LookupService lookupService;
  private final IndividualOpponentValidator individualOpponentValidator;

  @InitBinder(CURRENT_OPPONENT)
  protected void initBinder(WebDataBinder binder) {
    binder.addValidators(individualOpponentValidator);
  }

  /** Displays the opponents and other parties summary for an amendment. */
  @GetMapping("/amendments/sections/opponents")
  public String opponents(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final Model model) {

    ApplicationDetail application = applicationService.getApplication(applicationId).block();
    ApplicationSectionDisplay amendmentSections =
        amendmentService.getAmendmentSections(application, user);

    List<OpponentSectionDisplay> opponents =
        amendmentSections.getOpponentsAndOtherParties().getOpponents();

    // Set editable flag based on ebsId (only new opponents are editable)
    for (OpponentSectionDisplay opponent : opponents) {
      opponent.setEditable(opponent.getEbsId() == null);
    }

    model.addAttribute("opponents", opponents);

    return "amendments/sections/opponents-section";
  }

  /** Displays the form to add a new individual opponent. */
  @GetMapping("/amendments/sections/opponents/individual/add")
  public String addIndividual(final Model model) {
    model.addAttribute("amendment", true);
    populateIndividualDropdowns(model);
    model.addAttribute(CURRENT_OPPONENT, new IndividualOpponentFormData());
    return "application/opponents/opponents-individual-create";
  }

  /** Processes the form submission for adding a new individual opponent. */
  @PostMapping("/amendments/sections/opponents/individual/add")
  public String addIndividual(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @Validated @ModelAttribute(CURRENT_OPPONENT)
          final IndividualOpponentFormData opponentFormData,
      final BindingResult bindingResult,
      final Model model) {

    validateIndividual(opponentFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      model.addAttribute("amendment", true);
      populateIndividualDropdowns(model);
      return "application/opponents/opponents-individual-create";
    }

    applicationService.addOpponent(applicationId, opponentFormData, user);
    return "redirect:/amendments/sections/opponents";
  }

  /** Displays the form to edit an existing (newly added) individual opponent. */
  @GetMapping("/amendments/sections/opponents/individual/{opponentId}/edit")
  public String editIndividual(
      @PathVariable("opponentId") final Integer opponentId,
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      final Model model) {

    List<AbstractOpponentFormData> opponents = applicationService.getOpponents(applicationId);
    AbstractOpponentFormData currentOpponent =
        opponents.stream()
            .filter(o -> o.getId().equals(opponentId))
            .findFirst()
            .orElseThrow(() -> new CaabApplicationException("Opponent not found: " + opponentId));

    // Ensure it's an individual and it's editable (not from EBS)
    if (!(currentOpponent instanceof IndividualOpponentFormData)) {
      throw new CaabApplicationException("Opponent is not an individual");
    }

    if (Boolean.FALSE.equals(currentOpponent.getEditable())) {
      throw new CaabApplicationException("Original opponents cannot be edited.");
    }

    model.addAttribute("amendment", true);
    populateIndividualDropdowns(model);
    model.addAttribute(CURRENT_OPPONENT, currentOpponent);

    return "application/opponents/opponents-individual-edit";
  }

  /** Processes the form submission for editing an individual opponent. */
  @PostMapping("/amendments/sections/opponents/individual/{opponentId}/edit")
  public String editIndividual(
      @PathVariable("opponentId") final Integer opponentId,
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @Validated @ModelAttribute(CURRENT_OPPONENT)
          final IndividualOpponentFormData opponentFormData,
      final BindingResult bindingResult,
      final Model model) {

    validateIndividual(opponentFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      model.addAttribute("amendment", true);
      populateIndividualDropdowns(model);
      return "application/opponents/opponents-individual-edit";
    }

    applicationService.updateOpponent(applicationId, opponentId, opponentFormData, user);
    return "redirect:/amendments/sections/opponents";
  }

  private void validateIndividual(
      IndividualOpponentFormData opponentFormData, BindingResult bindingResult) {
    if (StringUtils.hasText(opponentFormData.getRelationshipToCase())) {
      RelationshipToCaseLookupValueDetail relationshipToCase =
          lookupService
              .getPersonToCaseRelationship(opponentFormData.getRelationshipToCase())
              .map(
                  opt ->
                      opt.orElse(
                          new RelationshipToCaseLookupValueDetail()
                              .code(opponentFormData.getRelationshipToCase())
                              .description(opponentFormData.getRelationshipToCase())))
              .blockOptional()
              .orElseThrow(
                  () ->
                      new CaabApplicationException(
                          "Failed to retrieve relationship to case: "
                              + opponentFormData.getRelationshipToCase()));

      opponentFormData.setDateOfBirthMandatory(relationshipToCase.getDateOfBirthMandatory());
    }
    individualOpponentValidator.validate(opponentFormData, bindingResult);
  }

  private void populateIndividualDropdowns(final Model model) {
    model.addAttribute(
        "contactTitles",
        lookupService
            .getCommonValues(CommonValueConstants.COMMON_VALUE_CONTACT_TITLE)
            .block()
            .getContent());
    model.addAttribute(
        "relationshipsToCase", lookupService.getPersonToCaseRelationships().block().getContent());
    model.addAttribute(
        "relationshipsToClient",
        lookupService
            .getCommonValues(CommonValueConstants.COMMON_VALUE_RELATIONSHIP_TO_CLIENT)
            .block()
            .getContent());
    model.addAttribute("countries", lookupService.getCountries().block().getContent());
    model.addAttribute(
        "legalAidedOptions", List.of(Pair.of(Boolean.FALSE, "No"), Pair.of(Boolean.TRUE, "Yes")));
  }
}
