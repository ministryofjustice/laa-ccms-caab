package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;

/**
 * Controller responsible for handling requests related to cases.
 */
@RequiredArgsConstructor
@Controller
public class CaseController {

  private final ApplicationService applicationService;

  /**
   * Displays the case details screen.
   *
   * @param ebsCase The case details from EBS.
   * @param model   The model used to pass data to the view.
   * @return The case details view.
   */

  @GetMapping("/cases/details")
  public String caseDetails(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase, Model model) {

    final ApplicationSectionDisplay applicationSectionDisplay =
        Optional.ofNullable(applicationService.getCaseDetailsDisplay(ebsCase))
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve application summary"));

    model.addAttribute("summary", applicationSectionDisplay);

    return "application/case-details";
  }

  @GetMapping("/cases/details/individual")
  public String caseDetailsIndividual(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase, Model model) {

    model.addAttribute("summary", new IndividualDetailsSectionDisplay(null, null, null));
    return "application/case-details-individual";
  }


}
