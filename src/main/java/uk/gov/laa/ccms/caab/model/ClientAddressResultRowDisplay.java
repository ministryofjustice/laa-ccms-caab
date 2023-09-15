package uk.gov.laa.ccms.caab.model;

import lombok.Data;

/**
 * Represents the display details for a single client address result row.
 */
@Data
public class ClientAddressResultRowDisplay {

  private String fullAddress;

  private String uprn;

  private String country;

  private String houseNameNumber;

  private String postcode;

  private String addressLine1;

  private String addressLine2;

  private String cityTown;

  private String county;
}
