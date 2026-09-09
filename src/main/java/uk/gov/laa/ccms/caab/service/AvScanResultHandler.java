package uk.gov.laa.ccms.caab.service;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.exception.AvScanException;
import uk.gov.laa.ccms.caab.exception.AvVirusFoundException;

/**
 * Helper service for handling AV scan results and file upload errors. Centralizes the logic for
 * rejecting files when scans fail or files are too large, ensuring consistent error messaging and
 * logging across controllers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AvScanResultHandler {

  private final AvScanService avScanService;

  /**
   * Performs an AV scan on the uploaded file and rejects the file field if the scan fails. Returns
   * true if the scan was rejected (i.e., the caller should treat this as a failure and return the
   * form with errors).
   *
   * <p>On failure, logs the exception and rejects the "file" field with a fixed, generic message.
   * The exception message is not exposed to the user to avoid leaking internal details (e.g.,
   * scanner implementation details, file paths, or stack traces).
   *
   * @param formData the evidence upload form data containing case and file details
   * @param bindingResult the binding result to hold validation errors
   * @return true if the scan was rejected, false if the scan passed
   */
  public boolean isScanRejected(
      final EvidenceUploadFormData formData, final BindingResult bindingResult) {
    try (var inputStream = formData.getFile().getInputStream()) {
      avScanService.performAvScan(
          formData.getCaseReferenceNumber(),
          formData.getProviderId(),
          formData.getDocumentSender(),
          formData.getCcmsModule(),
          formData.getSanitisedFileName(),
          inputStream);
      return false;
    } catch (AvVirusFoundException | AvScanException | IOException e) {
      log.error("Document AV scan or file processing failed", e);
      bindingResult.rejectValue(
          "file", "scan.failure", "Unable to scan the file. Please try again.");
      return true;
    }
  }

  /**
   * Performs an AV scan with minimal parameters (for cases where detailed case/provider context is
   * not available, such as claim uploads in provider requests).
   *
   * @param sanitisedFileName the sanitized filename for the document being scanned
   * @param file the multipart file to scan
   * @param bindingResult the binding result to hold validation errors
   * @return true if the scan was rejected, false if the scan passed
   */
  public boolean isScanRejected(
      final String sanitisedFileName, final MultipartFile file, final BindingResult bindingResult) {
    try (var inputStream = file.getInputStream()) {
      avScanService.performAvScan(null, null, null, null, sanitisedFileName, inputStream);
      return false;
    } catch (AvVirusFoundException | AvScanException | IOException e) {
      log.error("Document AV scan or file processing failed", e);
      bindingResult.rejectValue(
          "file", "scan.failure", "Unable to scan the file. Please try again.");
      return true;
    }
  }
}
