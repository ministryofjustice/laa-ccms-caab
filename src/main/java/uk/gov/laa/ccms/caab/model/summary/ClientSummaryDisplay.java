package uk.gov.laa.ccms.caab.model.summary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Represents the Summary display values for the application summary screen.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ClientSummaryDisplay extends ApplicationSummaryStatusDisplay {

  /**
   * The full display name of the client.
   */
  private String clientFullName;

  /**
   * The client reference number.
   */
  private String clientReferenceNumber;

}
