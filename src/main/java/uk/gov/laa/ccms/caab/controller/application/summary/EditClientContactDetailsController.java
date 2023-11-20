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
import uk.gov.laa.ccms.caab.bean.ClientFormDataContactDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientContactDetailsValidator;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.service.CommonLookupService;

/**
 * Controller for handling edits to client basic details during the application summary process.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
@SessionAttributes({CLIENT_FLOW_FORM_DATA})
public class EditClientContactDetailsController {

  private final CommonLookupService commonLookupService;

  private final ClientContactDetailsValidator clientContactDetailsValidator;

  @ModelAttribute("contactDetails")
  public ClientFormDataContactDetails getContactDetails() {
    return new ClientFormDataContactDetails();
  }

  /**
   * Handles the GET request for edit client contact details page.
   *
   * @param clientFlowFormData The data for client flow.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("/application/summary/client/details/contact")
  public String getClientDetailsBasic(
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) ClientFlowFormData clientFlowFormData,
      Model model) {

    populateDropdowns(model);
    ClientFormDataContactDetails contactDetails = clientFlowFormData.getContactDetails();
    contactDetails.setClientFlowFormAction(clientFlowFormData.getAction());

    model.addAttribute("contactDetails", contactDetails);

    return "application/summary/client-contact-details";
  }

  /**
   * Handles the edit contact client details submission.
   *
   * @param clientFlowFormData The data for create client flow.
   * @param contactDetails The contact details of the client.
   * @param bindingResult Validation result.
   * @param model The model for the view.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/application/summary/client/details/contact")
  public String postClientDetailsBasic(
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) ClientFlowFormData clientFlowFormData,
      @ModelAttribute("contactDetails") ClientFormDataContactDetails contactDetails,
      BindingResult bindingResult,
      Model model) {

    contactDetails.setPassword(clientFlowFormData.getContactDetails().getPassword());
    clientContactDetailsValidator.validate(contactDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      populateDropdowns(model);
      return "application/summary/client-contact-details";
    }

    clientFlowFormData.setContactDetails(contactDetails);
    model.addAttribute(CLIENT_FLOW_FORM_DATA, clientFlowFormData);

    return "redirect:/application/summary/client/details/summary";
  }

  /**
   * Populates dropdown options for the client contact details form.
   *
   * @param model The model for the view.
   */
  private void populateDropdowns(Model model) {
    DropdownBuilder builder = new DropdownBuilder(model);

    builder
        .addDropdown("correspondenceMethods",
            commonLookupService.getCorrespondenceMethods())
        .addDropdown("correspondenceLanguages",
            commonLookupService.getCorrespondenceLanguages())
        .build();
  }

}
