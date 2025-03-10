package uk.gov.laa.ccms.caab.controller.application.client;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_LANGUAGE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_METHOD;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataContactDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientContactDetailsValidator;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.service.LookupService;

/**
 * Controller for handling contact client details selection during the new application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({CLIENT_FLOW_FORM_DATA})
public class ClientContactDetailsController {

  private final LookupService lookupService;

  private final ClientContactDetailsValidator clientContactDetailsValidator;

  @ModelAttribute("contactDetails")
  public ClientFormDataContactDetails getContactDetails() {
    return new ClientFormDataContactDetails();
  }

  /**
   * Handles the GET request for client contact details page.
   *
   * @param clientFlowFormData The data for create client flow.
   * @param contactDetails The contact details of the client.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("/application/client/details/contact")
  public String clientDetailsContact(
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) ClientFlowFormData clientFlowFormData,
      @ModelAttribute("contactDetails") ClientFormDataContactDetails contactDetails,
      Model model) {

    populateDropdowns(model);
    contactDetails.setVulnerableClient(
        clientFlowFormData.getBasicDetails().getVulnerableClient());
    contactDetails.setClientFlowFormAction(clientFlowFormData.getAction());

    if (clientFlowFormData.getContactDetails() != null) {
      model.addAttribute("contactDetails", clientFlowFormData.getContactDetails());
    }

    return "application/client/contact-client-details";
  }

  /**
   * Handles the client contact details results submission.
   *
   * @param clientFlowFormData The data for create client flow.
   * @param contactDetails The contact details of the client.
   * @param bindingResult Validation result.
   * @param model The model for the view.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/application/client/details/contact")
  public String clientDetailsContact(
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) ClientFlowFormData clientFlowFormData,
      @ModelAttribute("contactDetails") ClientFormDataContactDetails contactDetails,
      BindingResult bindingResult,
      Model model) {

    // Cleanup mobile numbers if not selected in the UI.
    contactDetails.clearUnsetPhoneNumbers();
    clientContactDetailsValidator.validate(contactDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      populateDropdowns(model);
      // Extract global error codes for checkboxes to show error messages
      List<String> globalErrorCodes = bindingResult.getGlobalErrors().stream()
          .map(ObjectError::getCode)
          .toList();
      model.addAttribute("globalErrorCodes", globalErrorCodes);
      return "application/client/contact-client-details";
    }

    clientFlowFormData.setContactDetails(contactDetails);
    model.addAttribute(CLIENT_FLOW_FORM_DATA, clientFlowFormData);

    return "redirect:/application/client/details/address";
  }

  private void populateDropdowns(Model model) {
    new DropdownBuilder(model)
        .addDropdown("correspondenceMethods",
            lookupService.getCommonValues(COMMON_VALUE_CORRESPONDENCE_METHOD))
        .addDropdown("correspondenceLanguages",
            lookupService.getCommonValues(COMMON_VALUE_CORRESPONDENCE_LANGUAGE))
        .build();
  }
}
