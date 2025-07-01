package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.DelegatedFunctionsValidator;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.util.DateUtils;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller responsible for handling the application's delegated functions operations.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({APPLICATION_FORM_DATA})
public class DelegatedFunctionsController {

  private final DelegatedFunctionsValidator delegatedFunctionsValidator;
  private final ApplicationService applicationService;

  /**
   * Displays the delegated functions selection page.
   *
   * @param applicationFormData The details of the current application.
   * @return The path to the delegated functions selection view.
   */
  @GetMapping("/{caseContext}/delegated-functions")
  public String delegatedFunction(
      @PathVariable("caseContext") final CaseContext caseContext,
      @ModelAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData) {
    log.info("DelegatedFunction called with caseContext: {}", caseContext);
    return "application/select-delegated-functions";
  }

  /**
   * Processes the user's delegated functions selection and redirects accordingly.
   *
   * @param applicationFormData The details of the current application.
   * @param bindingResult       Validation result for the delegated functions form.
   * @return The path to the next step in the application process or the current page based on
   *         validation.
   */
  @PostMapping("/{caseContext}/delegated-functions")
  public String delegatedFunction(
      @PathVariable("caseContext") final CaseContext caseContext,
      @ModelAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData,
      BindingResult bindingResult) {

    delegatedFunctionsValidator.validate(applicationFormData, bindingResult);

    if (!applicationFormData.isDelegatedFunctions()) {
      applicationFormData.setDelegatedFunctionUsedDate(null);
    }

    if (bindingResult.hasErrors()) {
      return "application/select-delegated-functions";
    }

    if (caseContext.isAmendment()) {
      return "redirect:/amendments/create";
    }

    return "redirect:/application/client/search";
  }


  /**
   * Displays the delegated functions selection page.
   *
   * @return The path to the delegated functions selection view.
   */
  @GetMapping("/amendments/edit-delegated-functions")
  public String editDelegatedFunction(
      @SessionAttribute(APPLICATION) final ApplicationDetail tdsApplication,
      HttpSession httpSession, Model model) {

    Assert.notNull(tdsApplication.getApplicationType(), "TDS Application type must not be null");

    ApplicationFormData applicationFormData =
        (ApplicationFormData) httpSession.getAttribute(APPLICATION_FORM_DATA);

    if (applicationFormData == null) {
      applicationFormData = new ApplicationFormData();
    }

    applicationFormData.setDelegatedFunctions(
        Boolean.TRUE.equals(tdsApplication.getApplicationType().getDevolvedPowers().getUsed()));
    applicationFormData.setDelegatedFunctionUsedDate(
        tdsApplication.getApplicationType().getDevolvedPowers().getDateUsed() != null
            ? DateUtils.convertToComponentDate(
            tdsApplication.getApplicationType().getDevolvedPowers().getDateUsed())
            : null);

    model.addAttribute(APPLICATION_FORM_DATA, applicationFormData);
    model.addAttribute("caseContext", CaseContext.AMENDMENTS);
    model.addAttribute("edit", true);

    return "application/select-delegated-functions";
  }

  /**
   * Processes the user's delegated functions selection and redirects accordingly.
   *
   * @param applicationFormData The details of the current application.
   * @param bindingResult       Validation result for the delegated functions form.
   * @return The path to the next step in the application process or the current page based on
   *         validation.
   */
  @PostMapping("/amendments/edit-delegated-functions")
  public String editDelegatedFunction(
      @SessionAttribute(APPLICATION) final ApplicationDetail tdsApplication,
      @ModelAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData,
      @SessionAttribute(USER_DETAILS) UserDetail user,
      BindingResult bindingResult, Model model) {

    delegatedFunctionsValidator.validate(applicationFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      model.addAttribute("caseContext", CaseContext.AMENDMENTS);
      model.addAttribute("edit", true);
      return "application/select-delegated-functions";
    }

    if (!applicationFormData.isDelegatedFunctions()) {
      applicationFormData.setDelegatedFunctionUsedDate(null);
    }

    ApplicationType applicationType = tdsApplication.getApplicationType();
    applicationType.getDevolvedPowers().setUsed(applicationFormData.isDelegatedFunctions());
    applicationType.getDevolvedPowers().setDateUsed(applicationFormData.isDelegatedFunctions()
        ? DateUtils.convertToDate(applicationFormData.getDelegatedFunctionUsedDate()) : null);

    applicationService.putApplicationTypeFormData(tdsApplication.getId(), applicationType, user);

    return "redirect:/amendments/sections/linked-cases";
  }

}
