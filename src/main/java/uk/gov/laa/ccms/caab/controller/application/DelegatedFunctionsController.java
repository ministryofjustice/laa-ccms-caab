package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.ContextConstants.CONTEXT_NAME;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.DelegatedFunctionsValidator;
import uk.gov.laa.ccms.caab.constants.ContextConstants;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller responsible for handling the application's delegated functions operations.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({APPLICATION_FORM_DATA})
public class DelegatedFunctionsController {

  private final ApplicationService applicationService;
  private final DelegatedFunctionsValidator delegatedFunctionsValidator;

  /**
   * Displays the delegated functions selection page.
   *
   * @param applicationFormData The details of the current application.
   * @return The path to the delegated functions selection view.
   */
  @GetMapping("/{" + CONTEXT_NAME + "}/delegated-functions")
  public String delegatedFunction(
          @PathVariable(CONTEXT_NAME) final String caseContext,
          @ModelAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData) {
    return "application/select-delegated-functions";
  }

  /**
   * Processes the user's delegated functions selection and redirects accordingly.
   *
   * @param applicationFormData The details of the current application.
   * @param bindingResult Validation result for the delegated functions form.
   * @return The path to the next step in the application process or the current page based on
   *         validation.
   */
  @PostMapping("/{" + CONTEXT_NAME + "}/delegated-functions")
  public String delegatedFunction(
          @PathVariable(CONTEXT_NAME) final String caseContext,
          @SessionAttribute(USER_DETAILS) final UserDetail userDetails,
          @SessionAttribute(CASE) ApplicationDetail applicationDetail,
          @ModelAttribute(APPLICATION_FORM_DATA) ApplicationFormData applicationFormData,
          BindingResult bindingResult) {
    delegatedFunctionsValidator.validate(applicationFormData, bindingResult);

    if (!applicationFormData.isDelegatedFunctions()) {
      applicationFormData.setDelegatedFunctionUsedDate(null);
    }

    if (bindingResult.hasErrors()) {
      return "application/select-delegated-functions";
    }

    if (ContextConstants.AMENDMENTS.equals(caseContext)) {
      applicationService.createAndSubmitAmendmentForCase(applicationFormData,
          applicationDetail.getCaseReferenceNumber(),
          userDetails);
      // TODO: Redirect to amend case screen once implemented in CCMSPUI-504
      return "redirect:/application/%s/view".formatted(applicationDetail.getCaseReferenceNumber());
    }
    return "redirect:/application/client/search";
  }

}
