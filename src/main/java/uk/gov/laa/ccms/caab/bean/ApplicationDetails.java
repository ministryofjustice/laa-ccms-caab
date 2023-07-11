package uk.gov.laa.ccms.caab.bean;

import lombok.Data;

import java.io.Serializable;

@Data
public class ApplicationDetails implements Serializable {

    /**
     * The id of the Office related to this Application
     */
    private Integer officeId;

    /**
     * The id of the Category of Law related to this Application
     */
    private String categoryOfLawId;

    /**
     * The id of the Category of Law related to this Application
     */
    private String applicationTypeId;

    private String delegatedFunctionsOption;
    private String delegatedFunctionUsedDay;
    private String delegatedFunctionUsedMonth;
    private String delegatedFunctionUsedYear;


}
