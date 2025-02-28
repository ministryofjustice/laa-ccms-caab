package uk.gov.laa.ccms.caab.bean.validators.request;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.common.DynamicOptionFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestDetailsFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;
import uk.gov.laa.ccms.caab.bean.validators.file.FileUploadValidator;

/**
 * Validator component responsible for validating
 * {@link uk.gov.laa.ccms.caab.bean.request.ProviderRequestDetailsFormData} objects.
 */
@Component
public class ProviderRequestDetailsValidator extends FileUploadValidator {

  private static final String FIELD_TYPE_AMT = "AMT";
  private static final String FIELD_TYPE_INT = "INT";
  private static final String FIELD_TYPE_DATE = "DAT";

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
    final ProviderRequestDetailsFormData formData =
        (ProviderRequestDetailsFormData) target;

    if (formData.isClaimUploadEnabled()) {
      validateFile(formData, errors);
    }

    if (formData.getDynamicOptions() != null) {

      final List<String> keys = new ArrayList<>(
          formData
              .getDynamicOptions()
              .keySet());

      Collections.sort(keys);

      for (final String key : keys) {
        final DynamicOptionFormData value =
            formData.getDynamicOptions().get(key);

        if (value.isMandatory()) {
          validateRequiredField(String.format("dynamicOptions[%s].fieldValue", key),
              value.getFieldValue(), value.getFieldDescription(), errors);
        }

        if (StringUtils.hasText(value.getFieldValue())) {
          if (FIELD_TYPE_AMT.equals(value.getFieldType())) {
            validateCurrencyField(String.format("dynamicOptions[%s].fieldValue", key),
                value.getFieldValue(), value.getFieldDescription(), errors);

            validateNumericLimit(String.format("dynamicOptions[%s].fieldValue", key),
                value.getFieldValue(), value.getFieldDescription(), MAX_COST_LIMIT, errors);

          } else if (FIELD_TYPE_INT.equals(value.getFieldType())) {
            validateNumericField(String.format("dynamicOptions[%s].fieldValue", key),
                value.getFieldValue(), value.getFieldDescription(), errors);
          } else if (FIELD_TYPE_DATE.equals(value.getFieldType())) {
            validateValidDateField(value.getFieldValue(),
                String.format("dynamicOptions[%s].fieldValue", key),
                value.getFieldDescription(), DATE_FORMAT, errors);
          }
        }
      }
    }

    if (Boolean.TRUE.equals(formData.getIsAdditionalInformationPromptRequired())) {
      validateRequiredField("additionalInformation", formData.getAdditionalInformation(),
          formData.getAdditionalInformationLabel(), errors);
    }

    validateFieldMaxLength("additionalInformation", formData.getAdditionalInformation(),
        8000,  formData.getAdditionalInformationLabel(), errors);

  }
}
