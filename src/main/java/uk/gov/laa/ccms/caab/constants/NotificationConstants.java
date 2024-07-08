package uk.gov.laa.ccms.caab.constants;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Constants for Notifications.
 */
@Data
@Component
public class NotificationConstants {

  /**
   * Name of the model attribute used to hold the name of the field to sort by.
   */
  public static final String SORT_FIELD = "sortField";

  /**
   * Name of the model attribute used to hold the sorting direction.
   */
  public static final String SORT_DIRECTION = "sortDirection";

  /**
   * the maximum amount of poll request for a notification attachment request.
   */
  @Value("${notification.attachment.max-poll-count:6}")
  private Integer maxPollCount;

}
