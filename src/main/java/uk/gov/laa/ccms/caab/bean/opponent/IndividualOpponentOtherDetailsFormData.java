package uk.gov.laa.ccms.caab.bean.opponent;

import lombok.Data;

/** Optional employment and means-assessment details from legacy CA04AB. */
@Data
public class IndividualOpponentOtherDetailsFormData {
  private String employmentStatus;
  private String employerName;
  private String employerAddress;
  private Boolean courtOrderedMeansAssessment;
  private String assessedIncome;
  private String assessedIncomeFrequency;
  private String assessedAssets;
  private String assessmentDate;
  private String otherInformation;
}
