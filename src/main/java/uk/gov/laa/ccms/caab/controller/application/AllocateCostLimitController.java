package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;

import java.math.BigDecimal;
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
import uk.gov.laa.ccms.caab.bean.costs.CostsFormData;
import uk.gov.laa.ccms.caab.bean.validators.costs.AllocateCostLimitValidator;
import uk.gov.laa.ccms.caab.mapper.ProceedingAndCostsMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;

/** Controller responsible for handling cost limit allocation. */
@RequiredArgsConstructor
@Controller
@Slf4j
public class AllocateCostLimitController {
  private final ProceedingAndCostsMapper proceedingAndCostsMapper;
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

    final CostsFormData costsFormData = proceedingAndCostsMapper.toCostsForm(ebsCase);

    model.addAttribute("costDetails", costsFormData);
    model.addAttribute("totalRemaining", getTotalRemaining(costsFormData));
    model.addAttribute("case", ebsCase);
    return "application/cost-allocation";
  }

  /**
   * Displays the cost limitation allocation screen.
   *
   * @param costsFormData the submitted form data for costs.
   * @param ebsCase The case cost details from session.
   * @param model the Model object used to pass attributes to the view.
   * @return The cost limitation allocation view.
   */
  @PostMapping("/allocate-cost-limit")
  public String updateCost(
      @ModelAttribute("costDetails") CostsFormData costsFormData,
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      final Model model,
      final BindingResult bindingResult) {

    List<CostEntryDetail> costs =
        ebsCase.getCosts().getCostEntries().stream()
            .collect(Collectors.groupingBy(CostEntryDetail::getEbsId))
            .values()
            .stream()
            .map(list -> list.get(0))
            .toList();

    for (int i = 0; i < costs.size(); i++) {
      costs.get(i).setRequestedCosts(costsFormData.getCostEntries().get(i).getRequestedCosts());
    }
    costsFormData.setCostEntries(costs);

    allocateCostLimitValidator.validate(costsFormData, bindingResult);

    model.addAttribute("case", ebsCase);
    model.addAttribute("costDetails", costsFormData);
    model.addAttribute("totalRemaining", getTotalRemaining(costsFormData));

    return "application/cost-allocation";
  }

  /** Calculates the total requests costs by the granted cost limitation. */
  private BigDecimal getTotalRemaining(CostsFormData cost) {
    if (cost.getCostEntries() == null || cost.getGrantedCostLimitation() == null) {
      return BigDecimal.ZERO;
    }

    BigDecimal sum =
        cost.getCostEntries().stream()
            .distinct()
            .map(CostEntryDetail::getRequestedCosts)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    return cost.getGrantedCostLimitation().subtract(sum);
  }
}
