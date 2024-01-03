package uk.gov.laa.ccms.caab.bean;

import lombok.Data;

/**
 * Represents an address details form.
 */
@Data
public class AddressFormData {
  private String preferredAddress;

  private String country;
  private String houseNameNumber;
  private String postcode;
  private String careOf;
  private String addressLine1;
  private String addressLine2;
  private String cityTown;
  private String county;
}
