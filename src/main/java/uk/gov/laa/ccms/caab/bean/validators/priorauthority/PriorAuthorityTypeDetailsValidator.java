package uk.gov.laa.ccms.caab.bean.validators.priorauthority;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityTypeFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validator component responsible for validating
 * {@link uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityTypeFormData} objects.
 */
@Component
public class PriorAuthorityTypeDetailsValidator extends AbstractValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from
   *         {@link uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityTypeFormData},
   *         {@code false} otherwise.
   */
  @Override
  public boolean supports(final Class<?> clazz) {
    return PriorAuthorityTypeFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the prior authority type details in the
   * {@link uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityTypeFormData}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(final Object target, final Errors errors) {
    final PriorAuthorityTypeFormData priorAuthorityTypeDetails =
        (PriorAuthorityTypeFormData) target;

    validateRequiredField("priorAuthorityType", priorAuthorityTypeDetails.getPriorAuthorityType(),
        "Prior authority type", errors);

  }

}
