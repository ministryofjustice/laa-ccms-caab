package uk.gov.laa.ccms.caab.util;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.LINKED_CASES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_COSTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_OPPONENTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_PRIOR_AUTHORITIES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_PROCEEDINGS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_PROCEEDING;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_SCOPE_LIMITATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.EDIT_PROCEEDINGS_ALLOWED;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.EVIDENCE_REQUIRED;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.EVIDENCE_UPLOAD_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.IS_ORIGINAL_PROCEEDING;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ORIGINAL_PROCEEDING_LOOKUP;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PRIOR_AUTHORITY_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROCEEDING_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROCEEDING_FLOW_FORM_DATA_OLD;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROCEEDING_SCOPE_LIMITATIONS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SCOPE_LIMITATION_FLOW_FORM_DATA;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Objects;
import uk.gov.laa.ccms.caab.bean.ActiveCase;

/** The session state the application's editors keep for whichever application is open. */
public final class ApplicationEditorState {

  private static final List<String> EDITOR_ATTRIBUTES =
      List.of(
          APPLICATION,
          APPLICATION_COSTS,
          APPLICATION_OPPONENTS,
          LINKED_CASES,
          EVIDENCE_REQUIRED,
          EVIDENCE_UPLOAD_FORM_DATA,
          ORIGINAL_PROCEEDING_LOOKUP,
          EDIT_PROCEEDINGS_ALLOWED,
          IS_ORIGINAL_PROCEEDING,
          APPLICATION_PRIOR_AUTHORITIES,
          PRIOR_AUTHORITY_FLOW_FORM_DATA,
          APPLICATION_PROCEEDINGS,
          CURRENT_PROCEEDING,
          PROCEEDING_FLOW_FORM_DATA,
          PROCEEDING_FLOW_FORM_DATA_OLD,
          PROCEEDING_SCOPE_LIMITATIONS,
          SCOPE_LIMITATION_FLOW_FORM_DATA,
          CURRENT_SCOPE_LIMITATION);

  private ApplicationEditorState() {}

  /**
   * Clears the editors' state when a different application is opened, so nothing left from the
   * previous one can be changed through the new one.
   *
   * @param session the user's session
   * @param applicationId the application now being opened
   */
  public static void clearWhenApplicationChanges(HttpSession session, Integer applicationId) {
    Integer previous =
        session.getAttribute(ACTIVE_CASE) instanceof ActiveCase activeCase
            ? activeCase.getApplicationId()
            : null;
    if (previous == null || !Objects.equals(previous, applicationId)) {
      EDITOR_ATTRIBUTES.forEach(session::removeAttribute);
    }
  }
}
