package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceRequired;
import uk.gov.laa.ccms.caab.bean.evidence.EvidenceUploadFormData;
import uk.gov.laa.ccms.caab.constants.CcmsModule;
import uk.gov.laa.ccms.caab.model.BaseEvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetail;
import uk.gov.laa.ccms.data.model.EvidenceDocumentTypeLookupValueDetail;

@ExtendWith(MockitoExtension.class)
class EvidenceMapperTest {

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  CommonMapper commonMapper;

  @InjectMocks
  EvidenceMapper evidenceMapper;

  @Test
  void evidenceUploadFormDataToEvidenceDocumentDetail() throws IOException {
    EvidenceUploadFormData evidenceUploadFormData = new EvidenceUploadFormData();
    evidenceUploadFormData.setApplicationOrOutcomeId("123");
    evidenceUploadFormData.setCaseReferenceNumber("caseRef");
    evidenceUploadFormData.setCcmsModule(CcmsModule.APPLICATION);
    evidenceUploadFormData.setDocumentDescription("doc desc");
    evidenceUploadFormData.setDocumentSender("doc sender");
    evidenceUploadFormData.setDocumentType("docType");
    evidenceUploadFormData.setDocumentTypeDisplayValue("doc type");
    evidenceUploadFormData.setEvidenceTypes(List.of("type 1", "type 2"));
    evidenceUploadFormData.setFile(new MockMultipartFile(
        "theFile",
        "originalName",
        "contentType",
        "the file data".getBytes()));
    evidenceUploadFormData.setFileExtension("ext");
    evidenceUploadFormData.setProviderId(789);
    evidenceUploadFormData.setRegisteredDocumentId("regId");

    EvidenceDocumentDetail result =
        evidenceMapper.toEvidenceDocumentDetail(evidenceUploadFormData);

    assertNotNull(result);
    assertEquals(evidenceUploadFormData.getApplicationOrOutcomeId().toString(), result.getApplicationOrOutcomeId());
    assertNull(result.getAuditTrail());
    assertEquals(evidenceUploadFormData.getCaseReferenceNumber(), result.getCaseReferenceNumber());
    assertEquals(evidenceUploadFormData.getCcmsModule().getCode(), result.getCcmsModule());
    assertEquals(evidenceUploadFormData.getDocumentDescription(), result.getDescription());
    assertEquals(evidenceUploadFormData.getDocumentSender(), result.getDocumentSender());
    assertEquals(evidenceUploadFormData.getDocumentType(), result.getDocumentType().getId());
    assertEquals(evidenceUploadFormData.getDocumentTypeDisplayValue(), result.getDocumentType().getDisplayValue());
    assertEquals(String.join("^", evidenceUploadFormData.getEvidenceTypes()), result.getEvidenceDescriptions());
    assertEquals(Base64.getEncoder().encodeToString(evidenceUploadFormData.getFile().getBytes()), result.getFileData());
    assertEquals(evidenceUploadFormData.getFileExtension(), result.getFileExtension());
    assertEquals(evidenceUploadFormData.getFile().getOriginalFilename(), result.getFileName());
    assertNull(result.getId());
    assertNull(result.getNotificationReference());
    assertEquals(evidenceUploadFormData.getProviderId().toString(), result.getProviderId());
    assertEquals(evidenceUploadFormData.getRegisteredDocumentId(), result.getRegisteredDocumentId());
    assertNull(result.getTransferResponseCode());
    assertNull(result.getTransferResponseDescription());
    assertEquals(0, result.getTransferRetryCount());
    assertNull(result.getTransferStatus());
  }

  @Test
  void toEvidenceRequiredProvidedTrue() {
    EvidenceDocumentTypeLookupValueDetail evidenceDocumentTypeLookupValueDetail =
        new EvidenceDocumentTypeLookupValueDetail().code("thecode").description("evidence 1");

    BaseEvidenceDocumentDetail baseEvidenceDocumentDetail = new BaseEvidenceDocumentDetail()
        .evidenceDescriptions("hello^evidence 1^goodbye");

    EvidenceRequired result = evidenceMapper.toEvidenceRequired(
        evidenceDocumentTypeLookupValueDetail, List.of(baseEvidenceDocumentDetail));

    assertNotNull(result);
    assertEquals(evidenceDocumentTypeLookupValueDetail.getCode(), result.getCode());
    assertEquals(evidenceDocumentTypeLookupValueDetail.getDescription(), result.getDescription());
    assertTrue(result.getProvided());
  }

  @Test
  void toEvidenceRequiredProvidedFalse() {
    EvidenceDocumentTypeLookupValueDetail evidenceDocumentTypeLookupValueDetail =
        new EvidenceDocumentTypeLookupValueDetail().code("thecode").description("evidence 1");

    BaseEvidenceDocumentDetail baseEvidenceDocumentDetail = new BaseEvidenceDocumentDetail()
        .evidenceDescriptions("hello^evidence 2^goodbye");

    EvidenceRequired result = evidenceMapper.toEvidenceRequired(
        evidenceDocumentTypeLookupValueDetail, List.of(baseEvidenceDocumentDetail));

    assertNotNull(result);
    assertEquals(evidenceDocumentTypeLookupValueDetail.getCode(), result.getCode());
    assertEquals(evidenceDocumentTypeLookupValueDetail.getDescription(), result.getDescription());
    assertFalse(result.getProvided());
  }

  @Test
  void toEvidenceRequiredList() {
    EvidenceDocumentTypeLookupValueDetail docType1 =
        new EvidenceDocumentTypeLookupValueDetail().code("thecode").description("evidence 1");
    EvidenceDocumentTypeLookupValueDetail docType2 =
        new EvidenceDocumentTypeLookupValueDetail().code("thecode").description("evidence 2");

    BaseEvidenceDocumentDetail baseEvidenceDocumentDetail = new BaseEvidenceDocumentDetail()
        .evidenceDescriptions("hello^evidence 2^goodbye");

    List<EvidenceRequired> result = evidenceMapper.toEvidenceRequiredList(
        List.of(docType1, docType2), List.of(baseEvidenceDocumentDetail));

    assertNotNull(result);
    assertEquals(2, result.size());
  }
}
