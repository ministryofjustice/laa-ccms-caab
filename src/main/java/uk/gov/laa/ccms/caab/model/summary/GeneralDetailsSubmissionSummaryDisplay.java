package uk.gov.laa.ccms.caab.model.summary;

import java.util.Date;
import lombok.Data;

/**
 * Display details for general details submission summary.
 */
@Data
public class GeneralDetailsSubmissionSummaryDisplay {

  /**
   * The category of law.
   */
  private String categoryOfLaw;

  /**
   * The type of application.
   */
  private String applicationType;

  /**
   * The date of delegated functions.
   */
  private Date delegatedFunctionsDate;

  /**
   * The preferred address.
   */
  private String preferredAddress;

  /**
   * The country.
   */
  private String country;

  /**
   * The house name or number.
   */
  private String houseNameOrNumber;

  /**
   * The postcode.
   */
  private String postcode;

  /**
   * The care of (C/O) address line.
   */
  private String careOf;

  /**
   * The first line of the address.
   */
  private String addressLine1;

  /**
   * The second line of the address.
   */
  private String addressLine2;

  /**
   * The city.
   */
  private String city;

  /**
   * The county.
   */
  private String county;
}
