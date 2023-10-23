package uk.gov.laa.ccms.caab.constants;

import lombok.Data;

/**
 * Constants for common value types used in the application.
 */
@Data
public class CommonValueConstants {

  /**
   * Type used to retrieve the application types through getCommonValues in DataService.
   */
  public static final String COMMON_VALUE_APPLICATION_TYPE = "XXCCMS_APP_AMEND_TYPES";

  /**
   * Type used to retrieve the category of law through getCommonValues in DataService.
   */
  public static final String COMMON_VALUE_CATEGORY_OF_LAW = "XXCCMS_CATEGORY_OF_LAW";
  /**
   * Type used to retrieve the gender values through getCommonValues in DataService.
   */
  public static final String COMMON_VALUE_GENDER = "HZ_GENDER";

  /**
   * Type used to retrieve the unique identifier values through getCommonValues in DataService.
   */
  public static final String COMMON_VALUE_UNIQUE_IDENTIFIER_TYPE = "XXCCMS_UNIQUE_ID_TYPE";

  /**
   * Type used to retrieve the title values for contact details.
   */
  public static final String COMMON_VALUE_CONTACT_TITLE = "CONTACT_TITLE";

  /**
   * Type used to retrieve the marital values for contact details.
   */
  public static final String COMMON_VALUE_MARITAL_STATUS = "MARITAL_STATUS";

  /**
   * Type used to retrieve the correspondence method values for contact details.
   */
  public static final String COMMON_VALUE_CORRESPONDENCE_METHOD = "XXCCMS_PREF_CORRESPONDENCE";

  /**
   * Type used to retrieve the correspondence language values for contact details.
   */
  public static final String COMMON_VALUE_CORRESPONDENCE_LANGUAGE = "XXCCMS_PREF_LANGUAGE";

  /**
   * Type used to retrieve the ethnic origin values for equal opportunity monitoring details.
   */
  public static final String COMMON_VALUE_ETHNIC_ORIGIN = "XXCCMS_ETHNICITY";

  /**
   * Type used to retrieve the disability values for equal opportunity monitoring details.
   */
  public static final String COMMON_VALUE_DISABILITY = "XXCCMS_DISABILITY_TYPES";

  /**
   * Type used to retrieve the Notification Types for the Notification search.
   */
  public static final String COMMON_VALUE_NOTIFICATION_TYPE = "XXCCMS_NOTIFICATION_TYPES";
}
