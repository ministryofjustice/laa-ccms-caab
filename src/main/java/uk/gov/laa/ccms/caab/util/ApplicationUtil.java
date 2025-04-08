package uk.gov.laa.ccms.caab.util;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE;

import java.util.Date;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;

/**
 * Utility class for handling application details and related operations.
 */
public class ApplicationUtil {

  /**
   * Gets the application type or amendment type for Assessment input.
   *
   * @param application the application details
   * @return "APP_TYPE_SUBSTANTIVE" if the application type is "EXCEPTIONAL_CASE_FUNDING",
   *         otherwise the application type ID
   */
  public static String getAppAmendTypeAssessmentInput(final ApplicationDetail application) {
    if (application.getApplicationType().getId()
        .equalsIgnoreCase(APP_TYPE_EXCEPTIONAL_CASE_FUNDING)) {
      return APP_TYPE_SUBSTANTIVE;
    } else {
      return application.getApplicationType().getId();
    }
  }

  /**
   * Checks if the provider has a contract under devolved powers.
   *
   * @param application the application details
   * @return true if the devolved powers contract flag starts with "yes", otherwise false
   */
  public static boolean getProviderHasContractAssessmentInput(
      final ApplicationDetail application) {
    final String devolvedPowersContractFlag =
        application.getApplicationType().getDevolvedPowers().getContractFlag();

    if (devolvedPowersContractFlag != null) {
      return devolvedPowersContractFlag.toLowerCase().startsWith("yes");
    }
    return false;
  }

  /**
   * Checks if the application type is "EXCEPTIONAL_CASE_FUNDING".
   *
   * @param application the application details
   * @return true if the application type is "EXCEPTIONAL_CASE_FUNDING", otherwise false
   */
  public static boolean getEcfFlagAssessmentInput(final ApplicationDetail application) {
    return application
        .getApplicationType()
        .getId()
        .equalsIgnoreCase(APP_TYPE_EXCEPTIONAL_CASE_FUNDING);
  }

  /**
   * Checks if the LAR scope flag is set to true.
   *
   * @param application the application details
   * @return true if the LAR scope flag is not null and true, otherwise false
   */
  public static boolean getLarScopeFlagAssessmentInput(final ApplicationDetail application) {
    return application.getLarScopeFlag() != null
        && Boolean.TRUE.equals(application.getLarScopeFlag());
  }

  /**
   * Determines if the application is a new application or an amendment.
   *
   * @param application the application details
   * @return a string determining if the application is an application or an amendment
   */
  public static String getNewApplicationOrAmendment(final ApplicationDetail application) {
    if (application.getAmendment()) {
      return "AMENDMENT";
    }
    return "APPLICATION";
  }

  /**
   * Retrieves the most recent date on which any key data was modified within the given
   * application. This includes checks across proceedings and opponents involved in the
   * application.
   *
   * @param application the application to check for recent key data changes
   * @return the most recent date of modification, or null if no modifications have occurred
   */
  public static Date getDateOfLatestKeyChange(final ApplicationDetail application) {
    Date latestKeyChange = null;

    for (final ProceedingDetail proceeding : application.getProceedings()) {
      if (proceeding.getAuditTrail() != null
          && (latestKeyChange == null || latestKeyChange
          .before(proceeding.getAuditTrail().getLastSaved()))) {
        latestKeyChange = proceeding.getAuditTrail().getLastSaved();
      }

      if (proceeding.getScopeLimitations() != null && !proceeding.getScopeLimitations().isEmpty()) {
        for (final ScopeLimitationDetail scopeLimitation : proceeding.getScopeLimitations()) {
          if (scopeLimitation.getAuditTrail() != null
              && (latestKeyChange == null || latestKeyChange
              .before(scopeLimitation.getAuditTrail().getLastSaved()))) {
            latestKeyChange = scopeLimitation.getAuditTrail().getLastSaved();
          }
        }
      }

    }

    for (final OpponentDetail opponent : application.getOpponents()) {
      if (opponent.getAuditTrail() != null
          && (latestKeyChange == null || latestKeyChange
          .before(opponent.getAuditTrail().getLastSaved()))) {
        latestKeyChange = opponent.getAuditTrail().getLastSaved();
      }
    }
    return latestKeyChange;

  }


}
