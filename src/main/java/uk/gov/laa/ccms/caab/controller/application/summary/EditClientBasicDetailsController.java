package uk.gov.laa.ccms.caab.controller.application.summary;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;

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
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientBasicDetailsValidator;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.service.CommonLookupService;

/**
 * Controller for handling edits to client basic details during the application summary process.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
@SessionAttributes({CLIENT_FLOW_FORM_DATA})
public class EditClientBasicDetailsController {

  private final CommonLookupService commonLookupService;

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
  @GetMapping("/application/summary/client/details/basic")
  public String getClientDetailsBasic(
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) ClientFlowFormData clientFlowFormData,
      Model model) {

    populateDropdowns(model);
    ClientFormDataBasicDetails basicDetails = clientFlowFormData.getBasicDetails();
    basicDetails.setClientFlowFormAction(clientFlowFormData.getAction());

    model.addAttribute("basicDetails", basicDetails);
    return "application/summary/client-basic-details";
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
  @PostMapping("/application/summary/client/details/basic")
  public String postClientDetailsBasic(
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) ClientFlowFormData clientFlowFormData,
      @ModelAttribute("basicDetails") ClientFormDataBasicDetails basicDetails,
      BindingResult bindingResult,
      Model model) {

    clientBasicDetailsValidator.validate(basicDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      populateDropdowns(model);
      return "application/summary/client-basic-details";
    }

    clientFlowFormData.setBasicDetails(basicDetails);
    model.addAttribute(CLIENT_FLOW_FORM_DATA, clientFlowFormData);

    return "redirect:/application/summary/client/details/summary";
  }

  /**
   * Populates dropdown options for the client basic details form.
   *
   * @param model The model for the view.
   */
  private void populateDropdowns(Model model) {
    DropdownBuilder builder = new DropdownBuilder(model);

    builder
        .addDropdown("titles",
            commonLookupService.getContactTitles())
        .addDropdown("countries",
            commonLookupService.getCountries())
        .addDropdown("genders",
            commonLookupService.getGenders())
        .addDropdown("maritalStatusList",
            commonLookupService.getMaritalStatuses())
        .build();
  }
}
