package uk.gov.laa.ccms.caab.bean.validators.proceedings;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataMatterTypeDetails;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataProceedingDetails;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validator the proceeding details provided by proceeding flow forms.
 */
@Component
public class ProceedingTypeDetailsValidator extends AbstractValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from
   *         {@link uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataProceedingDetails},
   *         {@code false} otherwise.
   */
  @Override
  public boolean supports(final Class<?> clazz) {
    return ProceedingFormDataProceedingDetails.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the proceeding type details in the
   * {@link uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataProceedingDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(final Object target, final Errors errors) {
    final ProceedingFormDataProceedingDetails proceedingDetails =
        (ProceedingFormDataProceedingDetails) target;

    validateRequiredField("proceedingType", proceedingDetails.getProceedingType(),
        "Proceeding", errors);
  }

}
