package uk.gov.laa.ccms.caab.controller.application.section;

import static uk.gov.laa.ccms.caab.constants.CcmsModule.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DOCUMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.SendBy.ELECTRONIC;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.EVIDENCE_REQUIRED;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.EVIDENCE_UPLOAD_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.DisplayUtil.getCommaDelimitedString;
import static uk.gov.laa.ccms.caab.util.FileUtil.getFileExtension;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceRequired;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.bean.validators.evidence.EvidenceUploadValidator;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.caab.exception.AvScanException;
import uk.gov.laa.ccms.caab.exception.AvVirusFoundException;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.EvidenceMapper;
import uk.gov.laa.ccms.caab.model.BaseEvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetails;
import uk.gov.laa.ccms.caab.service.AvScanService;
import uk.gov.laa.ccms.caab.service.EvidenceService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.EvidenceDocumentTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller for handling the upload of evidence documents during the application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({EVIDENCE_REQUIRED, EVIDENCE_UPLOAD_FORM_DATA})
public class EvidenceSectionController {

  private final EvidenceService evidenceService;

  private final AvScanService avScanService;

  private final LookupService lookupService;

  private final EvidenceUploadValidator evidenceUploadValidator;

  private final EvidenceMapper evidenceMapper;


  /**
   * Handles the GET request for the evidence upload screen.
   *
   * @param activeCase         The details of the active case.
   * @param model              The model for the view.
   * @return The view name for the evidence upload view.
   */
  @GetMapping("/{caseContext}/sections/evidence")
  public String viewEvidenceRequired(
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      @PathVariable CaseContext caseContext,
      final Model model) {
    // Get the list of required evidence docs for this application.
    final Mono<List<EvidenceDocumentTypeLookupValueDetail>> evidenceRequiredMono =
        evidenceService.getDocumentsRequired(
            String.valueOf(activeCase.getApplicationId()),
            activeCase.getCaseReferenceNumber(),
            activeCase.getProviderId());

    // Retrieve the list of previously uploaded documents.
    final Mono<EvidenceDocumentDetails> evidenceUploadedMono =
        evidenceService.getEvidenceDocumentsForCase(
            activeCase.getCaseReferenceNumber(),
            APPLICATION);

    Tuple2<List<EvidenceDocumentTypeLookupValueDetail>,
        EvidenceDocumentDetails> combinedResult = Mono.zip(
            evidenceRequiredMono,
            evidenceUploadedMono)
        .blockOptional()
        .orElseThrow(() -> new CaabApplicationException("Failed to retrieve evidence data"));

    List<EvidenceDocumentTypeLookupValueDetail> evidenceRequiredLookups = combinedResult.getT1();
    List<BaseEvidenceDocumentDetail> evidenceUploaded = combinedResult.getT2().getContent();

    final List<EvidenceRequired> evidenceRequired =
        evidenceMapper.toEvidenceRequiredList(evidenceRequiredLookups, evidenceUploaded);

    model.addAttribute(EVIDENCE_REQUIRED, evidenceRequired);
    model.addAttribute("evidenceUploaded", evidenceUploaded);
    model.addAttribute("caseContext", caseContext.getPathValue());

    return "application/sections/evidence-section";
  }

  /**
   * Handles the GET request for the add evidence screen.
   *
   * @param activeCase Basic details of the active case.
   * @param userDetail The user details.
   * @param evidenceRequired List of evidence required.
   * @param model              The model for the view.
   * @return The view name for the evidence upload view.
   */
  @GetMapping("/{caseContext}/evidence/add")
  public String viewEvidenceUpload(
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      @SessionAttribute(USER_DETAILS) final UserDetail userDetail,
      @SessionAttribute(EVIDENCE_REQUIRED)
      final List<EvidenceRequired> evidenceRequired,
      @PathVariable CaseContext caseContext,
      final Model model) {

    // Initialise the form data object.
    final EvidenceUploadFormData evidenceUploadFormData = new EvidenceUploadFormData();
    evidenceUploadFormData.setApplicationOrOutcomeId(String.valueOf(activeCase.getApplicationId()));
    evidenceUploadFormData.setCaseReferenceNumber(activeCase.getCaseReferenceNumber());
    evidenceUploadFormData.setProviderId(activeCase.getProviderId());
    evidenceUploadFormData.setDocumentSender(userDetail.getLoginId());
    evidenceUploadFormData.setCcmsModule(APPLICATION);

    model.addAttribute(EVIDENCE_UPLOAD_FORM_DATA, evidenceUploadFormData);
    model.addAttribute("caseContext", caseContext.getPathValue());
    populateAddEvidenceModel(evidenceRequired, model);

    return "application/evidence/evidence-add";
  }

