package uk.gov.laa.ccms.caab.bean;

import lombok.Data;

@Data
public class ApplicationDetails {

    /**
     * The id of the Office related to this Application
     */
    private Integer officeId;

    /**
     * The id of the Category of Law related to this Application
     */
    private Integer categoryOfLawId;

    /**
     * The id of the Category of Law related to this Application
     */
    private Integer applicationTypeId;


}
