package uk.gov.laa.ccms.caab.bean.validators.awards;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.gov.laa.ccms.caab.bean.award.FinancialAwardFormData;

class FinancialAwardValidatorTest {

  private final FinancialAwardValidator validator = new FinancialAwardValidator();

  @Test
  void acceptsValidLegacyFinancialAwardFields() {
    final FinancialAwardFormData form = validForm();
    final BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "financialAward");

    validator.validate(form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void rejectsMissingRequiredFields() {
    final FinancialAwardFormData form = new FinancialAwardFormData();
    final BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "financialAward");

    validator.validate(form, errors);

    assertThat(errors.getFieldErrors())
        .extracting("field")
        .contains(
            "awardType",
            "description",
            "awardCode",
            "dateOfOrder",
            "awardAmount",
            "interimAward",
            "awardedBy");
  }

  @Test
  void rejectsInvalidDatesAmountLengthsAndAddressCharacters() {
    final FinancialAwardFormData form = validForm();
    form.setDateOfOrder("31/02/2025");
    form.setOrderServedDate(
        LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    form.setAwardAmount("12.345");
    form.setAwardJustifications("x".repeat(951));
    form.setAddressLine1("Address  with double spaces");
    form.setAddressLine2("x".repeat(36));
    form.setAddressLine3("Invalid@character");
    form.setOtherDetails("x".repeat(951));

    final BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "financialAward");

    validator.validate(form, errors);

    assertThat(errors.getFieldErrors())
        .extracting("field")
        .contains(
            "dateOfOrder",
            "orderServedDate",
            "awardAmount",
            "awardJustifications",
            "addressLine1",
            "addressLine2",
            "addressLine3",
            "otherDetails");
  }

  private FinancialAwardFormData validForm() {
    final FinancialAwardFormData form = new FinancialAwardFormData();
    form.setAwardType("DAMAGE");
    form.setDescription("Damage");
    form.setAwardCode("DAMAGE_AGR");
    form.setDateOfOrder("01/01/2025");
    form.setAwardAmount("1234.56");
    form.setInterimAward("0");
    form.setAwardedBy("COURT");
    form.setAwardJustifications("Justification");
    form.setOrderServedDate("02/01/2025");
    form.setAddressLine1("1 High Street");
    form.setAddressLine2("London");
    form.setAddressLine3("England");
    form.setStatutoryChargeExemptReason("Reason");
    form.setOtherDetails("Other details");
    return form;
  }
}
