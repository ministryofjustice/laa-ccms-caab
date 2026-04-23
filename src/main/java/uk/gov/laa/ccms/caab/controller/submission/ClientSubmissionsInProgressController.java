package uk.gov.laa.ccms.caab.controller.submission;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_CLIENT_NAMES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_REFERENCE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_POLL_COUNT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_CREATE_CLIENT;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_UPDATE_CLIENT;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.caab.constants.SubmissionConstants;
import uk.gov.laa.ccms.caab.model.BaseClientDetail;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.data.model.TransactionStatus;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller for client creation submissions in progress. */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ClientSubmissionsInProgressController {

  private final SubmissionConstants submissionConstants;

  private final ClientService clientService;

  /**
   * Handles the GET request for the client creation submission in progress screen.
   *
   * @return The view name for the submission in progress.
   */
  @GetMapping("/application/client-create")
  public String clientCreateSubmission(
      @SessionAttribute(SUBMISSION_TRANSACTION_ID) final String transactionId,
      final HttpSession session,
      final Model model) {

    model.addAttribute("submissionType", SUBMISSION_CREATE_CLIENT);

    final TransactionStatus clientStatus = clientService.getClientStatus(transactionId).block();

    if (clientStatus != null && StringUtils.hasText(clientStatus.getReferenceNumber())) {
      session.setAttribute(CLIENT_REFERENCE, clientStatus.getReferenceNumber());

      // Do some session tidy up
      session.removeAttribute(SUBMISSION_POLL_COUNT);
      session.removeAttribute(SUBMISSION_TRANSACTION_ID);
      session.removeAttribute(CLIENT_SEARCH_CRITERIA);
      session.removeAttribute(CLIENT_FLOW_FORM_DATA);

      return "redirect:/application/client-create/confirmed";
    }

    return viewIncludingPollCount(session, CaseContext.APPLICATION, SUBMISSION_CREATE_CLIENT);
  }

  /**
   * Handles the GET request for the client update submission in progress screen.
   *
   * @return The view name for the submission in progress.
   */
  @GetMapping("/{caseContext}/client-update")
  public String clientUpdateSubmission(
      @PathVariable("caseContext") final CaseContext caseContext,
      @SessionAttribute(SUBMISSION_TRANSACTION_ID) final String transactionId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(APPLICATION_CLIENT_NAMES) final BaseClientDetail baseClient,
      final HttpSession session,
      final Model model) {

    model.addAttribute("submissionType", SUBMISSION_UPDATE_CLIENT);

    final TransactionStatus clientStatus = clientService.getClientStatus(transactionId).block();

    if (clientStatus != null && StringUtils.hasText(clientStatus.getReferenceNumber())) {
      clientService.updateClientNames(clientStatus.getReferenceNumber(), user, baseClient).block();

      // Do some session tidy up
      session.removeAttribute(SUBMISSION_POLL_COUNT);
      session.removeAttribute(SUBMISSION_TRANSACTION_ID);
      session.removeAttribute(CLIENT_FLOW_FORM_DATA);
      session.removeAttribute(APPLICATION_CLIENT_NAMES);

      return "redirect:/%s/client-update/confirmed".formatted(caseContext.getPathValue());
    }

    return viewIncludingPollCount(session, caseContext, SUBMISSION_UPDATE_CLIENT);
  }

  /**
   * Handles the POST request for a failed client submission, redirecting back to the appropriate
   * summary page.
   *
   * @param caseContext the context for the case (e.g. application or amendments)
   * @param submissionType the type of submission that failed
   * @param session the HTTP session to be updated
   * @return a redirect back to the summary page
   */
  @PostMapping("/{caseContext}/{submissionType}/failed")
  public String submissionFailed(
      @PathVariable("caseContext") CaseContext caseContext,
      @PathVariable String submissionType,
      final HttpSession session) {
    session.removeAttribute(SUBMISSION_POLL_COUNT);
    if (caseContext.isApplication()) {
      if (SUBMISSION_CREATE_CLIENT.equals(submissionType)) {
        return "redirect:/application/client/details";
      } else if (SubmissionConstants.SUBMISSION_SUBMIT_CASE.equals(submissionType)) {
        return "redirect:/application/case-details";
      }
      return "redirect:/application/sections";
    } else {
      return "redirect:/amendments/summary";
    }
  }

  /**
   * Handles the GET request for a failed submission screen.
   *
   * @param submissionType the type of submission that failed
   * @param model the model to be updated with submission details
   * @return the view name for a failed submission.
   */
  @GetMapping("/{caseContext}/{submissionType}/failed")
  public String submissionsFailed(
      @PathVariable String submissionType,
      Model model) {

    model.addAttribute("submissionType", submissionType);

    return "submissions/submissionFailed";
  }

  private String viewIncludingPollCount(final HttpSession session, final CaseContext caseContext,
      final String submissionType) {
    int submissionPollCount = 0;

    if (session.getAttribute(SUBMISSION_POLL_COUNT) != null) {
      submissionPollCount = (int) session.getAttribute(SUBMISSION_POLL_COUNT);
      if (submissionPollCount >= submissionConstants.getMaxPollCount()) {
        return "redirect:/%s/%s/failed".formatted(caseContext.getPathValue(), submissionType);
      }
    }
    submissionPollCount += 1;
    session.setAttribute(SUBMISSION_POLL_COUNT, submissionPollCount);
    return "submissions/submissionInProgress";
  }
}
