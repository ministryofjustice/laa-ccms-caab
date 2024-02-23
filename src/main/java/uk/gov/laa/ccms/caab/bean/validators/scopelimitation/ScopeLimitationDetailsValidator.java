package uk.gov.laa.ccms.caab.bean.validators.scopelimitation;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataProceedingDetails;
import uk.gov.laa.ccms.caab.bean.scopelimitation.ScopeLimitationFormDataDetails;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validates the scope limitation details provided by scope limitation flow forms.
 */
@Component
public class ScopeLimitationDetailsValidator extends AbstractValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from
   *         {@link uk.gov.laa.ccms.caab.bean.scopelimitation.ScopeLimitationFormDataDetails},
   *         {@code false} otherwise.
   */
  @Override
  public boolean supports(final Class<?> clazz) {
    return ScopeLimitationFormDataDetails.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the scope limitation details in the
   * {@link uk.gov.laa.ccms.caab.bean.scopelimitation.ScopeLimitationFormDataDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(final Object target, final Errors errors) {
    final ScopeLimitationFormDataDetails scopeLimitationDetails =
        (ScopeLimitationFormDataDetails) target;

    validateRequiredField("scopeLimitation", scopeLimitationDetails.getScopeLimitation(),
        "Scope limitation", errors);
  }

}


