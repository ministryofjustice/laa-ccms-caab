package uk.gov.laa.ccms.caab.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.laa.ccms.caab.model.BaseNotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.util.FileUtil;
import uk.gov.laa.ccms.soa.gateway.model.BaseDocument;
import uk.gov.laa.ccms.soa.gateway.model.Document;

/**
 * Mapper class to convert Notification Attachments between various formats.
 */
@Mapper(componentModel = "spring")
public interface NotificationAttachmentMapper {

  @Mapping(target = "fileExtension", ignore = true)
  @Mapping(target = "documentType", source = "documentType.id")
  @Mapping(target = "text", source = "description")
  BaseDocument toBaseDocument(NotificationAttachmentDetail notificationAttachmentDetail);

  @Mapping(target = "documentId", ignore = true)
  @Mapping(target = "fileExtension", ignore = true)
  @Mapping(target = "statusDescription", ignore = true)
  @Mapping(target = "documentLink", ignore = true)
  @Mapping(target = "channel", source = "sendBy")
  @Mapping(target = "documentType", source = "documentType.id")
  @Mapping(target = "text", source = "description")
  Document toDocument(NotificationAttachmentDetail notificationAttachmentDetail);

  @Mapping(target = "providerId", ignore = true)
  @Mapping(target = "fileName", ignore = true)
  @Mapping(target = "number", ignore = true)
  @Mapping(target = "notificationReference", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "id", source = "document.documentId")
  @Mapping(target = "sendBy", source = "document.channel")
  @Mapping(target = "description", source = "document.text")
  @Mapping(target = "documentType.id", source = "document.documentType")
  @Mapping(target = "documentType.displayValue", source = "documentTypeDisplayValue")
  BaseNotificationAttachmentDetail toBaseNotificationAttachmentDetail(Document document,
      String documentTypeDisplayValue);

  /**
   * Set the file extension on the base document based on the filename of the notification
   * attachment.
   *
   * @param baseDocument                  The base document.
   * @param notificationAttachmentDetail  The notification attachment.
   */
  @AfterMapping
  default void setFileExtension(@MappingTarget BaseDocument baseDocument,
      NotificationAttachmentDetail notificationAttachmentDetail) {
    if (notificationAttachmentDetail.getSendBy().equals("E")) {
      baseDocument.setFileExtension(
          FileUtil.getFileExtension(notificationAttachmentDetail.getFileName()));
    }
  }

  /**
   * Set the file extension on the document based on the filename of the notification
   * attachment.
   *
   * @param document                      The document.
   * @param notificationAttachmentDetail  The notification attachment.
   */
  @AfterMapping
  default void setFileExtension(@MappingTarget Document document,
      NotificationAttachmentDetail notificationAttachmentDetail) {
    if (notificationAttachmentDetail.getSendBy().equals("E")) {
      document.setFileExtension(
          FileUtil.getFileExtension(notificationAttachmentDetail.getFileName()));
    }
  }
}
