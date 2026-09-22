package uk.gov.laa.ccms.caab.bean.validators.awards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
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
  void acceptsBlankOptionalFields() {
    final OtherAssetAwardFormData form = new OtherAssetAwardFormData();
    form.setAwardType("ASSET");
    form.setDescription("Asset");
    form.setAwardCode("OTH_ASSET");
    form.setDateOfOrder("01/01/2025");
    form.setAwardedBy("COURT");
    form.setValuationAmount("1000.50");
    form.setValuationDate("02/01/2025");
    form.setRecoveryOfAwardTimeRelated(false);
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void rejectsMissingRequiredAwardDetails() {
    final OtherAssetAwardFormData form = validForm();
    form.setDateOfOrder("");
    form.setAwardedBy(" ");
    form.setValuationAmount(null);
    form.setValuationDate("");
    form.setRecoveryOfAwardTimeRelated(null);
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.getFieldErrors())
        .extracting("field", "code")
        .contains(
            tuple("dateOfOrder", "required.dateOfOrder"),
            tuple("awardedBy", "required.awardedBy"),
            tuple("valuationAmount", "required.valuationAmount"),
            tuple("valuationDate", "required.valuationDate"),
            tuple("recoveryOfAwardTimeRelated", "required.recoveryOfAwardTimeRelated"));
  }

  @Test
  void rejectsMissingAwardMetadata() {
    final OtherAssetAwardFormData form = new OtherAssetAwardFormData();
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.getFieldErrors())
        .extracting("field")
        .contains("awardType", "description", "awardCode");
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
  void rejectsInvalidPercentages() {
    final OtherAssetAwardFormData form = validForm();
    form.setAwardedPercentage("100.01");
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
    form.setValuationCriteria("x".repeat(51));
    form.setRecovery("x".repeat(201));
    form.setNoRecoveryDetails("x".repeat(1001));
    form.setStatutoryChargeExemptReason("x".repeat(1001));
    final BeanPropertyBindingResult errors = errorsFor(form);

    validator.validate(form, errors);

    assertThat(errors.getFieldErrors())
        .extracting("field")
        .contains(
            "valuationCriteria", "recovery", "noRecoveryDetails", "statutoryChargeExemptReason");
  }

  private BeanPropertyBindingResult errorsFor(final OtherAssetAwardFormData form) {
    return new BeanPropertyBindingResult(form, "otherAssetAward");
  }

  private OtherAssetAwardFormData validForm() {
    final OtherAssetAwardFormData form = new OtherAssetAwardFormData();
    form.setAwardType("ASSET");
    form.setDescription("Asset");
    form.setAwardCode("OTH_ASSET");
    form.setDateOfOrder("01/01/2025");
    form.setAwardedBy("COURT");
    form.setValuationAmount("1000.50");
    form.setValuationCriteria("Market value");
    form.setValuationDate("02/01/2025");
    form.setAwardedPercentage("75.25");
    form.setRecoveredAmount("100.00");
    form.setRecoveredPercentage("10.00");
    form.setDisputedAmount("200.00");
    form.setAwardedAmount("750.38");
    form.setDisputedPercentage("20.00");
    form.setRecovery("Recovery details");
    form.setNoRecoveryDetails("No recovery details");
    form.setStatutoryChargeExemptReason("Exemption reason");
    form.setRecoveryOfAwardTimeRelated(true);
    return form;
  }
}
