package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.service.ApplicationService;

/** Controller responsible for handling cost limit allocation. */
@RequiredArgsConstructor
@Controller
@Slf4j
@SessionAttributes({CASE})
public class AllocateCostLimitController {
  private final ApplicationService applicationService;

  /**
   * Displays the cost limitation allocation screen.
   *
   * @param ebsCase The case details from EBS.
   * @param model the Model object used to pass attributes to the view.
   * @return The cost limitation allocation view.
   */
  @GetMapping("/allocate-cost-limit")
  public String caseDetails(@SessionAttribute(CASE) final ApplicationDetail ebsCase, Model model) {

    model.addAttribute("totalRemaining", getTotalRemaining(ebsCase));
    model.addAttribute("case", ebsCase);
    return "application/cost-allocation";
  }

  /**
   * Displays the cost limitation allocation screen.
   *
   * @param ebsCase The case cost details from session.
   * @param model the Model object used to pass attributes to the view.
   * @return The cost limitation allocation view.
   */
  @PostMapping("/allocate-cost-limit")
  public String updateCost(@ModelAttribute("ebsCase") ApplicationDetail ebsCase, Model model) {

    model.addAttribute("case", ebsCase);
    model.addAttribute("totalRemaining", getTotalRemaining(ebsCase));

    return "application/cost-allocation";
  }

  private BigDecimal getTotalRemaining(ApplicationDetail ebsCase) {
    BigDecimal sum =
        ebsCase.getCosts().getCostEntries().stream()
            .distinct()
            .map(CostEntryDetail::getRequestedCosts)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal grantedCostLimitation = ebsCase.getCosts().getGrantedCostLimitation();

    return grantedCostLimitation.subtract(sum);
  }
}
