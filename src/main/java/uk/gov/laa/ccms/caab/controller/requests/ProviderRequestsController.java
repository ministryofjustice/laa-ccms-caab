package uk.gov.laa.ccms.caab.controller.requests;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.REFERENCE_DATA_ITEM_TYPE_LOV;
import static uk.gov.laa.ccms.caab.constants.CcmsModule.REQUEST;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DOCUMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.SendBy.ELECTRONIC;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.EVIDENCE_UPLOAD_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROVIDER_REQUEST_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.DisplayUtil.getCommaDelimitedString;
import static uk.gov.laa.ccms.caab.util.FileUtil.getFileExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
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
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.BaseEvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetails;
import uk.gov.laa.ccms.caab.service.AvScanService;
import uk.gov.laa.ccms.caab.service.EvidenceService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.ProviderRequestService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestDataLookupValueDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestTypeLookupDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Controller for handling edits to client basic details during the application summary process. */
@Controller
@Slf4j
@RequiredArgsConstructor
@SessionAttributes(value = {PROVIDER_REQUEST_FLOW_FORM_DATA})
public class ProviderRequestsController {

  private final LookupService lookupService;
  private final EvidenceService evidenceService;
  private final ProviderRequestService providerRequestService;
  private final AvScanService avScanService;

  private final ProviderRequestTypesValidator providerRequestTypeValidator;
  private final ProviderRequestDetailsValidator providerRequestDetailsValidator;

  private final ProviderRequestDocumentUploadValidator providerRequestDocumentUploadValidator;

  private final ProviderRequestsMapper providerRequestsMapper;

  private static final String UNRELATED_CASE_REFERENCE = "-1";

