package uk.gov.laa.ccms.caab.bean.validators.proceedings;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataMatterTypeDetails;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validator the matter type details provided by proceeding flow forms.
 */
@Component
public class ProceedingMatterTypeDetailsValidator extends AbstractValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from
   *         {@link uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataMatterTypeDetails},
   *         {@code false} otherwise.
   */
  @Override
  public boolean supports(final Class<?> clazz) {
    return ProceedingFormDataMatterTypeDetails.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the matter type details in the
   * {@link uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataMatterTypeDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(final Object target, final Errors errors) {
    final ProceedingFormDataMatterTypeDetails matterTypeDetails =
        (ProceedingFormDataMatterTypeDetails) target;

    validateRequiredField("matterType", matterTypeDetails.getMatterType(),
        "Matter type", errors);
  }

}
