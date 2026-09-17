package uk.gov.laa.ccms.caab.bean.award;

import lombok.Data;

/** Form data for creating or updating a financial award. */
@Data
public class FinancialAwardFormData {

  private Integer id;
  private String awardType;
  private String description;
  private String awardCode;
  private String dateOfOrder;
  private String awardAmount;
  private String interimAward;
  private String awardedBy;
  private String awardJustifications;
  private String orderServedDate;
  private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  private String statutoryChargeExemptReason;
  private String otherDetails;
}
