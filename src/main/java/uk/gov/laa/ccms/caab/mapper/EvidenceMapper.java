package uk.gov.laa.ccms.caab.mapper;

import java.io.IOException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetail;

/**
 * Mapper class to convert Evidence between various formats.
 */
@Mapper(componentModel = "spring", uses = CommonMapper.class)
public interface EvidenceMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "notificationReference", ignore = true)
  @Mapping(target = "transferStatus", ignore = true)
  @Mapping(target = "transferResponseCode", ignore = true)
  @Mapping(target = "transferResponseDescription", ignore = true)
  @Mapping(target = "transferRetryCount", constant = "0")
  @Mapping(target = "fileName", source = "file.originalFilename")
  @Mapping(target = "fileData", source = "file")
  @Mapping(target = "description", source = "documentDescription")
  @Mapping(target = "documentType.id", source = "documentType")
  @Mapping(target = "documentType.displayValue", source = "documentTypeDisplayValue")
  @Mapping(target = "evidenceDescriptions", source = "evidenceTypes")
  EvidenceDocumentDetail toEvidenceDocumentDetail(final EvidenceUploadFormData formData);

  /**
   * Convenience method to retrieve the uploaded file bytes, and handle any IOException.
   *
   * @return file data bytes.
   * @throws CaabApplicationException if an error occurs.
   */
  default byte[] toFileBytes(MultipartFile file) throws CaabApplicationException {
    byte[] fileData = null;

    if (file != null) {
      try {
        fileData = file.getBytes();
      } catch (IOException ioe) {
        throw new CaabApplicationException("Failed to get uploaded file content", ioe);
      }
    }

    return fileData;
  }

}
