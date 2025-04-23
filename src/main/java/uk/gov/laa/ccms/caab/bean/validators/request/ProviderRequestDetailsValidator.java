package uk.gov.laa.ccms.caab.bean.validators.request;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.gov.laa.ccms.caab.bean.common.DynamicOptionFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestDetailsFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;
import uk.gov.laa.ccms.caab.bean.validators.file.FileUploadValidator;

/**
 * Validator component responsible for validating
 * {@link uk.gov.laa.ccms.caab.bean.request.ProviderRequestDetailsFormData} objects.
 */
@Component
@Slf4j
public class ProviderRequestDetailsValidator extends FileUploadValidator {

  private static final String FIELD_TYPE_AMT = "AMT";
  private static final String FIELD_TYPE_INT = "INT";
  private static final String FIELD_TYPE_DATE = "DAT";
  private static final String FIELD_TYPE_FTS = "FTS";
  private static final String FIELD_TYPE_FTL = "FTL";

  private static final String DATE_FORMAT = "dd/MM/yyyy";

  private static final BigDecimal MAX_COST_LIMIT = new BigDecimal("100000000.00");

  public ProviderRequestDetailsValidator(
      @Value("${laa.ccms.caab.claim-upload.valid-extensions}") final List<String> validExtensions,
      @Value("${spring.servlet.multipart.max-file-size}") final String maxFileSize) {
    super(validExtensions, maxFileSize);
  }

  @Override
  public boolean supports(final Class<?> clazz) {
    return ProviderRequestDetailsFormData.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(final Object target, final Errors errors) {
    final ProviderRequestDetailsFormData formData = (ProviderRequestDetailsFormData) target;

    if (formData.isClaimUploadEnabled()) {
      validateFile(formData, errors);
    }

    if (formData.getDynamicOptions() != null) {
      validateDynamicOptions(formData, errors);
    }

    validateAdditionalInformation(formData, errors);
  }

  private void validateDynamicOptions(final ProviderRequestDetailsFormData formData,
                                      final Errors errors) {
    formData.getDynamicOptions().entrySet().stream().sorted(Map.Entry.comparingByKey())
        .forEach(entry -> {
          String key = entry.getKey();
          DynamicOptionFormData value = entry.getValue();
          String fieldPath = "dynamicOptions[%s].fieldValue".formatted(key);

          if (value.isMandatory()) {
            validateRequiredField(fieldPath, value.getFieldValue(), value.getFieldDescription(),
                errors);
          }

          if (StringUtils.hasText(value.getFieldValue())) {
            validateFieldByType(fieldPath, value, errors);
          }
        });
  }

  private void validateFieldByType(String fieldPath, DynamicOptionFormData value, Errors errors) {
    switch (value.getFieldType()) {
      case FIELD_TYPE_AMT -> {
        validateCurrencyField(fieldPath, value.getFieldValue(), value.getFieldDescription(),
            errors);
        validateNumericLimit(fieldPath, value.getFieldValue(), value.getFieldDescription(),
            MAX_COST_LIMIT, errors);
      }
      case FIELD_TYPE_INT -> {
        validateFieldMaxLength(fieldPath, value.getFieldValue(), 30, value.getFieldDescription(),
            errors);
        validateNumericField(fieldPath, value.getFieldValue(), value.getFieldDescription(), errors);
      }
      case FIELD_TYPE_DATE ->
          validateValidDateField(value.getFieldValue(), fieldPath, value.getFieldDescription(),
              DATE_FORMAT, errors);
      case FIELD_TYPE_FTS ->
          validateFieldMaxLength(fieldPath, value.getFieldValue(), 30, value.getFieldDescription(),
              errors);
      case FIELD_TYPE_FTL ->
          validateFieldMaxLength(fieldPath, value.getFieldValue(), 80, value.getFieldDescription(),
              errors);
      default -> log.warn("Unsupported field type: {}", value.getFieldType());
    }
  }

  private void validateAdditionalInformation(final ProviderRequestDetailsFormData formData,
                                             final Errors errors) {
    if (Boolean.TRUE.equals(formData.getIsAdditionalInformationPromptRequired())) {
      validateRequiredField("additionalInformation", formData.getAdditionalInformation(),
          formData.getAdditionalInformationLabel(), errors);
    }

    validateFieldMaxLength("additionalInformation", formData.getAdditionalInformation(), 8000,
        formData.getAdditionalInformationLabel(), errors);
  }

}
