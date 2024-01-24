package uk.gov.laa.ccms.caab.controller.application.summary;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.SessionAttribute;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.model.CostStructure;
import uk.gov.laa.ccms.caab.model.PriorAuthority;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller for the application's proceedings and costs section.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class EditProceedingsAndCostsSectionController {

  private final ApplicationService applicationService;

  /**
   * Handles the GET request to fetch and display the proceedings and costs for a specific
   * application.
   *
   * @param applicationId The id of the application, retrieved from the session.
   * @param model The Model object to add attributes to for the view.
   * @param session The HttpSession object to add attributes to for the session.
   * @return The name of the view to be rendered.
   */
  @GetMapping("/application/summary/proceedings-and-costs")
  public String proceedingsAndCosts(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      final Model model,
      final HttpSession session) {

    final Mono<ResultsDisplay<Proceeding>> proceedingsMono =
        applicationService.getProceedings(applicationId);
    final Mono<CostStructure> costsMono =
        applicationService.getCosts(applicationId);
    final Mono<ResultsDisplay<PriorAuthority>> priorAuthoritiesMono =
        applicationService.getPriorAuthorities(applicationId);

    return Mono.zip(proceedingsMono,
            costsMono,
            priorAuthoritiesMono)
        .map(tuple -> {
          model.addAttribute("proceedings", tuple.getT1());
          session.setAttribute("proceedings", tuple.getT1());
          model.addAttribute("costs", tuple.getT2());
          session.setAttribute("costs", tuple.getT2());
          model.addAttribute("priorAuthorities", tuple.getT3());
          session.setAttribute("priorAuthorities", tuple.getT3());

          return "application/summary/proceedings-and-costs-section";
        }).block();
  }

  /**
   * Handles the GET request to make a specific proceeding the lead proceeding for a specific
   * application.
   *
   * @param applicationId The id of the application, retrieved from the session.
   * @param proceedingId The id of the proceeding to be made the lead proceeding.
   * @param user The UserDetail object representing the user, retrieved from the session.
   * @return A redirect instruction to the proceedings and costs view.
   */
  @GetMapping("/application/summary/proceedings/{proceeding-id}/make-lead")
  public String proceedingsMakeLead(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @PathVariable("proceeding-id") final Integer proceedingId,
      @SessionAttribute(USER_DETAILS) final UserDetail user) {

    applicationService.makeLeadProceeding(applicationId, proceedingId, user);

    return "redirect:/application/summary/proceedings-and-costs";
  }


}
