package uk.gov.laa.ccms.caab.bean.validators.costs;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
    final List<CostEntryDetail> costEntries = allocateCostsFormData.getCostEntries();

    final Integer size = costEntries.size();
    final List<BigDecimal> requestedCosts =
        costEntries.stream().map(CostEntryDetail::getRequestedCosts).collect(Collectors.toList());
    final List<BigDecimal> amountsBilled =
        costEntries.stream().map(CostEntryDetail::getAmountBilled).collect(Collectors.toList());

    for (int i = 0; i < size; i++) {
      BigDecimal requestedCost = requestedCosts.get(i);
      BigDecimal amountBilled = amountsBilled.get(i);

      if (amountBilled == null) {
        amountBilled = BigDecimal.ZERO;
      }

      // Is the entered cost less than the amount already billed
      if (requestedCost == null) {
        errors.rejectValue(
            "costEntries[" + i + "].requestedCosts",
            "costCostAllocation.requestedAmount.nullAmount");
        requestedCost = BigDecimal.ZERO;
      } else {
        if (requestedCost.compareTo(amountBilled) < 0) {
          errors.rejectValue(
              "costEntries[" + i + "].requestedCosts",
              "costCostAllocation.requestedAmount.belowBilledAmount");
        } else {
          total = total.add(requestedCost);
        }
        // Is the number smaller than the max limit (99999999.99)
        validateNumericLimit(
            "costEntries[" + i + "].requestedCosts",
            String.valueOf(requestedCost),
            "Requested cost limitation",
            MAX_COST_LIMIT,
            errors);

        // Does the number have a maximum of 2 Decimal Places
        validateNumberAllowed2dp(
            "costEntries[" + i + "].requestedCosts", String.valueOf(requestedCost), errors);
      }
    }
    if (!errors.hasErrors()) {
      BigDecimal remaining = allocateCostsFormData.getGrantedCostLimitation().subtract(total);
      // Are the entered costs more than the granted cost limitation
      int index =
          IntStream.range(0, size)
              .filter(
                  i -> {
                    Boolean isNew = allocateCostsFormData.getCostEntries().get(i).getNewEntry();
                    return Boolean.TRUE.equals(isNew);
                  })
              .findFirst()
              .orElse(-1);
      if (remaining.compareTo(BigDecimal.ZERO) < 0) {
        errors.rejectValue(
            "costEntries[" + (index == -1 ? 0 : index) + "].requestedCosts",
            "costCostAllocation.exceeded.requestedCost");
        errors.rejectValue("grantedCostLimitation", "costCostAllocation.empty.error");

      } else if (allocateCostsFormData.getCurrentProviderBilledAmount() != null
          && remaining.compareTo(allocateCostsFormData.getCurrentProviderBilledAmount()) < 0) {
        errors.rejectValue(
            "costEntries[" + (index == -1 ? 0 : index) + "].requestedCosts",
            "costCostAllocation.requestedAmount.belowBilledAmount");
      }
    }
  }
}
