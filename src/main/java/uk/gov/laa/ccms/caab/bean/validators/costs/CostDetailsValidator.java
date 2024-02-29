package uk.gov.laa.ccms.caab.bean.validators.costs;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.costs.CostsFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityFormDataDetails;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validates the cost details form.
 */
@Component
public class CostDetailsValidator extends AbstractValidator {

  private static final BigDecimal MAX_COST_LIMIT = new BigDecimal("100000000.00");

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from
   *         {@link uk.gov.laa.ccms.caab.bean.costs.CostsFormData},
   *         {@code false} otherwise.
   */
  @Override
  public boolean supports(final Class<?> clazz) {
    return CostsFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the cost details in the
   * {@link uk.gov.laa.ccms.caab.bean.costs.CostsFormData}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(final Object target, final Errors errors) {
    final CostsFormData costDetails = (CostsFormData) target;

    validateRequiredField("requestedCostLimitation",
        String.valueOf(costDetails.getRequestedCostLimitation()),
        "Requested cost limitation", errors);

    if (costDetails.getRequestedCostLimitation() != null) {
      validateCurrencyField("requestedCostLimitation",
          costDetails.getRequestedCostLimitation(),
          "Requested cost limitation", errors);



      validateNumericLimit("requestedCostLimitation", costDetails.getRequestedCostLimitation(),
          "Requested cost limitation", MAX_COST_LIMIT, errors);
    }

  }

}


