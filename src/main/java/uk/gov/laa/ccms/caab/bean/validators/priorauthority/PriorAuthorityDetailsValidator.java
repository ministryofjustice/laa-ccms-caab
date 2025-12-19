package uk.gov.laa.ccms.caab.bean.validators.priorauthority;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.common.DynamicOptionFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityDetailsFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validator component responsible for validating {@link
 * uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityDetailsFormData} objects.
 */
@Component
public class PriorAuthorityDetailsValidator extends AbstractValidator {

  private static final String FIELD_TYPE_AMT = "AMT";
  private static final String FIELD_TYPE_INT = "INT";
  private static final String FIELD_TYPE_FTS = "FTS";
  private static final String FIELD_TYPE_FTL = "FTL";

  private static final BigDecimal MAX_COST_LIMIT = new BigDecimal("100000000.00");

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from {@link
   *     uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityDetailsFormData}, {@code false}
   *     otherwise.
   */
  @Override
  public boolean supports(final Class<?> clazz) {
    return PriorAuthorityDetailsFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the prior authority type details in the {@link
   * uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityDetailsFormData}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(final Object target, final Errors errors) {
    final PriorAuthorityDetailsFormData priorAuthorityDetails =
        (PriorAuthorityDetailsFormData) target;

    validateRequiredField("summary", priorAuthorityDetails.getSummary(), "Summary", errors);

    validateRequiredField(
        "justification", priorAuthorityDetails.getSummary(), "Justification", errors);

    if (priorAuthorityDetails.isValueRequired()) {
      validateRequiredField(
          "amountRequested",
          priorAuthorityDetails.getAmountRequested(),
          "Amount requested",
          errors);

      if (StringUtils.hasText(priorAuthorityDetails.getAmountRequested())) {
        validateCurrencyField(
            "amountRequested",
            priorAuthorityDetails.getAmountRequested(),
            "Amount requested",
            errors);
      }
    }

    if (priorAuthorityDetails.getDynamicOptions() != null) {

      final List<String> keys = new ArrayList<>(priorAuthorityDetails.getDynamicOptions().keySet());

      Collections.sort(keys);

      for (final String key : keys) {
        final DynamicOptionFormData value = priorAuthorityDetails.getDynamicOptions().get(key);

        if (value.isMandatory()) {
          validateRequiredField(
              "dynamicOptions[%s].fieldValue".formatted(key),
              value.getFieldValue(),
              value.getFieldDescription(),
              errors);
        }

        if (StringUtils.hasText(value.getFieldValue())) {

          switch (value.getFieldType()) {
            case FIELD_TYPE_AMT -> {
              validateCurrencyField(
                  "dynamicOptions[%s].fieldValue".formatted(key),
                  value.getFieldValue(),
                  value.getFieldDescription(),
                  errors);

              validateNumericLimit(
                  "dynamicOptions[%s].fieldValue".formatted(key),
                  value.getFieldValue(),
                  value.getFieldDescription(),
                  MAX_COST_LIMIT,
                  errors);
            }
            case FIELD_TYPE_INT -> {
              validateNumericField(
                  "dynamicOptions[%s].fieldValue".formatted(key),
                  value.getFieldValue(),
                  value.getFieldDescription(),
                  errors);
            }
            case FIELD_TYPE_FTS -> {
              validateFieldMaxLength(
                  "dynamicOptions[%s].fieldValue".formatted(key),
                  value.getFieldValue(),
                  30,
                  value.getFieldDescription(),
                  errors);
            }
            case FIELD_TYPE_FTL ->
                validateFieldMaxLength(
                    "dynamicOptions[%s].fieldValue".formatted(key),
                    value.getFieldValue(),
                    80,
                    value.getFieldDescription(),
                    errors);
            default -> {
              break;
            }
          }
        }
      }
    }
  }
}
