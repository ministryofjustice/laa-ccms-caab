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
import uk.gov.laa.ccms.caab.bean.validators.client.ClientSearchCriteriaValidator;

@ExtendWith(SpringExtension.class)
class ClientSearchCriteriaValidatorTest {

  @InjectMocks
  private ClientSearchCriteriaValidator validator;
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
    assertEquals("required.forename", errors.getFieldError("forename").getCode());
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
    assertEquals("required.surname", errors.getFieldError("surname").getCode());
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

  @Test
  void testValidateDateOfBirth_Valid() {
    clientSearchCriteria.setDateOfBirth("01/12/1990");
    validator.validateDateOfBirth(clientSearchCriteria, errors, true);
    assertFalse(errors.hasErrors());
  }

  @Test
  void testValidateDateOfBirth_Invalid() {
    clientSearchCriteria.setDateOfBirth("");
    validator.validateDateOfBirth(clientSearchCriteria, errors, true);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("dateOfBirth"));
    assertEquals("required.dob", errors.getFieldError("dateOfBirth").getCode());
  }

  @ParameterizedTest
  @CsvSource({
      "abc/12/1990, dateOfBirth",
      "1/ab/1990, dateOfBirth",
      "1/12/abcd, dateOfBirth"
  })
  void testValidateDateOfBirth_InvalidNumeric(String dobDay, String field) {
    clientSearchCriteria.setDateOfBirth(dobDay);

    validator.validateDateOfBirth(clientSearchCriteria, errors, true);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError(field));
    assertEquals("invalid.format", errors.getFieldError(field).getCode());
  }

  @ParameterizedTest
  @CsvSource({"1, AB123456C",
      "2, TEST",
      "3, TEST"})
  void testValidateUniqueIdentifierType_Valid(Integer uniqueIdentifierType,
                                              String uniqueIdentifierValue) {
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
    assertEquals("invalid.uniqueIdentifierValue",
        errors.getFieldError("uniqueIdentifierValue").getCode());
  }

  @ParameterizedTest
  @CsvSource({"----"})
  void testValidateUniqueIdentifierType_InvalidHomeOfficeReference(
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
  void testValidateUniqueIdentifierType_InvalidCaseReferenceNumber(
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
  void testValidate_Invalid(String forename, String surname, String dobDay,
                            int expectedErrorCount) {
    clientSearchCriteria.setForename(forename);
    clientSearchCriteria.setSurname(surname);
    clientSearchCriteria.setDateOfBirth(dobDay);

    validator.validate(clientSearchCriteria, errors);

    assertTrue(errors.hasErrors());
    assertEquals(expectedErrorCount, errors.getErrorCount());
  }
}
