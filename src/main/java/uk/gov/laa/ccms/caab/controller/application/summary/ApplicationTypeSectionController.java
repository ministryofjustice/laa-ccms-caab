package uk.gov.laa.ccms.caab.controller.application.summary;


import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.text.ParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.DelegatedFunctionsValidator;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller for the application's application type section.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(APPLICATION_FORM_DATA)
public class ApplicationTypeSectionController {

  private final ApplicationService applicationService;

  private final DelegatedFunctionsValidator delegatedFunctionsValidator;

  /**
   * Creates a new instance of {@link uk.gov.laa.ccms.caab.bean.ApplicationFormData}.
   *
   * @return A new instance of {@link uk.gov.laa.ccms.caab.bean.ApplicationFormData}.
   */
  @ModelAttribute(APPLICATION_FORM_DATA)
  public ApplicationFormData getApplicationDetails() {
    return new ApplicationFormData();
  }

  /**
   * Handles the GET request for the application type section of application summary.
   *
   * @param applicationId The id of the application
   * @param activeCase The active case details to display in the header
   * @param model The model for the view.
   * @return The view name for the application summary page.
   */
  @GetMapping("/application/summary/application-type")
  public String applicationSummaryApplicationType(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      @ModelAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData,
      Model model) {

    applicationFormData = applicationService.getApplicationTypeFormData(applicationId);

    model.addAttribute(ACTIVE_CASE, activeCase);
    model.addAttribute(APPLICATION_FORM_DATA, applicationFormData);

    return "application/summary/application-type-section";
  }

  /**
   * Processes the user's delegated functions selection and redirects accordingly.
   *
   * @param applicationId The id of the application
   * @param activeCase The active case details to display in the header
   * @param user The details of the active user
   * @param applicationFormData The details of the current application.
   * @param bindingResult Validation result for the delegated functions form.
   * @return The path to the next step in the application summary edit or the current page based on
   *         validation.
   */
  @PostMapping("/application/summary/application-type")
  public String delegatedFunction(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      @SessionAttribute(USER_DETAILS) UserDetail user,
      @ModelAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData,
      BindingResult bindingResult,
      Model model) throws ParseException {
    delegatedFunctionsValidator.validate(applicationFormData, bindingResult);

    model.addAttribute("activeCase", activeCase);

    if (!applicationFormData.isDelegatedFunctions()) {
      applicationFormData.setDelegatedFunctionUsedDay(null);
      applicationFormData.setDelegatedFunctionUsedMonth(null);
      applicationFormData.setDelegatedFunctionUsedYear(null);
    }

    if (bindingResult.hasErrors()) {
      return "application/summary/application-type-section";
    }

    applicationService.patchApplicationType(applicationId, applicationFormData, user);

    return "redirect:/application/summary";
  }



}
