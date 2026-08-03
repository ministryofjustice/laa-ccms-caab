package uk.gov.laa.ccms.caab.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.format.DateTimeParseException;
import java.util.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DateUtilsTest {

  @ParameterizedTest
  @CsvSource({"1/1/2000, 01/01/2000", "01/01/2000, 01/01/2000", "7/5/2026, 07/05/2026"})
  void normaliseComponentDate_acceptsSingleAndDoubleDigitInput(
      String inputDate, String expectedNormalisedDate) {
    assertEquals(expectedNormalisedDate, DateUtils.normaliseComponentDate(inputDate));
  }

  @ParameterizedTest
  @CsvSource({"1/1/2000", "01/01/2000", "7/5/2026", "07/05/2026"})
  void convertToDateString_acceptsSingleAndDoubleDigitInput(String inputDate) {
    assertEquals(
        DateUtils.convertToDateString(DateUtils.normaliseComponentDate(inputDate)),
        DateUtils.convertToDateString(inputDate));
  }

  @ParameterizedTest
  @CsvSource({"1/1/2000, 01/01/2000", "01/01/2000, 01/01/2000"})
  void convertToDate_roundTripsToNormalisedComponentDate(String inputDate, String expectedDate) {
    Date parsedDate = DateUtils.convertToDate(inputDate);
    assertEquals(expectedDate, DateUtils.convertToComponentDate(parsedDate));
  }

  @ParameterizedTest
  @CsvSource({"31/02/2024", "99/99/9999", "invalid"})
  void normaliseComponentDate_rejectsInvalidDates(String inputDate) {
    assertThrows(DateTimeParseException.class, () -> DateUtils.normaliseComponentDate(inputDate));
  }

  @ParameterizedTest
  @CsvSource({"1/1/2000, 01/01/2000", "01/01/2000, 01/01/2000", "invalid, invalid"})
  void normaliseComponentDateIfValid_preservesInvalidAndNormalisesValid(
      String inputDate, String expectedValue) {
    assertEquals(expectedValue, DateUtils.normaliseComponentDateIfValid(inputDate));
  }
}
