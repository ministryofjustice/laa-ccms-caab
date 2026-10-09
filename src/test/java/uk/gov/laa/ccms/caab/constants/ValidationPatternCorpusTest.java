package uk.gov.laa.ccms.caab.constants;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.ADDRESS_CHARACTER_SET;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.ALPHA_NUMERIC_SLASH_SPACE_STRING;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.ALPHA_NUMERIC_SPACES_COMMAS;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CASE_REFERENCE_NUMBER_PATTERN;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_A;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_C;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_E;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_F;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.EMAIL_ADDRESS;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.HOME_OFFICE_NUMBER_PATTERN;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.INTERNATIONAL_POSTCODE;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.STANDARD_CHARACTER_SET;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.TELEPHONE_PATTERN;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * One payload corpus, applied to every character class at once.
 *
 * <p>The classes were previously tested only indirectly, through whichever validator happened to
 * use them, so widening one was invisible until someone read the regex. Adding a class here without
 * a deliberate exemption is what makes rule R3 - no class admits an angle bracket - enforceable.
 */
class ValidationPatternCorpusTest {

  /** Markup-injection payloads every character class is expected to reject. */
  private static final List<String> MARKUP_PAYLOADS =
      List.of(
          "<script>alert(1)</script>",
          "'><img src=x onerror=alert(1)>",
          "<svg/onload=alert(1)>",
          "<",
          ">");

  /** The classes that carry the baseline. Keyed by name so a failure says which one broke. */
  private static final Map<String, String> STRICT_CHARACTER_SETS =
      new LinkedHashMap<>(
          Map.ofEntries(
              Map.entry("STANDARD_CHARACTER_SET", STANDARD_CHARACTER_SET),
              Map.entry("CHARACTER_SET_A", CHARACTER_SET_A),
              Map.entry("CHARACTER_SET_C", CHARACTER_SET_C),
              Map.entry("CHARACTER_SET_E", CHARACTER_SET_E),
              Map.entry("CHARACTER_SET_F", CHARACTER_SET_F),
              Map.entry("ADDRESS_CHARACTER_SET", ADDRESS_CHARACTER_SET),
              Map.entry("ALPHA_NUMERIC_SLASH_SPACE_STRING", ALPHA_NUMERIC_SLASH_SPACE_STRING),
              Map.entry("ALPHA_NUMERIC_SPACES_COMMAS", ALPHA_NUMERIC_SPACES_COMMAS),
              Map.entry("HOME_OFFICE_NUMBER_PATTERN", HOME_OFFICE_NUMBER_PATTERN),
              Map.entry("CASE_REFERENCE_NUMBER_PATTERN", CASE_REFERENCE_NUMBER_PATTERN),
              Map.entry("INTERNATIONAL_POSTCODE", INTERNATIONAL_POSTCODE),
              Map.entry("TELEPHONE_PATTERN", TELEPHONE_PATTERN)));

  @Test
  @DisplayName("no baseline character set admits markup")
  void strictCharacterSetsRejectMarkup() {
    STRICT_CHARACTER_SETS.forEach(
        (name, pattern) ->
            MARKUP_PAYLOADS.forEach(
                payload ->
                    assertFalse(
                        payload.matches(pattern),
                        "%s accepted %s - no character set may admit an angle bracket (rule R3)"
                            .formatted(name, payload))));
  }

  @Test
  @DisplayName("email addresses cannot carry markup")
  void emailRejectsMarkup() {
    MARKUP_PAYLOADS.forEach(
        payload ->
            assertFalse(
                (payload + "@example.com").matches(EMAIL_ADDRESS),
                "EMAIL_ADDRESS accepted a local part containing " + payload));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "user@example.com",
        "first.last@sub.example.co.uk",
        "o'brien@example.com",
        "a&b@example.com"
      })
  void emailAcceptsLegitimateAddresses(final String address) {
    assertTrue(address.matches(EMAIL_ADDRESS), "rejected a valid address: " + address);
  }

  @ParameterizedTest
  @ValueSource(strings = {"x@y", "bad@no-tld", "no-at-sign.com", "a;b@example.com"})
  void emailRejectsMalformedAddresses(final String address) {
    assertFalse(address.matches(EMAIL_ADDRESS), "accepted an invalid address: " + address);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "O'Brien",
        "Smith-Jones",
        "21-27, St Pauls Street",
        "St Paul's Chambers",
        "Flat 3/1"
      })
  @DisplayName("real CCMS values are not caught by the baseline sets that should accept them")
  void addressSetAcceptsRealAddresses(final String value) {
    assertTrue(value.matches(ADDRESS_CHARACTER_SET), "ADDRESS_CHARACTER_SET rejected: " + value);
  }
}
