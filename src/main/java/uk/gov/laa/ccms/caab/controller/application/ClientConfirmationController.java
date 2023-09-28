package uk.gov.laa.ccms.caab.controller.application;


import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_INFORMATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
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
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.mapper.ClientResultDisplayMapper;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;


/**
 * Controller for handling client confirmation operations.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {
    USER_DETAILS,
    APPLICATION_DETAILS,
    CLIENT_INFORMATION
})
public class ClientConfirmationController {

  private final ClientService clientService;

  private final ApplicationService applicationService;

  private final ClientResultDisplayMapper clientResultDisplayMapper;

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
  public String clientConfirm(@PathVariable("client-reference-number") String clientReferenceNumber,
                              @SessionAttribute(USER_DETAILS) UserDetail user,
                              Model model, HttpSession session) {

    ClientDetail clientInformation = clientService.getClient(
            clientReferenceNumber,
            user.getLoginId(),
            user.getUserType()).block();

    session.setAttribute(CLIENT_INFORMATION, clientInformation);
    model.addAttribute("clientReferenceNumber", clientReferenceNumber);
    model.addAttribute("client", clientResultDisplayMapper
            .toClientResultRowDisplay(clientInformation));

    return "application/application-client-confirmation";
  }


  /**
   * Handles the POST request for confirmed client submission.
   *
   * @param confirmedClientReference The confirmed client reference number.
   * @param applicationDetails The application details from session.
   * @param clientInformation The client information from session.
   * @param user The user details from session.
   * @return A Mono containing a redirect string or error.
   */
  @PostMapping("/application/client/confirmed")
  public Mono<String> clientConfirmed(
          String confirmedClientReference,
          @SessionAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
          @SessionAttribute(CLIENT_INFORMATION) ClientDetail clientInformation,
          @SessionAttribute(USER_DETAILS) UserDetail user) {

    if (!confirmedClientReference.equals(clientInformation.getClientReferenceNumber())) {
      throw new RuntimeException("Client information does not match");
    }

    return applicationService.createApplication(applicationDetails, clientInformation, user)
        .doOnSuccess(createdApplication -> {
          applicationDetails.setApplicationCreated(true);
          log.info("Application details submitted");
        })
        .thenReturn("redirect:/application/agreement");
  }
}
