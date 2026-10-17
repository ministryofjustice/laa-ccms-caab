package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_LAND;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_OTHER_ASSET;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.AWARD_TYPE_FORM;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import uk.gov.laa.ccms.caab.bean.award.CostAwardFormData;
import uk.gov.laa.ccms.caab.bean.award.FinancialAwardFormData;
import uk.gov.laa.ccms.caab.bean.award.LandAwardFormData;
import uk.gov.laa.ccms.caab.bean.award.OtherAssetAwardFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.AwardTypeValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.CostAwardValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.FinancialAwardValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.LandAwardValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.OtherAssetAwardValidator;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.constants.CommonValueConstants;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.CostAwardMapper;
import uk.gov.laa.ccms.caab.mapper.FinancialAwardMapper;
import uk.gov.laa.ccms.caab.mapper.LandAwardMapper;
import uk.gov.laa.ccms.caab.mapper.OtherAssetAwardMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardRequest;
import uk.gov.laa.ccms.caab.model.LandAwardDetail;
import uk.gov.laa.ccms.caab.model.LandAwardRequest;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardRequest;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller responsible for handling award-related requests. */
@RequiredArgsConstructor
@Controller
@Slf4j
@SessionAttributes(AWARD_TYPE_FORM)
public class AwardController {

  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

  private final LookupService lookupService;
  private final CaseOutcomeService caseOutcomeService;
  private final AwardTypeValidator awardTypeValidator;
  private final CostAwardValidator costAwardValidator;
  private final FinancialAwardValidator financialAwardValidator;
  private final CostAwardMapper costAwardMapper;
  private final FinancialAwardMapper financialAwardMapper;
  private final OtherAssetAwardValidator otherAssetAwardValidator;
  private final OtherAssetAwardMapper otherAssetAwardMapper;
  private final LandAwardValidator landAwardValidator;
  private final LandAwardMapper landAwardMapper;

