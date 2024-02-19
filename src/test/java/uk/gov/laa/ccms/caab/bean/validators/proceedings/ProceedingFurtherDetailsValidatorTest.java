package uk.gov.laa.ccms.caab.bean.validators.proceedings;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFlowFormData;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataFurtherDetails;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataProceedingDetails;

@ExtendWith(SpringExtension.class)
class ProceedingFurtherDetailsValidatorTest {

  @InjectMocks
  private ProceedingFurtherDetailsValidator proceedingFurtherDetailsValidator;

  private ProceedingFlowFormData proceedingFlowFormData;

  private ProceedingFormDataFurtherDetails furtherDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    proceedingFlowFormData = new ProceedingFlowFormData("test");
    furtherDetails = new ProceedingFormDataFurtherDetails();
    errors = new BeanPropertyBindingResult(furtherDetails, "furtherDetails");
  }

  @Test
  public void supports_ReturnsTrueForProceedingFlowFormDataClass() {
    assertTrue(proceedingFurtherDetailsValidator.supports(ProceedingFlowFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(proceedingFurtherDetailsValidator.supports(Object.class));
  }

  @Test
  public void validate_WithNullFields_HasErrors() {
    proceedingFurtherDetailsValidator.validate(proceedingFlowFormData, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("clientInvolvementType"));
    assertEquals("required.clientInvolvementType", errors.getFieldError("clientInvolvementType").getCode());
    assertNotNull(errors.getFieldError("levelOfService"));
    assertEquals("required.levelOfService", errors.getFieldError("levelOfService").getCode());
  }

  @Test
  public void validate_WithValidFields_NoErrors() {
    furtherDetails.setClientInvolvementType("Valid Client Involvement Type");
    furtherDetails.setLevelOfService("Valid Level Of Service");
    proceedingFlowFormData.setFurtherDetails(furtherDetails);
    proceedingFurtherDetailsValidator.validate(proceedingFlowFormData, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  public void validate_WithValidFields_NoErrors_withTypeOfOrder() {
    final ProceedingFormDataProceedingDetails proceedingDetails = new ProceedingFormDataProceedingDetails();
    proceedingDetails.setOrderTypeRequired(true);
    furtherDetails.setClientInvolvementType("Valid Client Involvement Type");
    furtherDetails.setLevelOfService("Valid Level Of Service");
    furtherDetails.setTypeOfOrder("Valid Type Of Order");
    proceedingFlowFormData.setFurtherDetails(furtherDetails);
    proceedingFlowFormData.setProceedingDetails(proceedingDetails);
    proceedingFurtherDetailsValidator.validate(proceedingFlowFormData, errors);
    assertFalse(errors.hasErrors());
  }

}