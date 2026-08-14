package uk.gov.laa.ccms.caab.bean.validators.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.billing.UndertakingFormData;

@DisplayName("Billing undertaking validator test")
@ExtendWith(MockitoExtension.class)
class BillingUndertakingValidatorTest {

  @InjectMocks private BillingUndertakingValidator billingUndertakingValidator;

  private UndertakingFormData undertakingFormData;
  private Errors errors;

  @BeforeEach
  void setUp() {
    undertakingFormData = new UndertakingFormData();
    errors = new BeanPropertyBindingResult(undertakingFormData, "undertakingFormData");
  }

  @Nested
  @DisplayName("supports() tests")
  class SupportsTests {

    @Test
    @DisplayName("Should return true when expected class type")
    void supports_ReturnsTrueForUndertakingFormDataClass() {
      assertTrue(billingUndertakingValidator.supports(UndertakingFormData.class));
    }

    @Test
    @DisplayName("Should return false for other classes")
    void supports_ReturnsFalseForOtherClasses() {
      assertFalse(billingUndertakingValidator.supports(Object.class));
    }
  }

  @Nested
  @DisplayName("validate() tests")
  class ValidateTests {

    @Test
    @DisplayName("Should have errors when undertaking amount is missing")
    void validate_WithMissingUndertakingAmount_HasErrors() {
      undertakingFormData.setAcceptedTerms(true);

      billingUndertakingValidator.validate(undertakingFormData, errors);

      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("undertakingAmount"));
      assertEquals(
          "required.undertakingAmount", errors.getFieldError("undertakingAmount").getCode());
    }

    @Test
    @DisplayName("Should have errors when accepted terms is false")
    void validate_WithAcceptedTermsFalse_HasErrors() {
      undertakingFormData.setUndertakingAmount("100.00");
      undertakingFormData.setAcceptedTerms(false);

      billingUndertakingValidator.validate(undertakingFormData, errors);

      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("acceptedTerms"));
      assertEquals(
          "billing.acceptedTerms.required", errors.getFieldError("acceptedTerms").getCode());
    }

    @Test
    @DisplayName("Should have errors when undertaking amount has invalid currency format")
    void validate_WithInvalidCurrencyFormat_HasErrors() {
      undertakingFormData.setUndertakingAmount("not a number");
      undertakingFormData.setAcceptedTerms(true);

      billingUndertakingValidator.validate(undertakingFormData, errors);

      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("undertakingAmount"));
      assertEquals("invalid.currency", errors.getFieldError("undertakingAmount").getCode());
    }

    @Test
    @DisplayName("Should not have errors when undertaking amount and accepted terms are valid")
    void validate_WithValidData_NoErrors() {
      undertakingFormData.setUndertakingAmount("1234.56");
      undertakingFormData.setAcceptedTerms(true);
      undertakingFormData.setUndertakingMinimumAmount(new BigDecimal("100.00"));
      undertakingFormData.setUndertakingMaximumAmount(new BigDecimal("2000.00"));

      billingUndertakingValidator.validate(undertakingFormData, errors);

      assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Should have error when undertaking amount is below minimum")
    void validate_WhenBelowMinimum_HasErrors() {
      undertakingFormData.setUndertakingAmount("99.99");
      undertakingFormData.setAcceptedTerms(true);
      undertakingFormData.setUndertakingMinimumAmount(new BigDecimal("100.00"));
      undertakingFormData.setUndertakingMaximumAmount(new BigDecimal("900.00"));

      billingUndertakingValidator.validate(undertakingFormData, errors);

      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("undertakingAmount"));
      assertEquals(
          "billing.undertakingAmount.outOfRange",
          errors.getFieldError("undertakingAmount").getCode());
    }

    @Test
    @DisplayName("Should have error when undertaking amount is above maximum")
    void validate_WhenAboveMaximum_HasErrors() {
      undertakingFormData.setUndertakingAmount("900.01");
      undertakingFormData.setAcceptedTerms(true);
      undertakingFormData.setUndertakingMinimumAmount(new BigDecimal("100.00"));
      undertakingFormData.setUndertakingMaximumAmount(new BigDecimal("900.00"));

      billingUndertakingValidator.validate(undertakingFormData, errors);

      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("undertakingAmount"));
      assertEquals(
          "billing.undertakingAmount.outOfRange",
          errors.getFieldError("undertakingAmount").getCode());
    }

    @Test
    @DisplayName("Should not have errors when undertaking amount is within range")
    void validate_WhenWithinRange_NoErrors() {
      undertakingFormData.setUndertakingAmount("500.00");
      undertakingFormData.setAcceptedTerms(true);
      undertakingFormData.setUndertakingMinimumAmount(new BigDecimal("100.00"));
      undertakingFormData.setUndertakingMaximumAmount(new BigDecimal("900.00"));

      billingUndertakingValidator.validate(undertakingFormData, errors);

      assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Should have error when maximum is zero even if undertaking amount is zero")
    void validate_WhenMaximumIsZero_HasErrors() {
      undertakingFormData.setUndertakingAmount("0");
      undertakingFormData.setAcceptedTerms(true);
      undertakingFormData.setUndertakingMinimumAmount(BigDecimal.ZERO);
      undertakingFormData.setUndertakingMaximumAmount(BigDecimal.ZERO);

      billingUndertakingValidator.validate(undertakingFormData, errors);

      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("undertakingAmount"));
      assertEquals(
          "billing.undertakingAmount.outOfRange",
          errors.getFieldError("undertakingAmount").getCode());
    }
  }
}