  /**
   * Displays the Select Award Type screen.
   *
   * @param model the model used to pass data to the award type dropdown in the view
   * @return The Select Award Type view
   */
  @GetMapping("/case/outcome-and-awards/award-type")
  public String displaySelectAwardType(
      @SessionAttribute(value = CASE, required = false) final ApplicationDetail ebsCase,
      final Model model) {
    if (!hasOutcomeEditPermission(ebsCase)) {
      log.warn(
          "Rejecting award type selection for read-only or missing case {}",
          ebsCase == null ? null : ebsCase.getCaseReferenceNumber());
      return "redirect:/case/outcome-and-awards";
    }

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
      @SessionAttribute(value = CASE, required = false) final ApplicationDetail ebsCase,
      @ModelAttribute(AWARD_TYPE_FORM) final AwardTypeForm awardTypeForm,
      final BindingResult bindingResult,
      final Model model) {
    if (!hasOutcomeEditPermission(ebsCase)) {
      log.warn(
          "Rejecting award type selection for read-only or missing case {}",
          ebsCase == null ? null : ebsCase.getCaseReferenceNumber());
      return "redirect:/case/outcome-and-awards";
    }

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
    if (!hasOutcomeEditPermission(ebsCase)) {
      log.warn(
          "Rejecting financial award form load for read-only case {}",
          ebsCase.getCaseReferenceNumber());
      return "redirect:/case/outcome-and-awards";
    }

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
    if (!hasOutcomeEditPermission(ebsCase)) {
      log.warn(
          "Rejecting financial award update/create for read-only case {}",
          ebsCase.getCaseReferenceNumber());
      return "redirect:/case/outcome-and-awards";
    }

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
            user.getLoginId(),
            ebsCase.getCaseOutcome());
      } else {
        caseOutcomeService.updateFinancialAward(
            ebsCase.getCaseReferenceNumber(),
            user.getProvider().getId().intValue(),
            financialAward.getId(),
            request,
            user.getLoginId());
      }
    } catch (CaabApiClientException | IllegalStateException ex) {
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
    if (!hasOutcomeEditPermission(ebsCase)) {
      log.warn(
          "Rejecting land award form load for read-only case {}", ebsCase.getCaseReferenceNumber());
      return "redirect:/case/outcome-and-awards";
    }

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
    if (!hasOutcomeEditPermission(ebsCase)) {
      log.warn(
          "Rejecting land award update/create for read-only case {}",
          ebsCase.getCaseReferenceNumber());
      return "redirect:/case/outcome-and-awards";
    }

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

  /** Displays the other asset award screen for a new or existing award. */
  @GetMapping(
      value = {"/case/outcome-and-awards/asset", "/case/outcome-and-awards/asset/{awardId}"})
  public String otherAssetAward(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(value = AWARD_TYPE_FORM, required = false)
          final AwardTypeForm awardTypeForm,
      @PathVariable(value = "awardId", required = false) final Integer otherAssetAwardId,
      final Model model) {
    if (!hasOutcomeEditPermission(ebsCase)) {
      log.warn(
          "Rejecting other asset award form load for read-only case {}",
          ebsCase.getCaseReferenceNumber());
      return "redirect:/case/outcome-and-awards";
    }

    final OtherAssetAwardFormData formData;
    if (otherAssetAwardId == null) {
      if (awardTypeForm == null
          || !StringUtils.hasText(awardTypeForm.getAwardTypeCode())
          || !StringUtils.hasText(awardTypeForm.getAwardType())
          || !AWARD_TYPE_OTHER_ASSET.equals(awardTypeForm.getAwardType())) {
        log.warn("Other asset award page requested without complete award type details");
        return "redirect:/case/outcome-and-awards";
      }
      formData = new OtherAssetAwardFormData();
      formData.setAwardCode(awardTypeForm.getAwardTypeCode());
      formData.setAwardType(awardTypeForm.getAwardType());
    } else {
      final OtherAssetAwardDetail award =
          caseOutcomeService
              .getOtherAssetAward(
                  ebsCase.getCaseReferenceNumber(),
                  user.getProvider().getId().intValue(),
                  otherAssetAwardId)
              .orElseThrow(
                  () ->
                      new CaabApplicationException(
                          "Could not find other asset award with id: " + otherAssetAwardId));
      formData = otherAssetAwardMapper.toOtherAssetAwardFormData(award);
    }

    model.addAttribute("otherAssetAward", formData);
    populateOtherAssetAwardDropdowns(model);
    return "application/other-asset-award";
  }

  /** Creates or updates an other asset award and returns to the outcome and awards screen. */
  @PostMapping("/case/outcome-and-awards/asset")
  public String otherAssetAward(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(value = AWARD_TYPE_FORM, required = false)
          final AwardTypeForm awardTypeForm,
      @ModelAttribute("otherAssetAward") final OtherAssetAwardFormData otherAssetAward,
      final BindingResult bindingResult,
      final Model model) {
    if (!hasOutcomeEditPermission(ebsCase)) {
      log.warn(
          "Rejecting other asset award update/create for read-only case {}",
          ebsCase.getCaseReferenceNumber());
      return "redirect:/case/outcome-and-awards";
    }

    otherAssetAwardValidator.validate(otherAssetAward, bindingResult);

    if (otherAssetAward.getId() == null
        && (awardTypeForm == null
            || !StringUtils.hasText(awardTypeForm.getAwardTypeCode())
            || !StringUtils.hasText(awardTypeForm.getAwardType())
            || !Objects.equals(awardTypeForm.getAwardType(), AWARD_TYPE_OTHER_ASSET)
            || !Objects.equals(otherAssetAward.getAwardCode(), awardTypeForm.getAwardTypeCode())
            || !Objects.equals(otherAssetAward.getAwardType(), awardTypeForm.getAwardType()))) {
      bindingResult.reject(
          "otherAssetAward.awardType.mismatch",
          "The award type details are invalid for your session. Please select an award type again.");
    }

    if (bindingResult.hasErrors()) {
      populateOtherAssetAwardDropdowns(model);
      return "application/other-asset-award";
    }

    final OtherAssetAwardRequest request =
        otherAssetAwardMapper.toOtherAssetAwardRequest(otherAssetAward);
    try {
      if (otherAssetAward.getId() == null) {
        caseOutcomeService.createOtherAssetAward(
            ebsCase.getCaseReferenceNumber(),
            user.getProvider().getId().intValue(),
            request,
            user.getLoginId());
      } else {
        caseOutcomeService.updateOtherAssetAward(
            ebsCase.getCaseReferenceNumber(),
            user.getProvider().getId().intValue(),
            otherAssetAward.getId(),
            request,
            user.getLoginId());
      }
    } catch (CaabApiClientException | IllegalStateException ex) {
      log.warn("Failed to save other asset award with id: {}", otherAssetAward.getId(), ex);
      bindingResult.reject(
          "otherAssetAward.save.failed",
          "We could not save the other asset award. Please try again.");
      populateOtherAssetAwardDropdowns(model);
      return "application/other-asset-award";
    }

    return "redirect:/case/outcome-and-awards";
  }

  private void populateFinancialAwardDropdowns(final Model model) {
    model.addAttribute(
        "interimAwardOptions", getCommonValues(CommonValueConstants.COMMON_VALUE_INTERIM_AWARD));
    model.addAttribute(
        "awardedByOptions", getCommonValues(CommonValueConstants.COMMON_VALUE_AWARDED_BY));
  }

  private void populateOtherAssetAwardDropdowns(final Model model) {
    model.addAttribute(
        "valuationBasisOptions",
        Optional.ofNullable(
                lookupService
                    .getCommonValues(CommonValueConstants.COMMON_VALUE_VALUATION_BASIS)
                    .block())
            .map(CommonLookupDetail::getContent)
            .orElse(Collections.emptyList()));

    model.addAttribute(
        "awardedByOptions",
        Optional.ofNullable(
                lookupService.getCommonValues(CommonValueConstants.COMMON_VALUE_AWARDED_BY).block())
            .map(CommonLookupDetail::getContent)
            .orElse(Collections.emptyList()));

    model.addAttribute(
        "recoveryOptions",
        Optional.ofNullable(
                lookupService
                    .getCommonValues(CommonValueConstants.COMMON_VALUE_RECOVERY_ASSET)
                    .block())
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
        "recoveryOptions", getCommonValues(CommonValueConstants.COMMON_VALUE_RECOVERY_ASSET));
    model.addAttribute(
        "landRegistrationOptions",
        getCommonValues(CommonValueConstants.COMMON_VALUE_LAND_REGISTRATION));
    model.addAttribute("yesNoOptions", getCommonValues(CommonValueConstants.COMMON_VALUE_YES_NO));
  }

  private List<CommonLookupValueDetail> getCommonValues(final String type) {
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

  @GetMapping(
      value = {
        "/case/outcome-and-awards/cost-award",
        "/case/outcome-and-awards/cost-award/{awardId}"
      })
  public String costAward(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(value = AWARD_TYPE_FORM, required = false)
          final AwardTypeForm awardTypeForm,
      @PathVariable(value = "awardId", required = false) final Integer costAwardId,
      final Model model) {
    if (!hasOutcomeEditPermission(ebsCase)) {
      log.warn(
          "Rejecting cost award form load for read-only case {}", ebsCase.getCaseReferenceNumber());
      return "redirect:/case/outcome-and-awards";
    }

    final CostAwardFormData costAwardFormData;
    if (costAwardId == null) {
      if (!hasValidCostAwardTypeSelection(awardTypeForm)) {
        log.warn("Cost award page requested without complete award type details");
        return "redirect:/case/outcome-and-awards";
      }

      costAwardFormData = new CostAwardFormData();
      costAwardFormData.setAwardCode(awardTypeForm.getAwardTypeCode());
      costAwardFormData.setAwardType(awardTypeForm.getAwardType());
      costAwardFormData.setDescription(awardTypeForm.getDescription());
    } else {
      final CostAwardDetail award =
          caseOutcomeService
              .getCostAward(
                  ebsCase.getCaseReferenceNumber(),
                  user.getProvider().getId().intValue(),
                  costAwardId)
              .orElseThrow(
                  () ->
                      new CaabApplicationException(
                          "Could not find cost award with id: " + costAwardId));
      costAwardFormData = costAwardMapper.toCostAwardFormData(award);
    }

    initialiseCostAwardAmounts(costAwardFormData);
    populateCostAwardPage(model, costAwardFormData);
    return "application/cost-award";
  }

  /**
   * Creates or updates a cost award and returns to the outcome and awards screen.
   *
   * @return redirect to the outcome and awards screen when successful
   */
  @PostMapping("/case/outcome-and-awards/cost-award")
  public String costAward(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(value = AWARD_TYPE_FORM, required = false)
          final AwardTypeForm awardTypeForm,
      @RequestParam(value = "action", required = false) final String action,
      @ModelAttribute("costAward") final CostAwardFormData costAward,
      final BindingResult bindingResult,
      final Model model) {
    if (!hasOutcomeEditPermission(ebsCase)) {
      log.warn(
          "Rejecting cost award update/create for read-only case {}",
          ebsCase.getCaseReferenceNumber());
      return "redirect:/case/outcome-and-awards";
    }

    if ("calculate".equals(action)) {
      costAwardValidator.validateCostAmounts(costAward, bindingResult);
      initialiseCostAwardAmounts(costAward);
      populateCostAwardPage(model, costAward);
      return "application/cost-award";
    }

    costAwardValidator.validate(costAward, bindingResult);

    if (costAward.getId() == null
        && (awardTypeForm == null
            || !StringUtils.hasText(awardTypeForm.getAwardTypeCode())
            || !StringUtils.hasText(awardTypeForm.getAwardType())
            || !StringUtils.hasText(awardTypeForm.getDescription())
            || !Objects.equals(awardTypeForm.getAwardType(), AWARD_TYPE_COST)
            || !Objects.equals(costAward.getAwardCode(), awardTypeForm.getAwardTypeCode())
            || !Objects.equals(costAward.getAwardType(), awardTypeForm.getAwardType())
            || !Objects.equals(costAward.getDescription(), awardTypeForm.getDescription()))) {
      bindingResult.reject(
          "costAward.awardType.mismatch",
          "The award type details are invalid for your session. Please select an award type again.");
    }

    if (bindingResult.hasErrors()) {
      initialiseCostAwardAmounts(costAward);
      populateCostAwardPage(model, costAward);
      return "application/cost-award";
    }

    final CostAwardDetail request = costAwardMapper.toCostAward(costAward);
    try {
      if (costAward.getId() == null) {
        caseOutcomeService.createCostAward(
            ebsCase.getCaseReferenceNumber(),
            user.getProvider().getId().intValue(),
            request,
            user.getLoginId(),
            ebsCase.getCaseOutcome());
      } else {
        caseOutcomeService.updateCostAward(
            ebsCase.getCaseReferenceNumber(),
            user.getProvider().getId().intValue(),
            costAward.getId(),
            costAward,
            user.getLoginId());
      }
    } catch (CaabApiClientException | IllegalStateException ex) {
      log.warn("Failed to save cost award with id: {}", costAward.getId(), ex);
      bindingResult.reject(
          "costAward.save.failed", "We could not save the cost award. Please try again.");
      initialiseCostAwardAmounts(costAward);
      populateCostAwardPage(model, costAward);
      return "application/cost-award";
    }

    return "redirect:/case/outcome-and-awards";
  }

  private void populateCostAwardPage(final Model model, final CostAwardFormData costAwardFormData) {
    model.addAttribute("costAward", costAwardFormData);
    populateCostAwardDropdowns(model);
    model.addAttribute(
        "totalPreCertificateCosts",
        formatAmount(
            toAmount(costAwardFormData.getLaaFundedLegalCosts())
                .add(toAmount(costAwardFormData.getOtherPreCertificateCosts()))));
    model.addAttribute(
        "totalCertificateCostsAwarded",
        formatAmount(
            toAmount(costAwardFormData.getLaaRate())
                .add(toAmount(costAwardFormData.getMarketRate()))));
    model.addAttribute(
        "totalCostsAwarded",
        formatAmount(
            toAmount(costAwardFormData.getLaaFundedLegalCosts())
                .add(toAmount(costAwardFormData.getOtherPreCertificateCosts()))
                .add(toAmount(costAwardFormData.getLaaRate()))
                .add(toAmount(costAwardFormData.getMarketRate()))));
  }

  private void populateCostAwardDropdowns(final Model model) {
    model.addAttribute(
        "courtAssessmentStatuses",
        getCommonValues(CommonValueConstants.COMMON_VALUE_AWARD_ASSESSMENT_STATUS));
    model.addAttribute(
        "awardedByOptions", getCommonValues(CommonValueConstants.COMMON_VALUE_AWARDED_BY));
  }

  private boolean hasValidCostAwardTypeSelection(final AwardTypeForm awardTypeForm) {
    return awardTypeForm != null
        && StringUtils.hasText(awardTypeForm.getAwardTypeCode())
        && StringUtils.hasText(awardTypeForm.getAwardType())
        && StringUtils.hasText(awardTypeForm.getDescription())
        && AWARD_TYPE_COST.equals(awardTypeForm.getAwardType());
  }

  private boolean hasOutcomeEditPermission(final ApplicationDetail ebsCase) {
    return ebsCase != null
        && ebsCase.getAvailableFunctions() != null
        && (ebsCase.getAvailableFunctions().contains(FunctionConstants.OUTCOME_WITH_DISCHARGE)
            || ebsCase.getAvailableFunctions().contains(FunctionConstants.OUTCOME_NO_DISCHARGE));
  }

  private void initialiseCostAwardAmounts(final CostAwardFormData formData) {
    if (!StringUtils.hasText(formData.getLaaFundedLegalCosts())) {
      formData.setLaaFundedLegalCosts("0.00");
    }
    if (!StringUtils.hasText(formData.getOtherPreCertificateCosts())) {
      formData.setOtherPreCertificateCosts("0.00");
    }
    if (!StringUtils.hasText(formData.getLaaRate())) {
      formData.setLaaRate("0.00");
    }
    if (!StringUtils.hasText(formData.getMarketRate())) {
      formData.setMarketRate("0.00");
    }
    if (!StringUtils.hasText(formData.getInterestRate())) {
      formData.setInterestRate("0.00");
    }
  }

  private BigDecimal toAmount(final String value) {
    try {
      return StringUtils.hasText(value)
          ? new BigDecimal(value).setScale(2, RoundingMode.HALF_UP)
          : ZERO;
    } catch (NumberFormatException ex) {
      return ZERO;
    }
  }

  private String formatAmount(final BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }
}
