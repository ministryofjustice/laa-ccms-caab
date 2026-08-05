package uk.gov.laa.ccms.caab.bean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER;

import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientSearchCriteriaValidator;

@ExtendWith(MockitoExtension.class)
class ClientSearchCriteriaValidatorTest {

  @InjectMocks private ClientSearchCriteriaValidator validator;
  private ClientSearchCriteria clientSearchCriteria;
  private Errors errors;

  @BeforeEach
  void setUp() {
    clientSearchCriteria = new ClientSearchCriteria();
    errors = new BeanPropertyBindingResult(clientSearchCriteria, "clientSearchCriteria");
  }

  @Test
  void supports_ReturnsTrueForClientSearchDetailsClass() {
    assertTrue(validator.supports(ClientSearchCriteria.class));
  }

  @Test
  void testValidateForename_Valid() {
    clientSearchCriteria.setForename("John");
    validator.validateForename(clientSearchCriteria, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  void testValidateForename_Invalid() {
    validator.validateForename(clientSearchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("forename"));
    assertEquals(
        "required.forename", Objects.requireNonNull(errors.getFieldError("forename")).getCode());
  }

  @Test
  void validateInvalidForenameWithDoubleSpace() {
    clientSearchCriteria.setForename("a  b");
    validator.validateForename(clientSearchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validateInvalidForenameFirstCharacterAlpha() {
    clientSearchCriteria.setForename("1A ");
    validator.validateForename(clientSearchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validateInvalidForenameCharacterSetC() {
    clientSearchCriteria.setForename("A1 ");
    validator.validateForename(clientSearchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void testValidateSurnameAtBirth_Valid() {
    clientSearchCriteria.setSurname("Doe");
    validator.validateSurnameAtBirth(clientSearchCriteria, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  void testValidateSurnameAtBirth_Invalid() {
    validator.validateSurnameAtBirth(clientSearchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("surname"));
    assertEquals(
        "required.surname", Objects.requireNonNull(errors.getFieldError("surname")).getCode());
  }

  @Test
  void validateInvalidSurnameAtBirthWithDoubleSpace() {
    clientSearchCriteria.setSurname("a  b");
    validator.validateSurnameAtBirth(clientSearchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validateInvalidSurnameAtBirthFirstCharacterAlpha() {
    clientSearchCriteria.setSurname("1A ");
    validator.validateSurnameAtBirth(clientSearchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validateInvalidSurnameAtBirthCharacterSetC() {
    clientSearchCriteria.setSurname("A1 ");
    validator.validateSurnameAtBirth(clientSearchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @CsvSource({
    "1/12/1990, '', ''",
    "01/01/2000, '', ''",
    "1/12/2099, invalid.input, You must provide a date in the past for the Date of birth field. Please amend your entry.",
    "'', required.dob, Please complete 'Date of birth'",
    "abc/12/1990, invalid.format, Your input for 'Date of birth' is in an incorrect format. Please amend your entry.",
    "1/ab/1990, invalid.format, Your input for 'Date of birth' is in an incorrect format. Please amend your entry.",
    "1/12/abcd, invalid.format, Your input for 'Date of birth' is in an incorrect format. Please amend your entry.",
    "1-12-2026, invalid.format, Your input for 'Date of birth' is invalid. Please enter the date in DD/MM/YYYY format.",
    "4/3/07, invalid.format, Your input for 'Date of birth' is invalid. Please enter the date in DD/MM/YYYY format.",
    "13/12/1901, invalid.input, You must provide a date after 13 December 1901 for the Date of birth field. Please amend your entry."
  })
  void testValidateDateOfBirth_VariousValues(
      String dateOfBirth, String expectedErrorCode, String defaultErrorMessage) {
    clientSearchCriteria.setDateOfBirth(dateOfBirth);

    validator.validateDateOfBirth(clientSearchCriteria, errors, true);

    if (expectedErrorCode.isBlank()) {
      assertFalse(errors.hasErrors());
      assertNull(errors.getFieldError("dateOfBirth"));
    } else {
      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("dateOfBirth"));
      assertEquals(
          expectedErrorCode, Objects.requireNonNull(errors.getFieldError("dateOfBirth")).getCode());
      assertEquals(
          defaultErrorMessage,
          Objects.requireNonNull(errors.getFieldError("dateOfBirth")).getDefaultMessage());
    }
  }

  @ParameterizedTest
  @CsvSource({"1, AB123456C", "2, TEST", "3, TEST"})
  void testValidateUniqueIdentifierType_Valid(
      Integer uniqueIdentifierType, String uniqueIdentifierValue) {
    clientSearchCriteria.setUniqueIdentifierType(uniqueIdentifierType);
    clientSearchCriteria.setUniqueIdentifierValue(uniqueIdentifierValue);
    validator.validateUniqueIdentifierType(clientSearchCriteria, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  void testValidateUniqueIdentifierType_InvalidNationalInsuranceNumber() {
    clientSearchCriteria.setUniqueIdentifierType(UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER);
    clientSearchCriteria.setUniqueIdentifierValue("ABC123");
    validator.validateUniqueIdentifierType(clientSearchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("uniqueIdentifierValue"));
    assertEquals(
        "invalid.uniqueIdentifierValue",
        Objects.requireNonNull(errors.getFieldError("uniqueIdentifierValue")).getCode());
  }

  @ParameterizedTest
  @CsvSource({"----"})
  void testValidateUniqueIdentifierType_InvalidHomeOfficeReference(String uniqueIdentifierValue) {
    clientSearchCriteria.setUniqueIdentifierType(UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE);
    clientSearchCriteria.setUniqueIdentifierValue(uniqueIdentifierValue);
    validator.validateUniqueIdentifierType(clientSearchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("uniqueIdentifierValue"));
    assertEquals(
        "invalid.uniqueIdentifierValue",
        Objects.requireNonNull(errors.getFieldError("uniqueIdentifierValue")).getCode());
  }

  @ParameterizedTest
  @CsvSource({"TEST  TEST", "----"})
  void testValidateUniqueIdentifierType_InvalidCaseReferenceNumber(String uniqueIdentifierValue) {
    clientSearchCriteria.setUniqueIdentifierType(UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER);
    clientSearchCriteria.setUniqueIdentifierValue(uniqueIdentifierValue);
    validator.validateUniqueIdentifierType(clientSearchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("uniqueIdentifierValue"));
    assertEquals(
        "invalid.uniqueIdentifierValue",
        Objects.requireNonNull(errors.getFieldError("uniqueIdentifierValue")).getCode());
  }

  @Test
  void testValidate_Valid() {
    clientSearchCriteria.setForename("John");
    clientSearchCriteria.setSurname("Doe");
    clientSearchCriteria.setDateOfBirth("01/12/1990");
    clientSearchCriteria.setUniqueIdentifierType(1);
    clientSearchCriteria.setUniqueIdentifierValue("AB123456C");
    validator.validate(clientSearchCriteria, errors);
    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @CsvSource({
    "'','','', 3",
    "'',Doe,12/1990,2",
    "John,'',12/1990,2",
    "John,Doe,1990,1",
    "John,Doe,1/1990,1",
    "John,Doe,1/12/,1",
  })
  void testValidate_Invalid(
      String forename, String surname, String dobDay, int expectedErrorCount) {
    clientSearchCriteria.setForename(forename);
    clientSearchCriteria.setSurname(surname);
    clientSearchCriteria.setDateOfBirth(dobDay);

    validator.validate(clientSearchCriteria, errors);

    assertTrue(errors.hasErrors());
    assertEquals(expectedErrorCount, errors.getErrorCount());
  }
}
