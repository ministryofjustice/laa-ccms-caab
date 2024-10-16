package uk.gov.laa.ccms.caab.advice;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import uk.gov.laa.ccms.caab.controller.AssessmentController;
import uk.gov.laa.ccms.caab.controller.application.section.ApplicationSectionsController;
import uk.gov.laa.ccms.caab.controller.application.section.ApplicationSubmissionController;
import uk.gov.laa.ccms.caab.controller.application.section.ApplicationTypeSectionController;
import uk.gov.laa.ccms.caab.controller.application.section.EditClientAddressDetailsController;
import uk.gov.laa.ccms.caab.controller.application.section.EditClientAddressDetailsSearchController;
import uk.gov.laa.ccms.caab.controller.application.section.EditClientBasicDetailsController;
import uk.gov.laa.ccms.caab.controller.application.section.EditClientContactDetailsController;
import uk.gov.laa.ccms.caab.controller.application.section.EditClientDeceasedDetailsController;
import uk.gov.laa.ccms.caab.controller.application.section.EditClientEqualOpportunitiesMonitoringDetailsController;
import uk.gov.laa.ccms.caab.controller.application.section.EditClientSummaryController;
import uk.gov.laa.ccms.caab.controller.application.section.EditGeneralDetailsSectionController;
import uk.gov.laa.ccms.caab.controller.application.section.EditProceedingsAndCostsSectionController;
import uk.gov.laa.ccms.caab.controller.application.section.EvidenceSectionController;
import uk.gov.laa.ccms.caab.controller.application.section.OpponentsSectionController;
import uk.gov.laa.ccms.caab.controller.application.section.ProviderDetailsSectionController;
import uk.gov.laa.ccms.caab.controller.submission.CaseSubmissionController;


/**
 * Controller advice class responsible for adding active case to the model of selected controllers.
 * Adding it to the model will amend the header bar with case details.
 */
@ControllerAdvice(assignableTypes = {
    ApplicationSectionsController.class,
    ApplicationSubmissionController.class,
    ApplicationTypeSectionController.class,
    ProviderDetailsSectionController.class,
    EditClientAddressDetailsController.class,
    EditClientAddressDetailsSearchController.class,
    EditClientBasicDetailsController.class,
    EditClientContactDetailsController.class,
    EditClientDeceasedDetailsController.class,
    EditClientEqualOpportunitiesMonitoringDetailsController.class,
    EditClientSummaryController.class,
    EditGeneralDetailsSectionController.class,
    EditProceedingsAndCostsSectionController.class,
    OpponentsSectionController.class,
    EvidenceSectionController.class,
    AssessmentController.class,
    CaseSubmissionController.class
})
public class ActiveCaseModelAdvice {

  /**
   * Controller advice method responsible for adding active case to the model of selected
   * controllers.
   *
   * @param model the model view to be updated
   * @param session the session data
   */
  @ModelAttribute
  public void addActiveCaseToModel(final Model model, final HttpSession session) {
    if (session.getAttribute(ACTIVE_CASE) != null) {
      model.addAttribute(session.getAttribute(ACTIVE_CASE));
    }
  }
}
