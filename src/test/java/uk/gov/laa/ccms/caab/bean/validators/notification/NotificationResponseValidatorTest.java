package uk.gov.laa.ccms.caab.bean.validators.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.notification.NotificationResponseFormData;

@ExtendWith(MockitoExtension.class)
class NotificationResponseValidatorTest {

  @InjectMocks private NotificationResponseValidator notificationResponseValidator;

  private NotificationResponseFormData formData;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    formData = new NotificationResponseFormData();
    errors = new BeanPropertyBindingResult(formData, "notificationResponseFormData");
  }

  @Test
  public void supports_ReturnsTrueForNotificationResponseFormData() {
    assertTrue(notificationResponseValidator.supports(NotificationResponseFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(notificationResponseValidator.supports(Object.class));
  }

  @Test
  public void validate_missingAction_rejects() {
    notificationResponseValidator.validate(formData, errors);

    assertNotNull(errors.getFieldError("action"));
    assertEquals("required.action", errors.getFieldError("action").getCode());
  }

  @Test
  public void validate_actionOnly_passes() {
    formData.setAction("ACCEPT");

    notificationResponseValidator.validate(formData, errors);

    assertFalse(errors.hasErrors());
  }

  /**
   * The message is optional, so an absent or blank value must not be rejected - only a populated
   * one is checked.
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  public void validate_blankMessage_isAccepted(final String message) {
    formData.setAction("ACCEPT");
    formData.setMessage(message);

    notificationResponseValidator.validate(formData, errors);

    assertFalse(errors.hasErrors());
    assertNull(errors.getFieldError("message"));
  }

  @Test
  public void validate_nullMessage_isAccepted() {
    formData.setAction("ACCEPT");

    notificationResponseValidator.validate(formData, errors);

    assertNull(errors.getFieldError("message"));
  }

  /**
   * Before this validation existed the message was accepted verbatim, including markup, despite
   * being round-tripped through EBS and rendered back into the notification page.
   */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "<script>alert(1)</script>",
        "'><img src=x onerror=alert(1)>",
        "a < b",
        "closing bracket >"
      })
  @DisplayName("markup in the message to LAA is rejected")
  public void validate_messageContainingMarkup_rejects(final String message) {
    formData.setAction("ACCEPT");
    formData.setMessage(message);

    notificationResponseValidator.validate(formData, errors);

    assertNotNull(errors.getFieldError("message"));
    assertEquals("invalid.format", errors.getFieldError("message").getCode());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Please find the requested documents attached.",
        "Client's address is 21-27, St Pauls Street.",
        "Costs are £1,250.00 (inc. VAT) - see attached breakdown.",
        "Line one\r\nline two"
      })
  @DisplayName("ordinary provider messages still pass")
  public void validate_legitimateMessage_passes(final String message) {
    formData.setAction("ACCEPT");
    formData.setMessage(message);

    notificationResponseValidator.validate(formData, errors);

    assertFalse(errors.hasErrors(), "rejected a legitimate message: " + message);
  }

  @Test
  public void validate_messageAtMaximumLength_passes() {
    formData.setAction("ACCEPT");
    formData.setMessage("a".repeat(2000));

    notificationResponseValidator.validate(formData, errors);

    assertFalse(errors.hasErrors());
  }

  @Test
  public void validate_messageAboveMaximumLength_rejects() {
    formData.setAction("ACCEPT");
    formData.setMessage("a".repeat(2001));

    notificationResponseValidator.validate(formData, errors);

    assertNotNull(errors.getFieldError("message"));
    assertEquals("length.exceeds.max", errors.getFieldError("message").getCode());
  }
}
