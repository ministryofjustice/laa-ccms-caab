package uk.gov.laa.ccms.caab.constants;

import lombok.Data;

@Data
public class SessionConstants {

    /**
     * Session attribute used to keep track of the logged-in users details
     */
    public static final String USER_DETAILS = "user";

    /**
     * Session attribute used to keep track of application details during the creation of a new application
     */
    public static final String APPLICATION_DETAILS = "applicationDetails";

    /**
     * Session attribute used to keep track of client search criteria during the creation of a new application
     * Used when returning to the client search screen to prepopulate fields
     */
    public static final String CLIENT_SEARCH_CRITERIA = "clientSearchCriteria";

    /**
     * Session attribute used to keep track of client search results during the creation of a new application
     * Used when returning to client results screen to prepopulate table of results
     */
    public static final String CLIENT_SEARCH_RESULTS = "clientSearchResults";


}
