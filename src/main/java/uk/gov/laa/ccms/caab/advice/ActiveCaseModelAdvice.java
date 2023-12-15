package uk.gov.laa.ccms.caab.advice;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import uk.gov.laa.ccms.caab.controller.application.summary.ApplicationTypeSectionController;
import uk.gov.laa.ccms.caab.controller.application.summary.EditClientAddressDetailsController;
import uk.gov.laa.ccms.caab.controller.application.summary.EditClientAddressDetailsSearchController;
import uk.gov.laa.ccms.caab.controller.application.summary.EditClientBasicDetailsController;
import uk.gov.laa.ccms.caab.controller.application.summary.EditClientContactDetailsController;
import uk.gov.laa.ccms.caab.controller.application.summary.EditClientDeceasedDetailsController;
import uk.gov.laa.ccms.caab.controller.application.summary.EditClientEqualOpportunitiesMonitoringDetailsController;
import uk.gov.laa.ccms.caab.controller.application.summary.EditClientSummaryController;
import uk.gov.laa.ccms.caab.controller.application.summary.ProviderDetailsSectionController;


/**
 * Controller advice class responsible for adding active case to the model of selected controllers.
 * Adding it to the model will amend the header bar with case details.
 */
@ControllerAdvice(assignableTypes = {
    ApplicationTypeSectionController.class,
    ProviderDetailsSectionController.class,
    EditClientAddressDetailsController.class,
    EditClientAddressDetailsSearchController.class,
    EditClientBasicDetailsController.class,
    EditClientContactDetailsController.class,
    EditClientDeceasedDetailsController.class,
    EditClientEqualOpportunitiesMonitoringDetailsController.class,
    EditClientSummaryController.class
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
