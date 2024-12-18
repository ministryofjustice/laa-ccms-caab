package uk.gov.laa.ccms.caab.bean.validators.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.common.DynamicOptionFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestDetailsFormData;

@ExtendWith(SpringExtension.class)
class ProviderRequestDetailsValidatorTest {

  private ProviderRequestDetailsValidator providerRequestDetailsValidator;

  private ProviderRequestDetailsFormData formData;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    providerRequestDetailsValidator = new ProviderRequestDetailsValidator(
        List.of("pdf", "jpg", "png"),
        "5MB"
    );
    formData = new ProviderRequestDetailsFormData();
    errors = new BeanPropertyBindingResult(formData, "providerRequestDetailsFormData");
  }

  @Test
  @DisplayName("supports - Returns true for ProviderRequestDetailsFormData class")
  public void supports_ReturnsTrueForProviderRequestDetailsFormDataClass() {
    assertTrue(providerRequestDetailsValidator.supports(ProviderRequestDetailsFormData.class));
  }

  @Test
  @DisplayName("supports - Returns false for other classes")
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(providerRequestDetailsValidator.supports(Object.class));
  }

  @Test
  @DisplayName("validate - Adds error for invalid file when claim upload is enabled")
  public void validate_InvalidFile_HasErrors() {
    final MockMultipartFile invalidFile = new MockMultipartFile("file", "invalid.exe", "application/octet-stream", new byte[0]);
    formData.setFile(invalidFile);
    formData.setFileExtension("exe");
    formData.setClaimUploadEnabled(true);

    providerRequestDetailsValidator.validate(formData, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("file"));
  }

  @Test
  @DisplayName("validate - No errors for valid file when claim upload is enabled")
  public void validate_ValidFile_NoErrors() {
    final MockMultipartFile validFile = new MockMultipartFile("file", "valid.pdf", "application/pdf", new byte[3000000]);
    formData.setFile(validFile);
    formData.setFileExtension("pdf");
    formData.setClaimUploadEnabled(true);
    formData.setAdditionalInformation("");

    providerRequestDetailsValidator.validate(formData, errors);

    assertFalse(errors.hasErrors());
  }

  @Test
  @DisplayName("validate - Adds error for missing mandatory dynamic option field")
  public void validate_MissingMandatoryDynamicOptionField_HasErrors() {
    final DynamicOptionFormData mandatoryOption = new DynamicOptionFormData();
    mandatoryOption.setMandatory(true);
    mandatoryOption.setFieldDescription("Mandatory Field");

    final Map<String, DynamicOptionFormData> dynamicOptions = new HashMap<>();
    dynamicOptions.put("option1", mandatoryOption);
    formData.setDynamicOptions(dynamicOptions);

    providerRequestDetailsValidator.validate(formData, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("dynamicOptions[option1].fieldValue"));
  }

  @Test
  @DisplayName("validate - No errors for valid dynamic options")
  public void validate_ValidDynamicOptions_NoErrors() {
    final DynamicOptionFormData validOption = new DynamicOptionFormData();
    validOption.setMandatory(true);
    validOption.setFieldValue("100");
    validOption.setFieldDescription("Amount Field");
    validOption.setFieldType("AMT");

    final Map<String, DynamicOptionFormData> dynamicOptions = new HashMap<>();
    dynamicOptions.put("option1", validOption);
    formData.setDynamicOptions(dynamicOptions);
    formData.setAdditionalInformation("");
    providerRequestDetailsValidator.validate(formData, errors);

    assertFalse(errors.hasErrors());
  }

  @Test
  @DisplayName("validate - Adds error for numeric field exceeding limit")
  public void validate_NumericFieldExceedsLimit_HasErrors() {
    final DynamicOptionFormData option = new DynamicOptionFormData();
    option.setMandatory(true);
    option.setFieldValue("1000000000.00"); // Exceeds MAX_COST_LIMIT
    option.setFieldDescription("Amount Field");
    option.setFieldType("AMT");

    final Map<String, DynamicOptionFormData> dynamicOptions = new HashMap<>();
    dynamicOptions.put("option1", option);
    formData.setDynamicOptions(dynamicOptions);

    providerRequestDetailsValidator.validate(formData, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("dynamicOptions[option1].fieldValue"));
  }

  @Test
  @DisplayName("validate - Adds error for invalid date format")
  public void validate_InvalidDateFormat_HasErrors() {
    final DynamicOptionFormData dateOption = new DynamicOptionFormData();
    dateOption.setMandatory(true);
    dateOption.setFieldValue("2024-12-12"); // Invalid format
    dateOption.setFieldDescription("Date Field");
    dateOption.setFieldType("DAT");

    final Map<String, DynamicOptionFormData> dynamicOptions = new HashMap<>();
    dynamicOptions.put("option1", dateOption);
    formData.setDynamicOptions(dynamicOptions);

    providerRequestDetailsValidator.validate(formData, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("dynamicOptions[option1].fieldValue"));
  }

  @Test
  @DisplayName("validate - Adds error when additional information exceeds max length")
  public void validate_AdditionalInformationExceedsMaxLength_HasErrors() {
    formData.setAdditionalInformation("A".repeat(9000)); // Exceeds max length of 8000

    providerRequestDetailsValidator.validate(formData, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("additionalInformation"));
  }

  @Test
  @DisplayName("validate - No errors for valid additional information")
  public void validate_ValidAdditionalInformation_NoErrors() {
    formData.setAdditionalInformation("Valid information within limit");

    providerRequestDetailsValidator.validate(formData, errors);

    assertFalse(errors.hasErrors());
  }
}