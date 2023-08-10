package uk.gov.laa.ccms.caab.bean;

import lombok.Data;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.*;

/**
 * Represents the details of an application.
 */
@Data
public class ApplicationDetails {

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

}

