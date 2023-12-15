package uk.gov.laa.ccms.caab.controller.submission;

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
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.constants.SubmissionConstants;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientStatus;

/**
 * Controller for client creation submissions in progress.
 */
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
  @GetMapping("/submissions/client-create")
  public String clientCreateSubmission(
      @SessionAttribute(SUBMISSION_TRANSACTION_ID) final String transactionId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final HttpSession session,
      final Model model) {

    model.addAttribute("submissionType", SUBMISSION_CREATE_CLIENT);

    final ClientStatus clientStatus = clientService.getClientStatus(
        transactionId,
        user.getLoginId(),
        user.getUserType()).block();

    if (clientStatus != null && StringUtils.hasText(clientStatus.getClientReferenceNumber())) {
      session.setAttribute(CLIENT_REFERENCE, clientStatus.getClientReferenceNumber());

      //Do some session tidy up
      session.removeAttribute(SUBMISSION_TRANSACTION_ID);
      session.removeAttribute(CLIENT_SEARCH_CRITERIA);
      session.removeAttribute(CLIENT_FLOW_FORM_DATA);

      return "redirect:/submissions/client-create/confirmed";
    }

    return viewIncludingPollCount(session, SUBMISSION_CREATE_CLIENT);
  }

  /**
   * Handles the GET request for the client update submission in progress screen.
   *
   * @return The view name for the submission in progress.
   */
  @GetMapping("/submissions/client-update")
  public String clientUpdateSubmission(
      @SessionAttribute(SUBMISSION_TRANSACTION_ID) final String transactionId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final HttpSession session, final Model model) {

    model.addAttribute("submissionType", SUBMISSION_UPDATE_CLIENT);

    final ClientStatus clientStatus = clientService.getClientStatus(
        transactionId,
        user.getLoginId(),
        user.getUserType()).block();

    if (clientStatus != null && StringUtils.hasText(clientStatus.getClientReferenceNumber())) {
      //Do some session tidy up
      session.removeAttribute(SUBMISSION_TRANSACTION_ID);
      session.removeAttribute(CLIENT_FLOW_FORM_DATA);

      return "redirect:/submissions/client-update/confirmed";
    }

    return viewIncludingPollCount(session, SUBMISSION_UPDATE_CLIENT);
  }

  private String viewIncludingPollCount(
      final HttpSession session,
      final String submissionType) {
    int submissionPollCount = 0;

    if (session.getAttribute(SUBMISSION_POLL_COUNT) != null) {
      submissionPollCount = (int) session.getAttribute(SUBMISSION_POLL_COUNT);
      if (submissionPollCount >= submissionConstants.getMaxPollCount()) {
        return String.format("redirect:/submissions/%s/failed", submissionType);
      }
    }
    submissionPollCount = submissionPollCount + 1;
    session.setAttribute(SUBMISSION_POLL_COUNT, submissionPollCount);
    return "submissions/submissionInProgress";
  }

}
