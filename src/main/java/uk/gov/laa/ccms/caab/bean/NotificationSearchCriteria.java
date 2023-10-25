package uk.gov.laa.ccms.caab.bean;

import lombok.Data;

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
   * The day of the {@see dateFrom}.
   */
  private String notificationFromDateDay;

  /**
   * The month of the {@see dateFrom}.
   */
  private String notificationFromDateMonth;

  /**
   * The year of the {@see dateFrom}.
   */
  private String notificationFromDateYear;

  /**
   * The date from which to start the search.
   */
  private String dateFrom;

  /**
   * The day of the {@see dateTo}.
   */
  private String notificationToDateDay;

  /**
   * The month of the {@see dateTo}.
   */
  private String notificationToDateMonth;

  /**
   * The year of the {@see dateTo}.
   */
  private String notificationToDateYear;

  /**
   * The date to search up to.
   */
  private String dateTo;

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
   * reset the search criteria.
   *
   * @param criteria the criteria to reset
   */
  public static void reset(NotificationSearchCriteria criteria) {
    criteria.setSort("");
    criteria.setDateFrom("");
    criteria.setNotificationFromDateDay("");
    criteria.setNotificationFromDateMonth("");
    criteria.setNotificationFromDateYear("");
    criteria.setNotificationToDateDay("");
    criteria.setNotificationToDateMonth("");
    criteria.setNotificationToDateYear("");
    criteria.setDateTo("");
    criteria.setNotificationType("");
    criteria.setCaseReference("");
    criteria.setFeeEarnerId(null);
    criteria.setClientSurname("");
    criteria.setProviderCaseReference("");
    criteria.setAssignedToUserId("");
  }


}
