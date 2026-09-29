package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_LAND;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_OTHER_ASSET;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.AWARD_TYPE_FORM;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.AwardTypeForm;
import uk.gov.laa.ccms.caab.bean.award.FinancialAwardFormData;
import uk.gov.laa.ccms.caab.bean.award.LandAwardFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.AwardTypeValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.FinancialAwardValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.LandAwardValidator;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.constants.CommonValueConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.FinancialAwardMapper;
import uk.gov.laa.ccms.caab.mapper.LandAwardMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardRequest;
import uk.gov.laa.ccms.caab.model.LandAwardDetail;
import uk.gov.laa.ccms.caab.model.LandAwardRequest;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller responsible for handling award-related requests. */
@RequiredArgsConstructor
@Controller
@Slf4j
@SessionAttributes(AWARD_TYPE_FORM)
public class AwardController {

  private final LookupService lookupService;
  private final CaseOutcomeService caseOutcomeService;
  private final AwardTypeValidator awardTypeValidator;
  private final FinancialAwardValidator financialAwardValidator;
  private final FinancialAwardMapper financialAwardMapper;
  private final LandAwardValidator landAwardValidator;
  private final LandAwardMapper landAwardMapper;

  /**
   * Displays the Select Award Type screen.
   *
   * @param model the model used to pass data to the award type dropdown in the view
   * @return The Select Award Type view
   */
  @GetMapping("/case/outcome-and-awards/award-type")
  public String displaySelectAwardType(final Model model) {
    model.addAttribute(AWARD_TYPE_FORM, new AwardTypeForm());
    model.addAttribute("awardTypes", getAwardTypes());
    return "application/select-award-type";
  }

  /**
   * Handles submission of the Select Award Type form.
   *
   * @param awardTypeForm form containing the award type selected by the user
   * @return redirect to the appropriate award details screen or select award type view if the
   *     selected award type code cannot be found or its award type category is unsupported
   */
  @PostMapping("/case/outcome-and-awards/award-type")
  public String selectAwardType(
      @ModelAttribute(AWARD_TYPE_FORM) final AwardTypeForm awardTypeForm,
      final BindingResult bindingResult,
      final Model model) {
    awardTypeValidator.validate(awardTypeForm, bindingResult);

    final List<AwardTypeLookupValueDetail> awardTypes = getAwardTypes();

    if (bindingResult.hasErrors()) {
      model.addAttribute("awardTypes", awardTypes);
      return "application/select-award-type";
    }

    final Optional<AwardTypeLookupValueDetail> selectedAwardTypeOpt =
        awardTypes.stream()
            .filter(
                lookupItem ->
                    Objects.equals(lookupItem.getCode(), awardTypeForm.getAwardTypeCode()))
            .findFirst();

    if (selectedAwardTypeOpt.isEmpty()) {
      bindingResult.rejectValue(
          "awardTypeCode", "awardType.invalid", "Please select a valid award type.");
      model.addAttribute("awardTypes", awardTypes);
      return "application/select-award-type";
    }

    final AwardTypeLookupValueDetail selectedAwardType = selectedAwardTypeOpt.get();
    awardTypeForm.setDescription(
        StringUtils.capitalize(selectedAwardType.getAwardType().toLowerCase()));
    awardTypeForm.setAwardType(selectedAwardType.getAwardType());

    return switch (selectedAwardType.getAwardType()) {
      case AWARD_TYPE_COST -> "redirect:/case/outcome-and-awards/cost-award";
      case AWARD_TYPE_OTHER_ASSET -> "redirect:/case/outcome-and-awards/asset";
      case AWARD_TYPE_LAND -> "redirect:/case/outcome-and-awards/land-property";
      case AWARD_TYPE_FINANCIAL -> "redirect:/case/outcome-and-awards/financial-award";
      default -> {
        bindingResult.rejectValue(
            "awardTypeCode", "awardType.unsupported", "The selected award type is not supported.");
        model.addAttribute("awardTypes", awardTypes);
        yield "application/select-award-type";
      }
    };
  }

