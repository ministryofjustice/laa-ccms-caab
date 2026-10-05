package uk.gov.laa.ccms.caab.bean.validators.awards;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.STANDARD_CHARACTER_SET;
import static uk.gov.laa.ccms.caab.util.DateUtils.COMPONENT_DATE_PATTERN;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.award.TimeRecoveryFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validates the legacy AW07 inputs without applying the parent award's past-date rule. */
@Component
public class TimeRecoveryValidator extends AbstractValidator {

  @Override
  public boolean supports(final Class<?> clazz) {
    return TimeRecoveryFormData.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(final Object target, final Errors errors) {
    final TimeRecoveryFormData form = (TimeRecoveryFormData) target;
    validateRequiredField("triggeringEvent", form.getTriggeringEvent(), "Triggering Event", errors);
    validateRequiredField(
        "timeRelatedRecoveryDetails",
        form.getTimeRelatedRecoveryDetails(),
        "Other Details of Time Related Recovery",
        errors);
    validateText("triggeringEvent", form.getTriggeringEvent(), "Triggering Event", errors);
    validateText(
        "timeRelatedRecoveryDetails",
        form.getTimeRelatedRecoveryDetails(),
        "Other Details of Time Related Recovery",
        errors);

    if (StringUtils.hasText(form.getEffectiveDate())) {
      validateValidDateField(
          form.getEffectiveDate(),
          "effectiveDate",
          "Date if known",
          COMPONENT_DATE_PATTERN,
          errors);
    }
  }

  private void validateText(
      final String field, final String value, final String displayName, final Errors errors) {
    if (StringUtils.hasText(value)) {
      validateFieldMaxLength(field, value, 950, displayName, errors);
      validateFieldFormat(field, value, STANDARD_CHARACTER_SET, displayName, errors);
    }
  }
}
