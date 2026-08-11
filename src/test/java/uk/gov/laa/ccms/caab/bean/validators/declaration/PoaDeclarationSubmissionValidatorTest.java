package uk.gov.laa.ccms.caab.bean.validators.declaration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.SummarySubmissionFormData;
import uk.gov.laa.ccms.caab.bean.declaration.DynamicCheckbox;

@DisplayName("POA declaration submission validator tests")
class PoaDeclarationSubmissionValidatorTest {

  private PoaDeclarationSubmissionValidator validator;

  @BeforeEach
  void setUp() {
    validator = new PoaDeclarationSubmissionValidator();
  }

  private DynamicCheckbox option(final boolean checked) {
    final DynamicCheckbox checkbox = new DynamicCheckbox();
    checkbox.setChecked(checked);
    checkbox.setFieldValueDisplayValue("A declaration");
    return checkbox;
  }

  private Errors validate(final SummarySubmissionFormData formData) {
    final Errors errors = new BeanPropertyBindingResult(formData, "summarySubmissionFormData");
    validator.validate(formData, errors);
    return errors;
  }

  @Test
  @DisplayName("Passes when every declaration is acknowledged")
  void passesWhenAllChecked() {
    final SummarySubmissionFormData formData = new SummarySubmissionFormData();
    formData.setDeclarationOptions(List.of(option(true), option(true)));

    assertThat(validate(formData).hasErrors()).isFalse();
  }

  @Test
  @DisplayName("Fails when only some declarations are acknowledged")
  void failsWhenSomeUnchecked() {
    final SummarySubmissionFormData formData = new SummarySubmissionFormData();
    formData.setDeclarationOptions(List.of(option(true), option(false)));

    final Errors errors = validate(formData);
    assertThat(errors.hasErrors()).isTrue();
    assertThat(errors.getGlobalError().getCode()).isEqualTo("declaration.required");
  }

  @Test
  @DisplayName("Fails when no declarations are present")
  void failsWhenEmpty() {
    final SummarySubmissionFormData formData = new SummarySubmissionFormData();
    formData.setDeclarationOptions(List.of());

    assertThat(validate(formData).hasErrors()).isTrue();
  }

  @Test
  @DisplayName("Fails when the declaration options are null")
  void failsWhenNull() {
    assertThat(validate(new SummarySubmissionFormData()).hasErrors()).isTrue();
  }
}
