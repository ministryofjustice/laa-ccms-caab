package uk.gov.laa.ccms.caab.constants;

import lombok.Data;

/**
 * Constants for session attribute names used in the application.
 */
@Data
public class SessionConstants {

  /**
   * Session attribute used to keep track of the logged-in users details.
   */
  public static final String USER_DETAILS = "user";

  /**
   * Session attribute used to keep track of application details during the creation of a new
   * application.
   */
  public static final String APPLICATION_FORM_DATA = "applicationFormData";

  /**
   * Session attribute used to keep track of search criteria when copying a case.
   */
  public static final String CASE_SEARCH_CRITERIA = "caseSearchCriteria";

  /**
   * Session attribute used to keep track of copy case search results during the creation of a new
   * application.
   * Used when returning to copy case results screen to prepopulate table of results
   */
  public static final String CASE_SEARCH_RESULTS = "caseSearchResults";

  /**
   * Session attribute used to keep track of client search criteria during the creation of a new
   * application.
   * Used when returning to the client search screen to prepopulate fields
   */
  public static final String CLIENT_SEARCH_CRITERIA = "clientSearchCriteria";

  /**
   * Session attribute used to keep track of client search results during the creation of a new
   * application.
   * Used when returning to client results screen to prepopulate table of results
   */
  public static final String CLIENT_SEARCH_RESULTS = "clientSearchResults";

  /**
   * Session attribute used to keep track of client details through the multiple screens of the
   * client flows.
   */
  public static final String CLIENT_FLOW_FORM_DATA = "clientFlowFormData";

  /**
   * Session attribute used to keep track of client reference during the creation of a new
   * application.
   */
  public static final String CLIENT_REFERENCE = "clientReference";

  /**
   * Session attribute used to keep track of client details during the creation of a new
   * application.
   */
  public static final String CLIENT_INFORMATION = "clientInformation";

  /**
   * Session attribute used to keep track of address search results.
   * Used when returning to client details address screen to prepopulate the results.
   * Used for client and correspondence address search results.
   */
  public static final String ADDRESS_SEARCH_RESULTS = "addressSearchResults";

  /**
   * Session attribute used to keep track of submission transactions.
   */
  public static final String SUBMISSION_TRANSACTION_ID = "submissionTransactionId";

  /**
   * Session attribute used for maintaining current search criteria during a notifications search.
   */
  public static final String NOTIFICATION_SEARCH_CRITERIA = "notificationSearchCriteria";

  /**
   * Session attribute used for holding the Notifications retrieved from SOA.
   */
  public static final String NOTIFICATIONS_SEARCH_RESULTS = "notificationsSearchResults";

  /**
   * Session attribute used to keep track of the application, used for application summary page.
   */
  public static final String APPLICATION_ID = "applicationId";

  /**
   * Session attribute used to keep track of the active case details, used for application summary
   * page and its sections to render the case details within the header.
   */
  public static final String ACTIVE_CASE = "activeCase";

  /**
   * Session attribute used to keep track of the submission poll counts.
   */
  public static final String SUBMISSION_POLL_COUNT = "submissionPollCount";

  public static final String APPLICATION_CLIENT_NAMES = "applicationClientNames";

}
