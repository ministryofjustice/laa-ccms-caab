package uk.gov.laa.ccms.caab.bean.validators.application;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validates the application details provided by the user.
 */
@Component
public class PrivacyNoticeAgreementValidator extends AbstractValidator {

  /**
   * Determine if this validator can validate instances of the given class.
   *
   * @param clazz the class to check
   * @return whether this validator can validate instances of the given class.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return ApplicationFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validate the given target object.
   *
   * @param target the target object to validate
   * @param errors contextual state about the validation process
   */
  @Override
  public void validate(Object target, Errors errors) {
    ApplicationFormData applicationFormData = (ApplicationFormData) target;

    if (!applicationFormData.isAgreementAccepted()) {
      errors.rejectValue("agreementAccepted",
          "agreement.not.accepted",
          "Please complete 'I confirm my client (or their representative) has read and "
              + "agreed to the Privacy Notice'.");
    }
  }
}
