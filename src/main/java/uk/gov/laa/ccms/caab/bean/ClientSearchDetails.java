package uk.gov.laa.ccms.caab.bean;


import lombok.Data;

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
    String uniqueIdentifierType;

    /**
     * The value of the unique identifier for the client.
     */
    String uniqueIdentifierValue;
}
