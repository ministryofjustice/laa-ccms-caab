package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.EXCLUDED_APPLICATION_TYPE_CODES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.ApplicationTypeValidator;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

/**
 * Controller for handling application type selection during the application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({APPLICATION_FORM_DATA})
public class ApplicationTypeController {

  private final ApplicationTypeValidator applicationTypeValidator;
  private final ApplicationService applicationService;
  private final LookupService lookupService;

  /**
   * Handles the GET request for application type selection page.
   *
   * @param applicationFormData The application details from session.
   * @param model               The model for the view.
   * @return The view name for the application type selection page or a redirect if exceptional
   *     funding.
   */
  @GetMapping("/{caseContext}/application-type")
  public String applicationType(
      @PathVariable("caseContext") final CaseContext caseContext,
      @ModelAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData,
      Model model) {

    if (applicationFormData.isExceptionalFunding()) {
      log.warn("ApplicationTypeController hit despite exceptionalFunding being true. "
          + "Redirecting to client-search");
      return "redirect:/application/client/search";
    }

    model.addAttribute("applicationTypes", getFilteredApplicationTypes());

    return "application/select-application-type";
  }

  /**
   * Handles the POST request for application type selection form submission.
   *
   * @param applicationFormData The application details from session.
   * @param bindingResult       The result of data binding/validation.
   * @param model               The model for the view.
   * @return A redirect string or view name based on validation result.
   */
  @PostMapping("/{caseContext}/application-type")
  public String applicationType(
      @PathVariable("caseContext") final CaseContext caseContext,
      @ModelAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData,
      BindingResult bindingResult,
      Model model) {
    applicationTypeValidator.validate(applicationFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      model.addAttribute("applicationTypes", getFilteredApplicationTypes());
      return "application/select-application-type";
    }

    // When amendments, if amendment type is substantive, skip delegated functions and create the
    //  amendment.
    if (caseContext.isAmendment()
        && APP_TYPE_SUBSTANTIVE.equals(applicationFormData.getApplicationTypeCategory())) {
      return "redirect:/amendments/create";
    }

    return "redirect:/%s/delegated-functions".formatted(caseContext.getPathValue());
  }

  private List<CommonLookupValueDetail> getFilteredApplicationTypes() {
    return Optional.ofNullable(lookupService.getCommonValues(
            COMMON_VALUE_APPLICATION_TYPE).block())
        .orElse(new CommonLookupDetail())
        .getContent()
        .stream()
        .filter(applicationType -> {
          String code = applicationType.getCode().toUpperCase();
          return !EXCLUDED_APPLICATION_TYPE_CODES.contains(code);
        })
        .collect(Collectors.toList());
  }
}
