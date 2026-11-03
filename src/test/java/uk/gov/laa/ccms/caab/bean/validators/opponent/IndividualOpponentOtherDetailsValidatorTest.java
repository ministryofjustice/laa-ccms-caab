package uk.gov.laa.ccms.caab.bean.validators.opponent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentOtherDetailsFormData;

class IndividualOpponentOtherDetailsValidatorTest {
  private final IndividualOpponentOtherDetailsValidator validator =
      new IndividualOpponentOtherDetailsValidator();

  @Test
  void everyOtherDetailIsOptional() {
    assertThat(validate(new IndividualOpponentOtherDetailsFormData()).hasErrors()).isFalse();
  }

  @ParameterizedTest
  @CsvSource({
    "assessedIncome,-1",
    "assessedAssets,1.234",
    "assessedIncome,1e2",
    "assessedAssets,100000000",
    "assessedIncome,'1,000.00'",
    "employerName,<markup>",
    "employerAddress,<markup>",
    "otherInformation,<markup>",
    "assessmentDate,31/02/2026",
    "assessmentDate,29/02/2025",
    "assessmentDate,01/01/0000"
  })
  void rejectsInvalidFormats(String field, String value) {
    final var form = new IndividualOpponentOtherDetailsFormData();
    new BeanWrapperImpl(form).setPropertyValue(field, value);
    assertThat(validate(form).hasFieldErrors(field)).isTrue();
  }

  @ParameterizedTest
  @CsvSource({"employerName,35", "employerAddress,200", "otherInformation,2000"})
  void enforcesExactCharacterLimits(String field, int maximum) {
    final var form = new IndividualOpponentOtherDetailsFormData();
    final var wrapper = new BeanWrapperImpl(form);
    wrapper.setPropertyValue(field, "a".repeat(maximum));
    assertThat(validate(form).hasErrors()).isFalse();
    wrapper.setPropertyValue(field, "a".repeat(maximum + 1));
    assertThat(validate(form).hasFieldErrors(field)).isTrue();
  }

  @Test
  void allowsFutureDatesAndKeepsAssessmentValuesWhenAnswerIsNo() {
    final var form = new IndividualOpponentOtherDetailsFormData();
    form.setCourtOrderedMeansAssessment(false);
    form.setAssessedIncome("99999999.99");
    form.setAssessedAssets("0");
    form.setAssessmentDate("01/01/2099");
    assertThat(validate(form).hasErrors()).isFalse();
  }

  @ParameterizedTest
  @CsvSource({"01/01/1899,false", "12/12/1901,false", "13/12/1901,false", "14/12/1901,true"})
  void appliesTheStandardMinimumAssessmentDate(String date, boolean valid) {
    final var form = new IndividualOpponentOtherDetailsFormData();
    form.setAssessmentDate(date);

    assertThat(validate(form).hasFieldErrors("assessmentDate")).isEqualTo(!valid);
  }

  private BeanPropertyBindingResult validate(IndividualOpponentOtherDetailsFormData form) {
    final var errors = new BeanPropertyBindingResult(form, "otherDetails");
    validator.validate(form, errors);
    return errors;
  }
}
