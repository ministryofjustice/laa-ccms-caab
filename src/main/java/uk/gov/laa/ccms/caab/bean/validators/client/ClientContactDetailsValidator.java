package uk.gov.laa.ccms.caab.bean.validators.client;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validator component responsible for validating {@link uk.gov.laa.ccms.caab.bean.ClientDetails}
 * objects.
 */
@Component
public class ClientContactDetailsValidator extends AbstractValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from
   *         {@link uk.gov.laa.ccms.caab.bean.ClientDetails}, {@code false} otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return ClientDetails.class.isAssignableFrom(clazz);
  }

  /**
   * Validate the telephone numbers in the {@link ClientDetails}.
   *
   * @param clientDetails The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  private void validateTelephones(ClientDetails clientDetails, Errors errors) {
    // At least one telephone number should be provided
    if (!StringUtils.hasText(clientDetails.getTelephoneHome())
        && !StringUtils.hasText(clientDetails.getTelephoneWork())
        && !StringUtils.hasText(clientDetails.getTelephoneMobile())) {

      errors.reject("required.telephones",
          "Please provide at least one contact telephone number.");
    }

    // Validate each telephone number for non-numeric characters and minimum length
    validateTelephoneField("telephoneHome",
        clientDetails.getTelephoneHome(), "Telephone Home", errors);
    validateTelephoneField("telephoneWork",
        clientDetails.getTelephoneWork(), "Telephone Work", errors);
    validateTelephoneField("telephoneMobile",
        clientDetails.getTelephoneMobile(), "Mobile", errors);
  }

  /**
   * Validate the telephone numbers in the {@link ClientDetails}.
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
    }
  }

  /**
   * Validate the email address in the {@link ClientDetails}.
   *
   * @param clientDetails The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  private void validateEmailField(ClientDetails clientDetails, Errors errors) {
    if (!StringUtils.hasText(clientDetails.getEmailAddress())
        && clientDetails.getCorrespondenceMethod() != null
        && clientDetails.getCorrespondenceMethod().equalsIgnoreCase("E-mail")) {
      errors.rejectValue("emailAddress", "required.emailAddress",
          "Please provide an email address, or select another correspondence method.");
    }
  }

  /**
   * Validate the password and password reminder fields in the {@link ClientDetails}.
   *
   * @param clientDetails The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  private void validatePasswordNeedsReminder(ClientDetails clientDetails, Errors errors) {
    if (StringUtils.hasText(clientDetails.getPassword())) {
      if (clientDetails.getPassword().equalsIgnoreCase(clientDetails.getPasswordReminder())) {
        errors.rejectValue("password", "same.passwordReminder",
            "Your password reminder cannot be the same as your password. "
                + "Please amend your entry.");
      }
    }
  }

  /**
   * Validates the client contact details in the {@link ClientDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    ClientDetails clientDetails = (ClientDetails) target;

    validateRequiredField("password", clientDetails.getPassword(),
        "Password", errors);
    validateRequiredField("passwordReminder", clientDetails.getPasswordReminder(),
        "Password reminder", errors);

    validatePasswordNeedsReminder(clientDetails, errors);
    validateEmailField(clientDetails, errors);

    if (!clientDetails.getVulnerableClient()) {
      validateTelephones(clientDetails, errors);
      validateRequiredField("correspondenceMethod", clientDetails.getCorrespondenceMethod(),
          "Correspondence method", errors);
    }
  }

}
