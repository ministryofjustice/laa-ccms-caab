package uk.gov.laa.ccms.caab.bean.validators.awards;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.gov.laa.ccms.caab.bean.award.TimeRecoveryFormData;

class TimeRecoveryValidatorTest {

  private final TimeRecoveryValidator validator = new TimeRecoveryValidator();

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "31/12/2099", "14/12/1901", "29/02/2024"})
  void acceptsOptionalAndFutureDateAndSharedMinimumBoundary(String date) {
    assertThat(validate(date).hasErrors()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"31/12/1899", "01/01/1900", "12/12/1901", "13/12/1901"})
  void rejectsDatesOnOrBeforeSharedMinimum(String date) {
    assertThat(validate(date).getFieldError("effectiveDate"))
        .isNotNull()
        .satisfies(error -> assertThat(error.getCode()).isEqualTo("invalid.input"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"31/02/2026", "29/02/2025", "not a date", "2026-01-01"})
  void rejectsInvalidDatesUsingSharedValidation(String date) {
    assertThat(validate(date).getFieldError("effectiveDate"))
        .isNotNull()
        .satisfies(error -> assertThat(error.getCode()).isEqualTo("invalid.format"));
  }

  @Test
  void requiresBothTextFieldsAndChecksLegacyLengthAndCharacterSet() {
    final TimeRecoveryFormData missing = new TimeRecoveryFormData();
    final BeanPropertyBindingResult errors = new BeanPropertyBindingResult(missing, "timeRecovery");
    validator.validate(missing, errors);
    assertThat(errors.hasFieldErrors("triggeringEvent")).isTrue();
    assertThat(errors.hasFieldErrors("timeRelatedRecoveryDetails")).isTrue();

    final TimeRecoveryFormData tooLong = validForm();
    tooLong.setTriggeringEvent("a".repeat(951));
    tooLong.setTimeRelatedRecoveryDetails("Email @example");
    final BeanPropertyBindingResult invalid =
        new BeanPropertyBindingResult(tooLong, "timeRecovery");
    validator.validate(tooLong, invalid);
    assertThat(invalid.hasFieldErrors("triggeringEvent")).isTrue();
    assertThat(invalid.hasFieldErrors("timeRelatedRecoveryDetails")).isTrue();
  }

  private BeanPropertyBindingResult validate(final String date) {
    final TimeRecoveryFormData form = validForm();
    form.setEffectiveDate(date);
    final BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "timeRecovery");
    validator.validate(form, errors);
    return errors;
  }

  private TimeRecoveryFormData validForm() {
    final TimeRecoveryFormData form = new TimeRecoveryFormData();
    form.setTriggeringEvent("Payment on sale");
    form.setTimeRelatedRecoveryDetails("The property will be sold");
    return form;
  }
}
