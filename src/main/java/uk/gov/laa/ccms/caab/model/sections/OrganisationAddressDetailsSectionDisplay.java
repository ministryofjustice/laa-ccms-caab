package uk.gov.laa.ccms.caab.model.sections;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents the display details for an individual's address and contact details.
 *
 * @author Jamie Briggs
 */
@Getter
@Setter
public class OrganisationAddressDetailsSectionDisplay {

  private String houseNameNumber;
  private String addressLineOne;
  private String addressLineTwo;
  private String cityTown;
  private String county;
  private String country;
  private String postcode;
  private String telephone;
  private String email;
  private String fax;
  private String otherInformation;
}
