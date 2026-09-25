package uk.gov.laa.ccms.caab.bean.validators.awards;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.gov.laa.ccms.caab.bean.award.CostAwardFormData;

class CostAwardValidatorTest {

  private final CostAwardValidator validator = new CostAwardValidator();

  @Test
  void acceptsValidCostAwardFields() {
    final CostAwardFormData form = validForm();
    final BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "costAward");

    validator.validate(form, errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void rejectsMissingRequiredFields() {
    final CostAwardFormData form = new CostAwardFormData();
    final BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "costAward");

    validator.validate(form, errors);

    assertThat(errors.getFieldErrors())
        .extracting("field")
        .contains(
            "awardType",
            "description",
            "awardCode",
            "dateOfOrder",
            "courtAssessmentStatus",
            "awardedBy");
  }

  @Test
  void rejectsInvalidDatesAmountsLengthsAndAddressCharacters() {
    final CostAwardFormData form = validForm();
    form.setDateOfOrder("31/02/2025");
    form.setInterestStartDate(
        LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    form.setOrderServedDate(
        LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    form.setLaaFundedLegalCosts("12.345");
    form.setOtherPreCertificateCosts("100000000.00");
    form.setLaaRate("text");
    form.setMarketRate("12.345");
    form.setInterestRate("8.123");
    form.setAddressLine1("Address  with double spaces");
    form.setAddressLine2("x".repeat(36));
    form.setAddressLine3("Invalid@character");
    form.setOtherDetails("x".repeat(951));

    final BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "costAward");

    validator.validate(form, errors);

    assertThat(errors.getFieldErrors())
        .extracting("field")
        .contains(
            "dateOfOrder",
            "interestStartDate",
            "orderServedDate",
            "laaFundedLegalCosts",
            "otherPreCertificateCosts",
            "laaRate",
            "marketRate",
            "interestRate",
            "addressLine1",
            "addressLine2",
            "addressLine3",
            "otherDetails");
  }

  private CostAwardFormData validForm() {
    final CostAwardFormData form = new CostAwardFormData();
    form.setAwardType("COST");
    form.setDescription("Cost");
    form.setAwardCode("COST_AGR");
    form.setDateOfOrder("01/01/2025");
    form.setCourtAssessmentStatus("ASSESSED");
    form.setLaaFundedLegalCosts("1234.56");
    form.setOtherPreCertificateCosts("12.34");
    form.setLaaRate("45.67");
    form.setMarketRate("89.01");
    form.setAwardedBy("COURT");
    form.setInterestRate("8.5");
    form.setInterestStartDate("02/01/2025");
    form.setOrderServedDate("03/01/2025");
    form.setAddressLine1("1 High Street");
    form.setAddressLine2("London");
    form.setAddressLine3("England");
    form.setOtherDetails("Other details");
    return form;
  }
}
