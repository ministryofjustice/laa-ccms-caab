package uk.gov.laa.ccms.caab.bean.validators.proceedings;

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
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataProceedingDetails;

@ExtendWith(SpringExtension.class)
class ProceedingTypeDetailsValidatorTest {

  @InjectMocks
  private ProceedingTypeDetailsValidator proceedingTypeDetailsValidator;

  private ProceedingFormDataProceedingDetails proceedingDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    proceedingDetails = new ProceedingFormDataProceedingDetails();
    errors = new BeanPropertyBindingResult(proceedingDetails, "proceedingDetails");
  }

  @Test
  public void supports_ReturnsTrueForProceedingFormDataProceedingDetailsClass() {
    assertTrue(proceedingTypeDetailsValidator.supports(ProceedingFormDataProceedingDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(proceedingTypeDetailsValidator.supports(Object.class));
  }

  @Test
  public void validate_WithNullProceedingType_HasErrors() {
    proceedingDetails.setProceedingType(null);
    proceedingTypeDetailsValidator.validate(proceedingDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("proceedingType"));
    assertEquals("required.proceedingType", errors.getFieldError("proceedingType").getCode());
  }

  @Test
  public void validate_WithValidProceedingType_NoErrors() {
    proceedingDetails.setProceedingType("Valid Proceeding Type");
    proceedingTypeDetailsValidator.validate(proceedingDetails, errors);
    assertFalse(errors.hasErrors());
  }

}