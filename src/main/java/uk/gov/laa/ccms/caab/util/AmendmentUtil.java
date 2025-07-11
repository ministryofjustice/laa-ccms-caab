package uk.gov.laa.ccms.caab.util;

import java.util.ArrayList;
import uk.gov.laa.ccms.caab.constants.QuickEditTypeConstants;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;

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
      app.setCategoryOfLaw(null);
      app.setCosts(null);
    } else if (QuickEditTypeConstants.MESSAGE_TYPE_CASE_CORRESPONDENCE_PREFERENCE.equals(
        app.getQuickEditType())) {
      // TODO: Check if you can just set provider details to null once CCMSPUI-692 is completed
      app.getProviderDetails().setSupervisor(null);
      app.getProviderDetails().setFeeEarner(null);
      app.getProviderDetails().setProviderContact(null);
      app.setCategoryOfLaw(null);
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
      app.setCategoryOfLaw(null);
    }
  }
}
