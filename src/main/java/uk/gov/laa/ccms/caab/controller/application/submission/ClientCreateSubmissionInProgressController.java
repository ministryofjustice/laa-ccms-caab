package uk.gov.laa.ccms.caab.controller.application.submission;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_INFORMATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientStatus;

/**
 * Controller for client creation submissions in progress.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({
    CLIENT_DETAILS,
    SUBMISSION_TRANSACTION_ID
})
public class ClientCreateSubmissionInProgressController {

  private final ClientService clientService;

  //TODO Remove this
  @ModelAttribute(SUBMISSION_TRANSACTION_ID)
  public String getTransactionId(){
    return "202309221121586000634848422";
  }

  /**
   * Handles the GET request for the client creation submission in progress screen.
   *
   * @return The view name for the submission in progress.
   */
  @GetMapping("submissions/client-create")
  public String submissionsInProgress(
      @ModelAttribute(SUBMISSION_TRANSACTION_ID) String transactionId,
      @SessionAttribute(USER_DETAILS) UserDetail user,
      HttpSession session) {
    log.info("GET /submissions/client-create/{}", transactionId);

    ClientStatus clientStatus = clientService.getClientStatus(
        transactionId,
        user.getLoginId(),
        user.getUserType()).block();

    if (StringUtils.hasText(clientStatus.getClientReferenceNumber())){

      ClientDetail clientInformation = clientService.getClient(
          clientStatus.getClientReferenceNumber(),
          user.getLoginId(),
          user.getUserType()).block();

      session.setAttribute(CLIENT_INFORMATION, clientInformation);
      session.removeAttribute(SUBMISSION_TRANSACTION_ID);
      return String.format("redirect:/submissions/client-create/confirmed");
    }

    //TODO increment poll count

    return "submissions/submissionsInProgress";
  }
}
