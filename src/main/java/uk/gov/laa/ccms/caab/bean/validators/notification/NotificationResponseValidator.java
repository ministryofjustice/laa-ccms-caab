package uk.gov.laa.ccms.caab.bean.validators.notification;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.notification.NotificationResponseFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validate notification response form data. */
@Component
public class NotificationResponseValidator extends AbstractValidator {

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
  }
}
