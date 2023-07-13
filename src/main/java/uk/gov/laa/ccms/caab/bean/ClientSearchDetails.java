package uk.gov.laa.ccms.caab.bean;


import lombok.Data;

import java.io.Serializable;

@Data
public class ClientSearchDetails implements Serializable{

    String forename;
    String surname;

    String dobDay;
    String dobMonth;
    String dobYear;

    String gender;

    String uniqueIdentifierType;
    String uniqueIdentifierValue;
}
