package uk.gov.laa.ccms.caab.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.model.AvailableAction;
import uk.gov.laa.ccms.caab.util.view.ActionViewHelper;

class ActionViewHelperTest {

  @Test
  void testGetAllAvailableActionsWithOpenAmendment() {
    List<AvailableAction> actions = ActionViewHelper.getAllAvailableActions(true);
    assertThat(actions).isNotNull().isNotEmpty();
    // The first action should be the 'continue amendment' action
    AvailableAction first = actions.getFirst();
    assertThat(first.actionCode()).isEqualTo(FunctionConstants.AMEND_CASE);
    assertThat(first.actionKey()).isEqualTo("action.amendCase.continue.name");
    assertThat(first.descriptionKey()).isEqualTo("action.amendCase.continue.description");
    assertThat(first.link()).isEqualTo("/amendments/new");
  }

  @Test
  void testGetAllAvailableActionsWithNewAmendment() {
    List<AvailableAction> actions = ActionViewHelper.getAllAvailableActions(false);
    assertThat(actions).isNotNull().isNotEmpty();
    // The first action should be the 'new amendment' action
    AvailableAction first = actions.getFirst();
    assertThat(first.actionCode()).isEqualTo(FunctionConstants.AMEND_CASE);
    assertThat(first.actionKey()).isEqualTo("action.amendCase.new.name");
    assertThat(first.descriptionKey()).isEqualTo("action.amendCase.new.description");
    assertThat(first.link()).isEqualTo("/amendments/new");
  }

  @Test
  void testGetAllAvailableActionsStructureAndUniqueness() {
    List<AvailableAction> actions = ActionViewHelper.getAllAvailableActions(false);
    assertThat(actions).as("Actions list should not be null").isNotNull();
    // Expected size: 1 (amendment) + 12 (static) = 13
    assertThat(actions).as("Should be 13 actions in total (1 amendment + 12 static)").hasSize(13);

    // All action codes should be unique
    List<String> codes =
        actions.stream().map(AvailableAction::actionCode).toList();
    assertThat(codes).as("Action codes should be unique").doesNotHaveDuplicates();

    // Verify the static actions part
    List<AvailableAction> staticActions = actions.subList(1, actions.size());
    assertThat(staticActions).as("Should be 12 static actions").hasSize(12);

    // Spot check a few static actions for their properties
    // First static action: AMEND_CLIENT
    AvailableAction amendClientAction = staticActions.getFirst();
    assertThat(amendClientAction.actionCode()).isEqualTo(FunctionConstants.AMEND_CLIENT);
    assertThat(amendClientAction.actionKey()).isEqualTo("action.amendClient.name");
    assertThat(amendClientAction.descriptionKey()).isEqualTo("action.amendClient.description");
    assertThat(amendClientAction.link()).isEqualTo("/amendments/sections/client/details/summary");

    // Last static action: MEANS_REASSESSMENT (index 11 of staticActions)
    AvailableAction meansReassessmentAction = staticActions.get(11);
    assertThat(meansReassessmentAction.actionCode()).isEqualTo(
        FunctionConstants.MEANS_REASSESSMENT);
    assertThat(meansReassessmentAction.actionKey()).isEqualTo(
        "action.completeMeansReassessment.name");
    assertThat(meansReassessmentAction.descriptionKey())
        .isEqualTo("action.completeMeansReassessment.description");
    assertThat(meansReassessmentAction.link()).isEqualTo("#");
  }

  private record ExpectedAction(String actionCode, String actionKey, String descriptionKey,
                                String link) {
  }

  @Test
  void testStaticActionsHaveCorrectPropertiesAndOrder() {
    List<AvailableAction> allActions = ActionViewHelper.getAllAvailableActions(false);
    List<AvailableAction> staticActions = allActions.subList(1, allActions.size());

    assertThat(staticActions).as("Incorrect number of static actions").hasSize(12);

    // Define the expected static actions in order based on ActionViewHelper.AVAILABLE_ACTION_LIST
    List<ExpectedAction> expectedStaticActions = List.of(
        new ExpectedAction(FunctionConstants.AMEND_CLIENT, "action.amendClient.name",
            "action.amendClient.description", "/amendments/sections/client/details/summary"),
        new ExpectedAction(FunctionConstants.BILLING, "action.billing.name",
            "action.billing.description", "#"),
        new ExpectedAction(FunctionConstants.OUTCOME_WITH_DISCHARGE, "action.recordOutcome.name",
            "action.recordOutcome.description", "#"),
        new ExpectedAction(FunctionConstants.OUTCOME_NO_DISCHARGE, "action.recordOutcome.name",
            "action.recordOutcome.description", "#"),
        new ExpectedAction(FunctionConstants.SUBMIT_CASE_REQUEST, "action.submitCaseQuery.name",
            "action.submitCaseQuery.description", "#"),
        new ExpectedAction(FunctionConstants.VIEW_CASE, "action.viewCase.name",
            "action.viewCase.description", "/cases/details"),
        new ExpectedAction(FunctionConstants.NOTIFICATIONS, "action.viewNotifications.name",
            "action.viewNotifications.description", "#"),
        new ExpectedAction(FunctionConstants.VIEW_CASE_OUTCOME, "action.viewOutcome.name",
            "action.viewOutcome.description", "#"),
        new ExpectedAction(FunctionConstants.EDIT_PROVIDER, "action.amendProviderDetails.name",
            "action.amendProviderDetails.description", "/amendments/sections/provider-details"),
        new ExpectedAction(FunctionConstants.CASE_CORRESPONDENCE_PREFERENCE,
            "action.amendCorrespondenceAddress.name",
            "action.amendCorrespondenceAddress.description", "#"),
        new ExpectedAction(FunctionConstants.ALLOCATE_COST_LIMIT, "action.allocateCostLimit.name",
            "action.allocateCostLimit.description", "#"),
        new ExpectedAction(FunctionConstants.MEANS_REASSESSMENT,
            "action.completeMeansReassessment.name", "action.completeMeansReassessment.description",
            "#")
    );

    assertSoftly(softly -> {
      for (int i = 0; i < expectedStaticActions.size(); i++) {
        AvailableAction actual = staticActions.get(i);
        ExpectedAction expected = expectedStaticActions.get(i);
        softly.assertThat(actual.actionCode())
            .as("Action code mismatch for static action at index %d (%s)", i,
                expected.actionCode)
            .isEqualTo(expected.actionCode);
        softly.assertThat(actual.actionKey())
            .as("Action key mismatch for static action at index %d (%s)", i,
                expected.actionCode)
            .isEqualTo(expected.actionKey);
        softly.assertThat(actual.descriptionKey())
            .as("Description key mismatch for static action at index %d (%s)", i,
                expected.actionCode)
            .isEqualTo(expected.descriptionKey);
        softly.assertThat(actual.link())
            .as("Link mismatch for static action at index %d (%s)", i, expected.actionCode)
            .isEqualTo(expected.link);
      }
    });
  }
}
