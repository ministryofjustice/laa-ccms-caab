package uk.gov.laa.ccms.caab.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.NOTIFICATION_SEARCH_CRITERIA;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;

class NotificationsActiveCaseModelAdviceTest {

  private final NotificationsActiveCaseModelAdvice advice =
      new NotificationsActiveCaseModelAdvice();

  private static final ActiveCase ACTIVE_CASE_DETAIL =
      ActiveCase.builder().caseReferenceNumber("300000851818").client("Jane Doe").build();

  @Test
  @DisplayName("Should add the active case when the search originates from a case")
  void shouldAddActiveCaseWhenSearchOriginatesFromCase() {
    NotificationSearchCriteria criteria = new NotificationSearchCriteria();
    criteria.setOriginatesFromCase(true);

    MockHttpSession session = new MockHttpSession();
    session.setAttribute(ACTIVE_CASE, ACTIVE_CASE_DETAIL);
    session.setAttribute(NOTIFICATION_SEARCH_CRITERIA, criteria);

    Model model = new ExtendedModelMap();
    advice.addActiveCaseToModel(model, session);

    assertEquals(ACTIVE_CASE_DETAIL, model.getAttribute(ACTIVE_CASE));
  }

  @Test
  @DisplayName("Should not add the active case for a general notifications search")
  void shouldNotAddActiveCaseForGeneralSearch() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(ACTIVE_CASE, ACTIVE_CASE_DETAIL);
    session.setAttribute(NOTIFICATION_SEARCH_CRITERIA, new NotificationSearchCriteria());

    Model model = new ExtendedModelMap();
    advice.addActiveCaseToModel(model, session);

    assertFalse(model.containsAttribute(ACTIVE_CASE));
  }

  @Test
  @DisplayName("Should not add the active case when there is no search criteria")
  void shouldNotAddActiveCaseWhenNoSearchCriteria() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(ACTIVE_CASE, ACTIVE_CASE_DETAIL);

    Model model = new ExtendedModelMap();
    advice.addActiveCaseToModel(model, session);

    assertFalse(model.containsAttribute(ACTIVE_CASE));
  }

  @Test
  @DisplayName("Should not add the active case when there is no active case in the session")
  void shouldNotAddActiveCaseWhenNoActiveCaseInSession() {
    NotificationSearchCriteria criteria = new NotificationSearchCriteria();
    criteria.setOriginatesFromCase(true);

    MockHttpSession session = new MockHttpSession();
    session.setAttribute(NOTIFICATION_SEARCH_CRITERIA, criteria);

    Model model = new ExtendedModelMap();
    advice.addActiveCaseToModel(model, session);

    assertFalse(model.containsAttribute(ACTIVE_CASE));
  }
}
