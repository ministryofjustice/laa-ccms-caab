package uk.gov.laa.ccms.caab.bean.validators.billing;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.billing.UndertakingFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validates the undertaking form. */
@Component
public class BillingUndertakingValidator extends AbstractValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from {@link
   *     uk.gov.laa.ccms.caab.bean.billing.UndertakingFormData}, {@code false} otherwise.
   */
  @Override
  public boolean supports(final Class<?> clazz) {
    return UndertakingFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the undertaking details in the {@link UndertakingFormData}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(final Object target, final Errors errors) {
    final UndertakingFormData undertakingFormData = (UndertakingFormData) target;

    validateRequiredField(
        "undertakingAmount",
        undertakingFormData.getUndertakingAmount(),
        "Total Bills including Counsel will not exceed",
        errors);

    if (!undertakingFormData.isAcceptedTerms()) {
      errors.rejectValue("acceptedTerms", "billing.acceptedTerms.required");
    }

    if (undertakingFormData.getUndertakingAmount() != null
        && !undertakingFormData.getUndertakingAmount().isEmpty()) {
      validateCurrencyField(
          "undertakingAmount",
          undertakingFormData.getUndertakingAmount(),
          "Total Bills including Counsel will not exceed",
          errors);
    }

    if (!errors.hasFieldErrors("undertakingAmount")
        && undertakingFormData.getUndertakingMinimumAmount() != null
        && undertakingFormData.getUndertakingMaximumAmount() != null) {
      final BigDecimal undertakingAmount =
          new BigDecimal(undertakingFormData.getUndertakingAmount());
      if (undertakingFormData.getUndertakingMaximumAmount().compareTo(BigDecimal.ZERO) == 0
          || undertakingAmount.compareTo(undertakingFormData.getUndertakingMinimumAmount()) < 0
          || undertakingAmount.compareTo(undertakingFormData.getUndertakingMaximumAmount()) > 0) {
        errors.rejectValue("undertakingAmount", "billing.undertakingAmount.outOfRange");
      }
    }
  }
}
