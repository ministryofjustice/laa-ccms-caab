package uk.gov.laa.ccms.caab.bean.award;

import lombok.Data;

/** Form data for creating, calculating or updating a land award. */
@Data
public class LandAwardFormData {

  private Integer id;
  private String awardType;
  private String awardCode;
  private String dateOfOrder;
  private String description;
  private String titleNumber;
  private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  private String valuationAmount = "0.00";
  private String valuationCriteria;
  private String valuationDate;
  private String disputedPercentage = "0.00";
  private String awardedPercentage = "0.00";
  private String mortgageAmountDue = "0.00";
  private String equity = "0.00";
  private String awardedBy;
  private String recovery;
  private String noRecoveryDetails;
  private String statutoryChargeExemptReason;
  private String landChargeRegistration;
  private String registrationReference;
  private String recoveryOfAwardTimeRelated;
}
