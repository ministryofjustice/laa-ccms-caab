package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.bean.costs.AllocateCostsFormData;
import uk.gov.laa.ccms.caab.bean.validators.costs.AllocateCostLimitValidator;
import uk.gov.laa.ccms.caab.mapper.CopyApplicationMapper;
import uk.gov.laa.ccms.caab.mapper.ProceedingAndCostsMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;

/** Controller responsible for handling cost limit allocation. */
@RequiredArgsConstructor
@Controller
@Slf4j
public class AllocateCostLimitController {
  private final ProceedingAndCostsMapper proceedingAndCostsMapper;
  private final CopyApplicationMapper copyApplicationMapper;
  private final AllocateCostLimitValidator allocateCostLimitValidator;

  /**
   * Displays the cost limitation allocation screen.
   *
   * @param ebsCase The case details from EBS.
   * @param model the Model object used to pass attributes to the view.
   * @return The cost limitation allocation view.
   */
  @GetMapping("/allocate-cost-limit")
  public String caseDetails(@SessionAttribute(CASE) final ApplicationDetail ebsCase, Model model) {

    AllocateCostsFormData allocateCostsFormData =
        proceedingAndCostsMapper.toAllocateCostsForm(ebsCase);
    allocateCostsFormData.setTotalRemaining(getTotalRemaining(allocateCostsFormData));

    model.addAttribute("costDetails", allocateCostsFormData);
    model.addAttribute("case", ebsCase);
    return "application/cost-allocation";
  }

  /**
   * Displays the cost limitation allocation screen.
   *
   * @param allocateCostsFormData the submitted form data for costs.
   * @param ebsCase The case cost details from session.
   * @param action the button action (calculate or next).
   * @param model the Model object used to pass attributes to the view.
   * @return The cost limitation allocation view or redirect to review.
   */
  @PostMapping("/allocate-cost-limit")
  public String calculateCost(
      @ModelAttribute("costDetails") AllocateCostsFormData allocateCostsFormData,
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @org.springframework.web.bind.annotation.RequestParam(value = "action", required = false)
          String action,
      final Model model,
      final BindingResult bindingResult) {

    ApplicationDetail appCopy =
        copyApplicationMapper.copyApplication(new ApplicationDetail(), ebsCase);

    List<CostEntryDetail> costs =
        appCopy.getCosts().getCostEntries().stream().distinct().collect(Collectors.toList());

    proceedingAndCostsMapper.toAllocateCostsFormWithoutCostEntries(appCopy, allocateCostsFormData);

    // Update costs in a single pass
    for (int i = 0; i < costs.size(); i++) {
      CostEntryDetail cost = costs.get(i);
      CostEntryDetail formCost = allocateCostsFormData.getCostEntries().get(i);

      if (!cost.getRequestedCosts().equals(formCost.getRequestedCosts())) {
        cost.setNewEntry(true);
      }
      cost.setRequestedCosts(formCost.getRequestedCosts());
    }

    allocateCostsFormData.setCostEntries(costs);
    allocateCostsFormData.setTotalRemaining(getTotalRemaining(allocateCostsFormData));

    allocateCostLimitValidator.validate(allocateCostsFormData, bindingResult);

    if (bindingResult.hasErrors() || "calculate".equals(action)) {
      model.addAttribute("case", ebsCase);
      model.addAttribute("costDetails", allocateCostsFormData);
      return "application/cost-allocation";
    }

    ebsCase.getCosts().setCostEntries(costs);
    return "redirect:/allocate-cost-limit/review";
  }

  /**
   * Displays the review case costs screen.
   *
   * @param ebsCase The case details from EBS.
   * @param model the Model object used to pass attributes to the view.
   * @return The review case costs view.
   */
  @GetMapping("/allocate-cost-limit/review")
  public String reviewCaseCosts(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase, Model model) {

    AllocateCostsFormData allocateCostsFormData =
        proceedingAndCostsMapper.toAllocateCostsForm(ebsCase);
    allocateCostsFormData.setTotalRemaining(getTotalRemaining(allocateCostsFormData));

    model.addAttribute("costDetails", allocateCostsFormData);
    model.addAttribute("case", ebsCase);
    return "application/case-costs-review";
  }

  /**
   * Handles submission of the review case costs form.
   *
   * @param allocateCostsFormData the submitted form data for costs.
   * @param ebsCase The case cost details from session.
   * @param model the Model object used to pass attributes to the view.
   * @return Redirect to the next step in the workflow.
   */
  @PostMapping("/allocate-cost-limit/review")
  public String submitCaseCosts(
      @ModelAttribute("costDetails") AllocateCostsFormData allocateCostsFormData,
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      final Model model) {

    // TODO: Add API call to finalize the cost allocations
    log.info("Submitting case costs for case: {}", ebsCase.getCaseReferenceNumber());

    // TODO: Redirect to the next workflow step or show success message
    return "redirect:/case/overview";
  }

  /** Calculates the total requests costs by the granted cost limitation. */
  private BigDecimal getTotalRemaining(AllocateCostsFormData allocateCostsFormData) {
    if (allocateCostsFormData.getCostEntries() == null
        || allocateCostsFormData.getGrantedCostLimitation() == null) {
      return BigDecimal.ZERO;
    }

    BigDecimal sum =
        allocateCostsFormData.getCostEntries().stream()
            .distinct()
            .map(CostEntryDetail::getRequestedCosts)
            .map(x -> x == null ? BigDecimal.ZERO : x)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

    return allocateCostsFormData.getGrantedCostLimitation().subtract(sum);
  }
}
