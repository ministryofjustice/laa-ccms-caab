package uk.gov.laa.ccms.caab.controller.application.summary;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.model.OpponentRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;

/**
 * Controller for the application's opponents and other parties section.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class EditOpponentsSectionController {

  private final ApplicationService applicationService;

  /**
   * Handles the GET request to fetch and display the opponents and other parties for a specific
   * application.
   *
   * @param applicationId The id of the application, retrieved from the session.
   * @param model The Model object to add attributes to for the view.
   * @param session The HttpSession object to add attributes to for the session.
   * @return The name of the view to be rendered.
   */
  @GetMapping("/application/summary/opponents")
  public String opponents(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      final Model model,
      final HttpSession session) {

    final ResultsDisplay<OpponentRowDisplay> opponents =
        applicationService.getOpponents(applicationId);

    model.addAttribute("opponents", opponents);
    session.setAttribute("opponents", opponents);

    return "application/summary/opponents-section";
  }
}
