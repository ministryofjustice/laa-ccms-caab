package uk.gov.laa.ccms.caab.controller.requests;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.REFERENCE_DATA_ITEM_TYPE_LOV;
import static uk.gov.laa.ccms.caab.constants.CcmsModule.REQUEST;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DOCUMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.SendBy.ELECTRONIC;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.EVIDENCE_UPLOAD_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PRIOR_AUTHORITY_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROVIDER_REQUEST_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.DisplayUtil.getCommaDelimitedString;
import static uk.gov.laa.ccms.caab.util.FileUtil.getFileExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestDetailsFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestFlowFormData;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestTypeFormData;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestDocumentUploadValidator;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestTypesValidator;
import uk.gov.laa.ccms.caab.builders.DropdownBuilder;
import uk.gov.laa.ccms.caab.constants.CcmsModule;
import uk.gov.laa.ccms.caab.exception.AvScanException;
import uk.gov.laa.ccms.caab.exception.AvVirusFoundException;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.ProviderRequestsMapper;
import uk.gov.laa.ccms.caab.model.BaseEvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetails;
import uk.gov.laa.ccms.caab.service.AvScanService;
import uk.gov.laa.ccms.caab.service.EvidenceService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestDataLookupValueDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestTypeLookupDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

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
  private final EvidenceService evidenceService;

  private final ProviderRequestTypesValidator providerRequestTypeValidator;
  private final ProviderRequestDetailsValidator providerRequestDetailsValidator;

  private final ProviderRequestDocumentUploadValidator providerRequestDocumentUploadValidator;

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

    //reset the details data, so new document id and form details are created
    providerRequestFlow.resetRequestDetailsFormData();

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
    
    //todo pass in user and filter on user function codes

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
      @SessionAttribute(USER_DETAILS) final UserDetail userDetail,
      @SessionAttribute(PROVIDER_REQUEST_FLOW_FORM_DATA)
      final ProviderRequestFlowFormData providerRequestFlow,
      @RequestParam final String action,
      @ModelAttribute("providerRequestDetails")
      final ProviderRequestDetailsFormData providerRequestDetailsForm,
      final Model model,
      final BindingResult bindingResult
  ) {

    providerRequestsMapper.toProviderRequestDetailsFormData(
        providerRequestDetailsForm,
        providerRequestFlow);

    if ("document_upload".equals(action)) {
      providerRequestsDetails(providerRequestFlow, providerRequestDetailsForm, model);
      return "redirect:/provider-requests/documents";
    } else if ("document_delete".equals(action)) {

      evidenceService.removeDocument(
          String.valueOf(providerRequestFlow.getRequestDetailsFormData().getDocumentSessionId()),
          providerRequestDetailsForm.getDocumentIdToDelete(),
          REQUEST,
          userDetail.getLoginId());

      return providerRequestsDetails(providerRequestFlow, providerRequestDetailsForm, model);
    } else {
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
  }

  /**
   * Handles GET requests to add documents to a provider request.
   *
   * @param model the model to populate with attributes for the view
   * @return the name of the view for uploading provider request documents
   */
  @GetMapping("/provider-requests/documents")
  public String addDocumentsToRequest(
      final Model model) {

    model.addAttribute(EVIDENCE_UPLOAD_FORM_DATA, new EvidenceUploadFormData());

    populateAddEvidenceModel(model);

    return "requests/provider-request-doc-upload";
  }

  /**
   * Handles the POST request to upload a new evidence document.
   *
   * @param evidenceUploadFormData The upload form data.
   * @param userDetail The user details.
   * @param bindingResult The binding result for validation.
   * @param model         The model for the view.
   * @return The view name for the evidence upload view.
   */
  @PostMapping("/provider-requests/documents")
  public String addDocumentsToRequest(
      @SessionAttribute(USER_DETAILS)
      final UserDetail userDetail,
      @SessionAttribute(PROVIDER_REQUEST_FLOW_FORM_DATA)
      final ProviderRequestFlowFormData providerRequestFlow,
      @ModelAttribute(EVIDENCE_UPLOAD_FORM_DATA)
      final EvidenceUploadFormData evidenceUploadFormData,
      final BindingResult bindingResult,
      final Model model) {

    final String documentSessionId = providerRequestFlow.getRequestDetailsFormData()
        .getDocumentSessionId().toString();

    //set the additional details for the evidence upload
    evidenceUploadFormData.setApplicationOrOutcomeId(documentSessionId);
    evidenceUploadFormData.setCaseReferenceNumber("-1");
    evidenceUploadFormData.setProviderId(userDetail.getProvider().getId());
    evidenceUploadFormData.setDocumentSender(userDetail.getLoginId());
    evidenceUploadFormData.setCcmsModule(REQUEST);

    // Validate the evidence form data
    providerRequestDocumentUploadValidator.validate(evidenceUploadFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      populateAddEvidenceModel(model);
      return "requests/provider-request-doc-upload";
    }

    try {
      // Scan the document for viruses
      avScanService.performAvScan(
          evidenceUploadFormData.getCaseReferenceNumber(),
          evidenceUploadFormData.getProviderId(),
          evidenceUploadFormData.getDocumentSender(),
          evidenceUploadFormData.getCcmsModule(),
          evidenceUploadFormData.getFile().getOriginalFilename(),
          evidenceUploadFormData.getFile().getInputStream());
    } catch (AvVirusFoundException | AvScanException | IOException e) {
      bindingResult.rejectValue("file", "scan.failure", e.getMessage());

      populateAddEvidenceModel(model);
      return "requests/provider-request-doc-upload";
    }

    final String fileExtension = getFileExtension(evidenceUploadFormData.getFile());

    //todo if its a case related request we need to register the doc else we dont
    // will be done as part of amendments in a later story
    if (false) {
      // All clear, so register the document in EBS before saving to the TDS.
      final String registeredDocumentId = evidenceService.registerDocument(
              evidenceUploadFormData.getDocumentType(),
              fileExtension,
              evidenceUploadFormData.getDocumentDescription(),
              ELECTRONIC.getCode(),
              userDetail.getLoginId(),
              userDetail.getUserType())
          .blockOptional()
          .orElseThrow(() -> new CaabApplicationException("Failed to register document"));

      evidenceUploadFormData.setRegisteredDocumentId(registeredDocumentId);
    }

    evidenceService.addDocument(
            providerRequestsMapper.toProviderRequestDocumentDetail(evidenceUploadFormData),
            userDetail.getLoginId())
        .blockOptional()
        .orElseThrow(() -> new CaabApplicationException("Failed to save document"));

    return "redirect:/provider-requests/details";
  }

  /**
   * Exception handler to catch when the uploaded file is too large.
   *
   * @param evidenceUploadFormData - the form data for the page.
   * @param model - the model
   * @return the view name for the evidence-add screen.
   */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public String handleUploadFileTooLarge(
      @SessionAttribute(EVIDENCE_UPLOAD_FORM_DATA)
      final EvidenceUploadFormData evidenceUploadFormData,
      final Model model) {

    // Manually construct a BindingResult to hold the file size error.
    final BindingResult bindingResult = new BeanPropertyBindingResult(
        evidenceUploadFormData, EVIDENCE_UPLOAD_FORM_DATA);
    providerRequestDocumentUploadValidator.rejectFileSize(bindingResult);

    model.addAttribute(EVIDENCE_UPLOAD_FORM_DATA, evidenceUploadFormData);
    model.addAttribute(BindingResult.MODEL_KEY_PREFIX + EVIDENCE_UPLOAD_FORM_DATA,
        bindingResult);

    populateAddEvidenceModel(model);
    return "requests/provider-request-doc-upload";
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

    if (providerRequestDetailsForm.getDynamicOptions().isEmpty()) {
      providerRequestsMapper.populateProviderRequestDetailsForm(
          providerRequestDetailsForm,
          dynamicForm);
    }

    //if file upload is not enabled, then evidence upload is available
    if (!providerRequestDetailsForm.isFileUploadEnabled()) {
      final String documentSessionId = providerRequestFlow.getRequestDetailsFormData()
          .getDocumentSessionId().toString();

      final List<BaseEvidenceDocumentDetail> documents =
          evidenceService.getEvidenceDocumentsForApplicationOrOutcome(
                  documentSessionId,
                  CcmsModule.REQUEST)
              .map(EvidenceDocumentDetails::getContent)
              .blockOptional()
              .orElseThrow(() -> new CaabApplicationException(
                  String.format("Invalid document session id: %s", documentSessionId)));

      model.addAttribute("documentsUploaded", documents);
    } else {
      model.addAttribute("documentsUploaded", new ArrayList<>());
    }

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

  /**
   * Populates the model with attributes needed for the add evidence view.
   *
   * @param model the model to populate with dropdown options and file upload constraints
   */
  protected void populateAddEvidenceModel(final Model model) {
    final DropdownBuilder builder = new DropdownBuilder(model);
    builder
        .addDropdown("documentTypes",
            lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
        .build();

    model.addAttribute("validExtensions",
        getCommaDelimitedString(providerRequestDocumentUploadValidator.getValidExtensions()));
    model.addAttribute("maxFileSize",
        providerRequestDocumentUploadValidator.getMaxFileSize());
  }

}
