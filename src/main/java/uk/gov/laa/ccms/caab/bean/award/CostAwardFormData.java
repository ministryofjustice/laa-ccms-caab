package uk.gov.laa.ccms.caab.bean.award;

import lombok.Data;

/** Form data for creating or updating a cost award. */
@Data
public class CostAwardFormData {

  private Integer id;
  private String awardType;
  private String description;
  private String awardCode;
  private String dateOfOrder;
  private String courtAssessmentStatus;
  private String laaFundedLegalCosts;
  private String otherPreCertificateCosts;
  private String laaRate;
  private String marketRate;
  private String awardedBy;
  private String interestRate;
  private String interestStartDate;
  private String orderServedDate;
  private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  private String otherDetails;
}
