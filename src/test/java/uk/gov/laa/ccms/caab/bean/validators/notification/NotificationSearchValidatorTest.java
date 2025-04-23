package uk.gov.laa.ccms.caab.bean.validators.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class NotificationSearchValidatorTest {

  private NotificationSearchCriteria criteria;

  @InjectMocks
  private NotificationSearchValidator validator;

  private Errors errors;

  @BeforeEach
  void setup() {
    criteria = new NotificationSearchCriteria();
    errors = new BeanPropertyBindingResult(criteria, "criteria");
  }

  @Test
  void supports_ReturnsTrueForApplicationFormDataClass() {
    assertTrue(validator.supports(NotificationSearchCriteria.class));
  }

  @Test
  void supports_ReturnsFalseForOtherClasses() {
    assertFalse(validator.supports(Object.class));
  }


  @Test
  void allFieldsBlankValidator() {
    // Set dates to empty
    criteria.setNotificationFromDate("");
    criteria.setNotificationToDate("");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validateFromDate() {
    criteria.setNotificationFromDate("12/12/");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("notificationFromDate"));

  }

  @Test
  void validateToDate() {
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
  void validFromAndToDates() {
    criteria.setNotificationFromDate("12/12/2021");
    criteria.setNotificationToDate("13/12/2021");
    validator.validate(criteria, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  void invalidFromAndToDates() {
    criteria.setNotificationFromDate("13/12/2021");
    criteria.setNotificationToDate("12/12/2021");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validDates() {
    criteria.setNotificationFromDate("11/12/2021");
    criteria.setNotificationToDate("12/12/2022");
    validator.validate(criteria, errors);
    assertFalse(errors.hasErrors());
    assertEquals(0, errors.getErrorCount());
  }

  @Test
  @DisplayName("Should have validation errors when more than 3 years")
  void shouldHaveValidationErrorsWhenMoreThan3Years() {
    criteria.setNotificationFromDate("11/12/2021");
    criteria.setNotificationToDate("12/12/2024");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  @DisplayName("Should not have validation errors when just under 3 years")
  void shouldNotHaveValidationErrorsWhenJustUnder3Years() {
    criteria.setNotificationFromDate("11/12/2021");
    criteria.setNotificationToDate("10/12/2024");
    validator.validate(criteria, errors);
    assertFalse(errors.hasErrors());
    assertEquals(0, errors.getErrorCount());
  }

  @Test
  void caseRefDoubleSpaceValidation() {
    // Requires some text, otherwise value is disregarded for being blank
    criteria.setCaseReference("1  3");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void caseRefAlphaNumericValidation() {
    criteria.setCaseReference("$%^");
    validator.validate(criteria,errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validCaseRef() {
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
  void validateValidProviderCaseRef() {
    criteria.setProviderCaseReference("validProviderCaseRef");
    validator.validate(criteria, errors);
    assertFalse(errors.hasErrors());
    assertEquals(0, errors.getErrorCount());
  }

  @Test
  void providerCaseRefDoubleSpaceValidation() {
    // Requires some text, otherwise value is disregarded for being blank
    criteria.setProviderCaseReference("1  3");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }
}
