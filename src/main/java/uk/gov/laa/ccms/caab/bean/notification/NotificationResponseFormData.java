package uk.gov.laa.ccms.caab.bean.notification;

import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.NOTIFICATION_MESSAGE_CHARACTER_SIZE;

import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** Represents a notification attachment upload form. */
@Data
@Slf4j
public class NotificationResponseFormData implements Serializable {

  /** The type of response. */
  private String action;

  /** The supporting message of the response. */
  @Size(max = NOTIFICATION_MESSAGE_CHARACTER_SIZE)
  private String message;
}
