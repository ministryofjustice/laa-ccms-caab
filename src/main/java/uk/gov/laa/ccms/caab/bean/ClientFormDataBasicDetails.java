package uk.gov.laa.ccms.caab.bean;

import lombok.Data;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

@Data
public class ClientFormDataBasicDetails {

  private String title;
  private String surname;
  private String firstName;
  private String middleNames;
  private String surnameAtBirth;

  private String dobDay;
  private String dobMonth;
  private String dobYear;
  private String countryOfOrigin;
  private String nationalInsuranceNumber;
  private String homeOfficeNumber;
  private String gender;
  private String maritalStatus;
  private Boolean vulnerableClient = false;
  private Boolean highProfileClient = false;
  private Boolean vexatiousLitigant = false;
  private Boolean mentalIncapacity = false;

  //Required for template rendering
  private String clientFlowFormAction;
}
