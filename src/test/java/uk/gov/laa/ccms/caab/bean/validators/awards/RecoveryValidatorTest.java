package uk.gov.laa.ccms.caab.bean.validators.awards;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.gov.laa.ccms.caab.bean.award.RecoveryFormData;
import uk.gov.laa.ccms.caab.util.DateUtils;

class RecoveryValidatorTest {
  private final RecoveryValidator validator = new RecoveryValidator();
  private static final String[] AMOUNTS = {
    "solicitorRecoveryAmount",
    "solicitorAmountPaidToLsc",
    "courtRecoveryAmount",
    "courtAmountPaidToLsc",
    "clientRecoveryAmount",
    "clientAmountPaidToLsc",
    "offeredAmount"
  };
  private static final String[] DATES = {
    "solicitorRecoveryDate", "courtRecoveryDate", "clientRecoveryDate"
  };

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "0", "0.00", "1.2", "99999999.99", ".50", "12."})
  void acceptsOptionalLegacyMonetaryValuesForEveryAmount(String value) {
    final var form = new RecoveryFormData();
    final var wrapper = new BeanWrapperImpl(form);
    for (String field : AMOUNTS) {
      wrapper.setPropertyValue(field, value);
    }
    assertThat(validate(form).hasErrors()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"-1", "100000000", "1.234", "1,000", "£10", "abc", ".", "1e2"})
  void rejectsInvalidAmountsInEveryField(String value) {
    final var form = new RecoveryFormData();
    final var wrapper = new BeanWrapperImpl(form);
    for (String field : AMOUNTS) {
      wrapper.setPropertyValue(field, value);
    }
    final var errors = validate(form);
    for (String field : AMOUNTS) {
      assertThat(errors.hasFieldErrors(field)).as(field).isTrue();
    }
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "14/12/1901", "29/02/2024", "1/1/2000"})
  void acceptsOptionalValidReceiptDates(String date) {
    final var form = new RecoveryFormData();
    for (String field : DATES) {
      new BeanWrapperImpl(form).setPropertyValue(field, date);
    }
    assertThat(validate(form).hasErrors()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "31/12/1899",
        "01/01/1900",
        "12/12/1901",
        "13/12/1901",
        "29/02/2025",
        "31/04/2026",
        "2026-01-01",
        "date"
      })
  void rejectsInvalidReceiptDates(String date) {
    final var form = new RecoveryFormData();
    for (String field : DATES) {
      new BeanWrapperImpl(form).setPropertyValue(field, date);
    }
    final var errors = validate(form);
    for (String field : DATES) {
      assertThat(errors.hasFieldErrors(field)).as(field).isTrue();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"solicitorRecoveryDate", "courtRecoveryDate", "clientRecoveryDate"})
  void acceptsTodayButRejectsTomorrow(String field) {
    final var form = new RecoveryFormData();
    final var wrapper = new BeanWrapperImpl(form);
    wrapper.setPropertyValue(field, DateUtils.convertToComponentDate(LocalDate.now()));
    assertThat(validate(form).hasErrors()).isFalse();
    wrapper.setPropertyValue(field, DateUtils.convertToComponentDate(LocalDate.now().plusDays(1)));
    assertThat(validate(form).hasFieldErrors(field)).isTrue();
  }

  @Test
  void validatesOfferLengthAndLegacyCharactersWithoutMakingItRequired() {
    final var form = new RecoveryFormData();
    form.setOfferDetails("a".repeat(950));
    assertThat(validate(form).hasErrors()).isFalse();
    form.setOfferDetails("a".repeat(951));
    assertThat(validate(form).hasFieldErrors("offerDetails")).isTrue();
    form.setOfferDetails("Offer <script>");
    assertThat(validate(form).hasFieldErrors("offerDetails")).isTrue();
    form.setOfferDetails("Offer £100 / €200\r\nTerms & conditions");
    assertThat(validate(form).hasErrors()).isFalse();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t", "\r\n", " \t\r\n "})
  void preservesLegacyAcceptanceOfOptionalWhitespaceOnlyOffers(String offer) {
    final var form = new RecoveryFormData();
    form.setOfferDetails(offer);
    assertThat(validate(form).hasErrors()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {" ", "\t", "\n"})
  void enforcesOfferLengthEvenWhenItContainsOnlyWhitespace(String whitespace) {
    final var form = new RecoveryFormData();
    form.setOfferDetails(whitespace.repeat(950));
    assertThat(validate(form).hasErrors()).isFalse();
    form.setOfferDetails(whitespace.repeat(951));
    final var errors = validate(form);
    assertThat(errors.getFieldErrors("offerDetails"))
        .extracting(error -> error.getCode())
        .containsExactly("length.exceeds.max");
  }

  @Test
  void doesNotInventCrossFieldDependencies() {
    final var form = new RecoveryFormData();
    form.setSolicitorRecoveryAmount("99999999.99");
    form.setSolicitorAmountPaidToLsc("99999999.99");
    form.setCourtRecoveryAmount("0");
    form.setCourtAmountPaidToLsc("100");
    form.setClientRecoveryDate("14/12/1901");
    assertThat(validate(form).hasErrors()).isFalse();
  }

  @Test
  void reportsTheSharedLowerBoundForEveryReceiptDate() {
    final var form = new RecoveryFormData();
    for (String field : DATES) {
      new BeanWrapperImpl(form).setPropertyValue(field, "13/12/1901");
    }
    final var errors = validate(form);
    for (String field : DATES) {
      assertThat(errors.getFieldError(field).getCode()).isEqualTo("invalid.input");
      assertThat(errors.getFieldError(field).getDefaultMessage())
          .contains("date after 13 December 1901");
    }
  }

  @Test
  void acceptsMaximumTotalsAndNegativeUnrecoveredBalance() {
    final var errors = new BeanPropertyBindingResult(new RecoveryFormData(), "recovery");
    validator.validateTotals(
        BigDecimal.ZERO, new BigDecimal("99999999.99"), new BigDecimal("-99999999.99"), errors);
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void rejectsEveryTotalOutsideTheApiRange() {
    final var errors = new BeanPropertyBindingResult(new RecoveryFormData(), "recovery");
    validator.validateTotals(
        new BigDecimal("100000000.00"),
        new BigDecimal("100000000.00"),
        new BigDecimal("-100000000.00"),
        errors);
    assertThat(errors.getAllErrors())
        .extracting(error -> error.getCode())
        .containsExactly(
            "recovery.awardAmount.exceedsMax",
            "recovery.recoveredAmount.exceedsMax",
            "recovery.unrecoveredAmount.exceedsMax");
  }

  private BeanPropertyBindingResult validate(RecoveryFormData form) {
    final var errors = new BeanPropertyBindingResult(form, "recovery");
    validator.validate(form, errors);
    return errors;
  }
}
