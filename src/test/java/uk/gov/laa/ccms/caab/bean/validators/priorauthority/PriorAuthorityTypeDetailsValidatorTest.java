package uk.gov.laa.ccms.caab.bean.validators.priorauthority;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityTypeFormData;

@ExtendWith(SpringExtension.class)
class PriorAuthorityTypeDetailsValidatorTest {

  @InjectMocks
  private PriorAuthorityTypeDetailsValidator priorAuthorityTypeDetailsValidator;

  private PriorAuthorityTypeFormData priorAuthorityTypeDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    priorAuthorityTypeDetails = new PriorAuthorityTypeFormData();
    errors = new BeanPropertyBindingResult(priorAuthorityTypeDetails, "priorAuthorityTypeDetails");
  }

  @Test
  public void supports_ReturnsTrueForPriorAuthorityFormDataTypeDetailsClass() {
    assertTrue(priorAuthorityTypeDetailsValidator.supports(PriorAuthorityTypeFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(priorAuthorityTypeDetailsValidator.supports(Object.class));
  }

  @Test
  public void validate_WithNullPriorAuthorityType_HasErrors() {
    priorAuthorityTypeDetails.setPriorAuthorityType(null);
    priorAuthorityTypeDetailsValidator.validate(priorAuthorityTypeDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("priorAuthorityType"));
    assertEquals("required.priorAuthorityType", errors.getFieldError("priorAuthorityType").getCode());
  }

  @Test
  public void validate_WithValidPriorAuthorityType_NoErrors() {
    priorAuthorityTypeDetails.setPriorAuthorityType("Valid Prior Authority Type");
    priorAuthorityTypeDetailsValidator.validate(priorAuthorityTypeDetails, errors);
    assertFalse(errors.hasErrors());
  }
}