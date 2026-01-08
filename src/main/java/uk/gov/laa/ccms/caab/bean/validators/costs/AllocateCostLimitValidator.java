package uk.gov.laa.ccms.caab.bean.validators.costs;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.costs.AllocateCostsFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;

/** Validates allocate costs limit. */
@Component
public class AllocateCostLimitValidator extends AbstractValidator {

  private static final BigDecimal MAX_COST_LIMIT = new BigDecimal("100000000.00");

  @Override
  public boolean supports(final Class<?> clazz) {
    return CostEntryDetail.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {

    BigDecimal total = new BigDecimal(0);
    final AllocateCostsFormData allocateCostsFormData = (AllocateCostsFormData) target;
    final List<BigDecimal> requestedCosts =
        allocateCostsFormData.getCostEntries().stream()
            .map(CostEntryDetail::getRequestedCosts)
            .collect(Collectors.toList());
    final List<BigDecimal> amountsBilled =
        allocateCostsFormData.getCostEntries().stream()
            .map(CostEntryDetail::getAmountBilled)
            .collect(Collectors.toList());

    for (int i = 0; i < allocateCostsFormData.getCostEntries().size(); i++) {
      BigDecimal requestedCost = requestedCosts.get(i);
      BigDecimal amountBilled = amountsBilled.get(i);
      // Is the entered cost less than the amount already billed
      if (requestedCost.compareTo(amountBilled) < 0) {
        errors.rejectValue(
            "costEntries[" + i + "]", "costCostAllocation.requestedAmount.belowBilledAmount");
      } else {
        total = total.add(requestedCost);
      }
      // Is the number smaller than the max limit (99999999.99)
      validateNumericLimit(
          "costEntries[" + i + "]",
          String.valueOf(requestedCost),
          "Requested cost limitation",
          MAX_COST_LIMIT,
          errors);
    }
    if (!errors.hasErrors()) {
      BigDecimal remaining = allocateCostsFormData.getGrantedCostLimitation().subtract(total);
      // Are the entered costs more than the granted cost limitation
      if (remaining.compareTo(BigDecimal.ZERO) < 0) {
        errors.rejectValue("grantedCostLimitation", "costCostAllocation.exceeded.requestedCost");
      } else if (amountsBilled != null
          && remaining.compareTo(allocateCostsFormData.getCurrentProviderBilledAmount()) < 0) {
        errors.rejectValue(
            "currentProviderBilledAmount", "costCostAllocation.requestedAmount.belowBilledAmount");
      }
    }
  }
}
