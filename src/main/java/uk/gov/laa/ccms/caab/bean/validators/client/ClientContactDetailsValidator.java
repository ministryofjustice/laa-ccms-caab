package uk.gov.laa.ccms.caab.bean.validators.client;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientFormDataContactDetails;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validator component responsible for validating
 * {@link uk.gov.laa.ccms.caab.bean.ClientFormDataContactDetails} objects.
 */
@Component
public class ClientContactDetailsValidator extends AbstractValidator {

  private static String CORRESPONDENCE_EMAIL = "E-mail";

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from
   *         {@link uk.gov.laa.ccms.caab.bean.ClientFormDataContactDetails},
   *         {@code false} otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return ClientFormDataContactDetails.class.isAssignableFrom(clazz);
  }

  /**
   * Validate the telephone numbers in the {@link ClientFormDataContactDetails}.
   *
   * @param contactDetails The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  private void validateTelephones(ClientFormDataContactDetails contactDetails, Errors errors) {
    // At least one telephone number should be provided
    if (!contactDetails.isTelephoneHomePresent()
        && !contactDetails.isTelephoneWorkPresent()
        && !contactDetails.isTelephoneMobilePresent()) {
      errors.reject("required.telephones",
          "Please provide at least one contact telephone number.");
    }

    // Validate each telephone number for non-numeric characters and minimum length
    if (contactDetails.isTelephoneHomePresent()) {
      validateTelephoneField("telephoneHome",
          contactDetails.getTelephoneHome(), "Telephone Home", errors);
    }
    if (contactDetails.isTelephoneWorkPresent()) {
      validateTelephoneField("telephoneWork",
          contactDetails.getTelephoneWork(), "Telephone Work", errors);
    }
    if (contactDetails.isTelephoneMobilePresent()) {
      validateTelephoneField("telephoneMobile",
          contactDetails.getTelephoneMobile(), "Mobile", errors);
    }
  }

  /**
   * Validate the telephone numbers in the {@link ClientFormDataContactDetails}.
   *
   * @param field The field name being validated.
   * @param fieldValue The field value being validated.
   * @param displayValue The display value of the field being validated.
   * @param errors The Errors object to store validation errors.
   */
  private void validateTelephoneField(
      String field, String fieldValue, String displayValue, Errors errors) {
    if (fieldValue != null && !fieldValue.isEmpty()) {
      if (!fieldValue.matches("[0-9+/-]+")) {
        errors.rejectValue(field, "invalid." + field, "Your input for '"
            + displayValue + "' contains an invalid character. Please amend your entry.");
      } else if (fieldValue.length() < 8) {
        errors.rejectValue(field, "length." + field, "Your input for '"
            + displayValue + "' must contain at least 8 characters. Please amend your entry.");
      }
    } else {
      errors.rejectValue(field, "invalid." + field, "Please enter '"
          + displayValue + "'");
    }
  }

  /**
   * Validate the email address in the {@link ClientFormDataContactDetails}.
   *
   * @param contactDetails The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  private void validateEmailField(ClientFormDataContactDetails contactDetails, Errors errors) {
    if (!StringUtils.hasText(contactDetails.getEmailAddress())
        && CORRESPONDENCE_EMAIL.equalsIgnoreCase(contactDetails.getCorrespondenceMethod())) {
      errors.rejectValue("emailAddress", "required.emailAddress",
          "Please provide an email address, or select another correspondence method.");
    }
  }

  /**
   * Validate the password and password reminder fields in the {@link ClientFormDataContactDetails}.
   *
   * @param contactDetails The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  private void validatePasswordNeedsReminder(
      ClientFormDataContactDetails contactDetails, Errors errors) {
    if (StringUtils.hasText(contactDetails.getPassword())) {
      if (contactDetails.getPassword().equalsIgnoreCase(contactDetails.getPasswordReminder())) {
        errors.rejectValue("password", "same.passwordReminder",
            "Your password reminder cannot be the same as your password. "
                + "Please amend your entry.");
      }
    }
  }

  /**
   * Validates the client contact details in the {@link ClientFormDataContactDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    ClientFormDataContactDetails contactDetails = (ClientFormDataContactDetails) target;

    validateRequiredField("password", contactDetails.getPassword(),
        "Password", errors);
    validateRequiredField("passwordReminder", contactDetails.getPasswordReminder(),
        "Password reminder", errors);

    validatePasswordNeedsReminder(contactDetails, errors);
    validateEmailField(contactDetails, errors);

    if (!contactDetails.getVulnerableClient()) {
      validateTelephones(contactDetails, errors);
      validateRequiredField("correspondenceMethod", contactDetails.getCorrespondenceMethod(),
          "Correspondence method", errors);
    }
  }

}
