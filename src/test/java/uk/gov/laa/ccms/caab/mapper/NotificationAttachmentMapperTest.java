package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import uk.gov.laa.ccms.caab.bean.notification.NotificationAttachmentUploadFormData;
import uk.gov.laa.ccms.caab.constants.SendBy;
import uk.gov.laa.ccms.caab.model.BaseNotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.soa.gateway.model.BaseDocument;
import uk.gov.laa.ccms.soa.gateway.model.Document;

@ExtendWith(MockitoExtension.class)
public class NotificationAttachmentMapperTest {

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  CommonMapper commonMapper;

  @InjectMocks
  NotificationAttachmentMapper notificationAttachmentMapper = new NotificationAttachmentMapperImpl();

  @Test
  void testNotificationAttachmentDetail_toBaseDocument() {

    NotificationAttachmentDetail notificationAttachmentDetail = new NotificationAttachmentDetail();
    notificationAttachmentDetail.setId(123);
    notificationAttachmentDetail.setProviderId("456");
    notificationAttachmentDetail.setFileName("fileName.pdf");
    notificationAttachmentDetail.setFileData("fileData");
    notificationAttachmentDetail.setNotificationReference("789");
    notificationAttachmentDetail.setDocumentType(new StringDisplayValue().id("1").displayValue(
        "docTypeDisplayValue"));
    notificationAttachmentDetail.setDescription("description");
    notificationAttachmentDetail.setNumber(1L);
    notificationAttachmentDetail.setSendBy("E");
    notificationAttachmentDetail.setStatus("status");

    BaseDocument result =
        notificationAttachmentMapper.toBaseDocument(notificationAttachmentDetail);

    assertEquals(notificationAttachmentDetail.getDocumentType().getId(),
        result.getDocumentType());
    assertEquals(notificationAttachmentDetail.getDescription(), result.getText());
    assertEquals("pdf", result.getFileExtension());
  }

  @Test
  void testNotificationAttachmentDetail_toDocument() throws IOException {

    NotificationAttachmentDetail notificationAttachmentDetail = new NotificationAttachmentDetail();
    notificationAttachmentDetail.setId(123);
    notificationAttachmentDetail.setProviderId("456");
    notificationAttachmentDetail.setFileName("fileName.pdf");
    notificationAttachmentDetail.setFileData("fileData");
    notificationAttachmentDetail.setNotificationReference("789");
    notificationAttachmentDetail.setDocumentType(new StringDisplayValue().id("1").displayValue(
        "docTypeDisplayValue"));
    notificationAttachmentDetail.setDescription("description");
    notificationAttachmentDetail.setNumber(1L);
    notificationAttachmentDetail.setSendBy("E");
    notificationAttachmentDetail.setStatus("status");

    Document result =
        notificationAttachmentMapper.toDocument(notificationAttachmentDetail);

    assertEquals(notificationAttachmentDetail.getDocumentType().getId(),
        result.getDocumentType());
    assertEquals(notificationAttachmentDetail.getDescription(), result.getText());
    assertEquals("pdf", result.getFileExtension());
    assertEquals(notificationAttachmentDetail.getStatus(), result.getStatus());
    assertEquals(notificationAttachmentDetail.getFileData(), result.getFileData());
  }

  @Test
  void testDocument_toBaseNotificationAttachmentDetail() {

    Document document = new Document();
    document.setDocumentId("12345");
    document.setFileExtension("ext");
    document.setDocumentType("doc type");
    document.setFileData("fileData");
    document.setDocumentLink("documentLink");
    document.setChannel("E");
    document.setStatus("status");
    document.setStatusDescription("status description");
    document.setText("text");

    BaseNotificationAttachmentDetail result =
        notificationAttachmentMapper.toBaseNotificationAttachmentDetail(document,
            "documentTypeDisplayValue");

    assertNull(result.getProviderId());
    assertNull(result.getFileName());
    assertNull(result.getNumber());
    assertNull(result.getNotificationReference());
    assertNull(result.getAuditTrail());

    assertEquals(document.getDocumentId(), String.valueOf(result.getId()));
    assertEquals(document.getText(), result.getDescription());
    assertEquals(document.getDocumentType(), result.getDocumentType().getId());
    assertEquals("documentTypeDisplayValue", result.getDocumentType().getDisplayValue());
  }

