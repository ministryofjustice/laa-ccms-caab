package uk.gov.laa.ccms.caab.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;

/**
 * Utility class for preparing and handling notification search criteria before passing information
 * to EBS API.
 *
 * @author Jamie Briggs
 * @see NotificationSearchCriteria
 */
@Slf4j
public class NotificationSearchUtil {

  public static final DateTimeFormatter MOJ_DATE_PICKER = DateTimeFormatter.ofPattern("d/MM/yyyy");
  public static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");


  /**
   * Prepares and adjusts the notification search criteria by setting default date ranges if the
   * notificationFromDate or notificationToDate fields are empty or null. If neither date is set, it
   * defaults to a range of 3 years from the current date. If only one date is set, it calculates
   * the other date based on a 3-year window. Also ensures dates are in the format 'yyyy-MM-dd'
   * ready to be passed to EBS API.
   *
   * @param criteria the notification search criteria object containing search parameters, including
   *                 date ranges to be adjusted if necessary
   * @return the updated NotificationSearchCriteria object with adjusted date fields
   */
  public static NotificationSearchCriteria
  prepareNotificationSearchCriteria(NotificationSearchCriteria criteria) {
    NotificationSearchCriteria copyCriteria = new NotificationSearchCriteria(criteria);

    boolean fromNotSet = copyCriteria.getNotificationFromDate() == null
        || copyCriteria.getNotificationFromDate().isBlank();
    boolean toNotSet = copyCriteria.getNotificationToDate() == null
        || copyCriteria.getNotificationToDate().isBlank();

    try {
      // If neither date set
      if (fromNotSet && toNotSet) {
        LocalDate today = LocalDate.now();
        copyCriteria.setNotificationFromDate(today.minusYears(3).format(ISO));
        copyCriteria.setNotificationToDate(today.format(ISO));
      } else if (fromNotSet) {
        // If TO set but FROM not set => FROM = TO - 3 Years
        LocalDate notificationToDate = LocalDate.parse(criteria.getNotificationToDate(),
            MOJ_DATE_PICKER);
        copyCriteria.setNotificationFromDate(notificationToDate.minusYears(3).format(ISO));
        copyCriteria.setNotificationToDate(notificationToDate.format(ISO));
      } else if (toNotSet) {
        // If FROM set but TO not set => TO = FROM + 3 Years
        LocalDate notificationFromDate = LocalDate.parse(criteria.getNotificationFromDate(),
            MOJ_DATE_PICKER);
        LocalDate toDate = notificationFromDate.plusYears(3);
        // If to date is after today, set to today
        if (toDate.isAfter(LocalDate.now())) {
          toDate = LocalDate.now();
        }
        copyCriteria.setNotificationFromDate(notificationFromDate.format(ISO));
        copyCriteria.setNotificationToDate(toDate.format(ISO));
      } else {
        // Convert date formats
        LocalDate notificationToDate = LocalDate.parse(criteria.getNotificationToDate(),
            MOJ_DATE_PICKER);
        LocalDate notificationFromDate = LocalDate.parse(criteria.getNotificationFromDate(),
            MOJ_DATE_PICKER);
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

}
