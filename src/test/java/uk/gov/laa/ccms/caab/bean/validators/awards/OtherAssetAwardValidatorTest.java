package uk.gov.laa.ccms.caab.bean.validators.awards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.gov.laa.ccms.caab.bean.award.OtherAssetAwardFormData;

class OtherAssetAwardValidatorTest {

  private final OtherAssetAwardValidator validator = new OtherAssetAwardValidator();

  @Test
  void acceptsValidOtherAssetAwardFieldsAtTheirBoundaries() {
    final OtherAssetAwardFormData form = validForm();
    form.setValuationAmount("99999999.99");
    form.setAwardedPercentage("100");
    form.setRecoveredPercentage("0.00");
    form.setDisputedPercentage("100.00");
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void acceptsBlankOptionalFieldsWhenRecoveryIsSelected() {
    final OtherAssetAwardFormData form = new OtherAssetAwardFormData();
    form.setAwardType("ASSET");
    form.setDescription("Asset");
    form.setAwardCode("OTH_ASSET");
    form.setDateOfOrder("01/01/2025");
    form.setAwardedBy("COURT");
    form.setValuationAmount("1000.50");
    form.setValuationCriteria("AGREED");
    form.setValuationDate("02/01/2025");
    form.setRecovery("UNKNOWN");
    form.setRecoveryOfAwardTimeRelated(false);
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void acceptsAmountFieldsWithoutPercentageFields() {
    final OtherAssetAwardFormData form = validForm();
    form.setAwardedPercentage(null);
    form.setRecoveredPercentage(null);
    form.setDisputedPercentage(null);
    form.setAwardedAmount("750.38");
    form.setRecoveredAmount("100.00");
    form.setDisputedAmount("200.00");
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @ParameterizedTest
  @CsvSource({
    "false, false, false",
    "false, false, true",
    "false, true, false",
    "false, true, true",
    "true, false, false",
    "true, false, true",
    "true, true, false",
    "true, true, true"
  })
  void acceptsIndependentValueTypesForEachRow(
      final boolean disputedUsesAmount,
      final boolean awardedUsesAmount,
      final boolean recoveredUsesAmount) {
    final OtherAssetAwardFormData form = validForm();
    if (disputedUsesAmount) {
      form.setDisputedPercentage(null);
      form.setDisputedAmount("200.00");
    }
    if (awardedUsesAmount) {
      form.setAwardedPercentage(null);
      form.setAwardedAmount("750.38");
    }
    if (recoveredUsesAmount) {
      form.setRecoveredPercentage(null);
      form.setRecoveredAmount("100.00");
    }
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"disputed", "awarded", "recovered"})
  void reportsErrorsOnBothFieldsOnlyWithinTheConflictingPair(final String row) {
    final OtherAssetAwardFormData form = validForm();
    new BeanWrapperImpl(form).setPropertyValue(row + "Amount", "100.00");
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.getGlobalErrors()).isEmpty();
    assertThat(errors.getFieldErrors())
        .extracting("field", "code")
        .containsExactlyInAnyOrder(
            tuple(row + "Percentage", "invalid.awardRecovery.valueType"),
            tuple(row + "Amount", "invalid.awardRecovery.valueType"));
    assertThat(errors.getFieldError(row + "Amount").getDefaultMessage())
        .isEqualTo(errors.getFieldError(row + "Percentage").getDefaultMessage());
  }

  @Test
  void acceptsBlankAlternativesWithinMixedRows() {
    final OtherAssetAwardFormData form = validForm();
    form.setDisputedAmount(" ");
    form.setAwardedPercentage("");
    form.setAwardedAmount("750.38");
    form.setRecoveredAmount(null);
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void rejectsBothZeroValuesInEveryPair() {
    final OtherAssetAwardFormData form = validForm();
    form.setDisputedPercentage("0");
    form.setDisputedAmount("0.00");
    form.setAwardedPercentage("0.00");
    form.setAwardedAmount("0");
    form.setRecoveredPercentage("0.0");
    form.setRecoveredAmount("0.0");
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.getFieldErrors())
        .extracting("field", "code")
        .containsExactlyInAnyOrder(
            tuple("disputedPercentage", "invalid.awardRecovery.valueType"),
            tuple("disputedAmount", "invalid.awardRecovery.valueType"),
            tuple("awardedPercentage", "invalid.awardRecovery.valueType"),
            tuple("awardedAmount", "invalid.awardRecovery.valueType"),
            tuple("recoveredPercentage", "invalid.awardRecovery.valueType"),
            tuple("recoveredAmount", "invalid.awardRecovery.valueType"));
  }

  @Test
  void rejectsMissingRequiredAwardDetails() {
    final OtherAssetAwardFormData form = validForm();
    form.setDateOfOrder("");
    form.setDescription(" ");
    form.setAwardedBy(" ");
    form.setValuationAmount(null);
    form.setValuationCriteria("");
    form.setValuationDate("");
    form.setRecovery("");
    form.setRecoveryOfAwardTimeRelated(null);
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.getFieldErrors())
        .extracting("field", "code")
        .contains(
            tuple("dateOfOrder", "required.dateOfOrder"),
            tuple("description", "required.description"),
            tuple("awardedBy", "required.awardedBy"),
            tuple("valuationAmount", "required.valuationAmount"),
            tuple("valuationCriteria", "required.valuationCriteria"),
            tuple("valuationDate", "required.valuationDate"),
            tuple("recovery", "required.recovery"),
            tuple("recoveryOfAwardTimeRelated", "required.recoveryOfAwardTimeRelated"));
  }

