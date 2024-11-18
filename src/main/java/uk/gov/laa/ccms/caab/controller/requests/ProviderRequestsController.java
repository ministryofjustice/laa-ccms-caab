package uk.gov.laa.ccms.caab.controller.requests;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.REFERENCE_DATA_ITEM_TYPE_LOV;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PRIOR_AUTHORITY_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROVIDER_REQUEST_FLOW_FORM_DATA;

import java.io.IOException;
import java.util.ArrayList;
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
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestDetailsFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestFlowFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestTypeFormData;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestTypesValidator;
import uk.gov.laa.ccms.caab.constants.SendBy;
import uk.gov.laa.ccms.caab.exception.AvScanException;
import uk.gov.laa.ccms.caab.exception.AvVirusFoundException;
import uk.gov.laa.ccms.caab.mapper.ProviderRequestsMapper;
import uk.gov.laa.ccms.caab.service.AvScanService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestDataLookupValueDetail;
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

  private final ProviderRequestTypesValidator providerRequestTypeValidator;

  private final ProviderRequestDetailsValidator providerRequestDetailsValidator;

  private final ProviderRequestsMapper providerRequestsMapper;

  private final AvScanService avScanService;

  /**
   * Creates a new instance of {@link ProviderRequestFlowFormData}.
   *
   * @return A new instance of {@link ProviderRequestFlowFormData}.
   */
  @ModelAttribute(PROVIDER_REQUEST_FLOW_FORM_DATA)
  public ProviderRequestFlowFormData getProviderRequestFlowFormData() {
    return new ProviderRequestFlowFormData();
  }


  /**
   * Handles the GET request for selecting the provider requests type page.
   *
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("/provider-requests/types")
  public String getRequestType(
      @ModelAttribute(PROVIDER_REQUEST_FLOW_FORM_DATA)
      final ProviderRequestFlowFormData providerRequestFlow,
      final Model model) {

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
  protected void populateProviderRequestTypes(final Model model) {
    final List<ProviderRequestTypeLookupValueDetail> providerRequestTypes = Optional.ofNullable(
            lookupService.getProviderRequestTypes(false, null).block())
        .map(ProviderRequestTypeLookupDetail::getContent)
        .orElse(Collections.emptyList());

    model.addAttribute("providerRequestTypes",
        providerRequestTypes);
  }

  /**
   * Handles the GET request for the provider request details page.
   *
   * @param providerRequestFlow form data containing the current state of the provider request flow
   * @param model the model to store attributes for rendering the view
   * @return the view name for the provider request details page
   */
  @GetMapping("/provider-requests/details")
  public String getRequestDetail(
      @SessionAttribute(PROVIDER_REQUEST_FLOW_FORM_DATA)
      final ProviderRequestFlowFormData providerRequestFlow,
      final Model model) {

    final ProviderRequestDetailsFormData providerRequestDetailsForm =
        providerRequestFlow.getRequestDetailsFormData();

    return providerRequestsDetails(providerRequestFlow, providerRequestDetailsForm, model);
  }

  /**
   * Handles the POST request to submit provider request details.
   *
   * @param providerRequestFlow session attribute containing flow form data.
   * @param providerRequestDetailsForm form data for provider request details.
   * @param model Spring MVC model to hold attributes for the view.
   * @param bindingResult holds validation errors, if any.
   * @return todo
   */
  @PostMapping("/provider-requests/details")
  public String postRequestDetail(
      @SessionAttribute(PROVIDER_REQUEST_FLOW_FORM_DATA)
      final ProviderRequestFlowFormData providerRequestFlow,
      @ModelAttribute("providerRequestDetails")
      final ProviderRequestDetailsFormData providerRequestDetailsForm,
      final Model model,
      final BindingResult bindingResult
  ) {

    providerRequestsMapper.toProviderRequestDetailsFormData(
        providerRequestDetailsForm,
        providerRequestFlow);

    providerRequestDetailsValidator.validate(providerRequestDetailsForm, bindingResult);

    if (bindingResult.hasErrors()) {
      return providerRequestsDetails(providerRequestFlow, providerRequestDetailsForm, model);
    }

    if (providerRequestDetailsForm.isFileUploadEnabled()) {
      try {
        avScanService.performAvScan(
            null,
            null,
            null,
            null,
            providerRequestDetailsForm.getFile().getOriginalFilename(),
            providerRequestDetailsForm.getFile().getInputStream());
      } catch (final AvVirusFoundException | AvScanException | IOException e) {
        bindingResult.rejectValue("file", "scan.failure", e.getMessage());
        providerRequestDetailsForm.setFile(null);
        return providerRequestsDetails(providerRequestFlow, providerRequestDetailsForm, model);
      }
    }

    //todo as part of ccmspui-311 - submit general request
    return "redirect:/home";
  }

  /**
   * Retrieves and prepares data needed for the provider request details page.
   *
   * @param providerRequestFlow session attribute containing flow form data.
   * @param providerRequestDetailsForm form data for provider request details.
   * @param model Spring MVC model to hold attributes for the view.
   * @return the view name for provider request details page.
   */
  protected String providerRequestsDetails(
      final ProviderRequestFlowFormData providerRequestFlow,
      final ProviderRequestDetailsFormData providerRequestDetailsForm,
      final Model model) {

    final String type = providerRequestFlow.getRequestTypeFormData().getProviderRequestType();

    final ProviderRequestTypeLookupValueDetail dynamicForm = Optional.ofNullable(
            lookupService.getProviderRequestTypes(null, type).block())
        .map(ProviderRequestTypeLookupDetail::getContent)
        .orElse(Collections.emptyList())
        .stream()
        .findFirst()
        .orElse(null);

    if (dynamicForm == null) {
      return "error";
    }

    populateProviderRequestDetailsLookupDropdowns(model, dynamicForm);

    providerRequestDetailsForm.setFileUploadEnabled(
        dynamicForm.getIsFileUploadEnabled());
    providerRequestDetailsForm.setFileUploadLabel(
        dynamicForm.getFileUploadPrompt());
    providerRequestDetailsForm.setAdditionalInformationLabel(
        dynamicForm.getAdditionalInformationPrompt());

    providerRequestsMapper.populateProviderRequestDetailsForm(
        providerRequestDetailsForm,
        dynamicForm);

    providerRequestFlow.setRequestDetailsFormData(providerRequestDetailsForm);

    model.addAttribute("providerRequestDynamicForm", dynamicForm);
    model.addAttribute("providerRequestDetails", providerRequestDetailsForm);

    model.addAttribute(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow);

    return "requests/provider-request-detail";
  }

  /**
   * Populates lookup dropdowns for provider request details with dynamic data.
   *
   * @param model Spring MVC model to hold lookup attributes.
   * @param priorAuthorityType the provider request type details for data lookup.
   */
  protected void populateProviderRequestDetailsLookupDropdowns(
      final Model model,
      final ProviderRequestTypeLookupValueDetail priorAuthorityType) {

    final List<ProviderRequestDataLookupValueDetail> lookups =
        priorAuthorityType.getDataItems().stream()
            .filter(dataItem ->
                REFERENCE_DATA_ITEM_TYPE_LOV.equals(dataItem.getType()))
            .toList();

    final List<Mono<Void>> listOfMonos = new ArrayList<>();

    for (final ProviderRequestDataLookupValueDetail lookup : lookups) {
      final Mono<List<CommonLookupValueDetail>> commonValuesMono =
          lookupService.getCommonValues(lookup.getLovLookupType())
              .mapNotNull(CommonLookupDetail::getContent);

      // Subscribe to the Mono and add the attribute in the subscription
      final Mono<Void> mono = commonValuesMono.doOnNext(commonValues ->
          model.addAttribute(lookup.getCode(), commonValues)).then();
      listOfMonos.add(mono);

      Mono.when(listOfMonos).block();
    }
  }

}
