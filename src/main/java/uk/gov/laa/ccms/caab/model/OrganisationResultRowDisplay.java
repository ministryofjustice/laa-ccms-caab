package uk.gov.laa.ccms.caab.model;

import lombok.Data;

/**
 * Represents the display data for a organisation result row.
 */
@Data
public class OrganisationResultRowDisplay {

  /**
   * The partyId for the organisation.
   */
  private String partyId;

  /**
   * The name of the organisation.
   */
  private String name;

  /**
   * The type of the organisation.
   */
  private String type;

  /**
   * The display value for the type of the organisation.
   */
  private String typeDisplayValue;

  /**
   * The organisation city.
   */
  private String city;

  /**
   * The organisation postcode.
   */
  private String postcode;

}
