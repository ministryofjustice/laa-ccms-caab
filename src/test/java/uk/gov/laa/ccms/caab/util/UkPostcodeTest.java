package uk.gov.laa.ccms.caab.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class UkPostcodeTest {

  @ParameterizedTest
  @CsvSource({
    "sa58ab, 'SA5 8AB'",
    "SA58AB, 'SA5 8AB'",
    "' SA5 8AB ', 'SA5 8AB'",
    "gir0aa, 'GIR 0AA'"
  })
  void normalizeFormatsFullPostcodes(String input, String expected) {
    assertEquals(expected, UkPostcode.normalize(input));
  }

  @ParameterizedTest
  @ValueSource(strings = {"sa58ab", "SA58AB", "SA5 8AB", "GIR 0AA"})
  void isValidAcceptsFullPostcodesWithOrWithoutSpace(String postcode) {
    assertTrue(UkPostcode.isValid(postcode));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "SA5", "not a postcode"})
  void isValidRejectsIncompleteOrInvalidPostcodes(String postcode) {
    assertFalse(UkPostcode.isValid(postcode));
  }
}
