package uk.gov.laa.ccms.caab.controller.application;


import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.ApplicationSummaryDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;

/**
 * Controller for the application summary.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ApplicationSummaryController {

  private final ApplicationService applicationService;



  /**
   * Handles the GET request for application summary page.
   *
   * @param applicationId The id of the application
   * @param session The http session for the view.
   * @param model The model for the view.
   * @return The view name for the application summary page.
   */
  @GetMapping("/application/summary")
  public String applicationSummary(
      @SessionAttribute(APPLICATION_ID) String applicationId,
      HttpSession session,
      Model model) {

    ApplicationSummaryDisplay summary =
        applicationService.getApplicationSummary(applicationId).block();

    model.addAttribute("summary", summary);

    ActiveCase activeCase = ActiveCase.builder()
        .caseReferenceNumber(summary.getCaseReferenceNumber())
        .client(summary.getClientFullName())
        .providerCaseReferenceNumber(summary.getProviderCaseReferenceNumber())
        .build();

    model.addAttribute("activeCase", activeCase);
    session.setAttribute("activeCase", activeCase);

    return "application/summary-task-page";
  }

}
