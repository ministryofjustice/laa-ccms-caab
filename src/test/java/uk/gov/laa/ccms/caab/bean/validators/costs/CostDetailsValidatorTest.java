package uk.gov.laa.ccms.caab.bean.validators.costs;

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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.costs.CostsFormData;

@DisplayName("Cost details validator test")
@ExtendWith(SpringExtension.class)
class CostDetailsValidatorTest {

  @InjectMocks
  private CostDetailsValidator costDetailsValidator;

  private CostsFormData costsFormData;
  private Errors errors;

  @BeforeEach
  void setUp() {
    costsFormData = new CostsFormData(new BigDecimal("20000.00"));
    errors = new BeanPropertyBindingResult(costsFormData, "costsFormData");
  }

  @Nested
  @DisplayName("supports() tests")
  class SupportsTests {

    @Test
    @DisplayName("Should return true when expected class type")
    void supports_ReturnsTrueForCostsFormDataClass() {
      assertTrue(costDetailsValidator.supports(CostsFormData.class));
    }

    @Test
    @DisplayName("Should return false for other classes")
    void supports_ReturnsFalseForOtherClasses() {
      assertFalse(costDetailsValidator.supports(Object.class));
    }

  }

  @Nested
  @DisplayName("validate() tests")
  class ValidateTests {

    @Test
    @DisplayName("Should have errors when not a number")
    void validate_WithInvalidCurrencyFormat_HasErrors() {
      costsFormData.setRequestedCostLimitation("not a number");
      costDetailsValidator.validate(costsFormData, errors);
      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("requestedCostLimitation"));
      assertEquals("invalid.currency", errors.getFieldError("requestedCostLimitation").getCode());
    }

    @Test
    @DisplayName("Should have errors when exceeding cost limitation")
    void validate_WithExceedingCostLimitation_HasErrors() {
      costsFormData.setRequestedCostLimitation("100000001.00");
      costDetailsValidator.validate(costsFormData, errors);
      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("requestedCostLimitation"));
      assertEquals("value.exceeds.max", errors.getFieldError("requestedCostLimitation").getCode());
    }

    @Test
    @DisplayName("Should not have errors")
    void validate_WithValidAndWithinLimitCostLimitation_NoErrors() {
      costsFormData.setRequestedCostLimitation("99999999.99");
      costDetailsValidator.validate(costsFormData, errors);
      assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Should have errors when below granted amount")
    void shouldHaveErrorsWhenBelowGrantedAmount(){
      costsFormData.setRequestedCostLimitation("100.00");
      costDetailsValidator.validate(costsFormData, errors);
      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("requestedCostLimitation"));
      assertEquals("caseCostLimitation.requestedAmount.belowGrantedAmount", errors.getFieldError("requestedCostLimitation").getCode());
    }
  }
}
