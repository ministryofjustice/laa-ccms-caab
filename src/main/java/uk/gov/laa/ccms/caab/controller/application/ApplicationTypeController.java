package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.EXCLUDED_APPLICATION_TYPE_CODES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.validators.application.ApplicationTypeValidator;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

/**
 * Controller for handling application type selection during the application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(APPLICATION_DETAILS)
public class ApplicationTypeController {

  private final ApplicationTypeValidator applicationTypeValidator;

  private final CommonLookupService commonLookupService;

  /**
   * Handles the GET request for application type selection page.
   *
   * @param applicationDetails The application details from session.
   * @param model              The model for the view.
   * @return The view name for the application type selection page or a redirect if exceptional
   *     funding.
   */
  @GetMapping("/application/application-type")
  public String applicationType(
      @ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
      Model model) {
    log.info("GET /application/application-type: {}", applicationDetails);

    if (applicationDetails.isExceptionalFunding()) {
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
   * @param applicationDetails The application details from session.
   * @param bindingResult      The result of data binding/validation.
   * @param model              The model for the view.
   * @return A redirect string or view name based on validation result.
   */
  @PostMapping("/application/application-type")
  public String applicationType(
      @ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
      BindingResult bindingResult,
      Model model) {
    log.info("POST /application/application-type: {}", applicationDetails);
    applicationTypeValidator.validate(applicationDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      model.addAttribute("applicationTypes", getFilteredApplicationTypes());
      return "application/select-application-type";
    }

    return "redirect:/application/delegated-functions";
  }

  private List<CommonLookupValueDetail> getFilteredApplicationTypes() {
    return Optional.ofNullable(commonLookupService.getApplicationTypes().block())
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
