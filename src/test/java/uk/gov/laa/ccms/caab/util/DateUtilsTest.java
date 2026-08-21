package uk.gov.laa.ccms.caab.util;

import static org.junit.jupiter.api.Assertions.*;

import java.time.format.DateTimeParseException;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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

  @Test
  @DisplayName("sanitiseDateInput - accepts valid dates")
  void sanitiseDateInput_acceptsValidDates() {
    assertEquals("03/08/2026", DateUtils.sanitiseDateInput("03/08/2026"));
    assertEquals("1/1/2000", DateUtils.sanitiseDateInput("1/1/2000"));
    assertEquals("31/12/2099", DateUtils.sanitiseDateInput("31/12/2099"));
  }

  @ParameterizedTest
  @CsvSource({
    "03/08/2026\", 03/08/2026",
    "03/08/2026', 03/08/2026",
    "03-08-2026, 03082026",
    "03 08 2026, 03082026",
    "03/08/2026 , 03/08/2026",
    "03/08/2026!!!!, 03/08/2026"
  })
  @DisplayName("sanitiseDateInput - removes invalid characters")
  void sanitiseDateInput_removesInvalidCharacters(String input, String expected) {
    assertEquals(expected, DateUtils.sanitiseDateInput(input));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "   "})
  @DisplayName("sanitiseDateInput - blank")
  void sanitiseDateInput_preservesBlank(String input) {
    assertEquals(input, DateUtils.sanitiseDateInput(input));
  }

  @Test
  @DisplayName("sanitiseDateInput - handles null")
  void sanitiseDateInput_handlesNull() {
    assertNull(DateUtils.sanitiseDateInput(null));
  }

  @Test
  @DisplayName("hasInvalidDateCharacters - returns false for valid dates")
  void hasInvalidDateCharacters_returnsFalseForValidDates() {
    assertFalse(DateUtils.hasInvalidDateCharacters("03/08/2026"));
    assertFalse(DateUtils.hasInvalidDateCharacters("1/1/2000"));
    assertFalse(DateUtils.hasInvalidDateCharacters("31/12/2099"));
  }

  @ParameterizedTest
  @CsvSource({
    "03/08/2026\", true",
    "03/08/2026', true",
    "03-08-2026, true",
    "03 08 2026, true",
    "03/08/2026!, true",
    "03/08/2026@#$, true",
    "03/08/2026, false"
  })
  @DisplayName("hasInvalidDateCharacters - detects invalid characters")
  void hasInvalidDateCharacters_detectsInvalidCharacters(String input, boolean expected) {
    assertEquals(expected, DateUtils.hasInvalidDateCharacters(input));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "   "})
  @DisplayName("hasInvalidDateCharacters - returns false for blank")
  void hasInvalidDateCharacters_returnsFalseForBlank(String input) {
    assertFalse(DateUtils.hasInvalidDateCharacters(input));
  }

  @Test
  @DisplayName("hasInvalidDateCharacters - returns false for null")
  void hasInvalidDateCharacters_returnsFalseForNull() {
    assertFalse(DateUtils.hasInvalidDateCharacters(null));
  }
}
