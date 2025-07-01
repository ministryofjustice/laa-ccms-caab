package uk.gov.laa.ccms.caab.model.sections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Represents the Section display values for the application summary screen. */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ClientSectionDisplay extends ApplicationSectionStatusDisplay {

  /** The full display name of the client. */
  private String clientFullName;

  /** The client reference number. */
  private String clientReferenceNumber;
}
