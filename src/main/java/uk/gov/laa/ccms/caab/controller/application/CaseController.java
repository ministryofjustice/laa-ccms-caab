package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller responsible for handling requests related to cases.
 */
@RequiredArgsConstructor
@Controller
@Slf4j
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

  /**
   * Returns a display object containing an other party within a case.
   *
   * @param ebsCase The case details from EBS.
   * @param index   Index number of the OtherParty within the ebsCase.
   * @param model   The model used to pass data to the view.
   * @return The case details other party view.
   */
  @GetMapping("/cases/details/other-party/{index}")
  public String caseDetailsOtherParty(
      @SessionAttribute(CASE) final ApplicationDetail ebsCase,
      @PathVariable("index") final int index,
      Model model) {

    if (Objects.isNull(ebsCase.getOpponents()) || index >= ebsCase.getOpponents().size()) {
      throw new CaabApplicationException("Could not find opponent with index " + index);
    }

    final OpponentDetail opponentDetail = ebsCase.getOpponents().get(index);
    final IndividualDetailsSectionDisplay opponentDisplay =
        applicationService.getIndividualDetailsSectionDisplay(opponentDetail);

    model.addAttribute("otherParty", opponentDisplay);
    return "application/case-details-other-party";
  }

  /**
   * Handles the request to abandon amendments for a specific case.
   * This method is triggered by a GET request to display the confirmation page
   * for abandoning amendments.
   *
   * @param ebsCase the application details for the current case,
   *                retrieved from the session attribute
   * @return a string representing the view name for confirming the abandonment of amendments
   */

  /**
   * Displays the prior authority details for a given case.
   * Retrieves a specific prior authority detail using the provided index and adds it to the model
   * to be displayed in the view.
   *
   * @param ebsCase The case details retrieved from the session.
   * @param index   The zero-based index of the prior authority to be retrieved from
   *                the case details.
   * @param model   The model used to pass data to the view.
   * @return The view name for the prior authority review page.
   * @throws IllegalArgumentException if the list of prior authorities is empty or
   *                                  the specified index is invalid.
   */
  @GetMapping("/cases/details/prior-authority/{index}")
  public String getCaseDetailsView(@SessionAttribute(CASE) final ApplicationDetail ebsCase,
                                   @PathVariable final int index,
                                   Model model) {
    List<PriorAuthorityDetail> priorAuthorities = ebsCase.getPriorAuthorities();
    String errorMessage = "Could not find prior authority with index: %s".formatted(index);
    Assert.notEmpty(priorAuthorities, () -> errorMessage);
    Assert.isTrue(index < priorAuthorities.size(), () -> errorMessage);

    model.addAttribute("priorAuthority", priorAuthorities.get(index));
    return "application/prior-authority-review";
  }

  @GetMapping("/cases/amendment/abandon")
  public String handleAbandon(@SessionAttribute(CASE) final ApplicationDetail ebsCase) {
    log.info("Abandoning amendments requested for case id {}", ebsCase.getId());
    return "application/amendment-remove";
  }

  /**
   * Handles the confirmation of abandoning amendments for a specific case.
   * This method processes the request to abandon any ongoing amendments for the given case
   * and logs the associated information.
   *
   * @param ebsCase the application details for the current case,
   *                retrieved from the session attribute
   * @param user    the user details of the currently logged-in user,
   *                retrieved from the session attribute
   * @return a string representing the view name to be displayed after the amendments are abandoned
   */
  @PostMapping("/cases/amendment/abandon")
  public String handleAbandon(@SessionAttribute(CASE) final ApplicationDetail ebsCase,
                              @SessionAttribute(USER_DETAILS) UserDetail user) {
    log.info("Abandoning amendments for case id {}", ebsCase.getId());
    applicationService.abandonApplication(ebsCase, user);
    return "application/amendment-remove";
  }

}