  @Test
  void testNotificationAttachmentDetail_toNotificationAttachmentUploadFormData()
      throws IOException {

    NotificationAttachmentDetail notificationAttachmentDetail = new NotificationAttachmentDetail();
    notificationAttachmentDetail.setId(123);
    notificationAttachmentDetail.setProviderId("456");
    notificationAttachmentDetail.setFileName("fileName.pdf");
    notificationAttachmentDetail.setFileData("ZmlsZURhdGE=");
    notificationAttachmentDetail.setNotificationReference("789");
    notificationAttachmentDetail.setDocumentType(new StringDisplayValue().id("1").displayValue(
        "docTypeDisplayValue"));
    notificationAttachmentDetail.setDescription("description");
    notificationAttachmentDetail.setNumber(1L);
    notificationAttachmentDetail.setSendBy("E");
    notificationAttachmentDetail.setStatus("status");

    NotificationAttachmentUploadFormData result =
        notificationAttachmentMapper.toNotificationAttachmentUploadFormData(notificationAttachmentDetail);

    assertEquals(notificationAttachmentDetail.getDocumentType().getId(), result.getDocumentType());
    assertEquals(notificationAttachmentDetail.getDocumentType().getDisplayValue(),
      result.getDocumentTypeDisplayValue());
    assertEquals(notificationAttachmentDetail.getSendBy(), result.getSendBy().getCode());
    assertEquals(notificationAttachmentDetail.getProviderId(),
        String.valueOf(result.getProviderId()));
    assertEquals(notificationAttachmentDetail.getFileName(),
        result.getFile().getOriginalFilename());
    assertTrue(Arrays.equals(Base64.getDecoder().decode(notificationAttachmentDetail.getFileData()),
        result.getFile().getBytes()));
    assertEquals(notificationAttachmentDetail.getDescription(), result.getDocumentDescription());
    assertEquals(notificationAttachmentDetail.getStatus(), result.getStatus());
    assertEquals(notificationAttachmentDetail.getNumber(), result.getNumber());
    assertEquals(notificationAttachmentDetail.getNotificationReference(), result.getNotificationReference());
  }

  @Test
  void testNotificationAttachmentUploadFormData_toNotificationAttachmentDetail() {

    String fileData = "ZmlsZURhdGE=";

    NotificationAttachmentUploadFormData notificationAttachmentUploadFormData =
        new NotificationAttachmentUploadFormData();
    notificationAttachmentUploadFormData.setFile(
        new MockMultipartFile(
          "name",
          "filename.txt",
          null,
          Base64.getDecoder().decode(fileData)));
    notificationAttachmentUploadFormData.setProviderId(456);
    notificationAttachmentUploadFormData.setSendBy(SendBy.ELECTRONIC);
    notificationAttachmentUploadFormData.setDocumentType("DOCTYPE");
    notificationAttachmentUploadFormData.setDocumentTypeDisplayValue("Document Type");
    notificationAttachmentUploadFormData.setFileExtension("txt");
    notificationAttachmentUploadFormData.setDocumentDescription("Document Description");
    notificationAttachmentUploadFormData.setStatus("status");
    notificationAttachmentUploadFormData.setNumber(1L);
    notificationAttachmentUploadFormData.setNotificationReference("789");

    NotificationAttachmentDetail result =
        notificationAttachmentMapper.toNotificationAttachmentDetail(notificationAttachmentUploadFormData);

    assertNull(result.getId());
    assertNull(result.getAuditTrail());

    assertEquals(fileData, result.getFileData());
    assertEquals("456", result.getProviderId());
    assertEquals("Document Description", result.getDescription());
    assertEquals("filename.txt", result.getFileName());
    assertEquals("789", result.getNotificationReference());
    assertEquals(1L, result.getNumber());
    assertEquals("status", result.getStatus());
  }
}
