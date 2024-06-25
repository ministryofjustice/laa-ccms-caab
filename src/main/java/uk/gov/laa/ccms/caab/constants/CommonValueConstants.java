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
   * Type used to retrieve the case address option values for correspondence address details.
   */
  public static final String COMMON_VALUE_CASE_ADDRESS_OPTION = "XXCCMS_CASE_ADDRESS_OPTION";

  /**
   * Type used to retrieve the ethnic origin values for equal opportunity monitoring details.
   */
  public static final String COMMON_VALUE_ETHNIC_ORIGIN = "XXCCMS_ETHNICITY";

  /**
   * Type used to retrieve the disability values for equal opportunity monitoring details.
   */
  public static final String COMMON_VALUE_DISABILITY = "XXCCMS_DISABILITY_TYPES";

  /**
   * Type used to retrieve the level of service values.
   */
  public static final String COMMON_VALUE_LEVEL_OF_SERVICE = "XXCCMS_LEVEL_OF_SERVICE";

  /**
   * Type used to retrieve the matter type values.
   */
  public static final String COMMON_VALUE_MATTER_TYPES = "XXCCMS_MATTER_TYPE";

  /**
   * Type used to retrieve the client involvement type values.
   */
  public static final String COMMON_VALUE_CLIENT_INVOLVEMENT_TYPES = "XXCCMS_CLIENT_INVOLVE_TYPE";

  /**
   * Type used to retrieve the scope limitation values.
   */
  public static final String COMMON_VALUE_SCOPE_LIMITATIONS = "XXCCMS_SCOPE_LIMITATION";

  /**
   * Type used to retrieve the proceeding status values.
   */
  public static final String COMMON_VALUE_PROCEEDING_STATUS = "XXCCMS_PROCEEDING_DISP_STATUS";

  /**
   * Type used to retrieve the courts values.
   */
  public static final String COMMON_VALUE_COURTS = "XXCCMS_COURTS";

  /**
   * Type used to retrieve the Notification Types for the Notification search.
   */
  public static final String COMMON_VALUE_NOTIFICATION_TYPE = "XXCCMS_NOTIFICATION_TYPES";

  /**
   * Type used to retrieve the Case link types for an application's linked cases.
   */
  public static final String COMMON_VALUE_CASE_LINK_TYPE = "XXCCMS_CASE_LINK_TYPES";

  /**
   * Type used to retrieve the Case/application status.
   */
  public static final String COMMON_VALUE_APPLICATION_STATUS = "XXCCMS_APP_CASE_STATUS";

  /**
   * Type used to retrieve the Relationship to Client values.
   */
  public static final String COMMON_VALUE_RELATIONSHIP_TO_CLIENT = "XXCCMS_REL_TO_CLIENT";

  /**
   * Type used to retrieve the Organisation Type values.
   */
  public static final String COMMON_VALUE_ORGANISATION_TYPES = "XXCCMS_ORGANISATION_TYPE";

  /**
   * Type used to retrieve the Proceeding order type.
   */
  public static final String COMMON_VALUE_PROCEEDING_ORDER_TYPE = "XXCCMS_PROCEEDING_ORDER_TYPE";

  /**
   * Type used to retrieve the Progress status types.
   */
  public static final String COMMON_VALUE_PROGRESS_STATUS_TYPES = "XXCCMS_OPA_PROGRESS_STATUS";

  /**
   * Type used to retrieve OPA Evidence Document Types.
   */
  public static final String COMMON_VALUE_OPA_EVIDENCE_ITEMS = "XXCCMS_OPA_EVIDENCE_ITEMS";

  /**
   * Type used to retrieve Prior Authority Evidence Document Types.
   */
  public static final String COMMON_VALUE_PRIOR_AUTHORITY_EVIDENCE_ITEMS =
      "XXCCMS_PA_EVIDENCE_ITEMS";

  /**
   * Type used to retrieve Document Types.
   */
  public static final String COMMON_VALUE_DOCUMENT_TYPES = "XXCCMS_DOCUMENT_TYPES";

  /**
   * Lookup code for Case Outcome document types.
   */
  public static final String COMMON_VALUE_OUTCOME_DOCUMENT_CODE = "OUT_EV";
}