  /**
   * Displays the financial award screen for a new or existing award.
   *
   * @param financialAwardId existing financial award id, when editing
   * @return the financial award view
   */
  @GetMapping(
      value = {
        "/case/outcome-and-awards/financial-award",
        "/case/outcome-and-awards/financial-award/{awardId}"
      })
  public String financialAward(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(value = AWARD_TYPE_FORM, required = false)
          final AwardTypeForm awardTypeForm,
      @PathVariable(value = "awardId", required = false) final Integer financialAwardId,
      final Model model) {
    final FinancialAwardFormData formData;
    if (financialAwardId == null) {
      if (awardTypeForm == null
          || !StringUtils.hasText(awardTypeForm.getAwardTypeCode())
          || !StringUtils.hasText(awardTypeForm.getAwardType())
          || !StringUtils.hasText(awardTypeForm.getDescription())) {
        log.warn("Financial award page requested without complete award type details");
        return "redirect:/case/outcome-and-awards";
      }

      formData = new FinancialAwardFormData();
      formData.setAwardCode(awardTypeForm.getAwardTypeCode());
      formData.setAwardType(awardTypeForm.getAwardType());
      formData.setDescription(awardTypeForm.getDescription());
    } else {
      final FinancialAwardDetail award =
          caseOutcomeService
              .getFinancialAward(
                  ebsCase.getCaseReferenceNumber(),
                  user.getProvider().getId().intValue(),
                  financialAwardId)
              .orElseThrow(
                  () ->
                      new CaabApplicationException(
                          "Could not find financial award with id: " + financialAwardId));
      formData = financialAwardMapper.toFinancialAwardFormData(award);
    }

    model.addAttribute("financialAward", formData);
    populateFinancialAwardDropdowns(model);
    return "application/financial-award";
  }

  /**
   * Creates or updates a financial award and returns to the outcome and awards screen.
   *
   * @return redirect to the outcome and awards screen when successful
   */
  @PostMapping("/case/outcome-and-awards/financial-award")
  public String financialAward(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(value = AWARD_TYPE_FORM, required = false)
          final AwardTypeForm awardTypeForm,
      @ModelAttribute("financialAward") final FinancialAwardFormData financialAward,
      final BindingResult bindingResult,
      final Model model) {
    financialAwardValidator.validate(financialAward, bindingResult);

    if (financialAward.getId() == null
        && (awardTypeForm == null
            || !StringUtils.hasText(awardTypeForm.getAwardTypeCode())
            || !StringUtils.hasText(awardTypeForm.getAwardType())
            || !StringUtils.hasText(awardTypeForm.getDescription())
            || !Objects.equals(financialAward.getAwardCode(), awardTypeForm.getAwardTypeCode())
            || !Objects.equals(financialAward.getAwardType(), awardTypeForm.getAwardType())
            || !Objects.equals(financialAward.getDescription(), awardTypeForm.getDescription()))) {
      bindingResult.reject(
          "financialAward.awardType.mismatch",
          "The award type details are invalid for your session. Please select an award type again.");
    }

    if (bindingResult.hasErrors()) {
      populateFinancialAwardDropdowns(model);
      return "application/financial-award";
    }

    final FinancialAwardRequest request =
        financialAwardMapper.toFinancialAwardRequest(financialAward);
    try {
      if (financialAward.getId() == null) {
        caseOutcomeService.createFinancialAward(
            ebsCase.getCaseReferenceNumber(),
            user.getProvider().getId().intValue(),
            request,
            user.getLoginId());
      } else {
        caseOutcomeService.updateFinancialAward(
            ebsCase.getCaseReferenceNumber(),
            user.getProvider().getId().intValue(),
            financialAward.getId(),
            request,
            user.getLoginId());
      }
    } catch (CaabApiClientException ex) {
      log.warn("Failed to save financial award with id: {}", financialAward.getId(), ex);
      bindingResult.reject(
          "financialAward.save.failed", "We could not save the financial award. Please try again.");
      populateFinancialAwardDropdowns(model);
      return "application/financial-award";
    }

    return "redirect:/case/outcome-and-awards";
  }

  /** Displays the land award screen for a new or existing award. */
  @GetMapping(
      value = {
        "/case/outcome-and-awards/land-property",
        "/case/outcome-and-awards/land-property/{awardId}"
      })
  public String landAward(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(value = AWARD_TYPE_FORM, required = false)
          final AwardTypeForm awardTypeForm,
      @PathVariable(value = "awardId", required = false) final Integer landAwardId,
      final Model model) {
    final LandAwardFormData formData;
    if (landAwardId == null) {
      if (awardTypeForm == null
          || !StringUtils.hasText(awardTypeForm.getAwardTypeCode())
          || !StringUtils.hasText(awardTypeForm.getAwardType())
          || !AWARD_TYPE_LAND.equals(awardTypeForm.getAwardType())) {
        log.warn("Land award page requested without complete land award type details");
        return "redirect:/case/outcome-and-awards";
      }
      formData = new LandAwardFormData();
      formData.setAwardCode(awardTypeForm.getAwardTypeCode());
      formData.setAwardType(awardTypeForm.getAwardType());
    } else {
      final LandAwardDetail award =
          caseOutcomeService
              .getLandAward(
                  ebsCase.getCaseReferenceNumber(),
                  user.getProvider().getId().intValue(),
                  landAwardId)
              .orElseThrow(
                  () ->
                      new CaabApplicationException(
                          "Could not find land award with id: " + landAwardId));
      formData = landAwardMapper.toLandAwardFormData(award);
    }

    model.addAttribute("landAward", formData);
    populateLandAwardDropdowns(model);
    return "application/land-property";
  }

