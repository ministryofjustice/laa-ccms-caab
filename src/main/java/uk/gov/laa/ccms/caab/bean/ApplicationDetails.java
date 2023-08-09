package uk.gov.laa.ccms.caab.bean;

import lombok.Data;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

import java.io.Serializable;

/**
 * Represents the details of an application.
 */
@Data
public class ApplicationDetails implements Serializable {

    /**
     * The caseReferenceNumber selected for a Copy Case.
     */
    private String copyCaseReferenceNumber;

    /**
     * The ID of the office related to this application.
     */
    private Integer officeId;

    /**
     * The ID of the category of law related to this application.
     */
    private String categoryOfLawId;

    /**
     * Flag indicating whether exceptional funding has been requested for this application.
     */
    private boolean exceptionalFunding;

    /**
     * The ID of the application type related to this application.
     */
    private String applicationTypeId;

    /**
     * The category of the application type.
     */
    private String applicationTypeCategory;

    /**
     * The option for delegated functions.
     */
    private boolean delegatedFunctions = false;

    /**
     * The day when delegated function was used.
     */
    private String delegatedFunctionUsedDay;

    /**
     * The month when delegated function was used.
     */
    private String delegatedFunctionUsedMonth;

    /**
     * The year when delegated function was used.
     */
    private String delegatedFunctionUsedYear;

    /**
     * The client used within the application.
     */
    private ClientDetail client;
}

