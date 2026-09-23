package uk.gov.laa.ccms.caab.bean.award;

import lombok.Data;

/** Form data for creating or updating an other asset award. */
@Data
public class OtherAssetAwardFormData {

  private Integer id;
  private String awardType;
  private String description;
  private String awardCode;
  private String dateOfOrder;
  private String awardedBy;
  private String valuationAmount;
  private String valuationDate;
  private String awardedPercentage;
  private String recoveredAmount;
  private String recoveredPercentage;
  private String disputedAmount;
  private String awardedAmount;
  private String disputedPercentage;
  private String recovery;
  private String noRecoveryDetails;
  private String statutoryChargeExemptReason;
  private Boolean recoveryOfAwardTimeRelated;
}
