package uk.gov.laa.ccms.caab.bean.validators.notification;

import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.NOTIFICATION_MESSAGE_CHARACTER_SIZE;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.STANDARD_CHARACTER_SET;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.notification.NotificationResponseFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validate notification response form data. */
@Component
public class NotificationResponseValidator extends AbstractValidator {

  private static final String MESSAGE_DISPLAY_NAME = "Message to LAA";

  @Override
  public boolean supports(Class<?> clazz) {
    return NotificationResponseFormData.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    NotificationResponseFormData notificationResponseFormData =
        (NotificationResponseFormData) target;

    validateRequiredField(
        "action", notificationResponseFormData.getAction(), "Notification response action", errors);

    validateMessage(notificationResponseFormData.getMessage(), errors);
  }

  /**
   * Validate the free-text message sent to LAA. The field is optional, so it is only checked when a
   * value is present.
   *
   * @param message the submitted message.
   * @param errors the Errors object to store validation errors.
   */
  private void validateMessage(final String message, final Errors errors) {
    if (StringUtils.hasText(message)) {
      validateFieldFormat("message", message, STANDARD_CHARACTER_SET, MESSAGE_DISPLAY_NAME, errors);
      validateFieldMaxLength(
          "message", message, NOTIFICATION_MESSAGE_CHARACTER_SIZE, MESSAGE_DISPLAY_NAME, errors);
    }
  }
}
