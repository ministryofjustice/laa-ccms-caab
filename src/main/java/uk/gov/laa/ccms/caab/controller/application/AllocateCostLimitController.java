package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.service.ApplicationService;

/** Controller responsible for handling cost limit allocation. */
@RequiredArgsConstructor
@Controller
@Slf4j
public class AllocateCostLimitController {
  private final ApplicationService applicationService;

  /**
   * Displays the cost limitation allocation screen.
   *
   * @param ebsCase The case details from EBS.
   * @return The cost limitation allocation view.
   */
  @GetMapping("/allocate-cost-limit")
  public String caseDetails(@SessionAttribute(CASE) final ApplicationDetail ebsCase, Model model) {

    BigDecimal sum =
        ebsCase.getCosts().getCostEntries().stream()
            .distinct()
            .map(CostEntryDetail::getRequestedCosts)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal grantedCostLimitation = ebsCase.getCosts().getGrantedCostLimitation();

    model.addAttribute("totalRemaining", grantedCostLimitation.subtract(sum));
    model.addAttribute("case", ebsCase);
    return "application/cost-allocation";
  }
}
