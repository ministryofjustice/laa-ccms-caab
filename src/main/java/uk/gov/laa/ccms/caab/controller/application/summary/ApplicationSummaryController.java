package uk.gov.laa.ccms.caab.controller.application.summary;


import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationFullSummaryDisplay;
import uk.gov.laa.ccms.caab.model.ApplicationSummaryDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.UserDetail;

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
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final HttpSession session,
      final Model model) {

    final ApplicationDetail application =
        Optional.ofNullable(applicationService.getApplication(applicationId).block())
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve application detail"));

    final ApplicationSummaryDisplay summary =
        Optional.ofNullable(applicationService.getApplicationSummary(application, user))
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve application summary"));

    model.addAttribute("summary", summary);

    final ActiveCase activeCase = ActiveCase.builder()
        .applicationId(application.getId())
        .caseReferenceNumber(summary.getCaseReferenceNumber())
        .providerId(application.getProviderDetails().getProvider().getId())
        .client(summary.getClientFullName())
        .clientReferenceNumber(summary.getClientReferenceNumber())
        .providerCaseReferenceNumber(summary.getProviderCaseReferenceNumber())
        .build();

    model.addAttribute(ACTIVE_CASE, activeCase);
    session.setAttribute(ACTIVE_CASE, activeCase);
    session.removeAttribute(CLIENT_FLOW_FORM_DATA);

    return "application/summary/summary-task-page";
  }

  /**
   * Handles the GET request for application full summary page.
   *
   * @param applicationId The id of the application
   * @param session The http session for the view.
   * @param model The model for the view.
   * @return The view name for the application summary page.
   */
  @GetMapping("/application/summary/full")
  public String applicationFullSummary(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final HttpSession session,
      final Model model) {

    final ApplicationDetail application =
        Optional.ofNullable(applicationService.getApplication(applicationId).block())
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve application detail"));

    final ApplicationFullSummaryDisplay summary =
        Optional.ofNullable(applicationService.getApplicationFullSummary(application, user))
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve full application summary"));

    model.addAttribute("summary", summary);

    return "application/summary/full-summary";
  }

}
