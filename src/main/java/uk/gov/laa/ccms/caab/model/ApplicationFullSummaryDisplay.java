package uk.gov.laa.ccms.caab.model;

import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Represents the Summary display values for the full application summary screen.
 */
@Data
@Builder
public class ApplicationFullSummaryDisplay {

  private String applicationStatus;

  private String correspondenceMethod;

  private String clientFullName;

  private String applicationType;

  private String categoryOfLaw;

  private boolean devolvedPowersUsed;

  private Date devolvedPowersDate;

  private String providerCaseReference;

  private String feeEarner;

  private String providerName;

  private String supervisorName;

  private String officeName;

  private String providerContactName;

  private List<ProceedingRowDisplay> proceedings;

  private String requestedCostLimitation;

  private String grantedCostLimitation;

  private List<PriorAuthorityRowDisplay> priorAuthorities;

  private List<OpponentRowDisplay> opponents;

  private String meansAssessmentStatus;

  private String meritsAssessmentStatus;

  private String documentUploadStatus;


}
