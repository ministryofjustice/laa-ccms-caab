package uk.gov.laa.ccms.caab.controller.application.summary;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DOCUMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.EVIDENCE_REQUIRED;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.EVIDENCE_UPLOAD_FORM_DATA;

import java.util.List;
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
import reactor.util.function.Tuple2;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.bean.validators.evidence.EvidenceUploadValidator;
import uk.gov.laa.ccms.caab.constants.CcmsModule;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetails;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.EvidenceService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.EvidenceDocumentTypeLookupValueDetail;

/**
 * Controller for handling the upload of evidence documents during the application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(EVIDENCE_REQUIRED)
public class EvidenceSectionController {

  private final EvidenceService evidenceService;

  private final ApplicationService applicationService;

  private final LookupService lookupService;

  private final EvidenceUploadValidator evidenceUploadValidator;

  /**
   * Handles the GET request for the evidence upload screen.
   *
   * @param applicationId The id of the active application.
   * @param model              The model for the view.
   * @return The view name for the evidence upload view.
   */
  @GetMapping("/application/summary/evidence")
  public String viewEvidenceRequired(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      final Model model) {

    // Retrieve the application detail
    final ApplicationDetail application = applicationService.getApplication(applicationId)
        .blockOptional()
        .orElseThrow(() -> new CaabApplicationException(
            String.format("Failed to retrieve application with id: %s", applicationId)));

    // Get the list of required evidence docs for this application.
    final Mono<List<EvidenceDocumentTypeLookupValueDetail>> evidenceRequiredMono =
        evidenceService.getDocumentsRequired(
            applicationId,
            application.getCaseReferenceNumber(),
            application.getProviderDetails().getProvider().getId());

    // Retrieve the list of previously uploaded documents.
    final Mono<EvidenceDocumentDetails> evidenceUploadedMono =
        evidenceService.getUploadedDocuments(
            application.getCaseReferenceNumber(),
            CcmsModule.APPLICATION);

    Tuple2<List<EvidenceDocumentTypeLookupValueDetail>,
        EvidenceDocumentDetails> combinedResult = Mono.zip(
            evidenceRequiredMono,
            evidenceUploadedMono)
        .blockOptional()
        .orElseThrow(() -> new CaabApplicationException("Failed to retrieve evidence data"));

    model.addAttribute(EVIDENCE_REQUIRED, combinedResult.getT1());
    model.addAttribute("evidenceUploaded", combinedResult.getT2());

    return "application/summary/evidence-section";
  }

  /**
   * Handles the GET request for the add evidence screen.
   *
   * @param applicationId The id of the active application.
   * @param model              The model for the view.
   * @return The view name for the evidence upload view.
   */
  @GetMapping("/application/evidence/add")
  public String viewEvidenceUpload(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @ModelAttribute(EVIDENCE_UPLOAD_FORM_DATA)
      final EvidenceUploadFormData evidenceUploadFormData,
      @SessionAttribute(EVIDENCE_REQUIRED)
      final List<EvidenceDocumentTypeLookupValueDetail> evidenceRequired,
      final Model model) {

    populateAddEvidenceModel(evidenceRequired, model);

    return "application/evidence/evidence-add";
  }

  /**
   * Handles the POST request to upload a new evidence document.
   *
   * @param applicationId The id of the active application.
   * @param model              The model for the view.
   * @return The view name for the evidence upload view.
   */
  @PostMapping("/application/evidence/add")
  public String uploadEvidenceDocument(
      @SessionAttribute(APPLICATION_ID) final String applicationId,
      @ModelAttribute(EVIDENCE_UPLOAD_FORM_DATA)
      final EvidenceUploadFormData evidenceUploadFormData,
      @SessionAttribute(EVIDENCE_REQUIRED)
      final List<EvidenceDocumentTypeLookupValueDetail> evidenceRequired,
      final BindingResult bindingResult,
      Model model) {

    // Validate the evidence form data
    evidenceUploadValidator.validate(evidenceUploadFormData, bindingResult);

    if (bindingResult.hasErrors()) {
      populateAddEvidenceModel(evidenceRequired, model);
      return "application/evidence/evidence-add";
    }

    // Scan the document for viruses
//    ScanResult scanResult = avScanService.performAvScan(file.getInputStream(),
//        new AvScanDetails(caseId, provideId, userId, file.getOriginalFilename(),
//            EvidencDocumentHelper.getSourcePageFromCode(sourcePage)));
//
//      if (!scanResult.isClean() || !scanResult.isScanServiceEnabled()) {
//
//      }

    // All clear, so register the document in EBS before saving to the TDS.
    

    return "application/evidence/evidence-add";
  }

  private void populateAddEvidenceModel(
      List<EvidenceDocumentTypeLookupValueDetail> evidenceRequired,
      Model model) {
    // Get the full list of document types to display in a dropdown in the view.
    final CommonLookupDetail evidenceTypes =
        lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES)
            .blockOptional()
            .orElseThrow(() -> new CaabApplicationException(
                "Failed to retrieve evidence types"));

    model.addAttribute(EVIDENCE_REQUIRED, evidenceRequired);
    model.addAttribute("evidenceTypes", evidenceTypes);
  }
}
