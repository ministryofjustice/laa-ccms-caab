package uk.gov.laa.ccms.caab.constants;

/** Constants for character limitation sizes. */
public final class CharacterLimitationConstants {

  /** the default size for field lengths. */
  public static final int DEFAULT_CHARACTER_SIZE = 35;

  /** the size for postcode. */
  public static final int POSTCODE_CHARACTER_SIZE = 15;

  /** the size for telephone numbers and fax. */
  public static final int TELEPHONE_NUMBER_CHARACTER_SIZE = 15;

  /** the size for organisation name. */
  public static final int ORGANISATION_NAME_CHARACTER_SIZE = 360;

  /** the size for address line 1. */
  public static final int ADDRESS_LINE1_CHARACTER_SIZE = 70;

  /** the size for email address. */
  public static final int EMAIL_ADDRESS_CHARACTER_SIZE = 200;

  /** the size for other information. */
  public static final int OTHER_INFORMATION_CHARACTER_SIZE = 2000;

  /** the size for national insurance number. */
  public static final int NATIONAL_INSURANCE_NUMBER_CHARACTER_SIZE = 9;

  /** the size for special considerations. */
  public static final int SPECIAL_CONSIDERATIONS_CHARACTER_SIZE = 2000;

  /** the size for justification. */
  public static final int JUSTIFICATION_CHARACTER_SIZE = 200;

  private CharacterLimitationConstants() {}
}
