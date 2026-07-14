package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COST_ALLOCATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_RESULT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
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
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.costs.AllocateCostsFormData;
import uk.gov.laa.ccms.caab.bean.validators.costs.AllocateCostLimitValidator;
import uk.gov.laa.ccms.caab.mapper.CopyApplicationMapper;
import uk.gov.laa.ccms.caab.mapper.ProceedingAndCostsMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.service.AmendmentService;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller responsible for handling cost limit allocation. */
@RequiredArgsConstructor
@Controller
@Slf4j
public class AllocateCostLimitController {
  private final ProceedingAndCostsMapper proceedingAndCostsMapper;
  private final CopyApplicationMapper copyApplicationMapper;
  private final AllocateCostLimitValidator allocateCostLimitValidator;
  private final ApplicationService applicationService;
  private final AmendmentService amendmentService;

  /**
   * Displays the cost limitation allocation screen.
   *
   * @param ebsCase The case details from EBS.
   * @param userDetails The details of the currently authenticated user.
   * @param model the Model object used to pass attributes to the view.
   * @return The cost limitation allocation view.
   */
  @GetMapping("/allocate-cost-limit")
  public String caseDetails(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @SessionAttribute(USER_DETAILS) final UserDetail userDetails,
      Model model,
      HttpSession session) {

    AllocateCostsFormData allocateCostsFormData =
        (AllocateCostsFormData) session.getAttribute(COST_ALLOCATION_FORM_DATA);

    if (allocateCostsFormData == null) {
      final ApplicationDetail freshCase =
          applicationService.getCase(
              ebsCase.getCaseReferenceNumber(),
              userDetails.getProvider().getId(),
              userDetails.getUsername());

      // Update only the costs to avoid overwriting session-scoped metadata like availableFunctions.
      ebsCase.setCosts(freshCase.getCosts());

      allocateCostsFormData = proceedingAndCostsMapper.toAllocateCostsForm(ebsCase);
      session.setAttribute(COST_ALLOCATION_FORM_DATA, allocateCostsFormData);
    }

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
   * @param removeCounsel the index of a counsel to remove, when the remove action was used.
   * @param model the Model object used to pass attributes to the view.
   * @return The cost limitation allocation view or redirect to review.
   */
  @PostMapping("/allocate-cost-limit")
  public String calculateCost(
      @ModelAttribute("costDetails") AllocateCostsFormData allocateCostsFormData,
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @RequestParam(value = "action", required = false) final String action,
      @RequestParam(value = "removeCounsel", required = false) final Integer removeCounsel,
      final Model model,
      final BindingResult bindingResult,
      final HttpSession session) {

    ApplicationDetail appCopy =
        copyApplicationMapper.copyApplication(new ApplicationDetail(), ebsCase);

    List<CostEntryDetail> costs =
        appCopy.getCosts().getCostEntries().stream().distinct().collect(Collectors.toList());

    proceedingAndCostsMapper.toAllocateCostsFormWithoutCostEntries(appCopy, allocateCostsFormData);

    List<CostEntryDetail> formCosts = allocateCostsFormData.getCostEntries();
    List<CostEntryDetail> updatedCosts = new ArrayList<>();

    for (CostEntryDetail formCost : formCosts) {
      // Find matching cost in existing costs
      CostEntryDetail existingCost =
          costs.stream()
              .filter(
                  c -> {
                    if (formCost.getLscResourceId() != null) {
                      return Objects.equals(c.getLscResourceId(), formCost.getLscResourceId());
                    }
                    return Objects.equals(c.getResourceName(), formCost.getResourceName());
                  })
              .findFirst()
              .orElse(null);

      if (existingCost != null) {
        // Re-allocating an amount does not make the entry new: newEntry marks counsel added during
        // this amendment, which are the only ones that may be removed again.
        existingCost.setRequestedCosts(formCost.getRequestedCosts());
        updatedCosts.add(existingCost);
      } else {
        // This is a new entry (e.g. added counsel)
        formCost.setNewEntry(true);
        if (formCost.getRequestedCosts() == null) {
          formCost.setRequestedCosts(BigDecimal.ZERO);
        }
        if (formCost.getAmountBilled() == null) {
          formCost.setAmountBilled(BigDecimal.ZERO);
        }
        if (formCost.getCostCategory() == null) {
          formCost.setCostCategory("COUNSEL");
        }

        updatedCosts.add(formCost);
      }
    }

    allocateCostsFormData.setCostEntries(updatedCosts);

    if (removeCounsel != null) {
      allocateCostsFormData.setTotalRemaining(getTotalRemaining(allocateCostsFormData));
      session.setAttribute(COST_ALLOCATION_FORM_DATA, allocateCostsFormData);
      return "redirect:/allocate-cost-limit/counsel/%d/remove".formatted(removeCounsel);
    }

    allocateCostsFormData.setTotalRemaining(getTotalRemaining(allocateCostsFormData));
    session.setAttribute(COST_ALLOCATION_FORM_DATA, allocateCostsFormData);

    allocateCostLimitValidator.validate(allocateCostsFormData, bindingResult);

    if (bindingResult.hasErrors() || "calculate".equals(action)) {
      model.addAttribute("case", ebsCase);
      model.addAttribute("costDetails", allocateCostsFormData);
      return "application/cost-allocation";
    }

    ebsCase.getCosts().setCostEntries(updatedCosts);
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
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      final HttpSession session,
      Model model) {

    AllocateCostsFormData allocateCostsFormData =
        (AllocateCostsFormData) session.getAttribute(COST_ALLOCATION_FORM_DATA);

    if (allocateCostsFormData == null) {
      allocateCostsFormData = proceedingAndCostsMapper.toAllocateCostsForm(ebsCase);
      session.setAttribute(COST_ALLOCATION_FORM_DATA, allocateCostsFormData);
    }

    allocateCostsFormData.setTotalRemaining(getTotalRemaining(allocateCostsFormData));

    model.addAttribute("costDetails", allocateCostsFormData);
    model.addAttribute("case", ebsCase);
    return "application/case-costs-review";
  }

  /**
   * Handles submission of the review case costs form.
   *
   * <p>The review screen only confirms what the user already allocated, so the costs held in
   * session are submitted as-is. Re-binding them from the form would drop the values it does not
   * render, such as the requested cost limitation and a cost entry's EBS id.
   *
   * @param activeCase The case currently being amended.
   * @param userDetail The details of the currently authenticated user.
   * @param allocateCostsFormData The cost allocations captured earlier in the journey.
   * @return Redirect to the submission in progress screen.
   */
  @PostMapping("/allocate-cost-limit/review")
  public String submitCaseCosts(
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      @SessionAttribute(USER_DETAILS) final UserDetail userDetail,
      @SessionAttribute(COST_ALLOCATION_FORM_DATA)
          final AllocateCostsFormData allocateCostsFormData,
      final HttpSession session) {

    final String transactionId =
        amendmentService.submitQuickAmendmentCostAllocation(
            allocateCostsFormData, activeCase.getCaseReferenceNumber(), userDetail);

    session.setAttribute(SUBMISSION_TRANSACTION_ID, transactionId);
    session.removeAttribute(SUBMISSION_RESULT);
    session.removeAttribute(COST_ALLOCATION_FORM_DATA);

    return "redirect:/amendments/submit-case";
  }

  /**
   * Asks the user to confirm removal of a counsel added during this amendment.
   *
   * @param index the position of the counsel in the cost entries.
   * @param allocateCostsFormData The cost allocations captured so far.
   * @param model the Model object used to pass attributes to the view.
   * @return The remove counsel confirmation view.
   */
  @GetMapping("/allocate-cost-limit/counsel/{index}/remove")
  public String removeCounselConfirm(
      @PathVariable final int index,
      @SessionAttribute(COST_ALLOCATION_FORM_DATA)
          final AllocateCostsFormData allocateCostsFormData,
      final Model model) {

    final CostEntryDetail counsel = getRemovableCounsel(index, allocateCostsFormData);

    if (counsel == null) {
      return "redirect:/allocate-cost-limit";
    }

    model.addAttribute("counsel", counsel);
    model.addAttribute("counselIndex", index);
    return "application/counsel-remove";
  }

  /**
   * Removes a counsel added during this amendment.
   *
   * @param index the position of the counsel in the cost entries.
   * @param allocateCostsFormData The cost allocations to remove the counsel from.
   * @return Redirect back to the cost limitation allocation screen.
   */
  @PostMapping("/allocate-cost-limit/counsel/{index}/remove")
  public String removeCounsel(
      @PathVariable final int index,
      @SessionAttribute(COST_ALLOCATION_FORM_DATA)
          final AllocateCostsFormData allocateCostsFormData,
      final HttpSession session) {

    if (getRemovableCounsel(index, allocateCostsFormData) != null) {
      allocateCostsFormData.getCostEntries().remove(index);
      allocateCostsFormData.setTotalRemaining(getTotalRemaining(allocateCostsFormData));
      session.setAttribute(COST_ALLOCATION_FORM_DATA, allocateCostsFormData);
    }

    return "redirect:/allocate-cost-limit";
  }

  /**
   * Returns the counsel at the given position when it may be removed.
   *
   * <p>Only counsel added during this amendment can be removed; those already held against the case
   * in EBS cannot. The rule is enforced here rather than relying on the remove link being hidden
   * for them.
   *
   * @param index the position of the counsel in the cost entries.
   * @param allocateCostsFormData the cost allocations to look in.
   * @return the counsel to remove, or null when it cannot be removed.
   */
  private CostEntryDetail getRemovableCounsel(
      final int index, final AllocateCostsFormData allocateCostsFormData) {

    final List<CostEntryDetail> costEntries = allocateCostsFormData.getCostEntries();

    if (index < 0 || index >= costEntries.size()) {
      log.warn("Ignoring request to remove counsel at out of range index {}", index);
      return null;
    }

    final CostEntryDetail costEntry = costEntries.get(index);

    if (!Boolean.TRUE.equals(costEntry.getNewEntry())) {
      log.warn("Ignoring request to remove counsel {} which is already held on the case", index);
      return null;
    }

    return costEntry;
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
