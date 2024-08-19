package uk.gov.laa.ccms.caab.bean.notification;

import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import uk.gov.laa.ccms.caab.bean.file.FileUploadFormData;
import uk.gov.laa.ccms.caab.constants.SendBy;

/**
 * Represents a notification attachment upload form.
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class NotificationAttachmentUploadFormData extends FileUploadFormData implements
    Serializable {

  /**
   * The ID of the document.
   */
  private Integer documentId;

  /**
   * The related provider id.
   */
  private Integer providerId;

  /**
   * The method of communication, e.g. by Post or Electronic.
   */
  private SendBy sendBy;

  /**
   * The ID of the corresponding notification.
   */
  private String notificationReference;

  /**
   * The submission status.
   */
  private String status;

  /**
   * The number / sequence of this attachment.
   */
  private Long number;


}
