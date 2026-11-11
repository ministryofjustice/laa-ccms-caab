package uk.gov.laa.ccms.caab.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.laa.ccms.caab.controller.application.AmendCaseController;
import uk.gov.laa.ccms.caab.controller.application.section.EditProceedingsAndCostsSectionController;
import uk.gov.laa.ccms.caab.controller.application.section.EvidenceSectionController;
import uk.gov.laa.ccms.caab.controller.billing.BillingController;
import uk.gov.laa.ccms.caab.controller.notifications.ActionsAndNotificationsController;
import uk.gov.laa.ccms.caab.controller.submission.CaseSubmissionController;
import uk.gov.laa.ccms.caab.controller.submission.ClientSubmissionsInProgressController;
import uk.gov.laa.ccms.caab.logging.ChangesData;

/** Pins the GET handlers that change stored data, so their audit events say CHANGE. */
class ChangesDataRoutesTest {

  static Stream<Arguments> handlers() {
    return Stream.of(
        Arguments.of(AssessmentController.class, "assessmentGet"),
        Arguments.of(AmendCaseController.class, "startAmendment"),
        Arguments.of(EvidenceSectionController.class, "removeEvidenceDocument"),
        Arguments.of(EditProceedingsAndCostsSectionController.class, "proceedingsMakeLead"),
        Arguments.of(BillingController.class, "createPoa"),
        Arguments.of(BillingController.class, "createBill"),
        Arguments.of(ActionsAndNotificationsController.class, "removeDraftNotificationAttachment"),
        Arguments.of(CaseSubmissionController.class, "addCaseSubmission"),
        Arguments.of(ClientSubmissionsInProgressController.class, "clientUpdateSubmission"));
  }

  @ParameterizedTest(name = "{0} {1}")
  @MethodSource("handlers")
  void theHandlerIsMarkedAsChangingData(Class<?> controller, String handler) {
    Method method =
        Arrays.stream(controller.getDeclaredMethods())
            .filter(candidate -> candidate.getName().equals(handler))
            .findFirst()
            .orElseThrow();

    assertThat(method.isAnnotationPresent(ChangesData.class)).isTrue();
  }
}
