package uk.gov.laa.ccms.caab.bean.opponent;

import jakarta.validation.constraints.Size;
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
  @Size(max = 360)
  private String name;

  /**
   * The type of the organisation.
   */
  private String type;

  /**
   * The organisation city.
   */
  @Size(max = 35)
  private String city;

  /**
   * The organisation postcode.
   */
  @Size(max = 15)
  private String postcode;

}

