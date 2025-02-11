package uk.gov.laa.ccms.caab.bean.notification;

import java.io.Serializable;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a notification attachment upload form.
 */
@Data
@Slf4j
public class NotificationResponseFormData implements
    Serializable {

  /**
   * The type of response.
   */
  private String action;

  /**
   * The supporting message of the response.
   */
  private String message;

}
