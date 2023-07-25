package uk.gov.laa.ccms.caab.bean;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;

/**
 * Represents the details used for client search.
 */
@Data
public class ClientSearchCriteria implements Serializable {

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
        try {
            int year = Integer.parseInt(dobYear);
            int month = Integer.parseInt(dobMonth);
            int day = Integer.parseInt(dobDay);

            return String.format("%d-%02d-%02d", year, month, day);
        } catch (NumberFormatException e) {
            // Handle the exception if any of the dobYear, dobMonth, or dobDay is not a valid integer
            return null;
        }
    }


    public String getUniqueIdentifier(Integer matchingType){
        if(this.uniqueIdentifierType != null && this.uniqueIdentifierType == matchingType){
            return uniqueIdentifierValue;
        } else{
            return null;
        }
    }
}
