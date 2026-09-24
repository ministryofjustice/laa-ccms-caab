package uk.gov.laa.ccms.caab.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;

/**
 * Utility class for preparing and handling notification search criteria before passing information
 * to EBS API.
 *
 * @author Jamie Briggs
 * @see NotificationSearchCriteria
 */
@Slf4j
public final class NotificationSearchUtil {

  public static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * Prepares notification search criteria. When neither date is supplied, both are omitted so the
   * data API applies its rolling 36-month default from the database date (without an upper bound).
   * When one date is supplied, the other is calculated using the existing 3-year window. Supplied
   * dates are formatted as 'yyyy-MM-dd' for the EBS API.
   *
   * <p>Blank filters are also nulled, so that their query parameters are omitted rather than sent
   * empty. An empty parameter is a filter on the empty string, which matches no notifications
   * instead of leaving the filter off.
   *
   * @param criteria the notification search criteria object containing search parameters, including
   *     date ranges to be adjusted if necessary
   * @return the updated NotificationSearchCriteria object with adjusted date fields
   */
  public static NotificationSearchCriteria prepareNotificationSearchCriteria(
      NotificationSearchCriteria criteria) {
    NotificationSearchCriteria copyCriteria = new NotificationSearchCriteria(criteria);

    nullIfBlank(copyCriteria::getAssignedToUserId, copyCriteria::setAssignedToUserId);
    nullIfBlank(copyCriteria::getNotificationType, copyCriteria::setNotificationType);
    nullIfBlank(copyCriteria::getCaseReference, copyCriteria::setCaseReference);
    nullIfBlank(copyCriteria::getProviderCaseReference, copyCriteria::setProviderCaseReference);
    nullIfBlank(copyCriteria::getClientSurname, copyCriteria::setClientSurname);

    boolean fromNotSet =
        copyCriteria.getNotificationFromDate() == null
            || copyCriteria.getNotificationFromDate().isBlank();
    boolean toNotSet =
        copyCriteria.getNotificationToDate() == null
            || copyCriteria.getNotificationToDate().isBlank();

    if (fromNotSet && toNotSet) {
      copyCriteria.setNotificationFromDate(null);
      copyCriteria.setNotificationToDate(null);
      return copyCriteria;
    }

    try {
      if (fromNotSet) {
        // If TO set but FROM not set => FROM = TO - 3 Years
        LocalDate notificationToDate =
            DateUtils.convertToLocalDate(criteria.getNotificationToDate());
        copyCriteria.setNotificationFromDate(notificationToDate.minusYears(3).format(ISO));
        copyCriteria.setNotificationToDate(notificationToDate.format(ISO));
      } else if (toNotSet) {
        // If FROM set but TO not set => TO = FROM + 3 Years
        LocalDate notificationFromDate =
            DateUtils.convertToLocalDate(criteria.getNotificationFromDate());
        LocalDate toDate = notificationFromDate.plusYears(3);
        // If to date is after today, set to today
        if (toDate.isAfter(LocalDate.now())) {
          toDate = LocalDate.now();
        }
        copyCriteria.setNotificationFromDate(notificationFromDate.format(ISO));
        copyCriteria.setNotificationToDate(toDate.format(ISO));
      } else {
        // Convert date formats
        LocalDate notificationToDate =
            DateUtils.convertToLocalDate(criteria.getNotificationToDate());
        LocalDate notificationFromDate =
            DateUtils.convertToLocalDate(criteria.getNotificationFromDate());
        copyCriteria.setNotificationToDate(notificationToDate.format(ISO));
        copyCriteria.setNotificationFromDate(notificationFromDate.format(ISO));
      }
    } catch (Exception e) {
      // Don't rethrow. Rather if reaching this point, don't modify copyCriteria. Realistically,
      //  any date error would have been caught and throw prior to this method being called.
      log.error("Could not read date inputs: {}", e.getMessage());
    }

    return copyCriteria;
  }

  /** Clear a filter which holds only whitespace, so that its query parameter is omitted. */
  private static void nullIfBlank(Supplier<String> getter, Consumer<String> setter) {
    if (!StringUtils.hasText(getter.get())) {
      setter.accept(null);
    }
  }

  private NotificationSearchUtil() {}
}
