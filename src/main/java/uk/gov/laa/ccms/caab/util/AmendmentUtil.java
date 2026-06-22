package uk.gov.laa.ccms.caab.util;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.PROCEEDING_STATUS_UNCHANGED_DISPLAY;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import uk.gov.laa.ccms.caab.constants.QuickEditTypeConstants;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.AuditDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;

/**
 * Utility class containing methods for handling amendments in the application process. This class
 * provides functionalities to clean and prepare {@code ApplicationDetail} objects for different
 * types of quick amendment submissions.
 *
 * @see ApplicationDetail
 * @author Jamie Briggs
 */
public final class AmendmentUtil {

  /**
   * Cleans and prepares an {@code ApplicationDetail} object for submission based on the quick edit
   * type specified within the application. The method removes or resets specific properties of the
   * application, such as proceedings, opponents, provider details, correspondence address, and
   * other attributes, depending on the type of quick edit type.
   *
   * @param app the {@code ApplicationDetail} object to be cleaned and prepared for quick amend
   *     submission. The method modifies this object directly by resetting or clearing its
   *     properties.
   */
  public static void cleanAppForQuickAmendSubmit(ApplicationDetail app) {
    ArrayList<ProceedingDetail> noProceedings = new ArrayList<>();
    ArrayList<OpponentDetail> noOpponents = new ArrayList<>();
    app.setProceedings(noProceedings);
    app.setOpponents(noOpponents);
    app.setLarScopeFlag(null);
    if (QuickEditTypeConstants.MESSAGE_TYPE_EDIT_PROVIDER.equals(app.getQuickEditType())) {
      app.setCorrespondenceAddress(null);
      app.setCosts(null);
    } else if (QuickEditTypeConstants.MESSAGE_TYPE_CASE_CORRESPONDENCE_PREFERENCE.equals(
        app.getQuickEditType())) {
      // TODO: Check if you can just set provider details to null once CCMSPUI-692 is completed
      app.getProviderDetails().setSupervisor(null);
      app.getProviderDetails().setFeeEarner(null);
      app.getProviderDetails().setProviderContact(null);
      app.setCosts(null);
    } else if (QuickEditTypeConstants.MESSAGE_TYPE_ALLOCATE_COST_LIMIT.equals(
        app.getQuickEditType())) {
      // TODO: Check if you can just set provider details to null once CCMSPUI-692 is completed
      app.getProviderDetails().setSupervisor(null);
      app.getProviderDetails().setFeeEarner(null);
      app.getProviderDetails().setProviderContact(null);
      app.setCorrespondenceAddress(null);
    } else if (QuickEditTypeConstants.MESSAGE_TYPE_MEANS_REASSESSMENT.equals(
        app.getQuickEditType())) {
      // TODO: Check if you can just set provider details to null once CCMSPUI-692 is completed
      app.setMeansAssessmentAmended(true);
      app.setMeritsAssessmentAmended(false);
      app.getProviderDetails().setSupervisor(null);
      app.getProviderDetails().setFeeEarner(null);
      app.getProviderDetails().setProviderContact(null);
      app.setCorrespondenceAddress(null);
      app.setCosts(null);
    }
  }

  /** A draft amendment is treated as edited if it was saved more than this long after creation. */
  private static final long EDIT_THRESHOLD_MILLIS = 10_000L;

  /**
   * Determines whether a full case amendment actually changed anything relative to the original
   * case. Old PUI allows an unchanged amendment to be submitted; the new PUI blocks it (see ticket
   * CCMSPUI-932, scenario 5). A change is detected from the deterministic markers the amendment
   * journey maintains - a changed cost limit, a means/merits reassessment, an added/updated/removed
   * proceeding, or an added/removed opponent, prior authority or linked case - plus the application
   * and opponent last-saved timestamps, which catch general-details and opponent edits that have no
   * dedicated marker. When the original case is unavailable only the amendment-internal markers are
   * used. Erring towards "changed" keeps the legacy permissive behaviour when in doubt.
   *
   * @param amendment the draft amendment being submitted
   * @param originalCase the current EBS case the amendment is based on (may be {@code null})
   * @return {@code true} if the amendment contains at least one change
   */
  public static boolean hasChanges(
      final ApplicationDetail amendment, final ApplicationDetail originalCase) {
    if (amendment == null) {
      return false;
    }

    // Cost limit was explicitly changed during the amendment.
    if (amendment.getCostLimit() != null
        && Boolean.TRUE.equals(amendment.getCostLimit().getChanged())) {
      return true;
    }

    // A means or merits reassessment was performed (amended flags must be recomputed beforehand).
    if (Boolean.TRUE.equals(amendment.getMeansAssessmentAmended())
        || Boolean.TRUE.equals(amendment.getMeritsAssessmentAmended())) {
      return true;
    }

    // A proceeding was added or updated (status no longer "Unchanged").
    if (amendment.getProceedings() != null) {
      for (final ProceedingDetail proceeding : amendment.getProceedings()) {
        final String status =
            Optional.ofNullable(proceeding.getStatus())
                .map(StringDisplayValue::getDisplayValue)
                .orElse(null);
        if (status != null && !PROCEEDING_STATUS_UNCHANGED_DISPLAY.equalsIgnoreCase(status)) {
          return true;
        }
      }
    }

    // A proceeding, opponent, prior authority or linked case was added or removed.
    if (originalCase != null
        && (size(amendment.getProceedings()) != size(originalCase.getProceedings())
            || size(amendment.getOpponents()) != size(originalCase.getOpponents())
            || size(amendment.getPriorAuthorities()) != size(originalCase.getPriorAuthorities())
            || size(amendment.getLinkedCases()) != size(originalCase.getLinkedCases()))) {
      return true;
    }

    final Date created =
        Optional.ofNullable(amendment.getAuditTrail()).map(AuditDetail::getCreated).orElse(null);

    // The application was edited after it was created (e.g. general details), which advances its
    // last-saved timestamp.
    if (savedAfterCreation(created, amendment.getAuditTrail())) {
      return true;
    }

    // An individual opponent was edited after the amendment was created.
    if (amendment.getOpponents() != null) {
      for (final OpponentDetail opponent : amendment.getOpponents()) {
        if (savedAfterCreation(created, opponent.getAuditTrail())) {
          return true;
        }
      }
    }

    return false;
  }

  private static boolean savedAfterCreation(final Date created, final AuditDetail audit) {
    if (created == null || audit == null || audit.getLastSaved() == null) {
      return false;
    }
    return audit.getLastSaved().getTime() - created.getTime() > EDIT_THRESHOLD_MILLIS;
  }

  private static int size(final List<?> list) {
    return list == null ? 0 : list.size();
  }
}