  // Where to send the user if the provider request flow is no longer in the session (e.g. it was
  // completed and cleared, timed out, or a devtools restart wiped it) and they navigate back into a
  // mid-flow page. Restarting the wizard is safer than rendering a half-empty flow.
  private static final String RESTART_FLOW_REDIRECT = "redirect:/provider-requests/types";

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
      @RequestParam(required = false) final String caseReferenceNumber,
      @SessionAttribute(USER_DETAILS) final UserDetail userDetail,
      @SessionAttribute(name = CASE, required = false) ApplicationDetail ebsCase,
      final Model model) {

    // Entering the wizard fresh: clear any stale type, details and case context left over from a
    // previous request, so a general enquiry can't inherit a prior case linkage (and vice versa).
    providerRequestFlow.reset();

    String effectiveCaseRef = getEffectiveCaseReference(caseReferenceNumber);
    providerRequestFlow.setCaseReferenceNumber(effectiveCaseRef);

    applyCaseContext(model, effectiveCaseRef);

    model.addAttribute(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow);
    model.addAttribute("providerRequestTypeDetails", providerRequestFlow.getRequestTypeFormData());

    populateProviderRequestTypes(model, userDetail, caseReferenceNumber);

    return "requests/provider-request-type";
  }

  /**
   * Handles the POST request for submitting provider request type details.
   *
   * @param providerRequestFlow form data containing the current state of the provider request flow
   * @param providerRequestTypeDetails form data for the provider request type details
   * @param model the model to store attributes for rendering the view
   * @param bindingResult result of binding request type details with potential validation errors
   * @return the view name for the provider request type form if there are errors, otherwise a
   *     redirect to the provider request details page
   */
  @PostMapping("/provider-requests/types")
  public String requestTypePost(
      @SessionAttribute(name = PROVIDER_REQUEST_FLOW_FORM_DATA, required = false)
          final ProviderRequestFlowFormData providerRequestFlow,
      @ModelAttribute("providerRequestTypeDetails")
          final ProviderRequestTypeFormData providerRequestTypeDetails,
      @RequestParam(required = false) final String caseReferenceNumber,
      @SessionAttribute(USER_DETAILS) final UserDetail userDetail,
      final Model model,
      final BindingResult bindingResult) {

    if (providerRequestFlow == null) {
      return RESTART_FLOW_REDIRECT;
    }

    // The submitted case reference (hidden field) is authoritative, so the request can't be
    // re-linked to a different case by another enquiry wizard sharing the session flow.
    final String caseRef = getEffectiveCaseReference(caseReferenceNumber);
    providerRequestFlow.setCaseReferenceNumber(caseRef);

    providerRequestTypeValidator.validate(providerRequestTypeDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      applyCaseContext(model, caseRef);
      populateProviderRequestTypes(model, userDetail, caseRef);
      model.addAttribute(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow);
      model.addAttribute("providerRequestTypeDetails", providerRequestTypeDetails);
      return "requests/provider-request-type";
    }

    providerRequestFlow.setRequestTypeFormData(providerRequestTypeDetails);

    return detailsRedirect(caseRef, providerRequestTypeDetails.getProviderRequestType());
  }

  /**
   * Populates dropdown options for provider request types form. also filters based on user function
   * codes
   *
   * @param model The model for the view.
   * @param userDetail Logged-in user details.
   */
  protected void populateProviderRequestTypes(
      final Model model, UserDetail userDetail, String caseReferenceNumber) {

    List<String> functions =
        Optional.ofNullable(userDetail.getFunctions()).orElse(Collections.emptyList());

    boolean isCaseRelated = isValidCaseReference(caseReferenceNumber);

    final List<ProviderRequestTypeLookupValueDetail> providerRequestTypes =
        Optional.ofNullable(
                lookupService
                    .getProviderRequestTypes(isCaseRelated, null)
                    .map(ProviderRequestTypeLookupDetail::getContent)
                    .flatMapMany(Flux::fromIterable)
                    .filter(
                        it ->
                            it.getAccessFunctionCode() == null
                                || functions.contains(it.getAccessFunctionCode()))
                    .collectList()
                    .block())
            .orElse(Collections.emptyList());

    model.addAttribute("providerRequestTypes", providerRequestTypes);
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
      @RequestParam(required = false) final String caseReferenceNumber,
      @RequestParam(required = false) final String providerRequestType,
      @SessionAttribute(name = PROVIDER_REQUEST_FLOW_FORM_DATA, required = false)
          final ProviderRequestFlowFormData providerRequestFlow,
      @SessionAttribute(name = CASE, required = false) ApplicationDetail ebsCase,
      final Model model) {

    if (providerRequestFlow == null) {
      return RESTART_FLOW_REDIRECT;
    }

    // The page is self-describing: the case reference and request type come from the request so a
    // back-button GET rebuilds the correct form even if another wizard reset the shared session
    // flow. A missing/blank case reference resets to the unrelated marker.
    final String caseRef =
        applyRequestContext(providerRequestFlow, caseReferenceNumber, providerRequestType);
    if (hasNoRequestType(providerRequestFlow)) {
      return RESTART_FLOW_REDIRECT;
    }
    applyCaseContext(model, caseRef);

    populateAddEvidenceModel(model);

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
   * @return the view name or redirection for the appropriate linked page.
   */
  @PostMapping("/provider-requests/details")
  public String postRequestDetail(
      @SessionAttribute(USER_DETAILS) final UserDetail userDetail,
      @SessionAttribute(name = PROVIDER_REQUEST_FLOW_FORM_DATA, required = false)
          final ProviderRequestFlowFormData providerRequestFlow,
      @RequestParam final String action,
      @RequestParam(required = false) final String caseReferenceNumber,
      @RequestParam(required = false) final String providerRequestType,
      @ModelAttribute("providerRequestDetails")
          final ProviderRequestDetailsFormData providerRequestDetailsForm,
      final Model model,
      final BindingResult bindingResult) {

    if (providerRequestFlow == null) {
      return RESTART_FLOW_REDIRECT;
    }

    // The case reference and request type from the submitted page are authoritative, so the
    // submission can't be mis-linked or mis-typed by another wizard sharing the session flow.
    final String caseRef =
        applyRequestContext(providerRequestFlow, caseReferenceNumber, providerRequestType);
    if (hasNoRequestType(providerRequestFlow)) {
      return RESTART_FLOW_REDIRECT;
    }

    providerRequestsMapper.toProviderRequestDetailsFormData(
        providerRequestDetailsForm, providerRequestFlow);

    applyCaseContext(model, caseRef);

    final String requestType =
        providerRequestFlow.getRequestTypeFormData().getProviderRequestType();

    if ("document_upload".equals(action)) {
      providerRequestFlow.setRequestDetailsFormData(providerRequestDetailsForm);

      return documentsRedirect(caseRef, requestType);
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
        populateAddEvidenceModel(model);
        return providerRequestsDetails(providerRequestFlow, providerRequestDetailsForm, model);
      }

      if (providerRequestDetailsForm.isClaimUploadEnabled()) {
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

      // call soa-api a register the notification request
      // we need to pass the providerRequestFlow and user details into the service.
      final String notificationId =
          providerRequestService.submitProviderRequest(
              providerRequestFlow.getRequestTypeFormData(),
              providerRequestDetailsForm,
              caseRef,
              userDetail);

      // check if we are not a claim upload enabled request,
      // if not then we might have documents
      if (!providerRequestDetailsForm.isClaimUploadEnabled()) {
        // we need the session id, as this is key used to associate the documents with the request
        final String documentSessionId =
            providerRequestFlow.getRequestDetailsFormData().getDocumentSessionId().toString();

        // return all the documents for the request
        final EvidenceDocumentDetails documents =
            evidenceService
                .getEvidenceDocumentsForApplicationOrOutcome(documentSessionId, CcmsModule.REQUEST)
                .blockOptional()
                .orElseThrow(
                    () ->
                        new CaabApplicationException(
                            "Invalid document session id: %s".formatted(documentSessionId)));

        // using the list of documents, we need to upload them to ebs through soa.
        // we also update the status of the documents in the tds to say they have been uploaded
        evidenceService
            .uploadAndUpdateDocuments(
                documents, UNRELATED_CASE_REFERENCE, notificationId, userDetail)
            .block();
      }
      String redirectUrl = "/application/provider-request/confirmed";
      if (isValidCaseReference(caseRef)) {
        redirectUrl += "?caseReferenceNumber=" + caseRef;
      }
      return "redirect:" + redirectUrl;
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
      @RequestParam(required = false) final String caseReferenceNumber,
      @RequestParam(required = false) final String providerRequestType,
      @SessionAttribute(name = PROVIDER_REQUEST_FLOW_FORM_DATA, required = false)
          final ProviderRequestFlowFormData providerRequestFlow,
      final Model model) {

    if (providerRequestFlow == null) {
      return RESTART_FLOW_REDIRECT;
    }

    final String caseRef =
        applyRequestContext(providerRequestFlow, caseReferenceNumber, providerRequestType);
    if (hasNoRequestType(providerRequestFlow)) {
      return RESTART_FLOW_REDIRECT;
    }
    populateDocumentUploadModel(
        model, caseRef, providerRequestFlow.getRequestTypeFormData().getProviderRequestType());

    model.addAttribute(EVIDENCE_UPLOAD_FORM_DATA, new EvidenceUploadFormData());
    return "requests/provider-request-doc-upload";
  }

  /**
   * Handles the POST request to upload a new evidence document.
   *
   * @param evidenceUploadFormData The upload form data.
   * @param userDetail The user details.
   * @param bindingResult The binding result for validation.
   * @param model The model for the view.
   * @return The view name for the evidence upload view.
   */
  @PostMapping("/provider-requests/documents")
  public String addDocumentsToRequest(
      @SessionAttribute(USER_DETAILS) final UserDetail userDetail,
      @SessionAttribute(name = PROVIDER_REQUEST_FLOW_FORM_DATA, required = false)
          final ProviderRequestFlowFormData providerRequestFlow,
      @ModelAttribute(EVIDENCE_UPLOAD_FORM_DATA)
          final EvidenceUploadFormData evidenceUploadFormData,
      @RequestParam(required = false) String caseReferenceNumber,
      @RequestParam(required = false) String providerRequestType,
      final BindingResult bindingResult,
      final Model model) {

    if (providerRequestFlow == null) {
      return RESTART_FLOW_REDIRECT;
    }

    // The case reference and request type from the submitted page are authoritative, so the upload
    // stays tied to the correct enquiry even if another wizard shares the session flow. This must
    // happen before the document session id is read, so the upload never lands in another
    // enquiry's document session.
    final String caseRef =
        applyRequestContext(providerRequestFlow, caseReferenceNumber, providerRequestType);
    if (hasNoRequestType(providerRequestFlow)) {
      return RESTART_FLOW_REDIRECT;
    }

    final String documentSessionId =
        providerRequestFlow.getRequestDetailsFormData().getDocumentSessionId().toString();

    final String requestType =
        providerRequestFlow.getRequestTypeFormData().getProviderRequestType();

    // set the additional details for the evidence upload
    evidenceUploadFormData.setApplicationOrOutcomeId(documentSessionId);
    evidenceUploadFormData.setCaseReferenceNumber(caseRef);
    evidenceUploadFormData.setProviderId(userDetail.getProvider().getId());
    evidenceUploadFormData.setDocumentSender(userDetail.getLoginId());
    evidenceUploadFormData.setCcmsModule(REQUEST);

    // Validate the evidence form data
    providerRequestDocumentUploadValidator.validate(evidenceUploadFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      populateDocumentUploadModel(model, caseRef, requestType);
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
      populateDocumentUploadModel(model, caseRef, requestType);
      return "requests/provider-request-doc-upload";
    }

    final String fileExtension = getFileExtension(evidenceUploadFormData.getFile());

    // todo if its a case related request we need to register the doc else we dont
    // will be done as part of amendments in a later story
    if (false) {
      final String registeredDocumentId =
          evidenceService
              .registerDocument(
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

    evidenceService
        .addDocument(
            providerRequestsMapper.toProviderRequestDocumentDetail(evidenceUploadFormData),
            userDetail.getLoginId())
        .blockOptional()
        .orElseThrow(() -> new CaabApplicationException("Failed to save document"));

    return detailsRedirect(caseRef, requestType);
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
      @SessionAttribute(name = PROVIDER_REQUEST_FLOW_FORM_DATA, required = false)
          final ProviderRequestFlowFormData providerRequestFlow,
      final Model model) {

    if (providerRequestFlow == null) {
      return RESTART_FLOW_REDIRECT;
    }

    // Manually construct a BindingResult to hold the file size error.
    final BindingResult bindingResult =
        new BeanPropertyBindingResult(evidenceUploadFormData, EVIDENCE_UPLOAD_FORM_DATA);
    providerRequestDocumentUploadValidator.rejectFileSize(bindingResult);

    model.addAttribute(EVIDENCE_UPLOAD_FORM_DATA, evidenceUploadFormData);
    model.addAttribute(BindingResult.MODEL_KEY_PREFIX + EVIDENCE_UPLOAD_FORM_DATA, bindingResult);

    // Keep the hidden field and back link values so the page stays self-describing after the error.
    populateDocumentUploadModel(
        model,
        providerRequestFlow.getCaseReferenceNumber(),
        providerRequestFlow.getRequestTypeFormData().getProviderRequestType());

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

    final ProviderRequestTypeLookupValueDetail dynamicForm =
        Optional.ofNullable(lookupService.getProviderRequestTypes(null, type).block())
            .map(ProviderRequestTypeLookupDetail::getContent)
            .orElse(Collections.emptyList())
            .stream()
            .findFirst()
            .orElse(null);

    if (dynamicForm == null) {
      return "error";
    }

    if (providerRequestDetailsForm.getDynamicOptions() == null) {
      providerRequestDetailsForm.setDynamicOptions(new HashMap<>());
    } else {
      final Set<String> currentCodes =
          dynamicForm.getDataItems().stream()
              .map(ProviderRequestDataLookupValueDetail::getCode)
              .collect(Collectors.toSet());

      providerRequestDetailsForm
          .getDynamicOptions()
          .keySet()
          .removeIf(code -> !currentCodes.contains(code));
    }

    populateProviderRequestDetailsLookupDropdowns(model, dynamicForm);

    providerRequestDetailsForm.setClaimUploadEnabled(
        Boolean.TRUE.equals(dynamicForm.getIsClaimUploadEnabled()));
    providerRequestDetailsForm.setClaimUploadLabel(dynamicForm.getClaimUploadPrompt());
    providerRequestDetailsForm.setAdditionalInformationLabel(
        dynamicForm.getAdditionalInformationPrompt());
    // This field is to make the additional information prompt mandatory
    providerRequestDetailsForm.setIsAdditionalInformationPromptRequired(
        !dynamicForm.getIsClaimUploadEnabled() && dynamicForm.getDataItems().isEmpty());

    if (providerRequestDetailsForm.getDynamicOptions().isEmpty()) {
      providerRequestsMapper.populateProviderRequestDetailsForm(
          providerRequestDetailsForm, dynamicForm);
    }

    // if claim upload is not enabled, then evidence upload is available
    if (!providerRequestDetailsForm.isClaimUploadEnabled()) {
      final String documentSessionId =
          providerRequestFlow.getRequestDetailsFormData().getDocumentSessionId().toString();

      final List<BaseEvidenceDocumentDetail> documents =
          evidenceService
              .getEvidenceDocumentsForApplicationOrOutcome(documentSessionId, CcmsModule.REQUEST)
              .map(EvidenceDocumentDetails::getContent)
              .blockOptional()
              .orElseThrow(
                  () ->
                      new CaabApplicationException(
                          "Invalid document session id: %s".formatted(documentSessionId)));

      model.addAttribute("documentsUploaded", documents);
    } else {
      model.addAttribute("documentsUploaded", new ArrayList<>());
    }

    providerRequestFlow.setRequestDetailsFormData(providerRequestDetailsForm);

    model.addAttribute("providerRequestDynamicForm", dynamicForm);
    model.addAttribute("providerRequestDetails", providerRequestDetailsForm);
    // Carried as a hidden field so the submission's request type is bound to the page the user
    // actually saw, not to the shared session flow that another wizard may have changed.
    model.addAttribute("providerRequestType", type);

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
      final Model model, final ProviderRequestTypeLookupValueDetail priorAuthorityType) {

    final List<ProviderRequestDataLookupValueDetail> lookups =
        priorAuthorityType.getDataItems().stream()
            .filter(dataItem -> REFERENCE_DATA_ITEM_TYPE_LOV.equals(dataItem.getType()))
            .toList();

    final List<Mono<Void>> listOfMonos = new ArrayList<>();

    for (final ProviderRequestDataLookupValueDetail lookup : lookups) {
      final Mono<List<CommonLookupValueDetail>> commonValuesMono =
          lookupService
              .getCommonValues(lookup.getLovLookupType())
              .mapNotNull(CommonLookupDetail::getContent);

      // Subscribe to the Mono and add the attribute in the subscription
      final Mono<Void> mono =
          commonValuesMono
              .doOnNext(commonValues -> model.addAttribute(lookup.getCode(), commonValues))
              .then();
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
        .addDropdown("documentTypes", lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
        .build();

    model.addAttribute(
        "validExtensions",
        getCommaDelimitedString(providerRequestDocumentUploadValidator.getValidExtensions()));
    model.addAttribute("maxFileSize", providerRequestDocumentUploadValidator.getMaxFileSize());
    model.addAttribute(
        "claimValidExtensions",
        getCommaDelimitedString(providerRequestDetailsValidator.getValidExtensions()));
    model.addAttribute("claimMaxFileSize", providerRequestDetailsValidator.getMaxFileSize());
  }

  /**
   * Handles the POST request for the submission page.
   *
   * @param providerRequestFlow session attribute containing flow form data.
   * @param sessionStatus status of current session
   * @return the view for either the home page or case overview page
   */
  @PostMapping("/application/provider-request/confirmed")
  public String clientUpdateSubmitted(
      @SessionAttribute(name = PROVIDER_REQUEST_FLOW_FORM_DATA, required = false)
          final ProviderRequestFlowFormData providerRequestFlow,
      final SessionStatus sessionStatus) {

    // The flow is already gone (e.g. a double submit after completion); nothing to complete, just
    // send the user home.
    if (providerRequestFlow == null) {
      return "redirect:/home";
    }

    sessionStatus.setComplete();

    String caseRef = providerRequestFlow.getCaseReferenceNumber();
    if (isValidCaseReference(caseRef)) {
      return "redirect:/case/overview";
    }
    return "redirect:/home";
  }

  private String getEffectiveCaseReference(String caseReferenceNumber) {
    return isValidCaseReference(caseReferenceNumber)
        ? caseReferenceNumber
        : UNRELATED_CASE_REFERENCE;
  }

  private boolean isValidCaseReference(String caseReferenceNumber) {
    return caseReferenceNumber != null
        && caseReferenceNumber.matches("^\\d{12}$")
        && !UNRELATED_CASE_REFERENCE.equals(caseReferenceNumber);
  }

  /**
   * Adds the page's case reference to the model, and hides the case context banner when it does not
   * belong to this page. The banner is driven by the shared {@code ACTIVE_CASE} session attribute,
   * which outlives any one enquiry: navigating straight back into a general enquiry after visiting
   * a case would otherwise show that case's banner. Only the model is touched — {@code ACTIVE_CASE}
   * is shared with the amendment journey and must not be removed from the session.
   */
  private void applyCaseContext(final Model model, final String caseRef) {
    final boolean caseLinked = isValidCaseReference(caseRef);
    if (caseLinked) {
      model.addAttribute("caseReference", caseRef);
    }

    if (model.asMap().get(ACTIVE_CASE) instanceof ActiveCase activeCase) {
      final boolean bannerBelongsToPage =
          caseLinked && caseRef.equals(activeCase.getCaseReferenceNumber());
      if (!bannerBelongsToPage) {
        model.asMap().remove(ACTIVE_CASE);
      }
    }
  }

  /**
   * Applies the case reference and request type carried by a wizard page onto the shared session
   * flow, and discards any in-progress details that belong to a different enquiry. The pages are
   * self-describing so a back-button GET (or a submit from a page left open) rebuilds the correct,
   * uncontaminated form even if another enquiry wizard has since reset the shared session flow.
   *
   * @return the effective case reference for the page
   */
  private String applyRequestContext(
      final ProviderRequestFlowFormData providerRequestFlow,
      final String caseReferenceNumber,
      final String providerRequestType) {

    final String caseRef = getEffectiveCaseReference(caseReferenceNumber);
    providerRequestFlow.setCaseReferenceNumber(caseRef);

    // A blank/absent request type leaves the existing selection in place.
    if (providerRequestType != null && !providerRequestType.isBlank()) {
      providerRequestFlow.getRequestTypeFormData().setProviderRequestType(providerRequestType);
    }

    providerRequestFlow.alignRequestDetailsTo(
        caseRef, providerRequestFlow.getRequestTypeFormData().getProviderRequestType());

    return caseRef;
  }

  /**
   * Whether the flow still has no request type after the page's own context has been applied. The
   * type drives the dynamic form lookup, and a null one is an unfiltered query that would silently
   * render an arbitrary enquiry's form, so the wizard is restarted instead.
   */
  private boolean hasNoRequestType(final ProviderRequestFlowFormData providerRequestFlow) {
    final String requestType =
        providerRequestFlow.getRequestTypeFormData().getProviderRequestType();
    return requestType == null || requestType.isBlank();
  }

  /**
   * Populates the model the document upload page needs: the case context, the request type carried
   * as a hidden field, and the details URL for its back links. The back links must carry the same
   * context as the form, so returning to the details page is self-describing too.
   */
  private void populateDocumentUploadModel(
      final Model model, final String caseRef, final String requestType) {
    applyCaseContext(model, caseRef);
    model.addAttribute("providerRequestType", requestType);
    model.addAttribute(
        "providerRequestDetailsUrl",
        withRequestContext("/provider-requests/details", caseRef, requestType));
    populateAddEvidenceModel(model);
  }

  private String detailsRedirect(final String caseRef, final String providerRequestType) {
    return "redirect:"
        + withRequestContext("/provider-requests/details", caseRef, providerRequestType);
  }

  private String documentsRedirect(final String caseRef, final String providerRequestType) {
    return "redirect:"
        + withRequestContext("/provider-requests/documents", caseRef, providerRequestType);
  }

  /**
   * Builds a wizard URL that carries the case reference and request type so the target page is
   * self-describing and does not depend on the shared session flow to render or submit correctly.
   */
  private String withRequestContext(
      final String path, final String caseRef, final String providerRequestType) {
    final StringBuilder url = new StringBuilder(path);
    String separator = "?";
    if (isValidCaseReference(caseRef)) {
      url.append(separator).append("caseReferenceNumber=").append(caseRef);
      separator = "&";
    }
    if (providerRequestType != null && !providerRequestType.isBlank()) {
      url.append(separator).append("providerRequestType=").append(providerRequestType);
    }
    return url.toString();
  }
}
