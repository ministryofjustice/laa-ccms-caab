package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.Collections;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.bean.award.FinancialAwardFormData;
import uk.gov.laa.ccms.caab.bean.validators.awards.FinancialAwardValidator;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.constants.CommonValueConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.FinancialAwardMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardRequest;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller responsible for handling award-related requests. */
@RequiredArgsConstructor
@Controller
@Slf4j
public class AwardController {

  private final LookupService lookupService;
  private final CaseOutcomeService caseOutcomeService;
  private final FinancialAwardValidator financialAwardValidator;
  private final FinancialAwardMapper financialAwardMapper;

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
      // @SessionAttribute(value = AWARD_TYPE_FORM, required = false) final AwardTypeForm
      // awardTypeForm,
      @PathVariable(value = "awardId", required = false) final Integer financialAwardId,
      final Model model) {
    final FinancialAwardFormData formData;
    if (financialAwardId == null) {
      //      if (awardTypeForm == null
      //          || !StringUtils.hasText(awardTypeForm.getAwardCode())
      //          || !StringUtils.hasText(awardTypeForm.getAwardType())
      //          || !StringUtils.hasText(awardTypeForm.getDescription())) {
      //        log.warn("Financial award page requested without complete award type details");
      //        return "redirect:/case/outcome-and-awards";
      //      }
      formData = new FinancialAwardFormData();
      //      formData.setAwardCode(awardTypeForm.getAwardCode());
      //      formData.setAwardType(awardTypeForm.getAwardType());
      //      formData.setDescription(awardTypeForm.getDescription());
      formData.setAwardCode("DAMAGE_ARG");
      formData.setAwardType("DAMAGE");
      formData.setDescription("Damage");
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
      if (Boolean.FALSE.equals(award.getUpdateAllowed())) {
        return "redirect:/case/outcome-and-awards";
      }
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
      @ModelAttribute("financialAward") final FinancialAwardFormData financialAward,
      final BindingResult bindingResult,
      final Model model) {
    financialAwardValidator.validate(financialAward, bindingResult);
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
}
