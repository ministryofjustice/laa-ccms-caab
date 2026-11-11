package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_LAND;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_OTHER_ASSET;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ADDED_LIABLE_PARTY_OPPONENT_IDS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.AWARD_TYPE_FORM;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CREATED_LIABLE_PARTY_EBS_IDS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PARTY_LIST_ROW_IDENTITIES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CURRENCY_PATTERN;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.util.WebUtils;
import uk.gov.laa.ccms.caab.bean.AwardTypeForm;
import uk.gov.laa.ccms.caab.bean.award.CostAwardFormData;
import uk.gov.laa.ccms.caab.bean.award.FinancialAwardFormData;
import uk.gov.laa.ccms.caab.bean.award.LandAwardFormData;
import uk.gov.laa.ccms.caab.bean.award.OtherAssetAwardFormData;
import uk.gov.laa.ccms.caab.bean.award.TimeRecoveryAward;
import uk.gov.laa.ccms.caab.bean.award.TimeRecoveryFormData;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.AwardTypeValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.CostAwardValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.FinancialAwardValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.LandAwardValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.OtherAssetAwardValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.TimeRecoveryValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentOtherDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationOpponentValidator;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.client.EbsApiClientException;
import uk.gov.laa.ccms.caab.constants.CommonValueConstants;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.CostAwardMapper;
import uk.gov.laa.ccms.caab.mapper.FinancialAwardMapper;
import uk.gov.laa.ccms.caab.mapper.LandAwardMapper;
import uk.gov.laa.ccms.caab.mapper.LiablePartiesDisplayMapper;
import uk.gov.laa.ccms.caab.mapper.OpponentMapper;
import uk.gov.laa.ccms.caab.mapper.OtherAssetAwardMapper;
import uk.gov.laa.ccms.caab.mapper.context.LiablePartiesMappingContext;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardRequest;
import uk.gov.laa.ccms.caab.model.LandAwardDetail;
import uk.gov.laa.ccms.caab.model.LandAwardRequest;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardRequest;
import uk.gov.laa.ccms.caab.model.TimeRecoveryDetail;
import uk.gov.laa.ccms.caab.model.TimeRecoveryRequest;
import uk.gov.laa.ccms.caab.model.sections.LiablePartyDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.util.DateUtils;
import uk.gov.laa.ccms.caab.util.OpponentUtil;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller responsible for handling award-related requests. */
@RequiredArgsConstructor
@Controller
@Slf4j
@SessionAttributes(AWARD_TYPE_FORM)
public class AwardController {

  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

  private static final String PARTY_LIST_PATH =
      "/case/outcome-and-awards/{awardPath:cost-award|financial-award|land|asset}/{awardId}/liable-parties/add";
  private static final String PARTY_LIST_VIEW = "application/party-list-screen";

  /** Maximum number of party list render snapshots remembered per award, per session. */
  private static final int MAX_ROW_IDENTITY_SNAPSHOTS_PER_AWARD = 20;

  private final LookupService lookupService;
  private final CaseOutcomeService caseOutcomeService;
  private final ApplicationService applicationService;
  private final AwardTypeValidator awardTypeValidator;
  private final CostAwardValidator costAwardValidator;
  private final FinancialAwardValidator financialAwardValidator;
  private final CostAwardMapper costAwardMapper;
  private final FinancialAwardMapper financialAwardMapper;
  private final OtherAssetAwardValidator otherAssetAwardValidator;
  private final OtherAssetAwardMapper otherAssetAwardMapper;
  private final LandAwardValidator landAwardValidator;
  private final LandAwardMapper landAwardMapper;
  private final TimeRecoveryValidator timeRecoveryValidator;
  private final IndividualOpponentValidator individualOpponentValidator;
  private final OrganisationOpponentValidator organisationOpponentValidator;
  private final IndividualOpponentOtherDetailsValidator individualOpponentOtherDetailsValidator;
  private final OpponentMapper opponentMapper;
  private final LiablePartiesDisplayMapper liablePartiesDisplayMapper;

  /**
   * The Jakarta Bean Validation validator. The Spring {@code Validator} type is not used here as
   * every custom form validator bean also implements it, which would make injection ambiguous.
   */
  private final Validator beanValidator;

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

  /** Displays the award type before confirming deletion. */
  @GetMapping("/case/outcome-and-awards/awards/{awardType}/{awardId}/delete")
  public String displayDeleteAward(
      @SessionAttribute(value = CASE, required = false) final ApplicationDetail ebsCase,
      @PathVariable final String awardType,
      @PathVariable final Integer awardId,
      final Model model) {
    if (!hasOutcomeEditPermission(ebsCase)) {
      log.warn("Rejecting award deletion confirmation for a read-only or missing case");
      return "redirect:/case/outcome-and-awards";
    }
    model.addAttribute("awardType", awardType);
    model.addAttribute("awardId", awardId);
    return "application/delete-award";
  }

