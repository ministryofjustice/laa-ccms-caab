package uk.gov.laa.ccms.caab.bean;


import lombok.Data;
import uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants;

import java.io.Serializable;

/**
 * Represents the details used for client search.
 */
@Data
public class ClientSearchDetails implements Serializable {

    /**
     * The forename of the client.
     */
    String forename;

    /**
     * The surname of the client.
     */
    String surname;

    /**
     * The day of birth of the client.
     */
    String dobDay;

    /**
     * The month of birth of the client.
     */
    String dobMonth;

    /**
     * The year of birth of the client.
     */
    String dobYear;

    /**
     * The gender of the client.
     */
    String gender;

    /**
     * The type of unique identifier for the client.
     */
    Integer uniqueIdentifierType;

    /**
     * The value of the unique identifier for the client.
     */
    String uniqueIdentifierValue;

    public String getDateOfBirth(){
        if (dobYear == null || dobMonth == null || dobDay == null) {
            return null;
        }
        return dobYear + "-" + dobMonth + "-" + dobDay;
    }


    public String getUniqueIdentifier(Integer matchingType){
        if(this.uniqueIdentifierType != null && this.uniqueIdentifierType == matchingType){
            return uniqueIdentifierValue;
        } else{
            return null;
        }
    }
}
