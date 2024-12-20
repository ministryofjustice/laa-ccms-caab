package uk.gov.laa.ccms.caab.bean.validators.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;

@ExtendWith(SpringExtension.class)
class NotificationSearchValidatorTest {

  private NotificationSearchCriteria criteria;

  @InjectMocks
  private NotificationSearchValidator validator;

  private Errors errors;

  @BeforeEach
  public void setup() {
    criteria = new NotificationSearchCriteria();
    errors = new BeanPropertyBindingResult(criteria, "criteria");
  }

  @Test
  public void supports_ReturnsTrueForApplicationFormDataClass() {
    assertTrue(validator.supports(NotificationSearchCriteria.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(validator.supports(Object.class));
  }


  @Test
  void testAllFieldsBlankValidator() {
    // Set dates to empty
    criteria.setNotificationFromDate("");
    criteria.setNotificationToDate("");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void testValidateFromDate() {
    criteria.setNotificationFromDate("12/12/");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("notificationFromDate"));

  }

  @Test
  void testValidateToDate() {
    criteria.setNotificationToDate("12/12/");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("notificationToDate"));
  }

  @Test
  void validateNonNumericalDates() {
    criteria.setNotificationFromDate("A/12/2021");
    criteria.setNotificationToDate("13/12/B");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(2, errors.getErrorCount());
    assertNotNull(errors.getFieldError("notificationFromDate"));
    assertNotNull(errors.getFieldError("notificationToDate"));
  }

  @Test
  void validateInvalidDate() {
    criteria.setNotificationToDate("12/13/2021");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("notificationToDate"));
  }

  @Test
  void testValidFromAndToDates() {
    criteria.setNotificationFromDate("12/12/2021");
    criteria.setNotificationToDate("13/12/2021");
    validator.validate(criteria, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  void testInvalidFromAndToDates() {
    criteria.setNotificationFromDate("13/12/2021");
    criteria.setNotificationToDate("12/12/2021");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void testValidDates() {
    criteria.setNotificationFromDate("11/12/2021");
    criteria.setNotificationToDate("12/12/2022");
    validator.validate(criteria, errors);
    assertFalse(errors.hasErrors());
    assertEquals(0, errors.getErrorCount());
  }

  @Test
  @DisplayName("Should have validation errors when more than 3 years")
  void testShouldHaveValidationErrorsWhenMoreThan3Years() {
    criteria.setNotificationFromDate("11/12/2021");
    criteria.setNotificationToDate("12/12/2024");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  @DisplayName("Should not have validation errors when just under 3 years")
  void testShouldNotHaveValidationErrorsWhenJustUnder3Years() {
    criteria.setNotificationFromDate("11/12/2021");
    criteria.setNotificationToDate("10/12/2024");
    validator.validate(criteria, errors);
    assertFalse(errors.hasErrors());
    assertEquals(0, errors.getErrorCount());
  }

  @Test
  void testCaseRefDoubleSpaceValidation() {
    // Requires some text, otherwise value is disregarded for being blank
    criteria.setCaseReference("1  3");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void testCaseRefAlphaNumericValidation() {
    criteria.setCaseReference("$%^");
    validator.validate(criteria,errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void testValidCaseRef() {
    criteria.setCaseReference("30000aaa");
    validator.validate(criteria, errors);
    assertFalse(errors.hasErrors());
    assertEquals(0, errors.getErrorCount());
  }

  @Test
  void validateInvalidClientSurnameWithDoubleSpace() {
    criteria.setClientSurname("a  b");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validateInvalidateClientSurnameFirstCharacterAlpha() {
    criteria.setClientSurname("1A ");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validateInvalidateClientSurnameCharacterSetC() {
    criteria.setClientSurname("A1 ");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validateValidClientSurname() {
    criteria.setClientSurname("Humphreysismostvalid");
    validator.validate(criteria, errors);
    assertFalse(errors.hasErrors());
    assertEquals(0, errors.getErrorCount());
  }

  @Test
  void validateInvalidProviderCaseRef() {
    criteria.setProviderCaseReference("¢ref");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validateValidProviderCaseRef() {
    criteria.setProviderCaseReference("validProviderCaseRef");
    validator.validate(criteria, errors);
    assertFalse(errors.hasErrors());
    assertEquals(0, errors.getErrorCount());
  }

}
