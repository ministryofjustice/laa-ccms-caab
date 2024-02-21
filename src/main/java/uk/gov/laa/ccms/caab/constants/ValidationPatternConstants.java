package uk.gov.laa.ccms.caab.constants;

import lombok.Data;

/**
 * Constants for validation patterns used in the application.
 */
@Data
public class ValidationPatternConstants {

  /**
   * Validation pattern to check for numerical values.
   */
  public static final String NUMERIC_PATTERN = "[0-9]+";

  /**
   * Validation pattern to check for numerical values.
   */
  public static final String CURRENCY_PATTERN = "[0-9]+(.[0-9]{1,2})?";

  /**
   * Validation pattern for national insurance numbers.
   */
  public static final String NATIONAL_INSURANCE_NUMBER_PATTERN = "^[A-Za-z]{2}[0-9]{6}[A-Za-z]{1}$";

  /**
   * Validation pattern for home office reference numbers.
   */
  public static final String HOME_OFFICE_NUMBER_PATTERN = "^[A-Za-z0-9\s]*$";

  /**
   * Validation pattern for case reference numbers.
   */
  public static final String CASE_REFERENCE_NUMBER_PATTERN = "^[A-Za-z0-9\s/]*$";

  /**
   * Neagtive validation pattern for case reference numbers.
   */
  public static final String CASE_REFERENCE_NUMBER_NEGATIVE_PATTERN = "(.*)\s\s(.*)";

  /**
   * pattern for international postcodes.
   */
  public static final String INTERNATIONAL_POSTCODE = "^[A-Za-z0-9\\-\\(\\)\\/ ]*$";

  /**
   * pattern 1 for uk postcodes.
   */
  private static final String UK_POSTCODE_1 = "[A-Za-z][0-9] [0-9][A-Za-z]{2}";

  /**
   * pattern 2 for uk postcodes.
   */
  private static final String UK_POSTCODE_2 = "[A-Za-z][0-9]{2} [0-9][A-Za-z]{2}";

  /**
   * pattern 3 for uk postcodes.
   */
  private static final String UK_POSTCODE_3 = "[A-Za-z]{2}[0-9] [0-9][A-Za-z]{2}";

  /**
   * pattern 4 for uk postcodes.
   */
  private static final String UK_POSTCODE_4 = "[A-Za-z]{2}[0-9]{2} [0-9][A-Za-z]{2}";

  /**
   * pattern 5 for uk postcodes.
   */
  private static final String UK_POSTCODE_5 = "[A-Za-z][0-9][A-Za-z] [0-9][A-Za-z]{2}";

  /**
   * pattern 6 for uk postcodes.
   */
  private static final String UK_POSTCODE_6 = "[A-Za-z]{2}[0-9][A-Za-z] [0-9][A-Za-z]{2}";

  /**
   * pattern for all uk postcode formats.
   */
  public static final String UK_POSTCODE =
      "^((" + UK_POSTCODE_1
      + ")|(" + UK_POSTCODE_2
      + ")|(" + UK_POSTCODE_3
      + ")|(" + UK_POSTCODE_4
      + ")|(" + UK_POSTCODE_5
      + ")|(" + UK_POSTCODE_6 + "))*$";

  /**
   * pattern for double-space.
   */
  public static final String DOUBLE_SPACE = "[ ]{2,}";

  /**
   * pattern for case reference - contains alphanumerics and also allows a forward slash for
   *     legacy reasons.
   */
  public static final String ALPHA_NUMERIC_SLASH_SPACE_STRING = "^[A-Za-z0-9\\s/]*$";

  /**
   * pattern to test that the first character in a string is alphabetic.
   */
  public static final String FIRST_CHARACTER_MUST_BE_ALPHA = "^[A-Za-z].*";

  /**
   * pattern to match what is known in provider-ui as 'characterSetC'.
   * Valid characters are alphabetic, space, apostrophe and hyphen.
   */
  public static final String CHARACTER_SET_C = "^[A-Za-z\\'\\- ]*$";

  /**
   * pattern to match what is known in provider-ui as 'characterSetF.
   * Valid characters are A-Z a-z 0-9 & ' ( ) . * - / ! # $ % , ; ? @ [ \ ] _ ` | + = > £ :
   */
  public static final String CHARACTER_SET_F
      = "^[A-Za-z0-9\\&\\'\\(\\)\\.\\*\\-/!#$%,;\\?\\@\\[\\]_+\\=\\>£:&#92;&#96;\\\\]*$";
}
