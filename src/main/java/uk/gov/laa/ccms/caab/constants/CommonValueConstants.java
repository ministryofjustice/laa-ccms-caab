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
}
