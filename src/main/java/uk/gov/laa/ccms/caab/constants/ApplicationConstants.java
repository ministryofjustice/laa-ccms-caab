package uk.gov.laa.ccms.caab.constants;

import java.util.Arrays;
import java.util.List;
import lombok.Data;

/**
 * Constants for application-related values used in the application.
 */
@Data
public class ApplicationConstants {

  /**
   * The Application Type Code for Exceptional Case Funding.
   */
  public static final String APP_TYPE_EXCEPTIONAL_CASE_FUNDING = "ECF";

  /**
   * The Application Type Display Value for Exceptional Case Funding.
   */
  public static final String APP_TYPE_EXCEPTIONAL_CASE_FUNDING_DISPLAY = "Exceptional Case Funding";

  /**
   * The Application Type Code for Substantive.
   */
  public static final String APP_TYPE_SUBSTANTIVE = "SUB";

  /**
   * The Application Type Display Value for Substantive.
   */
  public static final String APP_TYPE_SUBSTANTIVE_DISPLAY = "Substantive";

  /**
   * The Application Type Code for Emergency.
   */
  public static final String APP_TYPE_EMERGENCY = "EMER";

  /**
   * The Application Type Display Value for Emergency.
   */
  public static final String APP_TYPE_EMERGENCY_DISPLAY = "Emergency";

  /**
   * The Application Type Code for Emergency Delegated Functions.
   */
  public static final String APP_TYPE_EMERGENCY_DEVOLVED_POWERS = "DP";

  /**
   * The Application Type Display Value for Emergency Delegated Functions.
   */
  public static final String APP_TYPE_EMERGENCY_DEVOLVED_POWERS_DISPLAY =
          "Emergency Delegated Functions";


  /**
   * The Application Type Code for Substantive with Delegated Function.
   */
  public static final String APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS = "SUBDP";

  /**
   * The Application Type Display Value for Substantive with Delegated Function.
   */
  public static final String APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS_DISPLAY =
          "Substantive Delegated Functions";


  /**
   * The Application Type Code list for codes to be excluded from application type screen's
   * dropdown.
   */
  public static final List<String> EXCLUDED_APPLICATION_TYPE_CODES =
          Arrays.asList(
                  APP_TYPE_EMERGENCY_DEVOLVED_POWERS,
                  APP_TYPE_EXCEPTIONAL_CASE_FUNDING,
                  APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS);

  /**
   * The actual value for the status of an unsubmitted application.
   */
  public static final String STATUS_UNSUBMITTED_ACTUAL_VALUE = "UNSUBMITTED";

  /**
   * The display value for the status of an unsubmitted application.
   */
  public static final String STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY = "Unsubmitted";

  /**
   * The status of a draft domain object (used for, for example, proceedings and bills).
   */
  public static final String STATUS_DRAFT = "Draft";

  /**
   * Display value for a Proceeding at status 'outcome'.
   */
  public static final String PROCEEDING_STATUS_OUTCOME_DISPLAY = "Outcome";

  /**
   * Display value for a Proceeding at status 'submitted'.
   */
  public static final String PROCEEDING_STATUS_SUBMITTED_DISPLAY = "Submitted";

  /**
   * Display value for a Proceeding at status 'added'.
   */
  public static final String PROCEEDING_STATUS_ADDED_DISPLAY = "Added";

  /**
   * Display value for a Proceeding at status 'unchanged'.
   */
  public static final String PROCEEDING_STATUS_UNCHANGED_DISPLAY = "Unchanged";

  /**
   * Display value for a Proceeding at status 'updated'.
   */
  public static final String PROCEEDING_STATUS_UPDATED_DISPLAY = "Updated";

  /**
   * Type value for an LOV Reference Data Item
   */
  public static final String REFERENCE_DATA_ITEM_TYPE_LOV = "LOV";

  /**
   * Type value for an AMOUNT Reference Data Item
   */
  public static final String REFERENCE_DATA_ITEM_TYPE_AMOUNT = "AMT";

  /**
   * Cost Award Type.
   */
  public static final String AWARD_TYPE_COST = "COST";

  /**
   * Description for Cost Award Type.
   */
  public static final String AWARD_TYPE_COST_DESCRIPTION = "Cost";

  /**
   * Financial Award Type.
   */
  public static final String AWARD_TYPE_FINANCIAL = "DAMAGE";

  /**
   * Description for Cost Award Type.
   */
  public static final String AWARD_TYPE_FINANCIAL_DESCRIPTION = "Damage";

  /**
   * Land Award Type.
   */
  public static final String AWARD_TYPE_LAND = "LAND";

  /**
   * Other Asset Award Type.
   */
  public static final String AWARD_TYPE_OTHER_ASSET = "ASSET";

}