  @Test
  void rejectsMissingAwardTypeMetadata() {
    final OtherAssetAwardFormData form = new OtherAssetAwardFormData();
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.getFieldErrors()).extracting("field").contains("awardType", "awardCode");
  }

  @Test
  void rejectsInvalidAndFutureDates() {
    final OtherAssetAwardFormData form = validForm();
    form.setDateOfOrder("31/02/2025");
    form.setValuationDate(
        LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.getFieldErrors())
        .extracting("field")
        .contains("dateOfOrder", "valuationDate");
  }

  @Test
  void rejectsInvalidAndExcessiveAmounts() {
    final OtherAssetAwardFormData form = validForm();
    form.setAwardedPercentage(null);
    form.setRecoveredPercentage(null);
    form.setDisputedPercentage(null);
    form.setValuationAmount("12.345");
    form.setRecoveredAmount("-1.00");
    form.setDisputedAmount("100000000.00");
    form.setAwardedAmount("not-money");
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.getFieldErrors())
        .extracting("field")
        .contains("valuationAmount", "recoveredAmount", "disputedAmount", "awardedAmount");
  }

  @Test
  void rejectsZeroValuationAmount() {
    final OtherAssetAwardFormData form = validForm();
    form.setValuationAmount("0.00");
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.getFieldErrors("valuationAmount"))
        .extracting("code")
        .contains("value.must.be.positive");
  }

  @Test
  void acceptsPercentagesAboveOneHundred() {
    final OtherAssetAwardFormData form = validForm();
    form.setAwardedPercentage("100.01");
    form.setRecoveredPercentage("999999.99");
    form.setDisputedPercentage("101");
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void rejectsInvalidPercentages() {
    final OtherAssetAwardFormData form = validForm();
    form.setAwardedPercentage("not-a-number");
    form.setRecoveredPercentage("12.345");
    form.setDisputedPercentage("-1");
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.getFieldErrors())
        .extracting("field")
        .contains("awardedPercentage", "recoveredPercentage", "disputedPercentage");
  }

  @Test
  void rejectsFieldsBeyondApiLengthLimits() {
    final OtherAssetAwardFormData form = validForm();
    form.setDescription("x".repeat(51));
    form.setValuationCriteria("x".repeat(51));
    form.setNoRecoveryDetails("x".repeat(951));
    form.setStatutoryChargeExemptReason("x".repeat(951));
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.getFieldErrors())
        .extracting("field")
        .contains(
            "description", "valuationCriteria", "noRecoveryDetails", "statutoryChargeExemptReason");
  }

  private BeanPropertyBindingResult errorsFor(final OtherAssetAwardFormData form) {
    return new BeanPropertyBindingResult(form, "otherAssetAward");
  }

  private OtherAssetAwardFormData validForm() {
    final OtherAssetAwardFormData form = new OtherAssetAwardFormData();
    form.setAwardType("ASSET");
    form.setDescription("Antique jewellery");
    form.setAwardCode("OTH_ASSET");
    form.setDateOfOrder("01/01/2025");
    form.setAwardedBy("COURT");
    form.setValuationAmount("1000.50");
    form.setValuationCriteria("AGREED");
    form.setValuationDate("02/01/2025");
    form.setAwardedPercentage("75.25");
    form.setRecoveredPercentage("10.00");
    form.setDisputedPercentage("20.00");
    form.setRecovery("Recovery details");
    form.setNoRecoveryDetails("No recovery details");
    form.setStatutoryChargeExemptReason("Exemption reason");
    form.setRecoveryOfAwardTimeRelated(true);
    return form;
  }
}
