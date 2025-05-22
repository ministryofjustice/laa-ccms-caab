package uk.gov.laa.ccms.caab.model.sections;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndividualEmploymentDetailsSectionDisplay {

  private String employersName;
  private String employmentStatus;
  private String employersAddress;
  private String certificateNumber;
  private String assessedIncomeFrequency;
  private LocalDate assessmentDate;
  private Boolean hadCourtOrderedMeansAssessment;
  private Boolean partyIsLegalAided;
  private BigDecimal assessedIncome;
  private BigDecimal assessedAssets;
  private String otherInformation;

}