  /** Deletes the selected award only after confirmation. */
  @PostMapping("/case/outcome-and-awards/awards/{awardType}/{awardId}/delete")
  public String deleteAward(
      @SessionAttribute(value = CASE, required = false) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @PathVariable final String awardType,
      @PathVariable final Integer awardId) {
    if (!hasOutcomeEditPermission(ebsCase)) {
      log.warn("Rejecting award deletion for a read-only or missing case");
      return "redirect:/case/outcome-and-awards";
    }
    caseOutcomeService.deleteAward(
        ebsCase.getCaseReferenceNumber(),
        user.getProvider().getId().intValue(),
        awardType,
        awardId,
        user.getLoginId());
    return "redirect:/case/outcome-and-awards";
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
   * Creates or updates a financial award and opens its liable parties.
   *
   * @return redirect to the liable parties screen when successful
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
      final Integer savedAwardId;
      if (financialAward.getId() == null) {
        savedAwardId =
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
        savedAwardId = financialAward.getId();
      }
      return "redirect:/case/outcome-and-awards/financial-award/"
          + savedAwardId
          + "/liable-parties";
    } catch (CaabApiClientException | IllegalStateException ex) {
      log.warn("Failed to save financial award with id: {}", financialAward.getId(), ex);
      bindingResult.reject(
          "financialAward.save.failed", "We could not save the financial award. Please try again.");
      populateFinancialAwardDropdowns(model);
      return "application/financial-award";
    }
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
      final Integer savedAwardId;
      if (landAward.getId() == null) {
        savedAwardId =
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
        savedAwardId = landAward.getId();
      }
      if ("Y".equalsIgnoreCase(landAward.getRecoveryOfAwardTimeRelated())) {
        return "redirect:/case/outcome-and-awards/land/" + savedAwardId + "/time-related-recovery";
      }
      return "redirect:/case/outcome-and-awards/land/" + savedAwardId + "/liable-parties";
    } catch (CaabApiClientException | IllegalStateException ex) {
      log.warn("Failed to save land award with id: {}", landAward.getId(), ex);
      bindingResult.reject(
          "landAward.save.failed", "We could not save the land award. Please try again.");
      populateLandAwardDropdowns(model);
      return "application/land-property";
    }
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

  /** Creates or updates an other asset award and opens time-related recovery when selected. */
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
      final Integer savedAwardId;
      if (otherAssetAward.getId() == null) {
        savedAwardId =
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
        savedAwardId = otherAssetAward.getId();
      }
      if (Boolean.TRUE.equals(otherAssetAward.getRecoveryOfAwardTimeRelated())) {
        return "redirect:/case/outcome-and-awards/asset/" + savedAwardId + "/time-related-recovery";
      }
      return "redirect:/case/outcome-and-awards/asset/" + savedAwardId + "/liable-parties";
    } catch (CaabApiClientException | IllegalStateException ex) {
      log.warn("Failed to save other asset award with id: {}", otherAssetAward.getId(), ex);
      bindingResult.reject(
          "otherAssetAward.save.failed",
          "We could not save the other asset award. Please try again.");
      populateOtherAssetAwardDropdowns(model);
      return "application/other-asset-award";
    }
  }

  /** Displays the saved or new time-related recovery for a land or other asset award. */
  @GetMapping("/case/outcome-and-awards/{awardPath:land|asset}/{awardId}/time-related-recovery")
  public String displayTimeRecovery(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      final Model model) {
    if (!hasOutcomeEditPermission(ebsCase)) {
      log.warn(
          "Rejecting time-related recovery for read-only case {}",
          ebsCase.getCaseReferenceNumber());
      return "redirect:/case/outcome-and-awards";
    }

    final TimeRecoveryAward award = loadTimeRecoveryAward(ebsCase, user, awardPath, awardId);
    if (!Boolean.TRUE.equals(award.recoveryOfAwardTimeRelated())) {
      log.warn(
          "Time-related recovery requested for {} award {} with flag not set", awardPath, awardId);
      return redirectToRecoveryParent(awardPath, awardId);
    }

    final TimeRecoveryFormData form = new TimeRecoveryFormData();
    final TimeRecoveryDetail existing = award.timeRecovery();
    if (existing != null) {
      form.setTriggeringEvent(existing.getTriggeringEvent());
      form.setTimeRelatedRecoveryDetails(existing.getTimeRelatedRecoveryDetails());
      if (existing.getEffectiveDate() != null) {
        form.setEffectiveDate(DateUtils.convertToComponentDate(existing.getEffectiveDate()));
      }
    }
    model.addAttribute("award", award);
    model.addAttribute("awardPath", awardPath);
    model.addAttribute("awardId", awardId);
    model.addAttribute("timeRecovery", form);
    return "application/time-related-recovery";
  }

  /** Saves time-related recovery against its owning land or other asset award. */
  @PostMapping("/case/outcome-and-awards/{awardPath:land|asset}/{awardId}/time-related-recovery")
  public String saveTimeRecovery(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      @ModelAttribute("timeRecovery") final TimeRecoveryFormData form,
      final BindingResult bindingResult,
      final Model model) {
    if (!hasOutcomeEditPermission(ebsCase)) {
      log.warn(
          "Rejecting time-related recovery for read-only case {}",
          ebsCase.getCaseReferenceNumber());
      return "redirect:/case/outcome-and-awards";
    }

    final TimeRecoveryAward award = loadTimeRecoveryAward(ebsCase, user, awardPath, awardId);
    if (!Boolean.TRUE.equals(award.recoveryOfAwardTimeRelated())) {
      log.warn(
          "Time-related recovery submitted for {} award {} with flag not set", awardPath, awardId);
      return redirectToRecoveryParent(awardPath, awardId);
    }

    timeRecoveryValidator.validate(form, bindingResult);
    if (bindingResult.hasErrors()) {
      model.addAttribute("award", award);
      model.addAttribute("awardPath", awardPath);
      model.addAttribute("awardId", awardId);
      return "application/time-related-recovery";
    }

    final TimeRecoveryRequest request =
        new TimeRecoveryRequest(form.getTriggeringEvent(), form.getTimeRelatedRecoveryDetails())
            .effectiveDate(
                StringUtils.hasText(form.getEffectiveDate())
                    ? DateUtils.convertToDate(form.getEffectiveDate())
                    : null);
    try {
      caseOutcomeService.upsertTimeRecovery(
          ebsCase.getCaseReferenceNumber(),
          user.getProvider().getId().intValue(),
          award.awardType(),
          awardId,
          request,
          user.getLoginId());
    } catch (CaabApiClientException | IllegalStateException ex) {
      log.warn("Failed to save time-related recovery for {} award {}", awardPath, awardId, ex);
      bindingResult.reject(
          "timeRecovery.save.failed",
          "We could not save the time-related recovery. Please try again.");
      model.addAttribute("award", award);
      model.addAttribute("awardPath", awardPath);
      model.addAttribute("awardId", awardId);
      return "application/time-related-recovery";
    }
    return "redirect:/case/outcome-and-awards/" + awardPath + "/" + awardId + "/liable-parties";
  }

  /** Displays the parties associated with one award in the current case outcome. */
  @GetMapping(
      "/case/outcome-and-awards/{awardPath:cost-award|financial-award|land|asset}/{awardId}/liable-parties")
  public String displayLiableParties(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      final Model model) {
    final var award =
        caseOutcomeService.getLiablePartiesDisplay(
            ebsCase.getCaseReferenceNumber(),
            user.getProvider().getId().intValue(),
            awardPath,
            awardId);

    model.addAttribute("award", award);
    model.addAttribute("awardPath", awardPath);
    model.addAttribute("awardId", awardId);
    model.addAttribute(
        "backUrl",
        award.recoveryOfAwardTimeRelated()
            ? "/case/outcome-and-awards/" + awardPath + "/" + awardId + "/time-related-recovery"
            : "/case/outcome-and-awards/"
                + ("land".equals(awardPath) ? "land-property" : awardPath)
                + "/"
                + awardId);
    return "application/liable-parties";
  }

  /**
   * Displays the party list used to select existing case parties to add to the award.
   *
   * @param ebsCase the case whose outcome owns the award
   * @param currentApplication the in-session application, if any
   * @param user the logged-in user
   * @param awardPath the award type's URL path segment
   * @param awardId the award to add parties to
   * @param session the HTTP session used to track party list state
   * @param model the model used by the view
   * @return the party list view, or a redirect to outcome and awards for a read-only case
   */
  @GetMapping(PARTY_LIST_PATH)
  public String displayPartyList(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(value = APPLICATION, required = false)
          final ApplicationDetail currentApplication,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      final HttpSession session,
      final Model model) {
    if (!hasOutcomeEditPermission(ebsCase)) {
      log.warn("Rejecting party list for read-only case {}", ebsCase.getCaseReferenceNumber());
      return "redirect:/case/outcome-and-awards";
    }
    synchronized (WebUtils.getSessionMutex(session)) {
      return renderPartyList(ebsCase, currentApplication, user, awardPath, awardId, session, model);
    }
  }

  /**
   * Links the selected parties to the award, then redirects to the liable parties summary.
   *
   * <p>The session mutex is held for the whole read-validate-persist-track sequence so that two
   * overlapping submissions cannot both pass the already-added check before either has recorded its
   * result.
   *
   * @param ebsCase the case whose outcome owns the award
   * @param currentApplication the in-session application, if any
   * @param user the logged-in user
   * @param awardPath the award type's URL path segment
   * @param awardId the award to add parties to
   * @param selectedPartyIds the submitted party list row ids
   * @param rowIdentityToken the token identifying the render the selection was made against
   * @param session the HTTP session used to track party list state
   * @param model the model used by the view
   * @return a redirect to the liable parties summary, or the party list view with errors
   */
  @PostMapping(PARTY_LIST_PATH)
  public String addSelectedParties(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(value = APPLICATION, required = false)
          final ApplicationDetail currentApplication,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      @RequestParam(value = "selectedPartyIds", required = false)
          final List<String> selectedPartyIds,
      @RequestParam(value = "rowIdentityToken", required = false) final String rowIdentityToken,
      final HttpSession session,
      final Model model) {
    if (!hasOutcomeEditPermission(ebsCase)) {
      log.warn(
          "Rejecting liable party selection for read-only case {}",
          ebsCase.getCaseReferenceNumber());
      return "redirect:/case/outcome-and-awards";
    }
    synchronized (WebUtils.getSessionMutex(session)) {
      final List<String> selectedRowIds =
          Optional.ofNullable(selectedPartyIds).orElse(List.of()).stream()
              .filter(StringUtils::hasText)
              .map(String::trim)
              .distinct()
              .toList();
      if (selectedRowIds.isEmpty()) {
        model.addAttribute("partyListError", true);
        return renderPartyList(
            ebsCase, currentApplication, user, awardPath, awardId, session, model);
      }

      final Optional<PartyListCandidates> candidates =
          loadPartyListCandidates(ebsCase, currentApplication, user, awardPath, awardId, session);
      if (candidates.isEmpty()) {
        return renderPartyListLoadError(model, awardPath, awardId);
      }

      final String awardKey = awardSessionKey(ebsCase, awardPath, awardId);
      final List<CandidateOpponentRow> selectedRows =
          candidates.get().rows().stream()
              .filter(row -> selectedRowIds.contains(String.valueOf(row.rowId())))
              .filter(row -> !row.alreadyAdded())
              .filter(row -> rowIdentityIsStable(session, awardKey, rowIdentityToken, row))
              .toList();
      // Unknown, malformed, already-added or stale row ids leave the sizes mismatched.
      if (selectedRows.size() != selectedRowIds.size()) {
        return renderPartyListUnresolvedError(
            ebsCase, currentApplication, user, awardPath, awardId, session, model);
      }

      try {
        caseOutcomeService.addLiableParties(
            ebsCase.getCaseReferenceNumber(),
            user.getProvider().getId().intValue(),
            awardPath,
            awardId,
            selectedRows.stream()
                .map(CandidateOpponentRow::existingOpponentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList(),
            selectedRows.stream()
                .filter(row -> row.existingOpponentId() == null)
                .map(CandidateOpponentRow::opponent)
                .toList(),
            user.getLoginId(),
            // Each success is tracked as it happens, so earlier links in the batch are still
            // recognised as already added if a later one fails.
            opponentId ->
                addToAwardSessionSet(
                    session, ADDED_LIABLE_PARTY_OPPONENT_IDS, awardKey, opponentId),
            created ->
                addToAwardSessionSet(
                    session, CREATED_LIABLE_PARTY_EBS_IDS, awardKey, created.getEbsId()));
      } catch (CaabApiClientException | IllegalStateException ex) {
        log.warn(
            "Failed to add selected liable parties for case {} award {} {}",
            ebsCase.getCaseReferenceNumber(),
            awardPath,
            awardId,
            ex);
        return renderPartyListUnresolvedError(
            ebsCase, currentApplication, user, awardPath, awardId, session, model);
      }

      return "redirect:/case/outcome-and-awards/" + awardPath + "/" + awardId + "/liable-parties";
    }
  }

  /**
   * Populates the model for the party list view. Must be called while holding the session mutex.
   */
  private String renderPartyList(
      final ApplicationDetail ebsCase,
      final ApplicationDetail currentApplication,
      final UserDetail user,
      final String awardPath,
      final Integer awardId,
      final HttpSession session,
      final Model model) {
    final Optional<PartyListCandidates> candidates =
        loadPartyListCandidates(ebsCase, currentApplication, user, awardPath, awardId, session);
    if (candidates.isEmpty()) {
      return renderPartyListLoadError(model, awardPath, awardId);
    }
    final List<CandidateOpponentRow> rows = candidates.get().rows();
    final LiablePartiesMappingContext context =
        LiablePartiesMappingContext.builder()
            .contactTitle(lookupService.getCommonValues(COMMON_VALUE_CONTACT_TITLE).block())
            .organisationRelationshipsToCase(
                lookupService.getOrganisationToCaseRelationships().block())
            .individualRelationshipsToCase(candidates.get().personRelationships())
            .build();
    final List<LiablePartyDisplay> parties =
        rows.stream()
            .map(
                row ->
                    liablePartiesDisplayMapper.toPartyListDisplay(
                        row.rowId(), row.opponent(), row.alreadyAdded(), context))
            .toList();

    model.addAttribute("awardPath", awardPath);
    model.addAttribute("awardId", awardId);
    model.addAttribute("parties", parties);
    model.addAttribute(
        "firstSelectablePartyId",
        parties.stream()
            .filter(party -> !party.alreadyAdded())
            .map(LiablePartyDisplay::id)
            .findFirst()
            .orElse(null));
    model.addAttribute(
        "rowIdentityToken",
        recordPartyListRowIdentities(session, awardSessionKey(ebsCase, awardPath, awardId), rows));
    return PARTY_LIST_VIEW;
  }

  /** Re-renders the party list with an error saying the selection could not be added. */
  private String renderPartyListUnresolvedError(
      final ApplicationDetail ebsCase,
      final ApplicationDetail currentApplication,
      final UserDetail user,
      final String awardPath,
      final Integer awardId,
      final HttpSession session,
      final Model model) {
    model.addAttribute("partyListError", true);
    model.addAttribute("partyListUnresolvedError", true);
    return renderPartyList(ebsCase, currentApplication, user, awardPath, awardId, session, model);
  }

  /**
   * Renders the party list in its error state when its candidates could not be loaded. The stale
   * in-session snapshot is deliberately not shown instead, as it could list removed opponents or
   * omit newly added ones. The list is left empty, with no selection token, until a retry succeeds.
   */
  private String renderPartyListLoadError(
      final Model model, final String awardPath, final Integer awardId) {
    model.addAttribute("awardPath", awardPath);
    model.addAttribute("awardId", awardId);
    model.addAttribute("parties", List.of());
    model.addAttribute("firstSelectablePartyId", null);
    model.addAttribute("rowIdentityToken", null);
    model.addAttribute("partyListError", true);
    model.addAttribute("partyListLoadError", true);
    return PARTY_LIST_VIEW;
  }

  /**
   * Builds the selectable party list rows, or returns empty if the active application or the
   * person-to-case relationships needed to validate candidates could not be loaded.
   */
  private Optional<PartyListCandidates> loadPartyListCandidates(
      final ApplicationDetail ebsCase,
      final ApplicationDetail currentApplication,
      final UserDetail user,
      final String awardPath,
      final Integer awardId,
      final HttpSession session) {
    final Optional<ApplicationDetail> activeApplication =
        resolveActiveApplication(ebsCase, currentApplication);
    if (activeApplication.isEmpty()) {
      return Optional.empty();
    }
    // A missing body or null content must not be treated as "no relationships": that would make
    // every date of birth optional and let an invalid individual through candidate validation.
    final Optional<RelationshipToCaseLookupDetail> personRelationships;
    try {
      personRelationships =
          lookupService
              .getPersonToCaseRelationships()
              .blockOptional()
              .filter(lookup -> lookup.getContent() != null);
    } catch (EbsApiClientException ex) {
      log.error("Error loading person-to-case relationships", ex);
      return Optional.empty();
    }
    if (personRelationships.isEmpty()) {
      return Optional.empty();
    }
    final AlreadyAddedIdentities alreadyAdded =
        collectAlreadyAddedIdentities(ebsCase, user, awardPath, awardId, session);
    return Optional.of(
        new PartyListCandidates(
            buildCandidateOpponentRows(
                activeApplication.get(), alreadyAdded, personRelationships.get().getContent()),
            personRelationships.get()));
  }

  /**
   * Resolves the application whose opponents populate the party list. A persisted draft for this
   * case is reloaded from the backend, because the opponent journeys persist their changes without
   * refreshing the in-session copy; otherwise the case's own opponents are used.
   *
   * @return the application to use, or empty if a persisted draft could not be reloaded, which
   *     callers must surface as an error rather than falling back to the stale session copy
   */
  private Optional<ApplicationDetail> resolveActiveApplication(
      final ApplicationDetail ebsCase, final ApplicationDetail currentApplication) {
    final boolean hasPersistedDraftForThisCase =
        currentApplication != null
            && currentApplication.getId() != null
            && Objects.equals(
                currentApplication.getCaseReferenceNumber(), ebsCase.getCaseReferenceNumber());
    if (!hasPersistedDraftForThisCase) {
      return Optional.of(ebsCase);
    }
    try {
      final Optional<ApplicationDetail> reloaded =
          applicationService
              .getApplication(String.valueOf(currentApplication.getId()))
              .blockOptional();
      if (reloaded.isEmpty()) {
        log.warn(
            "Reloading persisted opponents for application {} returned no data",
            currentApplication.getId());
      }
      return reloaded;
    } catch (CaabApiClientException | IllegalStateException ex) {
      log.warn(
          "Failed to reload persisted opponents for application {}",
          currentApplication.getId(),
          ex);
      return Optional.empty();
    }
  }

  /**
   * Collects the CAAB opponent ids and EBS ids of the parties already linked to the award, merged
   * with those recorded in session by this party list. The session records cover links the backend
   * read may not yet reflect, and created opponents whose real EBS id the backend replaces with a
   * synthetic one. Personal details are deliberately never used as an identity, since distinct
   * opponents can share them.
   */
  private AlreadyAddedIdentities collectAlreadyAddedIdentities(
      final ApplicationDetail ebsCase,
      final UserDetail user,
      final String awardPath,
      final Integer awardId,
      final HttpSession session) {
    final String awardKey = awardSessionKey(ebsCase, awardPath, awardId);
    final Set<Integer> opponentIds =
        new HashSet<>(readAwardSessionSet(session, ADDED_LIABLE_PARTY_OPPONENT_IDS, awardKey));
    final Set<String> ebsIds =
        new HashSet<>(readAwardSessionSet(session, CREATED_LIABLE_PARTY_EBS_IDS, awardKey));
    for (final OpponentDetail linked :
        caseOutcomeService.getAwardLiableParties(
            ebsCase.getCaseReferenceNumber(),
            user.getProvider().getId().intValue(),
            awardPath,
            awardId)) {
      Optional.ofNullable(linked.getId()).ifPresent(opponentIds::add);
      Optional.ofNullable(linked.getEbsId()).filter(StringUtils::hasText).ifPresent(ebsIds::add);
    }
    return new AlreadyAddedIdentities(opponentIds, ebsIds);
  }

  /** Builds the session map key scoping per-award tracking to one case and award. */
  private static String awardSessionKey(
      final ApplicationDetail ebsCase, final String awardPath, final Integer awardId) {
    return ebsCase.getCaseReferenceNumber() + ":" + awardPath + ":" + awardId;
  }

  @SuppressWarnings("unchecked")
  private static <T> Map<String, Set<T>> awardSessionSets(
      final HttpSession session, final String attribute) {
    return session.getAttribute(attribute) instanceof Map<?, ?> stored
        ? (Map<String, Set<T>>) stored
        : new HashMap<>();
  }

  private static <T> Set<T> readAwardSessionSet(
      final HttpSession session, final String attribute, final String awardKey) {
    return AwardController.<T>awardSessionSets(session, attribute).getOrDefault(awardKey, Set.of());
  }

  private static <T> void addToAwardSessionSet(
      final HttpSession session, final String attribute, final String awardKey, final T value) {
    final Map<String, Set<T>> sets = awardSessionSets(session, attribute);
    sets.computeIfAbsent(awardKey, unused -> new HashSet<>()).add(value);
    session.setAttribute(attribute, sets);
  }

  /**
   * Records which stable opponent identity each rendered row id corresponds to, under a fresh
   * random token that the form submits back (see {@link #rowIdentityIsStable}). Per-render tokens
   * stop two open tabs for the same award from overwriting each other's snapshot. Only the most
   * recent {@value #MAX_ROW_IDENTITY_SNAPSHOTS_PER_AWARD} snapshots per award are retained.
   *
   * @param session the HTTP session holding the snapshots
   * @param awardKey the session key identifying the case and award
   * @param rows the rows being rendered
   * @return the token identifying this render's snapshot
   */
  private static String recordPartyListRowIdentities(
      final HttpSession session, final String awardKey, final List<CandidateOpponentRow> rows) {
    final Map<Integer, String> identitiesByRowId =
        rows.stream()
            .collect(
                Collectors.toMap(
                    CandidateOpponentRow::rowId,
                    AwardController::rowIdentityKey,
                    (first, second) -> second));
    final Map<String, Map<String, Map<Integer, String>>> snapshotsByAward =
        readPartyListRowIdentities(session);
    final String token = UUID.randomUUID().toString();
    snapshotsByAward
        .computeIfAbsent(awardKey, unused -> new BoundedSnapshots())
        .put(token, identitiesByRowId);
    session.setAttribute(PARTY_LIST_ROW_IDENTITIES, snapshotsByAward);
    return token;
  }

  /**
   * Checks that a row id still resolves to the same opponent identity it had in the render
   * identified by the token. Ids of opponents without a CAAB id are list positions, so this stops a
   * selection resolving to a different opponent after the list has changed. A missing, unknown or
   * evicted token is treated as stale.
   */
  private static boolean rowIdentityIsStable(
      final HttpSession session,
      final String awardKey,
      final String rowIdentityToken,
      final CandidateOpponentRow row) {
    return StringUtils.hasText(rowIdentityToken)
        && Optional.ofNullable(readPartyListRowIdentities(session).get(awardKey))
            .map(snapshots -> snapshots.get(rowIdentityToken))
            .map(identities -> rowIdentityKey(row).equals(identities.get(row.rowId())))
            .orElse(false);
  }

  /** Builds the stable identity key a party list row id is expected to still resolve to. */
  private static String rowIdentityKey(final CandidateOpponentRow row) {
    return row.existingOpponentId() != null
        ? "ID:" + row.existingOpponentId()
        : "EBS:" + row.opponent().getEbsId();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Map<String, Map<Integer, String>>> readPartyListRowIdentities(
      final HttpSession session) {
    return session.getAttribute(PARTY_LIST_ROW_IDENTITIES) instanceof Map<?, ?> stored
        ? (Map<String, Map<String, Map<Integer, String>>>) stored
        : new HashMap<>();
  }

  /**
   * Builds the valid, identifiable candidate rows for the party list. Opponents with a CAAB id use
   * it as their row id and are linked by id; others use a negative row id derived from their list
   * position (guarded by {@link #rowIdentityIsStable}) and are created and linked by their full
   * details. A row is already added if either its CAAB id or its EBS id has been recorded.
   */
  private List<CandidateOpponentRow> buildCandidateOpponentRows(
      final ApplicationDetail activeApplication,
      final AlreadyAddedIdentities alreadyAdded,
      final List<RelationshipToCaseLookupValueDetail> personRelationships) {
    final List<OpponentDetail> opponents =
        Optional.ofNullable(activeApplication.getOpponents()).orElse(List.of());
    final List<CandidateOpponentRow> rows = new ArrayList<>();
    for (int i = 0; i < opponents.size(); i++) {
      final OpponentDetail opponent = opponents.get(i);
      if (opponent == null
          || (opponent.getId() == null && !StringUtils.hasText(opponent.getEbsId()))
          || !isValidPartyListOpponent(opponent, personRelationships)) {
        continue;
      }
      final Integer existingOpponentId = opponent.getId();
      rows.add(
          new CandidateOpponentRow(
              existingOpponentId != null ? existingOpponentId : -(i + 1),
              opponent,
              existingOpponentId,
              alreadyAdded.opponentIds().contains(existingOpponentId)
                  || alreadyAdded.ebsIds().contains(opponent.getEbsId())));
    }
    return rows;
  }

  /**
   * Checks whether an opponent could be created through the dedicated add screens, by applying the
   * form's Bean Validation constraints and custom validators (including, for individuals, the
   * other-details validator, since the full opponent is sent when it is created).
   */
  private boolean isValidPartyListOpponent(
      final OpponentDetail opponent,
      final List<RelationshipToCaseLookupValueDetail> personRelationships) {
    if (OpponentUtil.isOrganisation(opponent)) {
      final OrganisationOpponentFormData form =
          opponentMapper.toOrganisationOpponentFormData(
              opponent, null, null, null, null, null, false);
      return isValid(form, "organisationOpponent", organisationOpponentValidator::validate);
    }
    if (!OpponentUtil.INDIVIDUAL_OPPONENT.equalsIgnoreCase(opponent.getType())) {
      return false;
    }
    final IndividualOpponentFormData form =
        opponentMapper.toIndividualOpponentFormData(opponent, null, null, null, null, false);
    prepareIndividualFormForValidation(form, personRelationships);
    return isValid(form, "liablePartyIndividual", individualOpponentValidator::validateLiableParty)
        && isValid(
            opponentMapper.toIndividualOpponentOtherDetailsFormData(opponent),
            "liablePartyIndividualOtherDetails",
            individualOpponentOtherDetailsValidator::validate);
  }

  /** Applies Bean Validation followed by the given custom validation to a form. */
  private <T> boolean isValid(
      final T form, final String objectName, final BiConsumer<T, Errors> customValidation) {
    final Errors errors = new BeanPropertyBindingResult(form, objectName);
    new SpringValidatorAdapter(beanValidator).validate(form, errors);
    customValidation.accept(form, errors);
    return !errors.hasErrors();
  }

  /**
   * Mirrors {@link uk.gov.laa.ccms.caab.service.IndividualOpponentFormService#prepareForValidation}
   * but uses relationships fetched once per request, avoiding a remote call per individual while
   * the session mutex is held.
   */
  private static void prepareIndividualFormForValidation(
      final IndividualOpponentFormData individual,
      final List<RelationshipToCaseLookupValueDetail> personRelationships) {
    individual.setDateOfBirthMandatory(
        personRelationships.stream()
            .anyMatch(
                relationship ->
                    Objects.equals(individual.getRelationshipToCase(), relationship.getCode())
                        && Boolean.TRUE.equals(relationship.getDateOfBirthMandatory())));
    individual.setDateOfBirth(DateUtils.normaliseComponentDateIfValid(individual.getDateOfBirth()));
  }

  /**
   * A selectable party list row.
   *
   * @param rowId the id submitted when the row is selected
   * @param opponent the case opponent the row represents
   * @param existingOpponentId the CAAB opponent id to link, or {@code null} if it must be created
   * @param alreadyAdded whether the opponent is already linked to the award
   */
  private record CandidateOpponentRow(
      Integer rowId, OpponentDetail opponent, Integer existingOpponentId, boolean alreadyAdded) {}

  /**
   * The party list rows and the person relationships they were validated against.
   *
   * @param rows the candidate rows
   * @param personRelationships the person-to-case relationship lookup
   */
  private record PartyListCandidates(
      List<CandidateOpponentRow> rows, RelationshipToCaseLookupDetail personRelationships) {}

  /**
   * Identities of opponents already linked to an award.
   *
   * @param opponentIds the CAAB opponent ids
   * @param ebsIds the EBS ids
   */
  private record AlreadyAddedIdentities(Set<Integer> opponentIds, Set<String> ebsIds) {}

  /** Row identity snapshots for one award, evicting the oldest beyond a fixed limit. */
  private static final class BoundedSnapshots extends LinkedHashMap<String, Map<Integer, String>> {
    @java.io.Serial private static final long serialVersionUID = 1L;

    @Override
    protected boolean removeEldestEntry(final Map.Entry<String, Map<Integer, String>> eldest) {
      return size() > MAX_ROW_IDENTITY_SNAPSHOTS_PER_AWARD;
    }
  }

  private TimeRecoveryAward loadTimeRecoveryAward(
      final ApplicationDetail ebsCase,
      final UserDetail user,
      final String awardPath,
      final Integer awardId) {
    final String caseReference = ebsCase.getCaseReferenceNumber();
    final Integer providerId = user.getProvider().getId().intValue();
    return switch (awardPath) {
      case "land" -> {
        final LandAwardDetail award =
            caseOutcomeService
                .getLandAward(caseReference, providerId, awardId)
                .orElseThrow(
                    () ->
                        new CaabApplicationException(
                            "Could not find land award with id: " + awardId));
        yield new TimeRecoveryAward(
            AWARD_TYPE_LAND,
            award.getDescription(),
            award.getValuationAmount(),
            award.getRecoveryOfAwardTimeRelated(),
            award.getTimeRecovery());
      }
      case "asset" -> {
        final OtherAssetAwardDetail award =
            caseOutcomeService
                .getOtherAssetAward(caseReference, providerId, awardId)
                .orElseThrow(
                    () ->
                        new CaabApplicationException(
                            "Could not find other asset award with id: " + awardId));
        yield new TimeRecoveryAward(
            AWARD_TYPE_OTHER_ASSET,
            award.getDescription(),
            award.getValuationAmount(),
            award.getRecoveryOfAwardTimeRelated(),
            award.getTimeRecovery());
      }
      default ->
          throw new CaabApplicationException(
              "Unsupported time-related recovery award path: " + awardPath);
    };
  }

  private String redirectToRecoveryParent(final String awardPath, final Integer awardId) {
    final String parentPath = "land".equals(awardPath) ? "land-property" : awardPath;
    return "redirect:/case/outcome-and-awards/" + parentPath + "/" + awardId;
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
   * Creates or updates a cost award and continues to its liable parties.
   *
   * @return redirect to the liable parties screen when successful
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
      final Integer savedAwardId;
      if (costAward.getId() == null) {
        savedAwardId =
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
        savedAwardId = costAward.getId();
      }
      return "redirect:/case/outcome-and-awards/cost-award/" + savedAwardId + "/liable-parties";
    } catch (CaabApiClientException | IllegalStateException ex) {
      log.warn("Failed to save cost award with id: {}", costAward.getId(), ex);
      bindingResult.reject(
          "costAward.save.failed", "We could not save the cost award. Please try again.");
      initialiseCostAwardAmounts(costAward);
      populateCostAwardPage(model, costAward);
      return "application/cost-award";
    }
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
    // Invalid amounts count as zero so totals match validation and the client-side calculation.
    return value != null && value.matches(CURRENCY_PATTERN)
        ? new BigDecimal(value).setScale(2, RoundingMode.HALF_UP)
        : ZERO;
  }

  private String formatAmount(final BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }
}
