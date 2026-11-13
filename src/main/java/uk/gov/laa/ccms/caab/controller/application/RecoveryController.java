package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.math.BigDecimal;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.bean.award.RecoveryFormData;
import uk.gov.laa.ccms.caab.bean.validators.awards.RecoveryValidator;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.LiablePartiesDisplayMapper;
import uk.gov.laa.ccms.caab.mapper.RecoveryMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.BaseAwardDetail;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.RecoveryDetail;
import uk.gov.laa.ccms.caab.model.RecoveryRequest;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller responsible for recovery on cost and financial awards (legacy AW04). */
@RequiredArgsConstructor
@Controller
@Slf4j
public class RecoveryController {
  private final CaseOutcomeService caseOutcomeService;
  private final RecoveryValidator recoveryValidator;
  private final RecoveryMapper recoveryMapper;
  private final LiablePartiesDisplayMapper awardDisplayMapper;

  /** Displays recovery after liable parties for cost and financial awards (legacy AW04). */
  @GetMapping("/case/outcome-and-awards/{awardPath:cost-award|financial-award}/{awardId}/recovery")
  public String displayRecovery(
      @SessionAttribute(value = CASE, required = false) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      final Model model) {
    if (!hasOutcomeEditPermission(ebsCase)) {
      log.warn("Rejecting recovery form for a read-only or missing case");
      return "redirect:/case/outcome-and-awards";
    }
    final BaseAwardDetail award = loadRecoveryAward(ebsCase, user, awardPath, awardId);
    final RecoveryFormData form = savedRecoveryForm(award);
    model.addAttribute("recovery", form);
    populateRecoveryModel(model, award, awardPath, awardId, recoveryMapper.toRequest(form));
    return "application/recovery";
  }

  /** Calculates or saves recovery; Back and Cancel navigate through links without submitting. */
  @PostMapping("/case/outcome-and-awards/{awardPath:cost-award|financial-award}/{awardId}/recovery")
  public String submitRecovery(
      @SessionAttribute(value = CASE, required = false) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @PathVariable final String awardPath,
      @PathVariable final Integer awardId,
      @RequestParam(defaultValue = "next") final String action,
      @ModelAttribute("recovery") final RecoveryFormData form,
      final BindingResult errors,
      final Model model) {
    if (!hasOutcomeEditPermission(ebsCase)) {
      log.warn("Rejecting recovery submission for a read-only or missing case");
      return "redirect:/case/outcome-and-awards";
    }
    final BaseAwardDetail award = loadRecoveryAward(ebsCase, user, awardPath, awardId);
    if (!"next".equals(action) && !"calculate".equals(action)) {
      log.warn("Rejecting unsupported recovery action {}", action);
      errors.reject("recovery.action.invalid", "The recovery action is not supported.");
    }
    form.normaliseLegacyFields();
    recoveryValidator.validate(form, errors);
    if (errors.hasErrors()) {
      populateRecoveryModel(
          model, award, awardPath, awardId, recoveryMapper.toRequest(savedRecoveryForm(award)));
      return "application/recovery";
    }
    final RecoveryRequest request = recoveryMapper.toRequest(form);
    final RecoveryTotals totals = populateRecoveryModel(model, award, awardPath, awardId, request);
    if ("calculate".equals(action)) {
      return "application/recovery";
    }
    recoveryValidator.validateTotals(
        totals.awardAmount(), totals.recoveredAmount(), totals.unrecoveredAmount(), errors);
    if (errors.hasErrors()) {
      return "application/recovery";
    }
    try {
      caseOutcomeService.upsertRecovery(
          ebsCase.getCaseReferenceNumber(),
          user.getProvider().getId().intValue(),
          award.getAwardType(),
          awardId,
          request,
          user.getLoginId());
    } catch (CaabApiClientException | IllegalStateException ex) {
      log.warn("Failed to save recovery for {} award {}", awardPath, awardId, ex);
      errors.reject("recovery.save.failed", "We could not save the recovery. Please try again.");
      return "application/recovery";
    }
    return "redirect:/case/outcome-and-awards";
  }

  private BaseAwardDetail loadRecoveryAward(
      final ApplicationDetail ebsCase,
      final UserDetail user,
      final String awardPath,
      final Integer awardId) {
    final String reference = ebsCase.getCaseReferenceNumber();
    final Integer providerId = user.getProvider().getId().intValue();
    final Optional<? extends BaseAwardDetail> award =
        "cost-award".equals(awardPath)
            ? caseOutcomeService.getCostAward(reference, providerId, awardId)
            : caseOutcomeService.getFinancialAward(reference, providerId, awardId);
    return award.orElseThrow(
        () -> new CaabApplicationException("Could not find recovery award with id: " + awardId));
  }

  private RecoveryFormData savedRecoveryForm(final BaseAwardDetail award) {
    final RecoveryDetail recovery =
        switch (award) {
          case CostAwardDetail cost -> cost.getRecovery();
          case FinancialAwardDetail financial -> financial.getRecovery();
          default -> throw new CaabApplicationException("Unsupported recovery award");
        };
    return recovery == null ? new RecoveryFormData() : recoveryMapper.toForm(recovery);
  }

  private record RecoveryTotals(
      BigDecimal awardAmount, BigDecimal recoveredAmount, BigDecimal unrecoveredAmount) {}

  private RecoveryTotals populateRecoveryModel(
      final Model model,
      final BaseAwardDetail award,
      final String path,
      final Integer id,
      final RecoveryRequest request) {
    final BigDecimal amount =
        Optional.ofNullable(awardDisplayMapper.awardAmount(award))
            .orElse(BigDecimal.ZERO)
            .setScale(2);
    final BigDecimal recovered =
        request
            .getSolicitorRecoveryAmount()
            .add(request.getCourtRecoveryAmount())
            .add(request.getClientRecoveryAmount());
    model.addAttribute("award", award);
    model.addAttribute("awardAmount", amount);
    model.addAttribute("awardPath", path);
    model.addAttribute("awardId", id);
    model.addAttribute("recoveredAmount", recovered);
    model.addAttribute("unrecoveredAmount", amount.subtract(recovered));
    return new RecoveryTotals(amount, recovered, amount.subtract(recovered));
  }

  private boolean hasOutcomeEditPermission(final ApplicationDetail ebsCase) {
    return ebsCase != null
        && ebsCase.getAvailableFunctions() != null
        && (ebsCase.getAvailableFunctions().contains(FunctionConstants.OUTCOME_WITH_DISCHARGE)
            || ebsCase.getAvailableFunctions().contains(FunctionConstants.OUTCOME_NO_DISCHARGE));
  }
}
