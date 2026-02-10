package uk.gov.laa.ccms.caab.constants;

/** Constants for validation patterns used in the application. */
public class ValidationPatternConstants {

  /** The standard set of allowable characters. */
  public static final String STANDARD_CHARACTER_SET =
      "^[A-Za-z0-9 ~!\",#$%&\\'\\r\\n\\(\\)\\*\\+\\/:;\\.\\-=\\?\\[\\]_\\{\\}^£€]*$";

  /** Validation pattern to check for numerical values. */
  public static final String NUMERIC_PATTERN = "[0-9]+";

  // Validation pattern to check for number have a maximum of 2 Decimal Places
  public static final String MONETARY_INPUT_2DP = "[0-9]+(\\.[0-9][0-9]?)?";

  /** Validation pattern to check for currency values. */
  public static final String CURRENCY_PATTERN = "[0-9]+(.[0-9]{1,2})?";

  /** Validation pattern for an email address. */
  public static final String EMAIL_ADDRESS =
      "^[A-Za-z0-9!#$%&amp;&apos;\\*\\+\\-/=\\?^_`\\.\\{\\|\\}~]+@[A-Za-z0-9\\-\\.]+$";

  /** Validation pattern for national insurance numbers. */
  public static final String NATIONAL_INSURANCE_NUMBER_PATTERN = "^[A-Za-z]{2}[0-9]{6}[A-Za-z]{1}$";

  /** Validation pattern for home office reference numbers. */
  public static final String HOME_OFFICE_NUMBER_PATTERN = "^[A-Za-z0-9 ]*$";

  /** Validation pattern for case reference numbers. */
  public static final String CASE_REFERENCE_NUMBER_PATTERN = "^[A-Za-z0-9 /]*$";

  /** Neagtive validation pattern for case reference numbers. */
  public static final String CASE_REFERENCE_NUMBER_NEGATIVE_PATTERN = "(.*)\s\s(.*)";

  /** pattern for international postcodes. */
  public static final String INTERNATIONAL_POSTCODE = "^[A-Za-z0-9\\-\\(\\)\\/ ]*$";

  /** pattern 1 for uk postcodes. */
  private static final String UK_POSTCODE_1 = "[A-Za-z][0-9] [0-9][A-Za-z]{2}";

  /** pattern 2 for uk postcodes. */
  private static final String UK_POSTCODE_2 = "[A-Za-z][0-9]{2} [0-9][A-Za-z]{2}";

  /** pattern 3 for uk postcodes. */
  private static final String UK_POSTCODE_3 = "[A-Za-z]{2}[0-9] [0-9][A-Za-z]{2}";

  /** pattern 4 for uk postcodes. */
  private static final String UK_POSTCODE_4 = "[A-Za-z]{2}[0-9]{2} [0-9][A-Za-z]{2}";

  /** pattern 5 for uk postcodes. */
  private static final String UK_POSTCODE_5 = "[A-Za-z][0-9][A-Za-z] [0-9][A-Za-z]{2}";

  /** pattern 6 for uk postcodes. */
  private static final String UK_POSTCODE_6 = "[A-Za-z]{2}[0-9][A-Za-z] [0-9][A-Za-z]{2}";

  /** pattern for all uk postcode formats. */
  public static final String UK_POSTCODE =
      "^(("
          + UK_POSTCODE_1
          + ")|("
          + UK_POSTCODE_2
          + ")|("
          + UK_POSTCODE_3
          + ")|("
          + UK_POSTCODE_4
          + ")|("
          + UK_POSTCODE_5
          + ")|("
          + UK_POSTCODE_6
          + "))*$";

  /** Pattern for telephone numbers. */
  public static final String TELEPHONE_PATTERN = "^[0-9 \\+\\-\\(\\)]*$";

  /**
   * Pattern for double-space, also takes into account characters wrapped around the double space.
   */
  public static final String DOUBLE_SPACE = ".* {2,}.*";

  /**
   * pattern for case reference - contains alphanumerics and also allows a forward slash for legacy
   * reasons.
   */
  public static final String ALPHA_NUMERIC_SLASH_SPACE_STRING = "^[A-Za-z0-9\\s/]*$";

  /** pattern primarily for address house name or number. Alphanumerics with spaces and commas. */
  public static final String ALPHA_NUMERIC_SPACES_COMMAS = "^([A-Za-z0-9 ,])*$";

  /** pattern to test that the first character in a string is alphabetic. */
  public static final String FIRST_CHARACTER_MUST_BE_ALPHA = "^[A-Za-z].*";

  /** pattern to match what is known in provider-ui as 'characterSetA'. */
  public static final String CHARACTER_SET_A =
      "^[A-Za-z0-9 \\.,\\-\\(\\)/=!\"%&\\*;<>'\\r\\n\\+:\\?]*$";

  /**
   * pattern to match what is known in provider-ui as 'characterSetC'. Valid characters are
   * alphabetic, space, apostrophe and hyphen.
   */
  public static final String CHARACTER_SET_C = "^[A-Za-z\\'\\- ]*$";

  /**
   * pattern to match what is known in provider-ui as 'characterSetE'. Valid characters are
   * alphabetic, numeric, space, apostrophe and hyphen.
   */
  public static final String CHARACTER_SET_E = "^[A-Za-z0-9\\'\\- ]*$";

  /**
   * pattern to match what is known in provider-ui as 'characterSetF. Valid characters are A-Z a-z
   * 0-9 & ' ( ) . * - / ! # $ % , ; ? @ [ \ ] _ ` | + = > £ :
   */
  public static final String CHARACTER_SET_F =
      "^[A-Za-z0-9\\&\\'\\(\\)\\.\\*\\-/!#$%,;\\?\\@\\[\\]_+\\=\\>£:&#92;&#96;\\\\]*$";
}
