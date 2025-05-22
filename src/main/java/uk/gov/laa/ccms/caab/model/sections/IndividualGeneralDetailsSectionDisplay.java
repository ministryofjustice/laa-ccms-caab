package uk.gov.laa.ccms.caab.model.sections;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndividualGeneralDetailsSectionDisplay {

  private String title;
  private String firstName;
  private String middleNames;
  private String surname;
  private String dateOfBirth;
  private String relationshipToClient;
  private String relationshipToCase;
  private Boolean previouslyAppliedForPublicFunding;
  private String nationalInsuranceNumber;

}
