package uk.gov.laa.ccms.caab.model.sections;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndividualGeneralDetailsSectionDisplay {

  private String title;
  private String firstName;
  private String middleNames;
  private String surname;
  private LocalDate dateOfBirth;
  private String relationshipToClient;
  private String relationshipToCase;
  private Boolean publicFundingApplied;
  private String nationalInsuranceNumber;

}
