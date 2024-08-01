package uk.gov.laa.ccms.caab.controller.application.summary;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DISABILITY;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ETHNIC_ORIGIN;
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
import uk.gov.laa.ccms.caab.bean.ClientFormDataMonitoringDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientEqualOpportunitiesMonitoringDetailsValidator;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.service.LookupService;

/**
 * Controller for handling equal opportunities monitoring client details selection during the
 * new application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({CLIENT_FLOW_FORM_DATA})
public class EditClientEqualOpportunitiesMonitoringDetailsController {

  private final LookupService lookupService;

  private final ClientEqualOpportunitiesMonitoringDetailsValidator validator;

  @ModelAttribute("monitoringDetails")
  public ClientFormDataMonitoringDetails getMonitoringDetails() {
    return new ClientFormDataMonitoringDetails();
  }

  /**
   * Handles the GET request for edit client equal opportunities monitoring details page.
   *
   * @param clientFlowFormData The data for create client flow.
   * @param model The model for the view.
   * @return The view name for the client equal opportunities monitoring details page
   */
  @GetMapping("/application/sections/client/details/equal-opportunities-monitoring")
  public String clientDetailsEqualOpportunitiesMonitoring(
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) ClientFlowFormData clientFlowFormData,
      Model model) {

    populateDropdowns(model);
    ClientFormDataMonitoringDetails monitoringDetails = clientFlowFormData.getMonitoringDetails();
    monitoringDetails.setClientFlowFormAction(clientFlowFormData.getAction());

    if (clientFlowFormData.getMonitoringDetails() != null) {
      model.addAttribute("monitoringDetails",
          clientFlowFormData.getMonitoringDetails());
    }

    return "application/sections/client-equal-opportunities-monitoring";
  }

  /**
   * Handles the edit client equal opportunities monitoring details results submission.
   *
   * @param clientFlowFormData The data for create client flow.
   * @param monitoringDetails The monitoring details of the client.
   * @param bindingResult Validation result.
   * @param model The model for the view.
   * @return A redirect string to the client summary page.
   */
  @PostMapping("/application/sections/client/details/equal-opportunities-monitoring")
  public String clientDetailsEqualOpportunitiesMonitoring(
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) ClientFlowFormData clientFlowFormData,
      @ModelAttribute("monitoringDetails") ClientFormDataMonitoringDetails monitoringDetails,
      BindingResult bindingResult,
      Model model) {

    validator.validate(monitoringDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      populateDropdowns(model);
      return "application/sections/client-equal-opportunities-monitoring";
    }

    clientFlowFormData.setMonitoringDetails(monitoringDetails);
    model.addAttribute(CLIENT_FLOW_FORM_DATA, clientFlowFormData);

    return "redirect:/application/sections/client/details/summary";
  }

  private void populateDropdowns(Model model) {
    new DropdownBuilder(model)
        .addDropdown("ethnicOrigins",
            lookupService.getCommonValues(COMMON_VALUE_ETHNIC_ORIGIN))
        .addDropdown("disabilities",
            lookupService.getCommonValues(COMMON_VALUE_DISABILITY))
        .build();
  }
}
