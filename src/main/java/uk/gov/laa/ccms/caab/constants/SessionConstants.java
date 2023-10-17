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
  public static final String APPLICATION_DETAILS = "applicationDetails";

  /**
   * Session attribute used to keep track of search criteria when copying a case.
   */
  public static final String COPY_CASE_SEARCH_CRITERIA = "copyCaseSearchCriteria";

  /**
   * Session attribute used to keep track of copy case search results during the creation of a new
   * application.
   * Used when returning to copy case results screen to prepopulate table of results
   */
  public static final String COPY_CASE_SEARCH_RESULTS = "copyCaseSearchResults";

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
   * Session attribute used to keep track of client details during the creation of a new
   * application.
   */
  public static final String CLIENT_DETAILS = "clientDetails";

  /**
   * Session attribute used to keep track of client details during the creation of a new
   * application.
   */
  public static final String CLIENT_INFORMATION = "clientInformation";

  /**
   * Session attribute used to keep track of client address search results during the creation of a
   * new application.
   * Used when returning to client details address screen to prepopulate the results
   */
  public static final String CLIENT_ADDRESS_SEARCH_RESULTS = "clientAddressSearchResults";

  /**
   * Session attribute used to keep track of submission transactions.
   */
  public static final String SUBMISSION_TRANSACTION_ID = "submissionTransactionId";

  /**
   * Session attribute used to keep track of the application, used for application summary page.
   */
  public static final String APPLICATION_ID = "applicationId";


}
