package uk.gov.laa.ccms.caab.advice;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.NOTIFICATION_SEARCH_CRITERIA;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.controller.notifications.ActionsAndNotificationsController;
import uk.gov.laa.ccms.caab.controller.notifications.NotificationsSearchResultsController;

/**
 * Controller advice responsible for adding the active case to the model of the notifications
 * controllers, so that the header bar displays the case details.
 *
 * <p>This is kept separate from {@link ActiveCaseModelAdvice} because the notifications journey is
 * reached both from a case and from the home page. A case can remain in the session long after the
 * user has left it, so the case details are only added when the current search actually originates
 * from a case, otherwise a general notifications search would display an unrelated case.
 */
@ControllerAdvice(
    assignableTypes = {
      ActionsAndNotificationsController.class,
      NotificationsSearchResultsController.class
    })
public class NotificationsActiveCaseModelAdvice {

  /**
   * Add the active case to the model when the notification search originates from a case.
   *
   * @param model the model view to be updated
   * @param session the session data
   */
  @ModelAttribute
  public void addActiveCaseToModel(final Model model, final HttpSession session) {
    Object activeCase = session.getAttribute(ACTIVE_CASE);
    Object criteria = session.getAttribute(NOTIFICATION_SEARCH_CRITERIA);

    if (activeCase != null
        && criteria instanceof NotificationSearchCriteria searchCriteria
        && searchCriteria.isOriginatesFromCase()) {
      model.addAttribute(ACTIVE_CASE, activeCase);
    }
  }
}
