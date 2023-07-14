package uk.gov.laa.ccms.caab.constants;

import java.util.Arrays;
import java.util.List;

public class ApplicationConstants {

  /*
   * The Application Type Code for Exceptional Case Funding
   */
  public static final String APP_TYPE_EXCEPTIONAL_CASE_FUNDING = "ECF";

  /*
   * The Application Type Code for Substantive
   */
  public static final String APP_TYPE_SUBSTANTIVE = "SUB";

  /*
   * The Application Type Code for Emergency
   */
  public static final String APP_TYPE_EMERGENCY = "EMER";

  /*
   * The Application Type Code for Emergency Delegated Functions
   */
  public static final String APP_TYPE_EMERGENCY_DEVOLVED_POWERS = "DP";

  /*
   * The Application Type Code for Substantive with Delegated Function
   */
  public static final String APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS = "SUBDP";

  /*
   * The Application Type Code list for codes to be excluded from application type screen's dropdown
   */
  public static final List<String> EXCLUDED_APPLICATION_TYPE_CODES = Arrays.asList(APP_TYPE_EMERGENCY_DEVOLVED_POWERS,
          APP_TYPE_EXCEPTIONAL_CASE_FUNDING, APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS);
}
