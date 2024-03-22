package uk.gov.laa.ccms.caab.bean.opponent;

import java.io.Serializable;
import lombok.Data;

/**
 * Represents the criteria to search for an Organisation opponent.
 */
@Data
public class OrganisationSearchCriteria implements Serializable {

  /**
   * The name of the organisation.
   */
  private String name;

  /**
   * The type of the organisation.
   */
  private String type;

  /**
   * The organisation city.
   */
  private String city;

  /**
   * The organisation postcode.
   */
  private String postcode;

}

