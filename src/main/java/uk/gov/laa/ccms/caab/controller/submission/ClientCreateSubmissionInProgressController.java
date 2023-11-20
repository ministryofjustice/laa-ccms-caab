package uk.gov.laa.ccms.caab.controller.submission;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_REFERENCE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientStatus;

/**
 * Controller for client creation submissions in progress.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ClientCreateSubmissionInProgressController {

  private final ClientService clientService;

  /**
   * Handles the GET request for the client creation submission in progress screen.
   *
   * @return The view name for the submission in progress.
   */
  @GetMapping("/submissions/client-create")
  public String submissionInProgress(
      @SessionAttribute(SUBMISSION_TRANSACTION_ID) String transactionId,
      @SessionAttribute(USER_DETAILS) UserDetail user,
      HttpSession session, Model model) {

    model.addAttribute("submissionType", "client-create");

    ClientStatus clientStatus = clientService.getClientStatus(
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

    return viewIncludingPollCount(session);
  }

  private String viewIncludingPollCount(HttpSession session) {
    int submissionPollCount = 0;
    if (session.getAttribute("submissionPollCount") != null) {
      submissionPollCount = (int) session.getAttribute("submissionPollCount");
      if (submissionPollCount >= 6) {
        return "redirect:/submissions/client-create/failed";
      }
    }
    submissionPollCount = submissionPollCount + 1;
    session.setAttribute("submissionPollCount", submissionPollCount);
    return "submissions/submissionInProgress";
  }

}
