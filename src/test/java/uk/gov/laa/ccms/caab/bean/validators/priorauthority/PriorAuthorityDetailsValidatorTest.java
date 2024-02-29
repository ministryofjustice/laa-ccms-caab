package uk.gov.laa.ccms.caab.bean.validators.priorauthority;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityFormDataDetails;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityFormDataDynamicOption;

@ExtendWith(SpringExtension.class)
class PriorAuthorityDetailsValidatorTest {

  @InjectMocks
  private PriorAuthorityDetailsValidator priorAuthorityDetailsValidator;

  private PriorAuthorityFormDataDetails priorAuthorityFormDataDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    priorAuthorityFormDataDetails = new PriorAuthorityFormDataDetails();
    errors = new BeanPropertyBindingResult(priorAuthorityFormDataDetails, "priorAuthorityFormDataDetails");
  }

  @Test
  public void supports_ReturnsTrueForPriorAuthorityFormDataDetailsClass() {
    assertTrue(priorAuthorityDetailsValidator.supports(PriorAuthorityFormDataDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(priorAuthorityDetailsValidator.supports(Object.class));
  }

  @Test
  public void validate_WithMissingFields_HasErrors() {
    priorAuthorityFormDataDetails.setSummary(null);
    priorAuthorityFormDataDetails.setJustification(null);
    priorAuthorityFormDataDetails.setValueRequired(true);
    priorAuthorityFormDataDetails.setAmountRequested(null);

    priorAuthorityDetailsValidator.validate(priorAuthorityFormDataDetails, errors);

    assertTrue(errors.hasFieldErrors("summary"));
    assertTrue(errors.hasFieldErrors("justification"));
    assertTrue(errors.hasFieldErrors("amountRequested"));
  }

  @Test
  public void validate_WithInvalidCurrency_HasErrors() {
    priorAuthorityFormDataDetails.setValueRequired(true);
    priorAuthorityFormDataDetails.setAmountRequested("invalid");

    priorAuthorityDetailsValidator.validate(priorAuthorityFormDataDetails, errors);

    assertTrue(errors.hasFieldErrors("amountRequested"));
  }

  @Test
  public void validate_WithValidDetails_NoErrors() {
    priorAuthorityFormDataDetails.setSummary("Valid summary");
    priorAuthorityFormDataDetails.setJustification("Valid justification");
    priorAuthorityFormDataDetails.setValueRequired(true);
    priorAuthorityFormDataDetails.setAmountRequested("100.00");

    priorAuthorityDetailsValidator.validate(priorAuthorityFormDataDetails, errors);

    assertFalse(errors.hasErrors());
  }

  @Test
  public void validate_DynamicOptionsValidation_AMT() {
    final Map<String, PriorAuthorityFormDataDynamicOption> dynamicOptions = new HashMap<>();
    final PriorAuthorityFormDataDynamicOption option = new PriorAuthorityFormDataDynamicOption();
    option.setMandatory(true);
    option.setFieldType("AMT");
    option.setFieldValue("invalid");
    dynamicOptions.put("amtKey", option);
    priorAuthorityFormDataDetails.setDynamicOptions(dynamicOptions);

    priorAuthorityDetailsValidator.validate(priorAuthorityFormDataDetails, errors);

    assertTrue(errors.hasErrors());
  }

  @Test
  public void validate_DynamicOptionsValidation_INT() {
    final Map<String, PriorAuthorityFormDataDynamicOption> dynamicOptions = new HashMap<>();
    final PriorAuthorityFormDataDynamicOption option = new PriorAuthorityFormDataDynamicOption();
    option.setMandatory(true);
    option.setFieldType("INT");
    option.setFieldValue("invalid number");
    dynamicOptions.put("intKey", option);
    priorAuthorityFormDataDetails.setDynamicOptions(dynamicOptions);

    priorAuthorityDetailsValidator.validate(priorAuthorityFormDataDetails, errors);

    assertTrue(errors.hasFieldErrors("dynamicOptions[intKey].fieldValue"));
  }
}
