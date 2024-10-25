package uk.gov.laa.ccms.caab.bean.validators.notification;

import java.util.List;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.notification.NotificationAttachmentUploadFormData;
import uk.gov.laa.ccms.caab.bean.validators.file.FileUploadValidator;
import uk.gov.laa.ccms.caab.constants.SendBy;

/**
 * Validate the evidence document details provided by evidence upload flow.
 */
@Component
@Getter
public class NotificationAttachmentUploadValidator extends FileUploadValidator {

  protected NotificationAttachmentUploadValidator(
      @Value("${laa.ccms.caab.upload.valid-extensions}") List<String> validExtensions,
      @Value("${spring.servlet.multipart.max-file-size}") String maxFileSize) {
    super(validExtensions, maxFileSize);
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return NotificationAttachmentUploadFormData.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {

    final NotificationAttachmentUploadFormData notificationAttachmentUploadFormData =
        (NotificationAttachmentUploadFormData) target;

    // Postal upload has no file.
    if (notificationAttachmentUploadFormData.getSendBy().equals(SendBy.POSTAL)) {
      validateDocumentType(notificationAttachmentUploadFormData, errors);
      validateDocumentDescription(notificationAttachmentUploadFormData, errors);
    } else {
      validateFile(notificationAttachmentUploadFormData, errors);
      validateDocumentType(notificationAttachmentUploadFormData, errors);
      validateDocumentDescription(notificationAttachmentUploadFormData, errors);
    }

  }
}
