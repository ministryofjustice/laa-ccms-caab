package uk.gov.laa.ccms.caab.constants;

import java.util.Arrays;
import java.util.List;

public class ApplicationConstants {

  /*
   * The Application Type Code for Exceptional Case Funding
   */
  public static final String APP_TYPE_EXCEPTIONAL_CASE_FUNDING = "ECF";

  public static final String APP_TYPE_SUBSTANTIVE = "SUB";

  public static final String APP_TYPE_EMERGENCY = "EMER";

  public static final String APP_TYPE_EMERGENCY_DEVOLVED_POWERS = "DP";

  public static final String APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS = "SUBDP";

  public static final List<String> EXCLUDED_APPLICATION_TYPE_CODES = Arrays.asList(APP_TYPE_EMERGENCY_DEVOLVED_POWERS,
          APP_TYPE_EXCEPTIONAL_CASE_FUNDING, APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS);
}
