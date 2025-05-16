package uk.gov.laa.ccms.caab.controller.application.section;

import static uk.gov.laa.ccms.caab.constants.ContextConstants.CONTEXT_NAME;
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
import uk.gov.laa.ccms.caab.bean.ClientFormDataDeceasedDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientDeceasedDetailsValidator;

/**
 * Controller for handling edits to client deceased details during the application summary process.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
@SessionAttributes({CLIENT_FLOW_FORM_DATA})
public class EditClientDeceasedDetailsController {

  private final ClientDeceasedDetailsValidator clientDeceasedDetailsValidator;

  @ModelAttribute("deceasedDetails")
  public ClientFormDataDeceasedDetails getDeceasedDetails() {
    return new ClientFormDataDeceasedDetails();
  }

  /**
   * Handles the GET request for edit client deceased details page.
   *
   * @param clientFlowFormData The data for client flow.
   * @param deceasedDetails The data for the deceased client details.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("/{" + CONTEXT_NAME + "}/sections/client/details/deceased")
  public String clientDetailsDeceased(
      @PathVariable(CONTEXT_NAME) final String context,
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) ClientFlowFormData clientFlowFormData,
      @ModelAttribute("deceasedDetails") ClientFormDataDeceasedDetails deceasedDetails,
      Model model) {

    if (clientFlowFormData.getDeceasedDetails() != null) {
      model.addAttribute("deceasedDetails", clientFlowFormData.getDeceasedDetails());
    }

    return "application/sections/client-deceased-details";
  }

  /**
   * Handles the edit deceased client details submission.
   *
   * @param clientFlowFormData The data for create client flow.
   * @param deceasedDetails The data for the deceased client details.
   * @param bindingResult Validation result.
   * @param model The model for the view.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/{" + CONTEXT_NAME + "}/sections/client/details/deceased")
  public String postClientDetailsDeceased(
      @PathVariable(CONTEXT_NAME) final String context,
      @SessionAttribute(CLIENT_FLOW_FORM_DATA) ClientFlowFormData clientFlowFormData,
      @ModelAttribute("deceasedDetails") ClientFormDataDeceasedDetails deceasedDetails,
      BindingResult bindingResult,
      Model model) {

    clientDeceasedDetailsValidator.validate(deceasedDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      return "application/sections/client-deceased-details";
    }

    clientFlowFormData.setDeceasedDetails(deceasedDetails);
    model.addAttribute(CLIENT_FLOW_FORM_DATA, clientFlowFormData);

    return "redirect:/%s/sections/client/details/summary".formatted(context);
  }

}
