package uk.gov.laa.ccms.caab.bean.validators.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.PreCertificateAndLegalHelpCostsFormData;

@DisplayName("Pre-certificate and legal help costs validator test")
@ExtendWith(MockitoExtension.class)
class PreCertificateAndLegalHelpCostsValidatorTest {

  @InjectMocks private PreCertificateAndLegalHelpCostsValidator validator;

  private PreCertificateAndLegalHelpCostsFormData formData;
  private Errors errors;

  @BeforeEach
  void setUp() {
    formData = new PreCertificateAndLegalHelpCostsFormData();
    errors = new BeanPropertyBindingResult(formData, "preCertificateAndLegalHelpCosts");
  }

  @Nested
  @DisplayName("supports() tests")
  class SupportsTests {

    @Test
    @DisplayName("Should return true for expected class")
    void supportsExpectedClass() {
      assertTrue(validator.supports(PreCertificateAndLegalHelpCostsFormData.class));
    }

    @Test
    @DisplayName("Should return false for other classes")
    void supportsOtherClass() {
      assertFalse(validator.supports(Object.class));
    }
  }

  @Nested
  @DisplayName("validate() tests")
  class ValidateTests {

    @Test
    @DisplayName("Should allow empty values")
    void validateEmptyValuesHasNoErrors() {
      validator.validate(formData, errors);

      assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Should reject invalid pre-certificate costs")
    void validateInvalidPreCertificateCosts() {
      formData.setPreCertificateCosts("12a.50");

      validator.validate(formData, errors);

      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("preCertificateCosts"));
      assertEquals("invalid.currency", errors.getFieldError("preCertificateCosts").getCode());
    }

    @Test
    @DisplayName("Should reject invalid legal help costs")
    void validateInvalidLegalHelpCosts() {
      formData.setLegalHelpCosts("x");

      validator.validate(formData, errors);

      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("legalHelpCosts"));
      assertEquals("invalid.currency", errors.getFieldError("legalHelpCosts").getCode());
    }

    @Test
    @DisplayName("Should reject invalid office code")
    void validateInvalidOfficeCode() {
      formData.setOfficeCode("ABC123");

      validator.validate(formData, errors);

      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("officeCode"));
      assertEquals("invalid.format", errors.getFieldError("officeCode").getCode());
      assertEquals(
          "Your input for 'Office code' is in an incorrect format (NANNNA). Please amend your entry.",
          errors.getFieldError("officeCode").getDefaultMessage());
    }

    @Test
    @DisplayName("Should reject invalid unique file number")
    void validateInvalidUniqueFileNumber() {
      formData.setUniqueFileNumber("010124-001");

      validator.validate(formData, errors);

      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("uniqueFileNumber"));
      assertEquals("invalid.format", errors.getFieldError("uniqueFileNumber").getCode());
      assertEquals(
          "Your input for 'Unique file number' is in an incorrect format (DDMMYY/NNN). Please amend your entry.",
          errors.getFieldError("uniqueFileNumber").getDefaultMessage());
    }

    @Test
    @DisplayName("Should use generic format message when no specific format hint is supplied")
    void validateFieldFormatUsesGenericMessageWhenNoHintProvided() {
      var testValidator =
          new PreCertificateAndLegalHelpCostsValidator() {
            void runFieldValidation(Errors validationErrors) {
              validateFieldFormat(
                  "officeCode",
                  "ABC123",
                  "^[0-9][A-Za-z][0-9]{3}[A-Za-z]$",
                  "Office code",
                  validationErrors);
            }
          };

      testValidator.runFieldValidation(errors);

      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("officeCode"));
      assertEquals(
          "Your input for 'Office code' is in an incorrect format. Please amend your entry.",
          errors.getFieldError("officeCode").getDefaultMessage());
    }

    @Test
    @DisplayName("Should accept valid values")
    void validateValidValuesHasNoErrors() {
      formData.setPreCertificateCosts("12.50");
      formData.setLegalHelpCosts("7.25");
      formData.setOfficeCode("1A234B");
      formData.setUniqueFileNumber("010124/001");

      validator.validate(formData, errors);

      assertFalse(errors.hasErrors());
    }
  }
}
