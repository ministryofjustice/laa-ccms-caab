package uk.gov.laa.ccms.caab.util.view;

import java.util.LinkedList;
import java.util.List;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.model.AvailableAction;

/**
 * Utility class for managing and retrieving available actions for a case.
 */
public class ActionViewHelper {

  private static final List<AvailableAction> AVAILABLE_ACTION_LIST = List.of(
      new AvailableAction(
          FunctionConstants.AMEND_CLIENT,
          "action.amendClient.name",
          "action.amendClient.description",
          "/" + CaseContext.AMENDMENTS.getPathValue() + "/sections/client/details/summary"),
      new AvailableAction(
          FunctionConstants.BILLING, "action.billing.name", "action.billing.description", "#"),
      new AvailableAction(
          FunctionConstants.OUTCOME_WITH_DISCHARGE,
          "action.recordOutcome.name",
          "action.recordOutcome.description",
          "#"),
      new AvailableAction(
          FunctionConstants.OUTCOME_NO_DISCHARGE,
          "action.recordOutcome.name",
          "action.recordOutcome.description",
          "#"),
      new AvailableAction(
          FunctionConstants.SUBMIT_CASE_REQUEST,
          "action.submitCaseQuery.name",
          "action.submitCaseQuery.description",
          "#"),
      new AvailableAction(
          FunctionConstants.VIEW_CASE, "action.viewCase.name", "action.viewCase.description",
          "/cases/details"),
      new AvailableAction(
          FunctionConstants.NOTIFICATIONS,
          "action.viewNotifications.name",
          "action.viewNotifications.description",
          "#"),
      new AvailableAction(
          FunctionConstants.VIEW_CASE_OUTCOME,
          "action.viewOutcome.name",
          "action.viewOutcome.description",
          "#"),
      new AvailableAction(
          FunctionConstants.EDIT_PROVIDER,
          "action.amendProviderDetails.name",
          "action.amendProviderDetails.description",
          "#"),
      new AvailableAction(
          FunctionConstants.CASE_CORRESPONDENCE_PREFERENCE,
          "action.amendCorrespondenceAddress.name",
          "action.amendCorrespondenceAddress.description",
          "#"),
      new AvailableAction(
          FunctionConstants.ALLOCATE_COST_LIMIT,
          "action.allocateCostLimit.name",
          "action.allocateCostLimit.description",
          "#"),
      new AvailableAction(
          FunctionConstants.MEANS_REASSESSMENT,
          "action.completeMeansReassessment.name",
          "action.completeMeansReassessment.description",
          "#"));

  /**
   * Retrieves a list of available actions, including an amendment action based on
   * the amendment state.
   *
   * @param openAmendment Indicates if the amendment is open.
   * @return List of available actions.
   */
  public static List<AvailableAction> getAllAvailableActions(boolean openAmendment) {

    AvailableAction amendmentAction = openAmendment
        ? new AvailableAction(
        FunctionConstants.AMEND_CASE,
        "action.amendCase.continue.name",
        "action.amendCase.continue.description",
        "#")
        : new AvailableAction(
        FunctionConstants.AMEND_CASE,
        "action.amendCase.new.name",
        "action.amendCase.new.description",
        "#");

    LinkedList<AvailableAction> availableActions = new LinkedList<>(AVAILABLE_ACTION_LIST);
    availableActions.addFirst(amendmentAction);
    return availableActions;
  }
}
