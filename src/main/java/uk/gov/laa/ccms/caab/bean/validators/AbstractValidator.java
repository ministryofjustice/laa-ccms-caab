package uk.gov.laa.ccms.caab.bean.validators;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.NUMERIC_PATTERN;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public abstract class AbstractValidator implements Validator {

  private static String GENERIC_REQUIRED_ERROR = "Please complete '%s'.";
  private static String GENERIC_NUMERIC_REQUIRED = "Please enter a numeric value for %s.";

  protected void validateRequiredField(String field, String displayValue, Errors errors){
    ValidationUtils.rejectIfEmpty(errors, field, "required." + field,
        String.format(GENERIC_REQUIRED_ERROR, displayValue));
  }

  protected void validateNumericField(String field, String fieldValue, String displayValue, Errors errors){
    if (!fieldValue.matches(NUMERIC_PATTERN)) {
      errors.rejectValue(field, "invalid.numeric",
          String.format(GENERIC_NUMERIC_REQUIRED, displayValue));
    }

  }

}
