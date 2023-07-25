package uk.gov.laa.ccms.caab.bean;

import java.io.Serializable;
import lombok.Data;

/**
 * Represents the criteria to search for an application.
 */
@Data
public class ApplicationSearchCriteria implements Serializable {

    /**
     * The LAA Application/Case Reference
     */
    private String caseReference;
    /**
     * The client surname
     */
    private String clientSurname;

    /**
     * The provider case reference.
     */
    private String providerReference;

    /**
     * The id of the related Fee Earner.
     */
    private Integer feeEarnerId;

    /**
     * The id of the related Office.
     */
    private Integer officeId;

    /**
     * The actual status value for the Application
     */
    private String actualStatus;
}

