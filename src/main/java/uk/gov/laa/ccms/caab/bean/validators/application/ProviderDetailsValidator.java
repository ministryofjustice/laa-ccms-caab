package uk.gov.laa.ccms.caab.bean.validators.application;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validates the office details provided by the user. */
@Component
public class ProviderDetailsValidator extends AbstractValidator {

  private static final String PROVIDER_CASE_REF_CHAR_SET =
      "^[|A-Za-z0-9&'\\(\\)\\.\\*\\-\\/!#$%,;\\?\\@\\[\\]_`+\\=>£:\\`\\\\]*$";

  /**
   * Determine if this validator can validate instances of the given class.
   *
   * @param clazz the class to check
   * @return whether this validator can validate instances of the given class.
   */
  @Override
  public boolean supports(final Class<?> clazz) {
    return ApplicationFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validate the given target object.
   *
   * @param target the target object to validate
   * @param errors contextual state about the validation process
   */
  @Override
  public void validate(final Object target, final Errors errors) {
    final ApplicationFormData applicationFormData = (ApplicationFormData) target;

    if (applicationFormData.getProviderCaseReference() != null) {
      validateFieldFormat(
          "providerCaseReference",
          applicationFormData.getProviderCaseReference(),
          PROVIDER_CASE_REF_CHAR_SET,
          "Provider case reference",
          errors);
    }

    validateRequiredField(
        "contactNameId", applicationFormData.getContactNameId(), "Contact name", errors);
  }
}
