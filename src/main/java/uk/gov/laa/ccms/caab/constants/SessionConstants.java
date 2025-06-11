package uk.gov.laa.ccms.caab.constants;

/**
 * Constants for session attribute names used in the application.
 */
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
   * Session attribute used to keep track of the current case.
   */
  public static final String CASE = "case";

  /**
   * Session attribute used to keep track of the Case Reference Number, used for
   * case summary page.
   */
  public static final String CASE_REFERENCE_NUMBER = "caseReferenceNumber";

  /**
   * Session attribute used to keep track of the application, used for application summary page.
   */
  public static final String APPLICATION_ID = "applicationId";

  /**
   * Session attribute used to keep track of the application.
   */
  public static final String APPLICATION = "application";

  /**
   * Session attribute used to keep track of the application.
   */
  public static final String AMENDMENT = "amendment";

  /**
   * Session attribute used to keep track of the active case details, used for application summary
   * page and its sections to render the case details within the header.
   */
  public static final String ACTIVE_CASE = "activeCase";

  /**
   * Session attribute used to keep track of the submission poll counts.
   */
  public static final String SUBMISSION_POLL_COUNT = "submissionPollCount";

  /**
   * Session attribute used to keep track of the client names associated with the application.
   */
  public static final String APPLICATION_CLIENT_NAMES = "applicationClientNames";

  /**
   * Session attribute used to keep track of the opponents associated with the application.
   */
  public static final String APPLICATION_OPPONENTS = "opponents";

  /**
   * Session attribute used to keep track of criteria when searching for an organisation.
   */
  public static final String ORGANISATION_SEARCH_CRITERIA = "organisationSearchCriteria";

  /**
   * Session attribute used to keep track of organisation search results during the creation
   * of a new application opponent.
   * Used when returning to organisation search results screen to prepopulate table of results
   */
  public static final String ORGANISATION_SEARCH_RESULTS = "organisationSearchResults";

  /**
   * Session attribute used to keep track of the current opponent being processed or viewed.
   */
  public static final String CURRENT_OPPONENT = "opponent";

  /**
   * Session attribute used to keep track of the costs associated with the application.
   */
  public static final String APPLICATION_COSTS = "costs";

  /**
   * Session attribute used to keep track of the proceedings associated with the application.
   */
  public static final String APPLICATION_PROCEEDINGS = "proceedings";

  /**
   * Session attribute used to keep track of the current proceeding being processed or viewed.
   */
  public static final String CURRENT_PROCEEDING = "proceeding";

  /**
   * Session attribute used to keep track of the proceeding flow data during the creation or
   * modification of a proceeding.
   */
  public static final String PROCEEDING_FLOW_FORM_DATA = "proceedingFlow";

  /**
   * Session attribute used to keep track of the old proceeding flow data before
   * modifications were made.
   */
  public static final String PROCEEDING_FLOW_FORM_DATA_OLD = "oldProceedingFlow";

  /**
   * Session attribute used to keep track of the scope limitations associated with the proceeding.
   */
  public static final String PROCEEDING_SCOPE_LIMITATIONS = "scopeLimitations";

  /**
   * Session attribute used to keep track of the scope limitation flow data during the creation or
   * modification of a scope limitation.
   */
  public static final String SCOPE_LIMITATION_FLOW_FORM_DATA = "scopeLimitationFlow";

  /**
   * Session attribute used to keep track of the current scope limitation and its modifications.
   */
  public static final String CURRENT_SCOPE_LIMITATION = "scopeLimitation";

  /**
   * Session attribute used to keep track of the prior authorities associated with the application.
   */
  public static final String APPLICATION_PRIOR_AUTHORITIES = "priorAuthorities";

  /**
   * Session attribute used to keep track of the prior authorities flow data.
   */
  public static final String PRIOR_AUTHORITY_FLOW_FORM_DATA = "priorAuthorityFlow";

  /**
   * Session attribute used to hold the required evidence for an application.
   */
  public static final String EVIDENCE_REQUIRED = "evidenceRequired";

  /**
   * Session attribute used to hold the form data for uploading an evidence document.
   */
  public static final String EVIDENCE_UPLOAD_FORM_DATA = "evidenceUploadForm";

  /**
   * Session attribute used to hold the form data for uploading an evidence document.
   */
  public static final String SUBMISSION_SUMMARY = "submissionSummary";

  /**
   * Session attribute used to hold sections data for validation, when clicking complete
   * application button.
   */
  public static final String SECTIONS_DATA = "sectionsData";

  /**
   * Session attribute used to keep track of the provider request flow data.
   */
  public static final String PROVIDER_REQUEST_FLOW_FORM_DATA = "providerRequestFlow";

}
