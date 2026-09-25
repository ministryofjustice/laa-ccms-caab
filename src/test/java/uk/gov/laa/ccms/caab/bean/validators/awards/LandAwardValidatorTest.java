package uk.gov.laa.ccms.caab.bean.validators.awards;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.gov.laa.ccms.caab.bean.award.LandAwardFormData;

class LandAwardValidatorTest {

  private final LandAwardValidator validator = new LandAwardValidator();

  @Test
  void acceptsValidLegacyLandAwardFields() {
    final LandAwardFormData form = validForm();
    final BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "landAward");

    validator.validate(form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void rejectsMissingRequiredFields() {
    final LandAwardFormData form = new LandAwardFormData();
    form.setValuationAmount(null);
    form.setDisputedPercentage(null);
    form.setMortgageAmountDue(null);
    final BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "landAward");

    validator.validate(form, errors);

    assertThat(errors.getFieldErrors())
        .extracting("field")
        .contains(
            "awardType",
            "awardCode",
            "dateOfOrder",
            "description",
            "addressLine1",
            "valuationAmount",
            "valuationCriteria",
            "valuationDate",
            "disputedPercentage",
            "mortgageAmountDue",
            "awardedBy",
            "recovery",
            "recoveryOfAwardTimeRelated");
  }

  @Test
  void rejectsInvalidDatesAmountsLengthsAndAddressCharacters() {
    final LandAwardFormData form = validForm();
    form.setDateOfOrder("31/02/2025");
    form.setValuationDate(
        LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    form.setValuationAmount("100000000.00");
    form.setDisputedPercentage("12.345");
    form.setAwardedPercentage("invalid");
    form.setMortgageAmountDue("100000000.00");
    form.setDescription("x".repeat(36));
    form.setAddressLine1("Address  with double spaces");
    form.setAddressLine2("Invalid@character");
    form.setNoRecoveryDetails("x".repeat(951));
    form.setRegistrationReference("x".repeat(36));

    final BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "landAward");

    validator.validate(form, errors);

    assertThat(errors.getFieldErrors())
        .extracting("field")
        .contains(
            "dateOfOrder",
            "valuationDate",
            "valuationAmount",
            "disputedPercentage",
            "awardedPercentage",
            "mortgageAmountDue",
            "description",
            "addressLine1",
            "addressLine2",
            "noRecoveryDetails",
            "registrationReference");
  }

  @Test
  void usesAbstractValidatorEarliestDateForBothDates() {
    final LandAwardFormData form = validForm();
    form.setDateOfOrder("13/12/1901");
    form.setValuationDate("13/12/1901");
    final BeanPropertyBindingResult invalid = new BeanPropertyBindingResult(form, "landAward");

    validator.validate(form, invalid);

    assertThat(invalid.getFieldErrors())
        .extracting("field")
        .contains("dateOfOrder", "valuationDate");

    form.setDateOfOrder("14/12/1901");
    form.setValuationDate("14/12/1901");
    final BeanPropertyBindingResult valid = new BeanPropertyBindingResult(form, "landAward");

    validator.validate(form, valid);

    assertThat(valid.hasErrors()).isFalse();
  }

  private LandAwardFormData validForm() {
    final LandAwardFormData form = new LandAwardFormData();
    form.setAwardType("LAND");
    form.setAwardCode("LAND");
    form.setDateOfOrder("01/01/2025");
    form.setDescription("Land");
    form.setTitleNumber("AB123456");
    form.setAddressLine1("1 High Street");
    form.setAddressLine2("London");
    form.setAddressLine3("England");
    form.setValuationAmount("250000.00");
    form.setValuationCriteria("MARKET");
    form.setValuationDate("02/01/2025");
    form.setDisputedPercentage("50.00");
    form.setAwardedPercentage("25.00");
    form.setMortgageAmountDue("100000.00");
    form.setAwardedBy("COURT");
    form.setRecovery("RECOVERED");
    form.setNoRecoveryDetails("Details");
    form.setStatutoryChargeExemptReason("Reason");
    form.setLandChargeRegistration("REGISTERED");
    form.setRegistrationReference("REF123");
    form.setRecoveryOfAwardTimeRelated("Y");
    return form;
  }
}
