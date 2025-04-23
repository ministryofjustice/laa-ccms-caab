package uk.gov.laa.ccms.caab.util;

import java.util.Objects;
import java.util.Optional;
import uk.gov.laa.ccms.caab.constants.assessment.InstanceMappingPrefix;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;

/**
 * Utility class for handling proceeding-related operations.
 */
public class ProceedingUtil {
  private static final String NEW_PROCEEDING = "NEW";
  private static final String UNCHANGED_PROCEEDING = "UNCHANGED";
  private static final String CHANGED_PROCEEDING = "CHANGED";



  /**
   * Returns the assessment mapping ID for the given proceeding.
   *
   * @param proceeding the proceeding to get the OPA instance mapping ID for
   * @return the assessment mapping ID
   */
  public static String getAssessmentMappingId(final ProceedingDetail proceeding) {
    if (proceeding.getEbsId() == null) {
      return InstanceMappingPrefix.PROCEEDING.getPrefix() + proceeding.getId();
    }
    return proceeding.getEbsId();
  }

  /**
   * Returns the new or existing status of the given proceeding.
   *
   * @param proceeding the proceeding to get the new or existing status for
   * @return the new or existing status
   */
  public static String getNewOrExisting(final ProceedingDetail proceeding) {
    if (proceeding.getEbsId() == null) {
      return NEW_PROCEEDING;
    } else if (proceeding.getEdited()) {
      return CHANGED_PROCEEDING;
    } else {
      return UNCHANGED_PROCEEDING;
    }
  }

  /**
   * Retrieves the scope limitation identifier for a given proceeding. If multiple scope limitations
   * exist, returns "MULTIPLE". Otherwise, returns the identifier of the single scope limitation.
   *
   * @param proceeding the proceeding to evaluate for scope limitations
   * @return the scope limitation identifier or "MULTIPLE"
   */
  public static String getRequestedScopeForAssessmentInput(final ProceedingDetail proceeding) {
    return Optional.ofNullable(proceeding.getScopeLimitations())
        .filter(scopeLimitations -> !scopeLimitations.isEmpty())
        .map(scopeLimitations -> scopeLimitations.size() > 1 ? "MULTIPLE" :
            scopeLimitations.getFirst().getScopeLimitation().getId())
        .orElse(null);
  }

  /**
   * Checks if any of the scope limitations of the given proceeding detail is marked as default.
   *
   * @param proceeding the proceeding detail to check
   * @return {@code true} if there is a default scope limitation, {@code false} otherwise
   */
  public static boolean isScopeLimitDefault(final ProceedingDetail proceeding) {

    return Optional.ofNullable(proceeding.getScopeLimitations())
        .filter(scopeLimitationList -> !scopeLimitationList.isEmpty())
        .map(scopeLimitationList -> scopeLimitationList.stream()
            .filter(Objects::nonNull)
            .anyMatch(ScopeLimitationDetail::getDefaultInd))
        .isPresent();
  }

  /**
   * Retrieves a proceeding detail by the specified EBS ID from the given application detail.
   *
   * @param application the application detail containing the proceedings
   * @param id the EBS ID of the proceeding to retrieve
   * @return the proceeding detail with the specified EBS ID, or {@code null} if not found
   */
  public static ProceedingDetail getProceedingByEbsId(
      final ApplicationDetail application,
      final String id) {
    if (application.getProceedings() != null && id != null) {
      for (final ProceedingDetail proceeding : application.getProceedings()) {
        if (id.equals(proceeding.getEbsId())) {
          return proceeding;
        }
      }
    }
    return null;
  }

  /**
   * Retrieves a proceeding detail by the specified ID from the given application detail.
   *
   * @param application the application detail containing the proceedings
   * @param id the ID of the proceeding to retrieve
   * @return the proceeding detail with the specified ID, or {@code null} if not found
   */
  public static ProceedingDetail getProceedingById(
      final ApplicationDetail application,
      final Integer id) {
    if (application.getProceedings() != null && id != null) {
      for (final ProceedingDetail proceeding : application.getProceedings()) {
        if (proceeding.getId().equals(id)) {
          return proceeding;
        }
      }
    }
    return null;
  }

  private ProceedingUtil() {
  }

}
