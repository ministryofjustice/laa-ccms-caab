package uk.gov.laa.ccms.caab.controller.submission;

import static uk.gov.laa.ccms.caab.constants.ContextConstants.AMENDMENTS;
import static uk.gov.laa.ccms.caab.constants.ContextConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.ContextConstants.CONTEXT_NAME;
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
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.constants.SubmissionConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.BaseClientDetail;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.data.model.TransactionStatus;
import uk.gov.laa.ccms.data.model.UserDetail;

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
  @GetMapping("/application/client-create")
  public String clientCreateSubmission(
      @SessionAttribute(SUBMISSION_TRANSACTION_ID) final String transactionId,
      final HttpSession session,
      final Model model) {

    model.addAttribute("submissionType", SUBMISSION_CREATE_CLIENT);

    final TransactionStatus clientStatus = clientService.getClientStatus(
        transactionId).block();

    if (clientStatus != null && StringUtils.hasText(clientStatus.getReferenceNumber())) {
      session.setAttribute(CLIENT_REFERENCE, clientStatus.getReferenceNumber());

      //Do some session tidy up
      session.removeAttribute(SUBMISSION_TRANSACTION_ID);
      session.removeAttribute(CLIENT_SEARCH_CRITERIA);
      session.removeAttribute(CLIENT_FLOW_FORM_DATA);

      return "redirect:/application/client-create/confirmed";
    }

    return viewIncludingPollCount(session, SUBMISSION_CREATE_CLIENT);
  }

  /**
   * Handles the GET request for the client update submission in progress screen.
   *
   * @return The view name for the submission in progress.
   */
  @GetMapping("/{context}/client-update")
  public String clientUpdateSubmission(
      @PathVariable(CONTEXT_NAME) final String context,
      @SessionAttribute(SUBMISSION_TRANSACTION_ID) final String transactionId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(APPLICATION_CLIENT_NAMES) final BaseClientDetail baseClient,
      final HttpSession session, final Model model) {

    if (!APPLICATION.equalsIgnoreCase(context)
        && !AMENDMENTS.equalsIgnoreCase(context)) {
      throw new CaabApplicationException("Unknown context");
    }

    model.addAttribute("submissionType", SUBMISSION_UPDATE_CLIENT);

    final TransactionStatus clientStatus = clientService.getClientStatus(
        transactionId).block();

    if (clientStatus != null && StringUtils.hasText(clientStatus.getReferenceNumber())) {
      clientService.updateClientNames(
          clientStatus.getReferenceNumber(),
          user,
          baseClient).block();

      //Do some session tidy up
      session.removeAttribute(SUBMISSION_TRANSACTION_ID);
      session.removeAttribute(CLIENT_FLOW_FORM_DATA);
      session.removeAttribute(APPLICATION_CLIENT_NAMES);

      return "redirect:/%s/client-update/confirmed".formatted(context);
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
        return "redirect:/application/%s/failed".formatted(submissionType);
      }
    }
    submissionPollCount = submissionPollCount + 1;
    session.setAttribute(SUBMISSION_POLL_COUNT, submissionPollCount);
    return "submissions/submissionInProgress";
  }

}
