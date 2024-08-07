package uk.gov.laa.ccms.caab.model.summary;

import java.util.Date;
import lombok.Data;

@Data
public class OpponentSubmissionSummaryDisplay {

  private String type;
  private String title;
  private String surname;
  private String firstName;
  private String middleNames;
  private Date dateOfBirth;
  private String relationshipToCase;
  private String relationshipToClient;
  private String nationalInsuranceNumber;
  private String houseNameOrNumber;
  private String addressLine1;
  private String addressLine2;
  private String city;
  private String county;
  private String country;
  private String postcode;
  private String telephoneHome;
  private String telephoneWork;
  private String telephoneMobile;
  private String faxNumber;
  private String emailAddress;
  private Boolean legalAided;
  private String certificateNumber;
  private String organisationName;
  private String organisationType;
  private Boolean currentlyTrading;
  private String contactNameRole;
  private String otherInformation;
}
