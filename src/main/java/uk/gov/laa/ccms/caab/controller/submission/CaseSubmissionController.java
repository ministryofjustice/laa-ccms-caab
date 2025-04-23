package uk.gov.laa.ccms.caab.controller.submission;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_POLL_COUNT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_CREATE_CASE;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.constants.SubmissionConstants;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.TransactionStatus;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller for the add case submission into ebs.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class CaseSubmissionController {

  private final SubmissionConstants submissionConstants;

  private final ApplicationService applicationService;

  /**
   * Handles the creation of a case submission and updates the model and session with relevant
   * details. If the case status is confirmed, redirects to the submission confirmation page.
   *
   * @param transactionId the ID of the submission transaction
   * @param user the user details for the case submission
   * @param session the HTTP session to be updated
   * @param model the model to be updated with submission details
   * @return the view name or a redirect to the confirmed submission page if the
   *         case status is valid
   */
  @GetMapping("/submissions/case-create")
  public String addCaseSubmission(
      @SessionAttribute(SUBMISSION_TRANSACTION_ID) final String transactionId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final HttpSession session,
      final Model model) {

    model.addAttribute("submissionType", SUBMISSION_CREATE_CASE);

    final TransactionStatus caseStatus = applicationService.getCaseStatus(
        transactionId).block();

    if (caseStatus != null && StringUtils.hasText(caseStatus.getReferenceNumber())) {
      session.removeAttribute(SUBMISSION_TRANSACTION_ID);
      return "redirect:/submissions/%s/confirmed".formatted(SUBMISSION_CREATE_CASE);
    }

    return viewIncludingPollCount(session);
  }


  /**
   * Handles the confirmation of a case creation submission and updates the client session.
   *
   * @param session the HTTP session to be updated
   * @return a redirect to the home page after removing the active case attribute
   */
  @PostMapping("/submissions/case-create/confirmed")
  public String clientUpdateSubmitted(final HttpSession session) {
    session.removeAttribute(ACTIVE_CASE);
    return "redirect:/home";
  }

  /**
   * Returns the view based on the submission poll count from the session.
   * If the poll count exceeds the maximum, redirects to the failed submission page.
   *
   * @param session the HTTP session containing the submission poll count
   * @return the view name or a redirect to the failed submission page if the max poll count
   *         is exceeded
   */
  protected String viewIncludingPollCount(
      final HttpSession session) {
    int submissionPollCount = 0;

    if (session.getAttribute(SUBMISSION_POLL_COUNT) != null) {
      submissionPollCount = (int) session.getAttribute(SUBMISSION_POLL_COUNT);
      if (submissionPollCount >= submissionConstants.getMaxPollCount()) {
        return "redirect:/submissions/%s/failed".formatted(
            SubmissionConstants.SUBMISSION_CREATE_CASE);
      }
    }
    submissionPollCount = submissionPollCount + 1;
    session.setAttribute(SUBMISSION_POLL_COUNT, submissionPollCount);
    return "submissions/submissionInProgress";
  }


}
