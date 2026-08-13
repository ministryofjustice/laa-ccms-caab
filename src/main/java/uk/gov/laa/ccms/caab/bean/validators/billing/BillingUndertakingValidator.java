package uk.gov.laa.ccms.caab.bean.validators.billing;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.billing.UndertakingFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validates the cost details form. */
@Component
public class BillingUndertakingValidator extends AbstractValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from
   * {@link uk.gov.laa.ccms.caab.bean.billing.UndertakingFormData}, {@code false} otherwise.
   */
  @Override
  public boolean supports(final Class<?> clazz) {
    return UndertakingFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the cost details in the {@link uk.gov.laa.ccms.caab.bean.costs.CostsFormData}.
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
        "Undertaking",
        errors);

//    validateRequiredField(
//        "acceptedTerms",
//        undertakingFormData.getAcceptedTerms(),
//        "Accepted terms",
//        errors);

    if (undertakingFormData.getUndertakingAmount() != null
        && !undertakingFormData.getUndertakingAmount().isEmpty()) {
      validateCurrencyField(
          "undertakingAmount",
          undertakingFormData.getUndertakingAmount(),
          "Undertaking",
          errors);
    }
  }

  public void validateUndertakingRange(
      final BigDecimal undertakingAmount,
      final BigDecimal minimumUndertaking,
      final BigDecimal maximumUndertaking,
      final Errors errors) {

    if (undertakingAmount != null) {
      if (undertakingAmount.compareTo(minimumUndertaking) < 0
          || undertakingAmount.compareTo(maximumUndertaking) > 0) {
        errors.rejectValue(
            "undertakingAmount",
            "billing.undertakingAmount.outOfRange");
      }
    }
  }
}

