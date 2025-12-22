package uk.gov.laa.ccms.caab.bean.validators.costs;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.costs.CostsFormData;
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

    final CostsFormData costDetails = (CostsFormData) target;
    final List<BigDecimal> requestedCosts =
        costDetails.getCostEntries().stream()
            .map(CostEntryDetail::getRequestedCosts)
            .collect(Collectors.toList());
    final List<BigDecimal> amountsBilled =
        costDetails.getCostEntries().stream()
            .map(CostEntryDetail::getAmountBilled)
            .collect(Collectors.toList());

    for (int i = 0; i < costDetails.getCostEntries().size(); i++) {
      BigDecimal requestedCost = requestedCosts.get(i);
      BigDecimal amountBilled = amountsBilled.get(i);
      if (requestedCost.compareTo(amountBilled) < 0) {
        errors.rejectValue(
            "costEntries[" + i + "]", "costCostAllocation.requestedAmount.belowBilledAmount");
      }
      validateNumericLimit(
          "costEntries[" + i + "]",
          String.valueOf(requestedCost),
          "Requested cost limitation",
          MAX_COST_LIMIT,
          errors);
    }
  }
}
