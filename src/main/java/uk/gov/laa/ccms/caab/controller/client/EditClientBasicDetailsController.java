package uk.gov.laa.ccms.caab.controller.client;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_GENDER;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MARITAL_STATUS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientBasicDetailsValidator;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.caab.service.LookupService;

/**
 * Controller for handling edits to client basic details during the application summary process.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
@SessionAttributes({CLIENT_FLOW_FORM_DATA})
public class EditClientBasicDetailsController {

  private final LookupService lookupService;

  private final ClientBasicDetailsValidator clientBasicDetailsValidator;

  @ModelAttribute("basicDetails")
  public ClientFormDataBasicDetails getBasicDetails() {
    return new ClientFormDataBasicDetails();
  }

  /**
   * Handles the GET request for edit client basic details page.
   *
   * @param clientFlowFormData The data for client flow.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("/{caseContext}/sections/client/details/basic")
  public String getClientDetailsBasic(
      @PathVariable("caseContext") final CaseContext context,
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) final ClientFlowFormData clientFlowFormData,
      final Model model) {

    populateDropdowns(model);
    ClientFormDataBasicDetails basicDetails = clientFlowFormData.getBasicDetails();
    basicDetails.setClientFlowFormAction(clientFlowFormData.getAction());

    model.addAttribute("basicDetails", basicDetails);
    return "application/sections/client-basic-details";
  }

  /**
   * Handles the edit basic client details submission.
   *
   * @param clientFlowFormData The data for client flow.
   * @param basicDetails The basic details of the client.
   * @param bindingResult Validation result.
   * @param model The model for the view.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/{caseContext}/sections/client/details/basic")
  public String postClientDetailsBasic(
      @PathVariable("caseContext") final CaseContext caseContext,
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) final  ClientFlowFormData clientFlowFormData,
      @ModelAttribute("basicDetails") final ClientFormDataBasicDetails basicDetails,
      final BindingResult bindingResult,
      final Model model) {

    clientBasicDetailsValidator.validate(basicDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      populateDropdowns(model);
      return "application/sections/client-basic-details";
    }

    clientFlowFormData.setBasicDetails(basicDetails);
    model.addAttribute(CLIENT_FLOW_FORM_DATA, clientFlowFormData);

    return "redirect:/%s/sections/client/details/summary".formatted(caseContext.getPathValue());
  }

  /**
   * Populates dropdown options for the client basic details form.
   *
   * @param model The model for the view.
   */
  private void populateDropdowns(final Model model) {
    final DropdownBuilder builder = new DropdownBuilder(model);

    builder
        .addDropdown("titles",
            lookupService.getCommonValues(COMMON_VALUE_CONTACT_TITLE))
        .addDropdown("countries",
            lookupService.getCountries())
        .addDropdown("genders",
            lookupService.getCommonValues(COMMON_VALUE_GENDER))
        .addDropdown("maritalStatusList",
            lookupService.getCommonValues(COMMON_VALUE_MARITAL_STATUS))
        .build();
  }
}
