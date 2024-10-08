package uk.gov.laa.ccms.caab.mapper;

import java.util.List;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceRequired;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.model.BaseEvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.util.EvidenceUtil;
import uk.gov.laa.ccms.data.model.EvidenceDocumentTypeLookupValueDetail;

/**
 * Mapper class to convert Evidence between various formats.
 */
@Mapper(componentModel = "spring", uses = CommonMapper.class)
public interface EvidenceMapper {

  List<EvidenceRequired> toEvidenceRequiredList(
      final List<EvidenceDocumentTypeLookupValueDetail> evidenceTypes,
      @Context final List<BaseEvidenceDocumentDetail> evidenceUploaded);

  @Mapping(target = "code", source = "evidenceType.code")
  @Mapping(target = "description", source = "evidenceType.description")
  @Mapping(target = "provided", source = "evidenceType.description")
  EvidenceRequired toEvidenceRequired(
      final EvidenceDocumentTypeLookupValueDetail evidenceType,
      @Context final List<BaseEvidenceDocumentDetail> evidenceUploaded);

  /**
   * Check if any of the uploaded documents contains the provided evidence description.
   * Evidence descriptions are separated by a caret char.
   *
   * @param evidenceDescription - the evidence description.
   * @param evidenceUploaded - the list of uploaded evidence documents.
   * @return true if the evidence description appears in any uploaded document, false otherwise.
   */
  default Boolean isEvidenceProvided(
      final String evidenceDescription,
      @Context final List<BaseEvidenceDocumentDetail> evidenceUploaded) {
    return EvidenceUtil.isEvidenceProvided(evidenceDescription, evidenceUploaded);
  }

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
  @Mapping(target = "ccmsModule", source = "ccmsModule.code")
  EvidenceDocumentDetail toEvidenceDocumentDetail(final EvidenceUploadFormData formData);

}
