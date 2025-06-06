package uk.gov.laa.ccms.caab.controller.application.section;


import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SECTIONS_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.validators.application.ApplicationSectionValidator;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller for the application sections.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ApplicationSectionsController {

  private final ApplicationService applicationService;

  private final ApplicationSectionValidator applicationSectionValidator;

  /**
   * Handles the GET request for application summary page.
   *
   * @param applicationId The id of the application.
   * @param user The user requesting the summary.
   * @param session The http session for the view.
   * @param model The model for the view.
   * @return The view name for the application summary page.
   */
  @GetMapping("/application/sections")
  public String applicationSections(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final HttpSession session,
      final Model model) {

    final ApplicationDetail application =
        Optional.ofNullable(applicationService.getApplication(applicationId).block())
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve application detail"));

    final ApplicationSectionDisplay sections =
        Optional.ofNullable(applicationService.getApplicationSections(application, user))
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve section for application summary"));

    model.addAttribute("summary", sections);

    final ActiveCase activeCase = ActiveCase.builder()
        .applicationId(application.getId())
        .caseReferenceNumber(sections.getCaseReferenceNumber())
        .providerId(application.getProviderDetails().getProvider().getId())
        .client(sections.getClient().getClientFullName())
        .clientReferenceNumber(sections.getClient().getClientReferenceNumber())
        .providerCaseReferenceNumber(sections.getProvider().getProviderCaseReferenceNumber())
        .build();

    model.addAttribute(ACTIVE_CASE, activeCase);
    session.setAttribute(ACTIVE_CASE, activeCase);
    session.removeAttribute(CLIENT_FLOW_FORM_DATA);
    session.setAttribute(SECTIONS_DATA, sections);

    //create a new base object to store the form data
    model.addAttribute("formData", new Object());

    return "application/sections/task-page";
  }

  /**
   * Handles the GET request for the in-progress application summary page.
   *
   * @param activeCase The active case details
   * @param user The user requesting the summary.
   * @param model The model for the view.
   * @return The view name for the full application summary page.
   */
  @GetMapping("/application/sections/summary")
  public String viewInProgressSummary(
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      final Model model) {

    final ApplicationDetail application =
        Optional.ofNullable(applicationService.getApplication(
                String.valueOf(activeCase.getApplicationId())).block())
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve application detail"));

    final ApplicationSectionDisplay inProgressSummary =
        Optional.ofNullable(applicationService.getApplicationSections(application, user))
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve application summary"));

    model.addAttribute("summary", inProgressSummary);

    return "application/sections/application-summary";
  }



  /**
   * Handles the completion of an application section and performs validation.
   *
   * @param sectionData the data of the application section being completed
   * @param formData the form data submitted by the user
   * @param bindingResult the result of binding form data to the model
   * @param model the model used to pass data to the view
   * @return the view name for the task page if there are validation errors, or a redirect to the
   *         validation page if the section is valid
   */
  @PostMapping("/application/sections")
  public String completeApplication(
      @SessionAttribute(SECTIONS_DATA) final ApplicationSectionDisplay sectionData,
      @ModelAttribute("formData") final Object formData,
      final BindingResult bindingResult,
      final Model model) {

    //simple validation to ensure all sections are complete
    applicationSectionValidator.validate(sectionData, bindingResult);

    if (bindingResult.hasErrors()) {
      model.addAttribute("summary", sectionData);
      return "application/sections/task-page";
    }

    return "redirect:/application/validate";
  }

}