  /**
   * Handles the POST request to upload a new evidence document.
   *
   * @param evidenceUploadFormData The upload form data.
   * @param evidenceRequired List of evidence required.
   * @param userDetail The user details.
   * @param bindingResult The binding result for validation.
   * @param model         The model for the view.
   * @return The view name for the evidence upload view.
   */
  @PostMapping("/{caseContext}/evidence/add")
  public String uploadEvidenceDocument(
      @SessionAttribute(EVIDENCE_REQUIRED)
      final List<EvidenceRequired> evidenceRequired,
      @SessionAttribute(USER_DETAILS)
      final UserDetail userDetail,
      @ModelAttribute(EVIDENCE_UPLOAD_FORM_DATA)
      final EvidenceUploadFormData evidenceUploadFormData,
      final BindingResult bindingResult,
      @PathVariable CaseContext caseContext,
      Model model) {

    // Validate the evidence form data
    evidenceUploadValidator.validate(evidenceUploadFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      populateAddEvidenceModel(evidenceRequired, model);
      model.addAttribute("caseContext", caseContext.getPathValue());
      return "application/evidence/evidence-add";
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
      model.addAttribute("caseContext", caseContext.getPathValue());
      populateAddEvidenceModel(evidenceRequired, model);
      return "application/evidence/evidence-add";
    }

    final String fileExtension = getFileExtension(evidenceUploadFormData.getFile());

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

    evidenceService.addDocument(
        evidenceMapper.toEvidenceDocumentDetail(evidenceUploadFormData),
            userDetail.getLoginId())
        .blockOptional()
        .orElseThrow(() -> new CaabApplicationException("Failed to save document"));

    return "redirect:/%s/sections/evidence".formatted(caseContext.getPathValue());
  }

  /**
   * Exception handler to catch when the uploaded file is too large.
   *
   * @param evidenceRequired - the list of required evidence from the session.
   * @param evidenceUploadFormData - the form data for the page.
   * @param model - the model
   * @return the view name for the evidence-add screen.
   */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public String handleUploadFileTooLarge(
      HttpServletRequest request,
      @SessionAttribute(EVIDENCE_REQUIRED)
      final List<EvidenceRequired> evidenceRequired,
      @SessionAttribute(EVIDENCE_UPLOAD_FORM_DATA)
      final EvidenceUploadFormData evidenceUploadFormData,
      final Model model) {

    // Manually construct a BindingResult to hold the file size error.
    BindingResult bindingResult = new BeanPropertyBindingResult(
        evidenceUploadFormData, EVIDENCE_UPLOAD_FORM_DATA);
    evidenceUploadValidator.rejectFileSize(bindingResult);

    /*
     * todo: Find a way to retain the posted form data (doc type, description,
     *  selected evidence types)
     */
    model.addAttribute(EVIDENCE_UPLOAD_FORM_DATA, evidenceUploadFormData);
    model.addAttribute(BindingResult.MODEL_KEY_PREFIX + EVIDENCE_UPLOAD_FORM_DATA,
        bindingResult);

    populateAddEvidenceModel(evidenceRequired, model);
    return "application/evidence/evidence-add";
  }

  /**
   * Handles the GET request to remove an uploaded evidence document.
   *
   * @param evidenceDocumentId The id of the evidence document to remove.
   * @param activeCase Basic details of the active case.
   * @param userDetail The user details.
   * @return Redirect to the evidence summary view.
   */
  @GetMapping("/{caseContext}/evidence/{evidence-document-id}/remove")
  public String removeEvidenceDocument(
      @PathVariable("evidence-document-id") final Integer evidenceDocumentId,
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      @SessionAttribute(USER_DETAILS) final UserDetail userDetail,
      @PathVariable CaseContext caseContext) {

    evidenceService.removeDocument(
        String.valueOf(activeCase.getApplicationId()),
        evidenceDocumentId,
        APPLICATION,
        userDetail.getLoginId());

    return "redirect:/%s/sections/evidence".formatted(caseContext.getPathValue());
  }

  private void populateAddEvidenceModel(
      List<EvidenceRequired> evidenceRequired,
      Model model) {
    // Get the full list of document types to display in a dropdown in the view.
    final CommonLookupDetail evidenceTypes =
        lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES)
            .blockOptional()
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve evidence types"));

    model.addAttribute(EVIDENCE_REQUIRED, evidenceRequired);
    model.addAttribute("evidenceTypes", evidenceTypes.getContent());
    model.addAttribute("validExtensions",
        getCommaDelimitedString(evidenceUploadValidator.getValidExtensions()));
    model.addAttribute("maxFileSize",
        evidenceUploadValidator.getMaxFileSize());
  }


}
