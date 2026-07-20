package uk.gov.laa.ccms.caab.util;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE;

import java.util.Date;
import java.util.Optional;
import org.springframework.lang.Nullable;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.DevolvedPowersDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;

/** Utility class for handling application details and related operations. */
public final class ApplicationUtil {

  /**
   * Gets the application type or amendment type for Assessment input.
   *
   * @param application the application details
   * @return "APP_TYPE_SUBSTANTIVE" if the application type is "EXCEPTIONAL_CASE_FUNDING", otherwise
   *     the application type ID
   */
  public static String getAppAmendTypeAssessmentInput(final ApplicationDetail application) {
    final String applicationTypeId = getApplicationTypeId(application);
    return APP_TYPE_EXCEPTIONAL_CASE_FUNDING.equalsIgnoreCase(applicationTypeId)
        ? APP_TYPE_SUBSTANTIVE
        : applicationTypeId;
  }

  private static String getApplicationTypeId(final ApplicationDetail application) {
    return application.getApplicationType() == null
        ? null
        : application.getApplicationType().getId();
  }

  /**
   * Checks if the provider has a contract under devolved powers.
   *
   * @param application the application details
   * @return true if the devolved powers contract flag starts with "yes", otherwise false
   */
  public static boolean getProviderHasContractAssessmentInput(final ApplicationDetail application) {
    final String devolvedPowersContractFlag =
        Optional.ofNullable(application.getApplicationType())
            .map(ApplicationType::getDevolvedPowers)
            .map(DevolvedPowersDetail::getContractFlag)
            .orElse(null);

    return devolvedPowersContractFlag != null
        && devolvedPowersContractFlag.toLowerCase().startsWith("yes");
  }

  /**
   * Checks if the application type is "EXCEPTIONAL_CASE_FUNDING".
   *
   * @param application the application details
   * @return true if the application type is "EXCEPTIONAL_CASE_FUNDING", otherwise false
   */
  public static boolean getEcfFlagAssessmentInput(final ApplicationDetail application) {
    return APP_TYPE_EXCEPTIONAL_CASE_FUNDING.equalsIgnoreCase(getApplicationTypeId(application));
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
    return Boolean.TRUE.equals(application.getAmendment()) ? "AMENDMENT" : "APPLICATION";
  }

  /**
   * Retrieves the most recent date on which any key data was modified within the given application.
   * This includes checks across proceedings and opponents involved in the application.
   *
   * @param application the application to check for recent key data changes
   * @return the most recent date of modification, or null if no modifications have occurred
   */
  public static Date getDateOfLatestKeyChange(final ApplicationDetail application) {
    Date latestKeyChange = null;

    if (application.getProceedings() != null) {
      for (final ProceedingDetail proceeding : application.getProceedings()) {
        if (proceeding.getAuditTrail() != null
            && (latestKeyChange == null
                || latestKeyChange.before(proceeding.getAuditTrail().getLastSaved()))) {
          latestKeyChange = proceeding.getAuditTrail().getLastSaved();
        }

        if (proceeding.getScopeLimitations() != null
            && !proceeding.getScopeLimitations().isEmpty()) {
          for (final ScopeLimitationDetail scopeLimitation : proceeding.getScopeLimitations()) {
            if (scopeLimitation.getAuditTrail() != null
                && (latestKeyChange == null
                    || latestKeyChange.before(scopeLimitation.getAuditTrail().getLastSaved()))) {
              latestKeyChange = scopeLimitation.getAuditTrail().getLastSaved();
            }
          }
        }
      }
    }

    if (application.getOpponents() != null) {
      for (final OpponentDetail opponent : application.getOpponents()) {
        if (opponent.getAuditTrail() != null
            && (latestKeyChange == null
                || latestKeyChange.before(opponent.getAuditTrail().getLastSaved()))) {
          latestKeyChange = opponent.getAuditTrail().getLastSaved();
        }
      }
    }
    return latestKeyChange;
  }

  /**
   * Requires the EBS case to be available from session-backed state.
   *
   * @param ebsCase the EBS case from session state
   * @return the EBS case
   */
  public static ApplicationDetail requireEbsCase(@Nullable final ApplicationDetail ebsCase) {
    return Optional.ofNullable(ebsCase)
        .orElseThrow(() -> new CaabApplicationException("Failed to retrieve EBS case"));
  }

  private ApplicationUtil() {}
}
