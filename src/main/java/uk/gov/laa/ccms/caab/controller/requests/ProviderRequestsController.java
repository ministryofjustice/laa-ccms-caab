package uk.gov.laa.ccms.caab.controller.requests;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.PRIOR_AUTHORITY_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROVIDER_REQUEST_FLOW_FORM_DATA;

import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestFlowFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestTypeFormData;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestTypeDetailsValidator;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.ProviderRequestTypeLookupDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestTypeLookupValueDetail;

/**
 * Controller for handling edits to client basic details during the application summary process.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
@SessionAttributes(value = {
    PROVIDER_REQUEST_FLOW_FORM_DATA
})
public class ProviderRequestsController {

  private final LookupService lookupService;

  private final ProviderRequestTypeDetailsValidator providerRequestTypeValidator;


  /**
   * Handles the GET request for selecting the provider requests type page.
   *
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("/provider-requests/types")
  public String getRequestType(
      final Model model,
      final HttpSession session) {

    final ProviderRequestFlowFormData providerRequestFlow
        = new ProviderRequestFlowFormData();

    model.addAttribute(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow);
    model.addAttribute("providerRequestTypeDetails",
        providerRequestFlow.getRequestTypeFormData());

    populateProviderRequestTypes(model);

    return "requests/provider-request-type";
  }

  /**
   * Handles the POST request for submitting provider request type details.
   *
   * @param providerRequestFlow form data containing the current state of the provider request flow
   * @param providerRequestTypeDetails form data for the provider request type details
   * @param model the model to store attributes for rendering the view
   * @param bindingResult result of binding request type details with potential validation errors
   * @return the view name for the provider request type form if there are errors,
   *         otherwise a redirect to the provider request details page
   */
  @PostMapping("/provider-requests/types")
  public String requestTypePost(
      @SessionAttribute(PROVIDER_REQUEST_FLOW_FORM_DATA)
      final ProviderRequestFlowFormData providerRequestFlow,
      @ModelAttribute("providerRequestTypeDetails")
      final ProviderRequestTypeFormData providerRequestTypeDetails,
      final Model model,
      final BindingResult bindingResult) {

    providerRequestTypeValidator.validate(providerRequestTypeDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      populateProviderRequestTypes(model);
      model.addAttribute(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow);
      model.addAttribute("providerRequestTypeDetails", providerRequestTypeDetails);
      return "requests/provider-request-type";
    }

    providerRequestFlow.setRequestTypeFormData(providerRequestTypeDetails);
    model.addAttribute(PRIOR_AUTHORITY_FLOW_FORM_DATA, providerRequestFlow);

    return "redirect:/provider-requests/details";
  }

  /**
   * Populates dropdown options for provider request types form.
   *
   * @param model The model for the view.
   */
  private void populateProviderRequestTypes(final Model model) {
    final List<ProviderRequestTypeLookupValueDetail> providerRequestTypes = Optional.ofNullable(
            lookupService.getProviderRequestTypes(false, null).block())
        .map(ProviderRequestTypeLookupDetail::getContent)
        .orElse(Collections.emptyList());

    model.addAttribute("providerRequestTypes",
        providerRequestTypes);
  }

  @GetMapping("/provider-requests/details")
  public String getRequestDetail(
      final Model model) {

    return "requests/provider-request-detail";
  }


}
