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
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_POLL_COUNT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_RESULT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.security.AbsoluteSessionTimeoutFilter;

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

  private static final Set<String> KEPT_ON_PROVIDER_CHANGE =
      Set.of(
          HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
          "SPRING_SECURITY_SAVED_REQUEST",
          USER_DETAILS,
          AbsoluteSessionTimeoutFilter.AUTHENTICATED_AT);

  private ApplicationEditorState() {}

  /**
   * Clears everything the session holds for a case, application, client or journey, as after a
   * provider change: nothing begun under the old provider is carried over. The login, its timeout
   * and the framework's own state are kept; those keys are fully qualified, or listed here. So is
   * {@link SentApplications}, deliberately: it only locks a sent application while its submission
   * is pending, and clearing it would let switching provider and back reopen one mid-submission.
   *
   * @param session the user's session
   */
  public static void clearOpenApplication(HttpSession session) {
    Collections.list(session.getAttributeNames()).stream()
        .filter(name -> !name.contains(".") && !KEPT_ON_PROVIDER_CHANGE.contains(name))
        .forEach(session::removeAttribute);
  }

  /**
   * Clears the editors' and submission state when a different application is opened, so nothing
   * left from the previous one can be changed or submitted through the new one.
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
      List.of(SUBMISSION_TRANSACTION_ID, SUBMISSION_RESULT, SUBMISSION_POLL_COUNT)
          .forEach(session::removeAttribute);
    }
  }
}
