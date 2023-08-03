package uk.gov.laa.ccms.caab.bean;

import lombok.Data;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

import java.io.Serializable;
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
     * The display value of the office related to this application.
     */
    private String officeDisplayValue;

    /**
     * The ID of the category of law related to this application.
     */
    private String categoryOfLawId;

    /**
     * The display value of the category of law related to this application.
     */
    private String categoryOfLawDisplayValue;

    /**
     * Flag indicating whether exceptional funding has been requested for this application.
     */
    private boolean exceptionalFunding;

    /**
     * The ID of the application type related to this application.
     */
    private String applicationTypeId;

    /**
     * The ID of the application type display value related to this application.
     */
    private String applicationTypeDisplayValue;

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

    public Date getDelegatedFunctionDate() throws ParseException {
        String dateString = this.delegatedFunctionUsedDay + "-" + this.delegatedFunctionUsedMonth + "-" + this.delegatedFunctionUsedYear;
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        return sdf.parse(dateString);
    }

    public void setApplicationTypeAndDisplayValues() {
        boolean isDelegatedFunctions = this.isDelegatedFunctions();

        if (APP_TYPE_SUBSTANTIVE.equals(this.applicationTypeCategory)) {
            this.applicationTypeId = isDelegatedFunctions ? APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS : APP_TYPE_SUBSTANTIVE;
            this.applicationTypeDisplayValue = isDelegatedFunctions ? APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS_DISPLAY : APP_TYPE_SUBSTANTIVE_DISPLAY;
        } else if (APP_TYPE_EMERGENCY.equals(this.applicationTypeCategory)){
            this.applicationTypeId = isDelegatedFunctions ? APP_TYPE_EMERGENCY_DEVOLVED_POWERS : APP_TYPE_EMERGENCY;
            this.applicationTypeDisplayValue = isDelegatedFunctions ? APP_TYPE_EMERGENCY_DEVOLVED_POWERS_DISPLAY : APP_TYPE_EMERGENCY_DISPLAY;
        } else {
            this.applicationTypeId = APP_TYPE_EXCEPTIONAL_CASE_FUNDING;
            this.applicationTypeDisplayValue = APP_TYPE_EXCEPTIONAL_CASE_FUNDING_DISPLAY;
        }
    }
}

