package uk.gov.laa.ccms.caab.util;

import java.util.Locale;

/** Normalisation and validation helpers for full UK postcodes. */
public final class UkPostcode {

  private static final String FULL_POSTCODE_PATTERN =
      "^(?:[A-Z][0-9] [0-9][A-Z]{2}"
          + "|[A-Z][0-9]{2} [0-9][A-Z]{2}"
          + "|[A-Z]{2}[0-9] [0-9][A-Z]{2}"
          + "|[A-Z]{2}[0-9]{2} [0-9][A-Z]{2}"
          + "|[A-Z][0-9][A-Z] [0-9][A-Z]{2}"
          + "|[A-Z]{2}[0-9][A-Z] [0-9][A-Z]{2}"
          + "|GIR 0AA)$";

  private UkPostcode() {}

  /** Formats postcode input as uppercase with one space before the inward code. */
  public static String normalize(final String postcode) {
    if (postcode == null) {
      return null;
    }

    final String compactPostcode = postcode.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    if (compactPostcode.length() <= 3) {
      return compactPostcode;
    }

    return "%s %s"
        .formatted(
            compactPostcode.substring(0, compactPostcode.length() - 3),
            compactPostcode.substring(compactPostcode.length() - 3));
  }

  /**
   * Returns whether the value is a complete UK postcode, accepting either spaced or compact input.
   */
  public static boolean isValid(final String postcode) {
    return postcode != null && normalize(postcode).matches(FULL_POSTCODE_PATTERN);
  }
}
