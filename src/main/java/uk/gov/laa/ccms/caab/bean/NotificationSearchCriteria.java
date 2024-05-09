package uk.gov.laa.ccms.caab.bean;

import lombok.Data;
import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

/**
 * Represents the search criteria to search for Notifications.
 */
@Data
public class NotificationSearchCriteria {

  /**
   * The LAA Application/Case Reference.
   */
  private String caseReference;
  /**
   * The client surname.
   */
  private String clientSurname;

  /**
   * The provider case reference.
   */
  private String providerCaseReference;

  /**
   * The id of the related Fee Earner.
   */
  private Integer feeEarnerId;

  /**
   * The id of the user to whom the notification is assigned.
   */
  private String assignedToUserId;

  /**
   * Switch for whether to include closed notifications.
   */
  private boolean includeClosed;

  /**
   * The type of notification to search for.
   */
  private String notificationType;

  /**
   * The day of the {@see notificationFromDate}.
   */
  private String notificationFromDateDay;

  /**
   * The month of the {@see notificationFromDate}.
   */
  private String notificationFromDateMonth;

  /**
   * The year of the {@see notificationFromDate}.
   */
  private String notificationFromDateYear;

  /**
   * The date from which to start the search.
   */
  private String notificationFromDate;

  /**
   * The day of the {@see notificationToDate}.
   */
  private String notificationToDateDay;

  /**
   * The month of the {@see notificationToDate}.
   */
  private String notificationToDateMonth;

  /**
   * The year of the {@see notificationToDate}.
   */
  private String notificationToDateYear;

  /**
   * The date to search up to.
   */
  private String notificationToDate;

  /**
   * the logged-in user.
   */
  private String loginId;

  /**
   * The logged-in user's role.
   */
  private String userType;

  /**
   * the sort field and direction.
   */
  private String sort;

  /**
   * Retrieves the formatted date to search from.
   *
   * @return The formatted from date (yyyy-MM-dd).
   */
  public String getDateFrom() {
    return getDate(notificationFromDateYear, notificationFromDateMonth, notificationFromDateDay);
  }

  /**
   * Retrieves the formatted date to search up to.
   *
   * @return The formatted to date (yyyy-MM-dd).
   */
  public String getDateTo() {
    return getDate(notificationToDateYear, notificationToDateMonth, notificationToDateDay);
  }

  /**
   * Returns an ISO formatted date based on the day, month, and year values.
   *
   * @return The formatted to date (yyyy-MM-dd), or null if the date components are not valid
   *         integers.
   */
  private String getDate(String yearInput, String monthInput, String dayInput) {
    if (!StringUtils.hasText(yearInput)
        && !StringUtils.hasText(monthInput)
        && !StringUtils.hasText(dayInput)) {
      return null;
    }
    try {
      int year = Integer.parseInt(yearInput);
      int month = Integer.parseInt(monthInput);
      int day = Integer.parseInt(dayInput);

      return String.format("%d-%02d-%02d", year, month, day);
    } catch (NumberFormatException e) {
      // Handle the exception if any of the year, month or day is not a valid integer
      throw new CaabApplicationException("Unable to format date", e);
    }
  }

  /**
   * reset the search criteria.
   *
   * @param criteria the criteria to reset
   */
  public static void reset(NotificationSearchCriteria criteria) {
    criteria.setSort("");
    criteria.setNotificationFromDateDay("");
    criteria.setNotificationFromDateMonth("");
    criteria.setNotificationFromDateYear("");
    criteria.setNotificationFromDate("");
    criteria.setNotificationToDateDay("");
    criteria.setNotificationToDateMonth("");
    criteria.setNotificationToDateYear("");
    criteria.setNotificationToDate("");
    criteria.setNotificationType("");
    criteria.setCaseReference("");
    criteria.setFeeEarnerId(null);
    criteria.setClientSurname("");
    criteria.setProviderCaseReference("");
    criteria.setAssignedToUserId("");
  }


}
