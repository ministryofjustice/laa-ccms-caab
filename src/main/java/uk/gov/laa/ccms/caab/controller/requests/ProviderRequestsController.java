package uk.gov.laa.ccms.caab.controller.requests;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.REFERENCE_DATA_ITEM_TYPE_LOV;
import static uk.gov.laa.ccms.caab.constants.CcmsModule.REQUEST;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DOCUMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.SendBy.ELECTRONIC;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_PROVIDER_REQUEST_EVIDENCE_UPLOAD_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_PROVIDER_REQUEST_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.GENERAL_PROVIDER_REQUEST_EVIDENCE_UPLOAD_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.GENERAL_PROVIDER_REQUEST_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROVIDER_REQUEST_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_RESULT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.DisplayUtil.getCommaDelimitedString;
import static uk.gov.laa.ccms.caab.util.FileUtil.getFileExtension;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import reactor.core.publisher.Flux;
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
import uk.gov.laa.ccms.caab.constants.ProviderRequestFlowType;
import uk.gov.laa.ccms.caab.exception.AvScanException;
import uk.gov.laa.ccms.caab.exception.AvVirusFoundException;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.ProviderRequestsMapper;
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
@SessionAttributes(
    value = {
      GENERAL_PROVIDER_REQUEST_FLOW_FORM_DATA,
      CASE_PROVIDER_REQUEST_FLOW_FORM_DATA,
      GENERAL_PROVIDER_REQUEST_EVIDENCE_UPLOAD_FORM_DATA,
      CASE_PROVIDER_REQUEST_EVIDENCE_UPLOAD_FORM_DATA
    })
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

  /**
   * Creates a new instance of {@link ProviderRequestFlowFormData}.
   *
   * @return A new instance of {@link ProviderRequestFlowFormData}.
   */
  @ModelAttribute(GENERAL_PROVIDER_REQUEST_FLOW_FORM_DATA)
  public ProviderRequestFlowFormData getGeneralProviderRequestFlowFormData() {
    return new ProviderRequestFlowFormData();
  }

  /** Creates a new instance of {@link ProviderRequestFlowFormData} for case-scoped requests. */
  @ModelAttribute(CASE_PROVIDER_REQUEST_FLOW_FORM_DATA)
  public ProviderRequestFlowFormData getCaseProviderRequestFlowFormData() {
    return new ProviderRequestFlowFormData();
  }

  /** Creates a new instance of {@link EvidenceUploadFormData} for general provider requests. */
  @ModelAttribute(GENERAL_PROVIDER_REQUEST_EVIDENCE_UPLOAD_FORM_DATA)
  public EvidenceUploadFormData getGeneralProviderRequestEvidenceUploadFormData() {
    return new EvidenceUploadFormData();
  }

  /** Creates a new instance of {@link EvidenceUploadFormData} for case provider requests. */
  @ModelAttribute(CASE_PROVIDER_REQUEST_EVIDENCE_UPLOAD_FORM_DATA)
  public EvidenceUploadFormData getCaseProviderRequestEvidenceUploadFormData() {
    return new EvidenceUploadFormData();
  }

  /**
   * Handles the GET request for selecting the provider requests type page.
   *
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("/general-provider-requests/types")
  public String getGeneralRequestType(
      @ModelAttribute(GENERAL_PROVIDER_REQUEST_FLOW_FORM_DATA)
          final ProviderRequestFlowFormData providerRequestFlow,
      @SessionAttribute(USER_DETAILS) final UserDetail userDetail,
      final Model model,
      HttpSession session) {
    return requestTypeGet(
        providerRequestFlow,
        UNRELATED_CASE_REFERENCE,
        userDetail,
        model,
        session,
        ProviderRequestFlowType.GENERAL);
  }

  /** Handles the GET request for selecting the case-scoped provider request type page. */
  @GetMapping("/case-provider-requests/types")
  public String getCaseRequestType(
      @ModelAttribute(CASE_PROVIDER_REQUEST_FLOW_FORM_DATA)
          final ProviderRequestFlowFormData providerRequestFlow,
      @RequestParam(required = false) final String caseReferenceNumber,
      @SessionAttribute(USER_DETAILS) final UserDetail userDetail,
      final Model model,
      HttpSession session) {
    return requestTypeGet(
        providerRequestFlow,
        caseReferenceNumber,
        userDetail,
        model,
        session,
        ProviderRequestFlowType.CASE);
  }

  private String requestTypeGet(
      final ProviderRequestFlowFormData providerRequestFlow,
      final String caseReferenceNumber,
      final UserDetail userDetail,
      final Model model,
      final HttpSession session,
      final ProviderRequestFlowType flowType) {
    final String effectiveCaseRef =
        flowType.isCaseScoped()
            ? initialiseCaseRequestScope(providerRequestFlow, caseReferenceNumber, model)
            : initialiseGeneralRequestScope(providerRequestFlow, session, model);
    providerRequestFlow.setCaseReferenceNumber(effectiveCaseRef);

    // reset the details data, so new document id and form details are created
    providerRequestFlow.resetRequestDetailsFormData();

    addProviderRequestFlowModel(
        model, flowType, providerRequestFlow, effectiveCaseRef, "/types", null);
    model.addAttribute("providerRequestTypeDetails", providerRequestFlow.getRequestTypeFormData());

    populateProviderRequestTypes(model, userDetail, flowType.isCaseScoped());

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
  @PostMapping("/general-provider-requests/types")
  public String requestTypeGeneralPost(
      @SessionAttribute(GENERAL_PROVIDER_REQUEST_FLOW_FORM_DATA)
          final ProviderRequestFlowFormData providerRequestFlow,
      @ModelAttribute("providerRequestTypeDetails")
          final ProviderRequestTypeFormData providerRequestTypeDetails,
      @SessionAttribute(USER_DETAILS) final UserDetail userDetail,
      final Model model,
      final BindingResult bindingResult) {
    return requestTypePost(
        providerRequestFlow,
        providerRequestTypeDetails,
        userDetail,
        model,
        bindingResult,
        ProviderRequestFlowType.GENERAL);
  }

  /** Handles the POST request for submitting case-scoped provider request type details. */
  @PostMapping("/case-provider-requests/types")
  public String requestTypeCasePost(
      @SessionAttribute(CASE_PROVIDER_REQUEST_FLOW_FORM_DATA)
          final ProviderRequestFlowFormData providerRequestFlow,
      @ModelAttribute("providerRequestTypeDetails")
          final ProviderRequestTypeFormData providerRequestTypeDetails,
      @SessionAttribute(USER_DETAILS) final UserDetail userDetail,
      final Model model,
      final BindingResult bindingResult) {
    return requestTypePost(
        providerRequestFlow,
        providerRequestTypeDetails,
        userDetail,
        model,
        bindingResult,
        ProviderRequestFlowType.CASE);
  }

  private String requestTypePost(
      final ProviderRequestFlowFormData providerRequestFlow,
      final ProviderRequestTypeFormData providerRequestTypeDetails,
      final UserDetail userDetail,
      final Model model,
      final BindingResult bindingResult,
      final ProviderRequestFlowType flowType) {
    providerRequestTypeValidator.validate(providerRequestTypeDetails, bindingResult);

    String caseRef = providerRequestFlow.getCaseReferenceNumber();
    addProviderRequestFlowModel(model, flowType, providerRequestFlow, caseRef, "/types", null);

    if (bindingResult.hasErrors()) {
      addCaseReferenceIfValid(model, caseRef);
      populateProviderRequestTypes(model, userDetail, flowType.isCaseScoped());
      model.addAttribute("providerRequestTypeDetails", providerRequestTypeDetails);
      return "requests/provider-request-type";
    }

    providerRequestFlow.setRequestTypeFormData(providerRequestTypeDetails);

    return "redirect:" + buildFlowUrl(flowType, "/details", caseRef);
  }

  /**
   * Populates dropdown options for provider request types form. also filters based on user function
   * codes
   *
   * @param model The model for the view.
   * @param userDetail Logged-in user details.
   */
  protected void populateProviderRequestTypes(
      final Model model, UserDetail userDetail, boolean isCaseRelated) {

    List<String> functions =
        Optional.ofNullable(userDetail.getFunctions()).orElse(Collections.emptyList());

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
  @GetMapping("/general-provider-requests/details")
  public String getGeneralRequestDetail(
      @SessionAttribute(GENERAL_PROVIDER_REQUEST_FLOW_FORM_DATA)
          final ProviderRequestFlowFormData providerRequestFlow,
      final Model model,
      HttpSession session) {
    return requestDetailGet(
        UNRELATED_CASE_REFERENCE,
        providerRequestFlow,
        model,
        session,
        ProviderRequestFlowType.GENERAL);
  }

  /** Handles the GET request for the case-scoped provider request details page. */
  @GetMapping("/case-provider-requests/details")
  public String getCaseRequestDetail(
      @RequestParam(required = false) final String caseReferenceNumber,
      @SessionAttribute(CASE_PROVIDER_REQUEST_FLOW_FORM_DATA)
          final ProviderRequestFlowFormData providerRequestFlow,
      final Model model,
      HttpSession session) {
    return requestDetailGet(
        caseReferenceNumber, providerRequestFlow, model, session, ProviderRequestFlowType.CASE);
  }

  private String requestDetailGet(
      final String caseReferenceNumber,
      final ProviderRequestFlowFormData providerRequestFlow,
      final Model model,
      final HttpSession session,
      final ProviderRequestFlowType flowType) {
    final String caseRef =
        flowType.isCaseScoped()
            ? initialiseCaseRequestScope(providerRequestFlow, caseReferenceNumber, model)
            : initialiseGeneralRequestScope(providerRequestFlow, session, model);
    providerRequestFlow.setCaseReferenceNumber(caseRef);
    addProviderRequestFlowModel(
        model, flowType, providerRequestFlow, caseRef, "/details", "/types");
    populateAddEvidenceModel(model);

    final ProviderRequestDetailsFormData providerRequestDetailsForm =
        providerRequestFlow.getRequestDetailsFormData();

    return providerRequestsDetails(
        providerRequestFlow, providerRequestDetailsForm, model, flowType);
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
  @PostMapping("/general-provider-requests/details")
  public String postGeneralRequestDetail(
      @SessionAttribute(USER_DETAILS) final UserDetail userDetail,
      @SessionAttribute(GENERAL_PROVIDER_REQUEST_FLOW_FORM_DATA)
          final ProviderRequestFlowFormData providerRequestFlow,
      @RequestParam final String action,
      @ModelAttribute("providerRequestDetails")
          final ProviderRequestDetailsFormData providerRequestDetailsForm,
      final Model model,
      final BindingResult bindingResult,
      final HttpSession session) {
    return requestDetailPost(
        userDetail,
        providerRequestFlow,
        action,
        providerRequestDetailsForm,
        model,
        bindingResult,
        session,
        ProviderRequestFlowType.GENERAL);
  }

  /** Handles the POST request to submit case-scoped provider request details. */
  @PostMapping("/case-provider-requests/details")
  public String postCaseRequestDetail(
      @SessionAttribute(USER_DETAILS) final UserDetail userDetail,
      @SessionAttribute(CASE_PROVIDER_REQUEST_FLOW_FORM_DATA)
          final ProviderRequestFlowFormData providerRequestFlow,
      @RequestParam final String action,
      @ModelAttribute("providerRequestDetails")
          final ProviderRequestDetailsFormData providerRequestDetailsForm,
      final Model model,
      final BindingResult bindingResult,
      final HttpSession session) {
    return requestDetailPost(
        userDetail,
        providerRequestFlow,
        action,
        providerRequestDetailsForm,
        model,
        bindingResult,
        session,
        ProviderRequestFlowType.CASE);
  }

  private String requestDetailPost(
      final UserDetail userDetail,
      final ProviderRequestFlowFormData providerRequestFlow,
      final String action,
      final ProviderRequestDetailsFormData providerRequestDetailsForm,
      final Model model,
      final BindingResult bindingResult,
      final HttpSession session,
      final ProviderRequestFlowType flowType) {
    providerRequestsMapper.toProviderRequestDetailsFormData(
        providerRequestDetailsForm, providerRequestFlow);

    String caseRef = providerRequestFlow.getCaseReferenceNumber();
    addCaseReferenceIfValid(model, caseRef);

    if ("document_upload".equals(action)) {
      providerRequestFlow.setRequestDetailsFormData(providerRequestDetailsForm);

      return "redirect:" + buildFlowUrl(flowType, "/documents", caseRef);
    } else if ("document_delete".equals(action)) {

      evidenceService.removeDocument(
          String.valueOf(providerRequestFlow.getRequestDetailsFormData().getDocumentSessionId()),
          providerRequestDetailsForm.getDocumentIdToDelete(),
          REQUEST,
          userDetail.getLoginId());

      return providerRequestsDetails(
          providerRequestFlow, providerRequestDetailsForm, model, flowType);
    } else {
      providerRequestDetailsValidator.validate(providerRequestDetailsForm, bindingResult);

      if (bindingResult.hasErrors()) {
        populateAddEvidenceModel(model);
        return providerRequestsDetails(
            providerRequestFlow, providerRequestDetailsForm, model, flowType);
      }

      if (providerRequestDetailsForm.isClaimUploadEnabled()) {
        try {
          avScanService.performAvScan(
              null,
              null,
              null,
              null,
              providerRequestDetailsForm.getSanitisedFileName(),
              providerRequestDetailsForm.getFile().getInputStream());
        } catch (final AvVirusFoundException | AvScanException | IOException e) {
          bindingResult.rejectValue("file", "scan.failure", e.getMessage());
          providerRequestDetailsForm.setFile(null);
          return providerRequestsDetails(
              providerRequestFlow, providerRequestDetailsForm, model, flowType);
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
      session.setAttribute(SUBMISSION_RESULT, "confirmed");
      String redirectUrl =
          "/application/submit-%s-provider-request/confirmed"
              .formatted(flowType.isCaseScoped() ? "case" : "general");
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
  @GetMapping("/general-provider-requests/documents")
  public String addDocumentsToGeneralRequest(
      @SessionAttribute(GENERAL_PROVIDER_REQUEST_FLOW_FORM_DATA)
          final ProviderRequestFlowFormData providerRequestFlow,
      final Model model,
      HttpSession session) {
    return addDocumentsToRequestGet(
        UNRELATED_CASE_REFERENCE,
        providerRequestFlow,
        model,
        session,
        ProviderRequestFlowType.GENERAL);
  }

  /** Handles GET requests to add documents to a case-scoped provider request. */
  @GetMapping("/case-provider-requests/documents")
  public String addDocumentsToCaseRequest(
      @RequestParam(required = false) final String caseReferenceNumber,
      @SessionAttribute(CASE_PROVIDER_REQUEST_FLOW_FORM_DATA)
          final ProviderRequestFlowFormData providerRequestFlow,
      final Model model,
      HttpSession session) {
    return addDocumentsToRequestGet(
        caseReferenceNumber, providerRequestFlow, model, session, ProviderRequestFlowType.CASE);
  }

  private String addDocumentsToRequestGet(
      final String caseReferenceNumber,
      final ProviderRequestFlowFormData providerRequestFlow,
      final Model model,
      final HttpSession session,
      final ProviderRequestFlowType flowType) {
    final String caseRef =
        flowType.isCaseScoped()
            ? initialiseCaseRequestScope(providerRequestFlow, caseReferenceNumber, model)
            : initialiseGeneralRequestScope(providerRequestFlow, session, model);
    addProviderRequestFlowModel(
        model, flowType, providerRequestFlow, caseRef, "/documents", "/details");

    EvidenceUploadFormData evidenceUploadFormData = new EvidenceUploadFormData();
    addEvidenceUploadFormModel(model, flowType, evidenceUploadFormData);
    populateAddEvidenceModel(model);
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
  @PostMapping("/general-provider-requests/documents")
  public String addDocumentsToGeneralRequest(
      @SessionAttribute(USER_DETAILS) final UserDetail userDetail,
      @SessionAttribute(GENERAL_PROVIDER_REQUEST_FLOW_FORM_DATA)
          final ProviderRequestFlowFormData providerRequestFlow,
      @ModelAttribute(GENERAL_PROVIDER_REQUEST_EVIDENCE_UPLOAD_FORM_DATA)
          final EvidenceUploadFormData evidenceUploadFormData,
      final BindingResult bindingResult,
      final Model model) {
    return addDocumentsToRequestPost(
        userDetail,
        providerRequestFlow,
        evidenceUploadFormData,
        UNRELATED_CASE_REFERENCE,
        bindingResult,
        model,
        ProviderRequestFlowType.GENERAL);
  }

  /** Handles the POST request to upload a new case-scoped evidence document. */
  @PostMapping("/case-provider-requests/documents")
  public String addDocumentsToCaseRequest(
      @SessionAttribute(USER_DETAILS) final UserDetail userDetail,
      @SessionAttribute(CASE_PROVIDER_REQUEST_FLOW_FORM_DATA)
          final ProviderRequestFlowFormData providerRequestFlow,
      @ModelAttribute(CASE_PROVIDER_REQUEST_EVIDENCE_UPLOAD_FORM_DATA)
          final EvidenceUploadFormData evidenceUploadFormData,
      @RequestParam(required = false) String caseReferenceNumber,
      final BindingResult bindingResult,
      final Model model) {
    return addDocumentsToRequestPost(
        userDetail,
        providerRequestFlow,
        evidenceUploadFormData,
        caseReferenceNumber,
        bindingResult,
        model,
        ProviderRequestFlowType.CASE);
  }

  private String addDocumentsToRequestPost(
      final UserDetail userDetail,
      final ProviderRequestFlowFormData providerRequestFlow,
      final EvidenceUploadFormData evidenceUploadFormData,
      final String caseReferenceNumber,
      final BindingResult bindingResult,
      final Model model,
      final ProviderRequestFlowType flowType) {
    final String documentSessionId =
        providerRequestFlow.getRequestDetailsFormData().getDocumentSessionId().toString();

    final String caseRef =
        flowType.isCaseScoped()
            ? resolveCaseReference(
                caseReferenceNumber, providerRequestFlow.getCaseReferenceNumber())
            : UNRELATED_CASE_REFERENCE;

    // set the additional details for the evidence upload
    evidenceUploadFormData.setApplicationOrOutcomeId(documentSessionId);
    evidenceUploadFormData.setCaseReferenceNumber(caseRef);
    evidenceUploadFormData.setProviderId(userDetail.getProvider().getId());
    evidenceUploadFormData.setDocumentSender(userDetail.getLoginId());
    evidenceUploadFormData.setCcmsModule(REQUEST);
    addEvidenceUploadFormModel(model, flowType, evidenceUploadFormData);
    addProviderRequestFlowModel(
        model, flowType, providerRequestFlow, caseRef, "/documents", "/details");
    // Validate the evidence form data
    providerRequestDocumentUploadValidator.validate(evidenceUploadFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      addCaseReferenceIfValid(model, caseRef);
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
          evidenceUploadFormData.getSanitisedFileName(),
          evidenceUploadFormData.getFile().getInputStream());
    } catch (AvVirusFoundException | AvScanException | IOException e) {
      bindingResult.rejectValue("file", "scan.failure", e.getMessage());
      addCaseReferenceIfValid(model, caseRef);
      populateAddEvidenceModel(model);
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

    return "redirect:" + buildFlowUrl(flowType, "/details", caseRef);
  }

  /**
   * Exception handler to catch when the uploaded file is too large.
   *
   * @param request the current request.
   * @param session the current session.
   * @param model - the model
   * @return the view name for the evidence-add screen.
   */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public String handleUploadFileTooLarge(
      final HttpServletRequest request, final HttpSession session, final Model model) {
    ProviderRequestFlowType flowType = getFlowType(request.getRequestURI());
    final String evidenceUploadSessionAttribute = flowType.getEvidenceUploadSessionAttribute();
    EvidenceUploadFormData evidenceUploadFormData =
        (EvidenceUploadFormData) session.getAttribute(evidenceUploadSessionAttribute);
    ProviderRequestFlowFormData providerRequestFlow =
        (ProviderRequestFlowFormData) session.getAttribute(flowType.getFlowSessionAttribute());
    String caseRef =
        providerRequestFlow != null ? providerRequestFlow.getCaseReferenceNumber() : null;

    // Manually construct a BindingResult to hold the file size error.
    final BindingResult bindingResult =
        new BeanPropertyBindingResult(evidenceUploadFormData, evidenceUploadSessionAttribute);
    providerRequestDocumentUploadValidator.rejectFileSize(bindingResult);

    addEvidenceUploadFormModel(model, flowType, evidenceUploadFormData);
    model.addAttribute(
        BindingResult.MODEL_KEY_PREFIX + evidenceUploadSessionAttribute, bindingResult);

    addProviderRequestFlowModel(
        model, flowType, providerRequestFlow, caseRef, "/documents", "/details");

    populateAddEvidenceModel(model);
    return "requests/provider-request-doc-upload";
  }

  /** Handles the POST request for the general submission page. */
  @PostMapping("/application/submit-general-provider-request/confirmed")
  public String generalProviderRequestSubmitted(final HttpSession session, final Model model) {
    return providerRequestSubmitted(session, model, ProviderRequestFlowType.GENERAL);
  }

  /** Handles the POST request for the case submission page. */
  @PostMapping("/application/submit-case-provider-request/confirmed")
  public String caseProviderRequestSubmitted(final HttpSession session, final Model model) {
    return providerRequestSubmitted(session, model, ProviderRequestFlowType.CASE);
  }

  private String providerRequestSubmitted(
      final HttpSession session, final Model model, final ProviderRequestFlowType flowType) {
    session.removeAttribute(SUBMISSION_RESULT);
    model.asMap().remove(flowType.getFlowSessionAttribute());
    model.asMap().remove(flowType.getEvidenceUploadSessionAttribute());
    session.removeAttribute(flowType.getFlowSessionAttribute());
    session.removeAttribute(flowType.getEvidenceUploadSessionAttribute());
    return "redirect:" + flowTypeReturnUrl(flowType);
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
      final Model model,
      final ProviderRequestFlowType flowType) {

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

    addProviderRequestFlowModel(
        model,
        flowType,
        providerRequestFlow,
        providerRequestFlow.getCaseReferenceNumber(),
        "/details",
        "/types");

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

  private String resolveCaseReference(
      final String caseReferenceNumber, final String existingCaseReference) {
    String resolvedCaseReference;
    if (isValidCaseReference(caseReferenceNumber)) {
      resolvedCaseReference = caseReferenceNumber;
    } else if (isValidCaseReference(existingCaseReference)) {
      resolvedCaseReference = existingCaseReference;
    } else {
      resolvedCaseReference = UNRELATED_CASE_REFERENCE;
    }

    if (!isValidCaseReference(resolvedCaseReference)) {
      throw new CaabApplicationException("Case provider requests require a valid case reference");
    }
    return resolvedCaseReference;
  }

  private String initialiseGeneralRequestScope(
      final ProviderRequestFlowFormData providerRequestFlow,
      final HttpSession session,
      final Model model) {
    session.removeAttribute(ACTIVE_CASE);
    model.asMap().remove(ACTIVE_CASE);
    session.removeAttribute(CASE);
    model.asMap().remove(CASE);
    providerRequestFlow.setCaseReferenceNumber(UNRELATED_CASE_REFERENCE);
    return UNRELATED_CASE_REFERENCE;
  }

  private String initialiseCaseRequestScope(
      final ProviderRequestFlowFormData providerRequestFlow,
      final String caseReferenceNumber,
      final Model model) {
    final String resolvedCaseReference =
        resolveCaseReference(caseReferenceNumber, providerRequestFlow.getCaseReferenceNumber());
    model.addAttribute("caseReference", resolvedCaseReference);
    providerRequestFlow.setCaseReferenceNumber(resolvedCaseReference);
    return resolvedCaseReference;
  }

  private boolean isValidCaseReference(String caseReferenceNumber) {
    return caseReferenceNumber != null
        && caseReferenceNumber.matches("^\\d{12}$")
        && !UNRELATED_CASE_REFERENCE.equals(caseReferenceNumber);
  }

  private void addCaseReferenceIfValid(Model model, String caseRef) {
    if (isValidCaseReference(caseRef)) {
      model.addAttribute("caseReference", caseRef);
    }
  }

  private void addProviderRequestFlowModel(
      final Model model,
      final ProviderRequestFlowType flowType,
      final ProviderRequestFlowFormData providerRequestFlow,
      final String caseRef,
      final String submitPath,
      final String backPath) {
    model.addAttribute(PROVIDER_REQUEST_FLOW_FORM_DATA, providerRequestFlow);
    model.addAttribute("providerRequestSubmitUrl", flowType.getBasePath() + submitPath);
    model.addAttribute(
        "providerRequestBackUrl",
        backPath == null ? flowTypeReturnUrl(flowType) : buildFlowUrl(flowType, backPath, caseRef));
  }

  private void addEvidenceUploadFormModel(
      final Model model,
      final ProviderRequestFlowType flowType,
      final EvidenceUploadFormData evidenceUploadFormData) {
    model.addAttribute(
        "providerRequestEvidenceUploadFormAttribute", flowType.getEvidenceUploadSessionAttribute());
    model.addAttribute(flowType.getEvidenceUploadSessionAttribute(), evidenceUploadFormData);
  }

  private String buildFlowUrl(
      final ProviderRequestFlowType flowType, final String path, final String caseReferenceNumber) {
    final String url = flowType.getBasePath() + path;
    if (!flowType.isCaseScoped()) {
      return url;
    }
    return url + "?caseReferenceNumber=" + resolveCaseReference(caseReferenceNumber, null);
  }

  private String flowTypeReturnUrl(final ProviderRequestFlowType flowType) {
    return flowType.isCaseScoped() ? "/case/overview" : "/home";
  }

  private ProviderRequestFlowType getFlowType(String requestUri) {
    return requestUri.contains(ProviderRequestFlowType.CASE.getBasePath())
        ? ProviderRequestFlowType.CASE
        : ProviderRequestFlowType.GENERAL;
  }
}
