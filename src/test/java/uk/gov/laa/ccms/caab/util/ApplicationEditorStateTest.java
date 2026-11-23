package uk.gov.laa.ccms.caab.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_PRIOR_AUTHORITIES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_PROCEEDING;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PRIOR_AUTHORITY_FLOW_FORM_DATA;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;

class ApplicationEditorStateTest {

  private static MockHttpSession sessionEditing(Integer applicationId) {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(ACTIVE_CASE, ActiveCase.builder().applicationId(applicationId).build());
    session.setAttribute(APPLICATION_PRIOR_AUTHORITIES, List.of());
    session.setAttribute(CURRENT_PROCEEDING, new ProceedingDetail());
    session.setAttribute(PRIOR_AUTHORITY_FLOW_FORM_DATA, new Object());
    return session;
  }

  @Test
  void clearsEditorStateWhenAnotherApplicationIsOpened() {
    MockHttpSession session = sessionEditing(1);

    ApplicationEditorState.clearWhenApplicationChanges(session, 2);

    assertNull(session.getAttribute(APPLICATION_PRIOR_AUTHORITIES));
    assertNull(session.getAttribute(PRIOR_AUTHORITY_FLOW_FORM_DATA));
    assertNull(session.getAttribute(CURRENT_PROCEEDING));
  }

  @Test
  void clearsEditorStateWhenNoApplicationWasOpen() {
    MockHttpSession session = sessionEditing(null);

    ApplicationEditorState.clearWhenApplicationChanges(session, 2);

    assertNull(session.getAttribute(CURRENT_PROCEEDING));
  }

  @Test
  void keepsEditorStateWhenTheSameApplicationIsReopened() {
    MockHttpSession session = sessionEditing(1);

    ApplicationEditorState.clearWhenApplicationChanges(session, 1);

    assertNotNull(session.getAttribute(APPLICATION_PRIOR_AUTHORITIES));
    assertNotNull(session.getAttribute(CURRENT_PROCEEDING));
  }
}
