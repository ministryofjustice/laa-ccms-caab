package uk.gov.laa.ccms.caab.bean.validators.priorauthority;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.common.DynamicOptionFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityDetailsFormData;

@ExtendWith(SpringExtension.class)
class PriorAuthorityDetailsValidatorTest {

  @InjectMocks private PriorAuthorityDetailsValidator priorAuthorityDetailsValidator;

  private PriorAuthorityDetailsFormData priorAuthorityDetailsFormData;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    priorAuthorityDetailsFormData = new PriorAuthorityDetailsFormData();
    errors =
        new BeanPropertyBindingResult(
            priorAuthorityDetailsFormData, "priorAuthorityFormDataDetails");
  }

  @Test
  public void supports_ReturnsTrueForPriorAuthorityFormDataDetailsClass() {
    assertTrue(priorAuthorityDetailsValidator.supports(PriorAuthorityDetailsFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(priorAuthorityDetailsValidator.supports(Object.class));
  }

  @Test
  public void validate_WithMissingFields_HasErrors() {
    priorAuthorityDetailsFormData.setSummary(null);
    priorAuthorityDetailsFormData.setJustification(null);
    priorAuthorityDetailsFormData.setValueRequired(true);
    priorAuthorityDetailsFormData.setAmountRequested(null);

    priorAuthorityDetailsValidator.validate(priorAuthorityDetailsFormData, errors);

    assertTrue(errors.hasFieldErrors("summary"));
    assertTrue(errors.hasFieldErrors("justification"));
    assertTrue(errors.hasFieldErrors("amountRequested"));
  }

  @Test
  public void validate_WithInvalidCurrency_HasErrors() {
    priorAuthorityDetailsFormData.setValueRequired(true);
    priorAuthorityDetailsFormData.setAmountRequested("invalid");

    priorAuthorityDetailsValidator.validate(priorAuthorityDetailsFormData, errors);

    assertTrue(errors.hasFieldErrors("amountRequested"));
  }

  @Test
  public void validate_WithValidDetails_NoErrors() {
    priorAuthorityDetailsFormData.setSummary("Valid summary");
    priorAuthorityDetailsFormData.setJustification("Valid justification");
    priorAuthorityDetailsFormData.setValueRequired(true);
    priorAuthorityDetailsFormData.setAmountRequested("100.00");

    priorAuthorityDetailsValidator.validate(priorAuthorityDetailsFormData, errors);

    assertFalse(errors.hasErrors());
  }

  @Test
  public void validate_DynamicOptionsValidation_AMT() {
    final Map<String, DynamicOptionFormData> dynamicOptions = new HashMap<>();
    final DynamicOptionFormData option = new DynamicOptionFormData();
    option.setMandatory(true);
    option.setFieldType("AMT");
    option.setFieldValue("invalid");
    dynamicOptions.put("amtKey", option);
    priorAuthorityDetailsFormData.setDynamicOptions(dynamicOptions);

    priorAuthorityDetailsValidator.validate(priorAuthorityDetailsFormData, errors);

    assertTrue(errors.hasErrors());
  }

  @Test
  public void validate_DynamicOptionsValidation_INT() {
    final Map<String, DynamicOptionFormData> dynamicOptions = new HashMap<>();
    final DynamicOptionFormData option = new DynamicOptionFormData();
    option.setMandatory(true);
    option.setFieldType("INT");
    option.setFieldValue("invalid number");
    dynamicOptions.put("intKey", option);
    priorAuthorityDetailsFormData.setDynamicOptions(dynamicOptions);

    priorAuthorityDetailsValidator.validate(priorAuthorityDetailsFormData, errors);

    assertTrue(errors.hasFieldErrors("dynamicOptions[intKey].fieldValue"));
  }
}
