package uk.gov.laa.ccms.caab.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_PRIOR_AUTHORITIES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_REFERENCE_NUMBER;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_REFERENCE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_PROCEEDING;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PRIOR_AUTHORITY_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_RESULT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_SUMMARY;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.Collections;
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

  @Test
  void clearOpenApplicationDropsTheApplicationAndCase() {
    MockHttpSession session = sessionEditing(1);
    session.setAttribute(APPLICATION_ID, "1");
    session.setAttribute(CASE, new Object());
    session.setAttribute(CASE_REFERENCE_NUMBER, "300001");
    session.setAttribute(SUBMISSION_SUMMARY, new Object());
    session.setAttribute(SUBMISSION_TRANSACTION_ID, "transaction1");
    session.setAttribute(APPLICATION_FORM_DATA, new Object());
    session.setAttribute(CLIENT_REFERENCE, "client1");
    session.setAttribute(CLIENT_FLOW_FORM_DATA, new Object());

    ApplicationEditorState.clearOpenApplication(session);

    assertNull(session.getAttribute(APPLICATION_FORM_DATA));
    assertNull(session.getAttribute(CLIENT_REFERENCE));
    assertNull(session.getAttribute(CLIENT_FLOW_FORM_DATA));

    assertNull(session.getAttribute(CASE_REFERENCE_NUMBER));
    assertNull(session.getAttribute(SUBMISSION_SUMMARY));
    assertNull(session.getAttribute(SUBMISSION_TRANSACTION_ID));

    assertNull(session.getAttribute(ACTIVE_CASE));
    assertNull(session.getAttribute(APPLICATION_ID));
    assertNull(session.getAttribute(CASE));
    assertNull(session.getAttribute(CURRENT_PROCEEDING));
  }

  @Test
  void providerChangeKeepsOnlyTheLoginAndFrameworkState() {
    MockHttpSession session = sessionEditing(1);
    session.setAttribute("costAllocationFormData", new Object());
    session.setAttribute("individualLiablePartyFlow", new Object());
    session.setAttribute(USER_DETAILS, new Object());
    session.setAttribute("SPRING_SECURITY_CONTEXT", new Object());
    session.setAttribute("authenticatedAt", new Object());
    session.setAttribute("org.springframework.security.web.csrf.CSRF_TOKEN", new Object());

    ApplicationEditorState.clearOpenApplication(session);

    assertThat(Collections.list(session.getAttributeNames()))
        .containsExactlyInAnyOrder(
            USER_DETAILS,
            "SPRING_SECURITY_CONTEXT",
            "authenticatedAt",
            "org.springframework.security.web.csrf.CSRF_TOKEN");
  }

  @Test
  void providerChangeKeepsTheSentApplicationLock() {
    MockHttpSession session = sessionEditing(1);
    SentApplications.record(session, 1, "transaction1");

    ApplicationEditorState.clearOpenApplication(session);

    assertThat(SentApplications.transactionFor(session, 1)).contains("transaction1");
  }

  @Test
  void openingAnotherApplicationDropsTheSubmissionState() {
    MockHttpSession session = sessionEditing(1);
    session.setAttribute(SUBMISSION_TRANSACTION_ID, "transaction1");
    session.setAttribute(SUBMISSION_RESULT, "confirmed");

    ApplicationEditorState.clearWhenApplicationChanges(session, 2);

    assertNull(session.getAttribute(SUBMISSION_TRANSACTION_ID));
    assertNull(session.getAttribute(SUBMISSION_RESULT));
  }
}
