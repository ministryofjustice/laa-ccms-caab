package uk.gov.laa.ccms.caab.bean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

@ExtendWith(SpringExtension.class)
public class ClientSearchCriteriaValidatorTest {

  @InjectMocks
  private ClientSearchCriteriaValidator validator;
  private ClientSearchCriteria clientSearchCriteria;
  private Errors errors;

  @BeforeEach
  public void setUp() {
    clientSearchCriteria = new ClientSearchCriteria();
    errors = new BeanPropertyBindingResult(clientSearchCriteria, "clientSearchCriteria");
  }

  @Test
  public void supports_ReturnsTrueForClientSearchDetailsClass() {
    assertTrue(validator.supports(ClientSearchCriteria.class));
  }

  @Test
  public void testValidateForename_Valid() {
    clientSearchCriteria.setForename("John");
    validator.validateForename(clientSearchCriteria, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  public void testValidateForename_Invalid() {
    validator.validateForename(clientSearchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("forename"));
    assertEquals("required.forename", errors.getFieldError("forename").getCode());
  }

  @Test
  public void testValidateSurnameAtBirth_Valid() {
    clientSearchCriteria.setSurname("Doe");
    validator.validateSurnameAtBirth(clientSearchCriteria, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  public void testValidateSurnameAtBirth_Invalid() {
    validator.validateSurnameAtBirth(clientSearchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("surname"));
    assertEquals("required.surname", errors.getFieldError("surname").getCode());
  }

  @Test
  public void testValidateDateOfBirth_Valid() {
    clientSearchCriteria.setDobDay("01");
    clientSearchCriteria.setDobMonth("12");
    clientSearchCriteria.setDobYear("1990");
    validator.validateDateOfBirth(clientSearchCriteria, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  public void testValidateDateOfBirth_Invalid() {
    clientSearchCriteria.setDobDay("");
    clientSearchCriteria.setDobMonth("");
    clientSearchCriteria.setDobYear("");
    validator.validateDateOfBirth(clientSearchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("dobDay"));
    assertEquals("required.dob-day", errors.getFieldError("dobDay").getCode());
    assertNotNull(errors.getFieldError("dobMonth"));
    assertEquals("required.dob-month", errors.getFieldError("dobMonth").getCode());
    assertNotNull(errors.getFieldError("dobYear"));
    assertEquals("required.dob-year", errors.getFieldError("dobYear").getCode());
  }

  @ParameterizedTest
  @CsvSource({
      "abc, 12, 1990, dobDay",
      "1, ab, 1990, dobMonth",
      "1, 12, abcd, dobYear"
  })
  public void testValidateDateOfBirth_InvalidNumeric(String dobDay, String dobMonth, String dobYear,
                                                     String field) {
    clientSearchCriteria.setDobDay(dobDay);
    clientSearchCriteria.setDobMonth(dobMonth);
    clientSearchCriteria.setDobYear(dobYear);
    validator.validateDateOfBirth(clientSearchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError(field));
    assertEquals("invalid.numeric", errors.getFieldError(field).getCode());
  }

  @ParameterizedTest
  @CsvSource({"1, AB123456C",
      "2, TEST",
      "3, TEST"})
  public void testValidateUniqueIdentifierType_Valid(Integer uniqueIdentifierType,
                                                     String uniqueIdentifierValue) {
    clientSearchCriteria.setUniqueIdentifierType(uniqueIdentifierType);
    clientSearchCriteria.setUniqueIdentifierValue(uniqueIdentifierValue);
    validator.validateUniqueIdentifierType(clientSearchCriteria, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  public void testValidateUniqueIdentifierType_InvalidNationalInsuranceNumber() {
    clientSearchCriteria.setUniqueIdentifierType(UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER);
    clientSearchCriteria.setUniqueIdentifierValue("ABC123");
    validator.validateUniqueIdentifierType(clientSearchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("uniqueIdentifierValue"));
    assertEquals("invalid.uniqueIdentifierValue",
        errors.getFieldError("uniqueIdentifierValue").getCode());
  }

  @ParameterizedTest
  @CsvSource({"----"})
  public void testValidateUniqueIdentifierType_InvalidHomeOfficeReference(
      String uniqueIdentifierValue) {
    clientSearchCriteria.setUniqueIdentifierType(UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE);
    clientSearchCriteria.setUniqueIdentifierValue(uniqueIdentifierValue);
    validator.validateUniqueIdentifierType(clientSearchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("uniqueIdentifierValue"));
    assertEquals("invalid.uniqueIdentifierValue",
        errors.getFieldError("uniqueIdentifierValue").getCode());
  }

  @ParameterizedTest
  @CsvSource({"TEST  TEST",
      "----"})
  public void testValidateUniqueIdentifierType_InvalidCaseReferenceNumber(
      String uniqueIdentifierValue) {
    clientSearchCriteria.setUniqueIdentifierType(UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER);
    clientSearchCriteria.setUniqueIdentifierValue(uniqueIdentifierValue);
    validator.validateUniqueIdentifierType(clientSearchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("uniqueIdentifierValue"));
    assertEquals("invalid.uniqueIdentifierValue",
        errors.getFieldError("uniqueIdentifierValue").getCode());
  }

  @Test
  public void testValidate_Valid() {
    clientSearchCriteria.setForename("John");
    clientSearchCriteria.setSurname("Doe");
    clientSearchCriteria.setDobDay("01");
    clientSearchCriteria.setDobMonth("12");
    clientSearchCriteria.setDobYear("1990");
    clientSearchCriteria.setUniqueIdentifierType(1);
    clientSearchCriteria.setUniqueIdentifierValue("AB123456C");
    validator.validate(clientSearchCriteria, errors);
    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @CsvSource({
      "'','','','','', 5",
      "'',Doe,'',12,1990,2",
      "John,'','',12,1990,2",
      "John,Doe,'','',1990,2",
      "John,Doe,1,'',1990,1",
      "John,Doe,1,12,'',1",
  })
  public void testValidate_Invalid(String forename, String surname, String dobDay,
                                   String dobMonth, String dobYear, int expectedErrorCount) {
    clientSearchCriteria.setForename(forename);
    clientSearchCriteria.setSurname(surname);
    clientSearchCriteria.setDobDay(dobDay);
    clientSearchCriteria.setDobMonth(dobMonth);
    clientSearchCriteria.setDobYear(dobYear);

    validator.validate(clientSearchCriteria, errors);

    assertTrue(errors.hasErrors());
    assertEquals(expectedErrorCount, errors.getErrorCount());
  }
}
