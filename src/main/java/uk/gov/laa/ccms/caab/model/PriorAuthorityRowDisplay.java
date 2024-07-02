package uk.gov.laa.ccms.caab.model;

import lombok.Builder;
import lombok.Data;

/**
 * Represents the display values for a Prior Authority.
 */
@Data
@Builder
public class PriorAuthorityRowDisplay {

  private String summary;

  private String type;

  private String amountRequested;

  private String status;

}
