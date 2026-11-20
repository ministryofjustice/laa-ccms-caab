package uk.gov.laa.ccms.caab.bean.validators.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.gov.laa.ccms.caab.bean.CaseDischargeFormData;

class CaseDischargeValidatorTest {
  private final CaseDischargeValidator validator = new CaseDischargeValidator();

  @Test
  void unansweredDischargeChoiceIsRequiredButNoDoesNotRequireOtherFields() {
    final var form = new CaseDischargeFormData();
    assertThat(validate(form).hasFieldErrors("dischargeCaseInd")).isTrue();
    form.setDischargeCaseInd(false);
    assertThat(validate(form).hasErrors()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "  "})
  void yesRequiresNonBlankReasonAndPrivateContinuationChoice(String blank) {
    final var form = new CaseDischargeFormData();
    form.setDischargeCaseInd(true);
    form.setDischargeReason(blank);
    final var errors = validate(form);
    assertThat(errors.hasFieldErrors("dischargeReason")).isTrue();
    assertThat(errors.hasFieldErrors("clientContinueInd")).isTrue();
    form.setDischargeReason("CD");
    form.setClientContinueInd(false);
    assertThat(validate(form).hasErrors()).isFalse();
  }

  @Test
  void optionalOtherDetailsAccept950ButReject951Characters() {
    final var form = new CaseDischargeFormData();
    form.setDischargeCaseInd(false);
    form.setOtherDetails("a".repeat(950));
    assertThat(validate(form).hasErrors()).isFalse();
    form.setOtherDetails("a".repeat(951));
    assertThat(validate(form).hasFieldErrors("otherDetails")).isTrue();
  }

  private BeanPropertyBindingResult validate(CaseDischargeFormData form) {
    final var errors = new BeanPropertyBindingResult(form, "discharge");
    validator.validate(form, errors);
    return errors;
  }
}
