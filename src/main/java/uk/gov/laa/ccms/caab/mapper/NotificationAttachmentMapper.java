package uk.gov.laa.ccms.caab.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
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

  @AfterMapping
  default void setFileExtension(@MappingTarget BaseDocument baseDocument,
      NotificationAttachmentDetail notificationAttachmentDetail) {
    baseDocument.setFileExtension(
        FileUtil.getFileExtension(notificationAttachmentDetail.getFileName()));
  }

  @Mapping(target = "documentId", ignore = true)
  @Mapping(target = "fileExtension", ignore = true)
  @Mapping(target = "statusDescription", ignore = true)
  @Mapping(target = "documentLink", ignore = true)
  @Mapping(target = "channel", ignore = true)
  @Mapping(target = "documentType", source = "documentType.id")
  @Mapping(target = "text", source = "description")
  Document toDocument(NotificationAttachmentDetail notificationAttachmentDetail);

  @AfterMapping
  default void setFileExtension(@MappingTarget Document document,
      NotificationAttachmentDetail notificationAttachmentDetail) {
    document.setFileExtension(
        FileUtil.getFileExtension(notificationAttachmentDetail.getFileName()));
  }

}
