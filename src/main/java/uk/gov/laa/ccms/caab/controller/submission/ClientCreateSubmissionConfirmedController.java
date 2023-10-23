package uk.gov.laa.ccms.caab.controller.submission;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_INFORMATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import java.text.ParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

/**
 * Controller for confirmed create client submissions.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ClientCreateSubmissionConfirmedController {

  private final ApplicationService applicationService;

  /**
   * Handles the POST request for a client creation submission.
   *
   * @param applicationFormData The application details from session.
   * @param user The user details from session.
   * @param clientInformation The client details from the session
   * @param session The http session for the view.
   * @return The view name for a client creation submission page.
   */
  @PostMapping("/submissions/client-create/confirmed")
  public String submissionConfirmed(
      @SessionAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData,
      @SessionAttribute(USER_DETAILS) UserDetail user,
      @SessionAttribute(CLIENT_INFORMATION) ClientDetail clientInformation,
      HttpSession session
  ) throws ParseException {

    return applicationService.createApplication(applicationFormData, clientInformation, user)
        .doOnSuccess(applicationId -> {
          log.info("Application details submitted");
          log.debug("Application details submitted: {}", applicationFormData);
          session.removeAttribute(APPLICATION_FORM_DATA);
          session.removeAttribute(CLIENT_INFORMATION);
          session.setAttribute(APPLICATION_ID, applicationId);
        })
        .thenReturn("redirect:/application/summary").block();
  }


}
