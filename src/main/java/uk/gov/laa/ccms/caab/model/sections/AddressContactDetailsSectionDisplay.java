package uk.gov.laa.ccms.caab.model.sections;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressContactDetailsSectionDisplay {

  private String houseNameNumber;
  private String addressLineOne;
  private String addressLineTwo;
  private String cityTown;
  private String county;
  private String country;
  private String postcode;
  private String telephoneHome;
  private String telephoneWork;
  private String telephoneMobile;
  private String email;
  private String fax;
}
