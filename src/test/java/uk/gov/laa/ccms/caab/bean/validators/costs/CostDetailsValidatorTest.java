package uk.gov.laa.ccms.caab.bean.validators.costs;

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
import uk.gov.laa.ccms.caab.bean.costs.CostsFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityFormDataTypeDetails;

@ExtendWith(SpringExtension.class)
class CostDetailsValidatorTest {

  @InjectMocks
  private CostDetailsValidator costDetailsValidator;

  private CostsFormData costsFormData;
  private Errors errors;

  @BeforeEach
  public void setUp() {
    costsFormData = new CostsFormData();
    errors = new BeanPropertyBindingResult(costsFormData, "costsFormData");
  }

  @Test
  public void supports_ReturnsTrueForCostsFormDataClass() {
    assertTrue(costDetailsValidator.supports(CostsFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(costDetailsValidator.supports(Object.class));
  }

  @Test
  public void validate_WithInvalidCurrencyFormat_HasErrors() {
    costsFormData.setRequestedCostLimitation("not a number");
    costDetailsValidator.validate(costsFormData, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("requestedCostLimitation"));
    assertEquals("invalid.currency", errors.getFieldError("requestedCostLimitation").getCode());
  }

  @Test
  public void validate_WithExceedingCostLimitation_HasErrors() {
    costsFormData.setRequestedCostLimitation("100000001.00");
    costDetailsValidator.validate(costsFormData, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("requestedCostLimitation"));
    assertEquals("value.exceeds.max", errors.getFieldError("requestedCostLimitation").getCode());
  }

  @Test
  public void validate_WithValidAndWithinLimitCostLimitation_NoErrors() {
    costsFormData.setRequestedCostLimitation("99999999.99");
    costDetailsValidator.validate(costsFormData, errors);
    assertFalse(errors.hasErrors());
  }
}
