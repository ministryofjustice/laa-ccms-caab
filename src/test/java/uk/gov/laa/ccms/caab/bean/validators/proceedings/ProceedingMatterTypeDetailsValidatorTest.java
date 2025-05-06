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
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataMatterTypeDetails;

@ExtendWith(SpringExtension.class)
class ProceedingMatterTypeDetailsValidatorTest {

  @InjectMocks
  private ProceedingMatterTypeDetailsValidator proceedingMatterTypeDetailsValidator;

  private ProceedingFormDataMatterTypeDetails matterTypeDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    matterTypeDetails = new ProceedingFormDataMatterTypeDetails(); // Assuming that the default constructor sets all fields to null.
    errors = new BeanPropertyBindingResult(matterTypeDetails, "matterTypeDetails");
  }

  @Test
  public void supports_ReturnsTrueForProceedingFormDataMatterTypeDetailsClass() {
    assertTrue(proceedingMatterTypeDetailsValidator.supports(ProceedingFormDataMatterTypeDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(proceedingMatterTypeDetailsValidator.supports(Object.class));
  }

  @Test
  public void validate_WithNullMatterType_HasErrors() {
    matterTypeDetails.setMatterType(null);
    proceedingMatterTypeDetailsValidator.validate(matterTypeDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("matterType"));
    assertEquals("required.matterType", errors.getFieldError("matterType").getCode());
  }

  @Test
  public void validate_WithValidMatterType_NoErrors() {
    matterTypeDetails.setMatterType("Valid Matter Type");
    proceedingMatterTypeDetailsValidator.validate(matterTypeDetails, errors);
    assertFalse(errors.hasErrors());
  }

}
