package uk.gov.laa.ccms.caab.bean.validators.proceedings;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataProceedingDetails;

@ExtendWith(SpringExtension.class)
class ProceedingDetailsValidatorTest {

  @InjectMocks
  private ProceedingDetailsValidator proceedingDetailsValidator;

  private ProceedingFormDataProceedingDetails proceedingDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    proceedingDetails = new ProceedingFormDataProceedingDetails(); // Assuming that the default constructor sets all fields to null.
    errors = new BeanPropertyBindingResult(proceedingDetails, "proceedingDetails");
  }

  @Test
  public void supports_ReturnsTrueForProceedingFormDataProceedingDetailsClass() {
    assertTrue(proceedingDetailsValidator.supports(ProceedingFormDataProceedingDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(proceedingDetailsValidator.supports(Object.class));
  }

  @Test
  public void validate_WithNullProceedingType_HasErrors() {
    proceedingDetails.setProceedingType(null);
    proceedingDetailsValidator.validate(proceedingDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("proceedingType"));
    assertEquals("required.proceedingType", errors.getFieldError("proceedingType").getCode());
  }

  @Test
  public void validate_WithValidProceedingType_NoErrors() {
    proceedingDetails.setProceedingType("Valid Proceeding Type");
    proceedingDetailsValidator.validate(proceedingDetails, errors);
    assertFalse(errors.hasErrors());
  }

}