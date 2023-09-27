package uk.gov.laa.ccms.caab.controller.application.submission;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_INFORMATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

/**
 * Controller for confirmed submissions.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({
    CLIENT_INFORMATION,
    APPLICATION_DETAILS,
    USER_DETAILS
})
public class ClientCreateSubmissionConfirmedController {

  private final ClientService clientService;

  private final ApplicationService applicationService;

  @GetMapping("submissions/client-create/confirmed")
  public String submissionsConfirmed() {
    log.info("GET /submissions/client-create/confirmed");

    return "submissions/submissionsConfirmed";
  }

  @PostMapping("submissions/client-create/confirmed")
  public Mono<String> submissionsConfirmed(
      @SessionAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
      @SessionAttribute(USER_DETAILS) UserDetail user,
      @SessionAttribute(CLIENT_INFORMATION) ClientDetail clientInformation
  ){

    return applicationService.createApplication(applicationDetails, clientInformation, user)
        .doOnSuccess(createdApplication -> {
          applicationDetails.setApplicationCreated(true);
          log.info("Application details submitted: {}", applicationDetails);
        })
        .thenReturn("redirect:/application/summary");
  }
}
