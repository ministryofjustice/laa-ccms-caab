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
   * The date from which to start the search.
   */
  private String dateFrom;

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
   * a switch to ascertain whether to recreate or reuse for sorting.
   */
  private boolean instantiated;

  /**
   * the sort field and direction.
   */
  private String sort;

}
