package uk.gov.laa.ccms.caab.controller.client;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_INFORMATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import java.text.ParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.mapper.ResultDisplayMapper;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

/** Controller for handling client confirmation operations. */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {USER_DETAILS, APPLICATION_FORM_DATA, CLIENT_INFORMATION})
public class ClientConfirmationController {

  private final ClientService clientService;

  private final ApplicationService applicationService;

  private final ResultDisplayMapper resultDisplayMapper;

  /**
   * Handles the GET request for client confirmation page.
   *
   * @param clientReferenceNumber The client reference number.
   * @param user The user details from session.
   * @param model The model for the view.
   * @param session The HTTP session.
   * @return The view name for the client confirmation page.
   */
  @GetMapping("/application/client/{client-reference-number}/confirm")
  public String clientConfirm(
      @PathVariable("client-reference-number") final String clientReferenceNumber,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      Model model,
      HttpSession session) {

    ClientDetail clientInformation =
        clientService
            .getClient(clientReferenceNumber, user.getLoginId(), user.getUserType())
            .block();

    session.setAttribute(CLIENT_INFORMATION, clientInformation);
    model.addAttribute("clientReferenceNumber", clientReferenceNumber);
    model.addAttribute("client", resultDisplayMapper.toClientResultRowDisplay(clientInformation));

    return "application/application-client-confirmation";
  }

  /**
   * Handles the POST request for confirmed client submission.
   *
   * @param confirmedClientReference The confirmed client reference number.
   * @param applicationFormData The application details from session.
   * @param clientInformation The client information from session.
   * @param user The user details from session.
   * @return A Mono containing a redirect string or error.
   */
  @PostMapping("/application/client/confirmed")
  public Mono<String> clientConfirmed(
      String confirmedClientReference,
      @SessionAttribute(APPLICATION_FORM_DATA) final ApplicationFormData applicationFormData,
      @SessionAttribute(CLIENT_INFORMATION) final ClientDetail clientInformation,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      HttpSession session)
      throws ParseException {

    if (!confirmedClientReference.equals(clientInformation.getClientReferenceNumber())) {
      throw new RuntimeException("Client information does not match");
    }

    return applicationService
        .createApplication(applicationFormData, clientInformation, user)
        .doOnSuccess(
            applicationId -> {
              applicationFormData.setApplicationCreated(true);
              log.info("Application details submitted");
              session.setAttribute(APPLICATION_ID, applicationId);
            })
        .thenReturn("redirect:/application/agreement");
  }
}