  /** Calculates or persists a land award. */
  @PostMapping("/case/outcome-and-awards/land-property")
  public String landAward(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(value = AWARD_TYPE_FORM, required = false)
          final AwardTypeForm awardTypeForm,
      @ModelAttribute("landAward") final LandAwardFormData landAward,
      @RequestParam(value = "action", defaultValue = "next") final String action,
      final BindingResult bindingResult,
      final Model model) {
    landAwardValidator.validate(landAward, bindingResult);

    if (landAward.getId() == null
        && (awardTypeForm == null
            || !Objects.equals(landAward.getAwardCode(), awardTypeForm.getAwardTypeCode())
            || !Objects.equals(landAward.getAwardType(), awardTypeForm.getAwardType())
            || !AWARD_TYPE_LAND.equals(landAward.getAwardType()))) {
      bindingResult.reject(
          "landAward.awardType.mismatch",
          "The award type details are invalid for your session. Please select an award type again.");
    }

    if (!bindingResult.hasErrors()) {
      landAward.setEquity(
          new BigDecimal(landAward.getValuationAmount())
              .subtract(new BigDecimal(landAward.getMortgageAmountDue()))
              .toPlainString());
    }

    if (bindingResult.hasErrors() || "calculate".equals(action)) {
      populateLandAwardDropdowns(model);
      return "application/land-property";
    }

    final LandAwardRequest request = landAwardMapper.toLandAwardRequest(landAward);
    try {
      if (landAward.getId() == null) {
        caseOutcomeService.createLandAward(
            ebsCase.getCaseReferenceNumber(),
            user.getProvider().getId().intValue(),
            request,
            user.getLoginId());
      } else {
        caseOutcomeService.updateLandAward(
            ebsCase.getCaseReferenceNumber(),
            user.getProvider().getId().intValue(),
            landAward.getId(),
            request,
            user.getLoginId());
      }
    } catch (CaabApiClientException | IllegalStateException ex) {
      log.warn("Failed to save land award with id: {}", landAward.getId(), ex);
      bindingResult.reject(
          "landAward.save.failed", "We could not save the land award. Please try again.");
      populateLandAwardDropdowns(model);
      return "application/land-property";
    }

    return "redirect:/case/outcome-and-awards";
  }

  private void populateFinancialAwardDropdowns(final Model model) {
    model.addAttribute(
        "interimAwardOptions",
        Optional.ofNullable(
                lookupService
                    .getCommonValues(CommonValueConstants.COMMON_VALUE_INTERIM_AWARD)
                    .block())
            .map(CommonLookupDetail::getContent)
            .orElse(Collections.emptyList()));
    model.addAttribute(
        "awardedByOptions",
        Optional.ofNullable(
                lookupService.getCommonValues(CommonValueConstants.COMMON_VALUE_AWARDED_BY).block())
            .map(CommonLookupDetail::getContent)
            .orElse(Collections.emptyList()));
  }

  private void populateLandAwardDropdowns(final Model model) {
    model.addAttribute(
        "valuationBasisOptions",
        getCommonValues(CommonValueConstants.COMMON_VALUE_VALUATION_BASIS));
    model.addAttribute(
        "awardedByOptions", getCommonValues(CommonValueConstants.COMMON_VALUE_AWARDED_BY));
    model.addAttribute(
        "recoveryOptions", getCommonValues(CommonValueConstants.COMMON_VALUE_LAND_RECOVERY));
    model.addAttribute(
        "landRegistrationOptions",
        getCommonValues(CommonValueConstants.COMMON_VALUE_LAND_REGISTRATION));
    model.addAttribute("yesNoOptions", getCommonValues(CommonValueConstants.COMMON_VALUE_YES_NO));
  }

  private List<?> getCommonValues(final String type) {
    return Optional.ofNullable(lookupService.getCommonValues(type).block())
        .map(CommonLookupDetail::getContent)
        .orElse(Collections.emptyList());
  }

  private List<AwardTypeLookupValueDetail> getAwardTypes() {
    return lookupService
        .getAwardTypes()
        .blockOptional()
        .map(AwardTypeLookupDetail::getContent)
        .orElse(Collections.emptyList());
  }
}
