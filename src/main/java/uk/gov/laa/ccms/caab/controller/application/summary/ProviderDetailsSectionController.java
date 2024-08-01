package uk.gov.laa.ccms.caab.controller.application.summary;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.Collections;
import java.util.List;
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
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.ProviderDetailsValidator;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller for the application's provider details section.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ProviderDetailsSectionController {

  private final ApplicationService applicationService;

  private final ProviderService providerService;

  private final ProviderDetailsValidator providerDetailsValidator;

  /**
   * Handles the GET request for the application type section of application summary.
   *
   * @param applicationId The id of the application
   * @param activeCase The active case details to display in the header
   * @param model The model for the view.
   * @return The view name for the application summary page.
   */
  @GetMapping("/application/sections/provider-details")
  public String applicationSummaryProviderDetails(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      @SessionAttribute(USER_DETAILS) UserDetail user,
      Model model) {

    ApplicationFormData applicationFormData =
        applicationService.getProviderDetailsFormData(applicationId);

    populateDropdowns(applicationFormData, user, model);
    model.addAttribute(ACTIVE_CASE, activeCase);

    return "application/sections/provider-details-section";
  }

  /**
   * Processes the applications provider details selection and redirects accordingly.
   *
   * @param applicationId The id of the application
   * @param activeCase The active case details to display in the header
   * @param user The details of the active user
   * @param applicationFormData The details of the current application.
   * @param bindingResult Validation result for the delegated functions form.
   * @param model The model for the view.
   * @return The path to the next step in the application summary edit or the current page based on
   *         validation.
   */
  @PostMapping("/application/sections/provider-details")
  public String applicationSummaryProviderDetails(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      @SessionAttribute(USER_DETAILS) UserDetail user,
      @ModelAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData,
      BindingResult bindingResult,
      Model model) {

    providerDetailsValidator.validate(applicationFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      populateDropdowns(applicationFormData, user, model);
      model.addAttribute(ACTIVE_CASE, activeCase);

      return "application/sections/provider-details-section";
    }

    applicationService.updateProviderDetails(applicationId, applicationFormData, user);

    return "redirect:/application/sections";
  }

  /**
   * Populates dropdown options for the provider details selection section.
   *
   * @param applicationFormData The details of the current application.
   * @param user The details of the active user
   * @param model The model for the view.
   */
  private void populateDropdowns(
      ApplicationFormData applicationFormData,
      UserDetail user,
      Model model) {

    ProviderDetail provider = Optional.ofNullable(user.getProvider())
        .map(providerData -> providerService.getProvider(providerData.getId()).block())
        .orElse(null);

    List<ContactDetail> feeEarners = Optional.ofNullable(provider)
        .map(p -> providerService.getFeeEarnersByOffice(p, applicationFormData.getOfficeId()))
        .orElse(Collections.emptyList());

    List<ContactDetail> contactNames = Optional.ofNullable(provider)
        .map(ProviderDetail::getContactNames)
        .orElse(Collections.emptyList());

    model.addAttribute("feeEarners", feeEarners);
    model.addAttribute("contactNames", contactNames);
    model.addAttribute(APPLICATION_FORM_DATA, applicationFormData);

  }

}
