package uk.gov.laa.ccms.caab.bean.validators.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
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
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void testValidateFromDate() {
    criteria.setNotificationFromDateDay("12");
    criteria.setNotificationFromDateMonth("12");
    criteria.setNotificationFromDateYear("");

    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("notificationFromDate"));

  }

  @Test
  void testValidateToDate() {
    criteria.setNotificationToDateDay("12");
    criteria.setNotificationToDateMonth("12");
    criteria.setNotificationToDateYear("");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
    assertNotNull(errors.getFieldError("notificationToDate"));
  }

  @Test
  void validateNonNumericalDates() {
    criteria.setNotificationFromDateDay("A");
    criteria.setNotificationFromDateMonth("12");
    criteria.setNotificationFromDateYear("2021");
    criteria.setNotificationToDateDay("13");
    criteria.setNotificationToDateMonth("12");
    criteria.setNotificationToDateYear("B");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(4, errors.getErrorCount());
    assertNotNull(errors.getFieldError("notificationFromDate"));
    assertNotNull(errors.getFieldError("notificationFromDateDay"));
    assertNotNull(errors.getFieldError("notificationToDate"));
    assertNotNull(errors.getFieldError("notificationToDateYear"));
  }

  @Test
  void validateInvalidDate() {
    criteria.setNotificationToDateDay("12");
    criteria.setNotificationToDateMonth("12");
    criteria.setNotificationToDateYear("2021/");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(2, errors.getErrorCount());
    assertNotNull(errors.getFieldError("notificationToDateYear"));
    assertNotNull(errors.getFieldError("notificationToDate"));
  }

  @Test
  void testValidFromAndToDates() {
    criteria.setNotificationFromDateDay("12");
    criteria.setNotificationFromDateMonth("12");
    criteria.setNotificationFromDateYear("2021");
    criteria.setNotificationToDateDay("13");
    criteria.setNotificationToDateMonth("12");
    criteria.setNotificationToDateYear("2021");
    validator.validate(criteria, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  void testInvalidFromAndToDates() {
    criteria.setNotificationFromDateDay("13");
    criteria.setNotificationFromDateMonth("12");
    criteria.setNotificationFromDateYear("2021");
    criteria.setNotificationToDateDay("12");
    criteria.setNotificationToDateMonth("12");
    criteria.setNotificationToDateYear("2021");
    validator.validate(criteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void testValidDates() {
    criteria.setNotificationFromDateDay("11");
    criteria.setNotificationFromDateMonth("12");
    criteria.setNotificationFromDateYear("2021");
    criteria.setNotificationToDateDay("12");
    criteria.setNotificationToDateMonth("12");
    criteria.setNotificationToDateYear("2022");
    validator.validate(criteria, errors);
    assertFalse(errors.hasErrors());
    assertEquals(0, errors.getErrorCount());
  }

  @Test
  void testCaseRefDoubleSpaceValidation() {
    criteria.setCaseReference("  ");
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
