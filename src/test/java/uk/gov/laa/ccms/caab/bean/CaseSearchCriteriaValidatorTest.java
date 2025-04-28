package uk.gov.laa.ccms.caab.bean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.validators.application.CaseSearchCriteriaValidator;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class CaseSearchCriteriaValidatorTest {

  @InjectMocks
  private CaseSearchCriteriaValidator validator;

  private CaseSearchCriteria searchCriteria;

  private Errors errors;

  @BeforeEach
  void setUp() {
    searchCriteria =
        new CaseSearchCriteria(); // Assuming that the default constructor sets all fields to null.
    errors = new BeanPropertyBindingResult(searchCriteria, "applicationSearchCriteria");
  }

  @Test
  void supports_ReturnsTrueForSupportedClass() {
    assertTrue(validator.supports(CaseSearchCriteria.class));
  }

  @Test
  void supports_ReturnsFalseForOtherClasses() {
    assertFalse(validator.supports(Object.class));
  }

  @Test
  void validate_validatesNoSearchCriteria() {
    validator.validate(searchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getGlobalErrorCount());
    assertEquals("required.atLeastOneSearchCriteria", errors.getGlobalErrors().getFirst().getCode());
  }

  @Test
  void validate_validatesSearchCriteriaCaseRef() {
    searchCriteria.setCaseReference("123");
    validator.validate(searchCriteria, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  void validate_validatesSearchCriteriaSurname() {
    searchCriteria.setClientSurname("surname");
    validator.validate(searchCriteria, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  void validate_validatesSearchCriteriaOffice() {
    searchCriteria.setOfficeId(1);
    validator.validate(searchCriteria, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  void validate_validatesSearchCriteriaProviderRef() {
    searchCriteria.setProviderCaseReference("provref");
    validator.validate(searchCriteria, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  void validate_validatesSearchCriteriaFeeEarner() {
    searchCriteria.setFeeEarnerId(123);
    validator.validate(searchCriteria, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  void caseRefDoubleSpaceValidation() {
    // Requires some text, otherwise value is disregarded for being blank
    searchCriteria.setCaseReference("1  3");
    validator.validate(searchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void caseRefAlphaNumericValidation() {
    searchCriteria.setCaseReference("$%^");
    validator.validate(searchCriteria,errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validCaseRef() {
    searchCriteria.setCaseReference("30000aaa");
    validator.validate(searchCriteria, errors);
    assertFalse(errors.hasErrors());
    assertEquals(0, errors.getErrorCount());
  }

  @Test
  void validateInvalidClientSurnameWithDoubleSpace() {
    searchCriteria.setClientSurname("a  b");
    validator.validate(searchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validateInvalidateClientSurnameFirstCharacterAlpha() {
    searchCriteria.setClientSurname("1A ");
    validator.validate(searchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validateInvalidateClientSurnameCharacterSetC() {
    searchCriteria.setClientSurname("A1 ");
    validator.validate(searchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validateValidClientSurname() {
    searchCriteria.setClientSurname("Humphreysismostvalid");
    validator.validate(searchCriteria, errors);
    assertFalse(errors.hasErrors());
    assertEquals(0, errors.getErrorCount());
  }

  @Test
  void validateValidProviderCaseRef() {
    searchCriteria.setProviderCaseReference("validProviderCaseRef");
    validator.validate(searchCriteria, errors);
    assertFalse(errors.hasErrors());
    assertEquals(0, errors.getErrorCount());
  }

  @Test
  void providerCaseRefDoubleSpaceValidation() {
    // Requires some text, otherwise value is disregarded for being blank
    searchCriteria.setProviderCaseReference("1  3");
    validator.validate(searchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }
}
