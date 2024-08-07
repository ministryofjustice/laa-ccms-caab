package uk.gov.laa.ccms.caab.model.sections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Represents the Section display values for an Application Provider.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderSectionDisplay extends ApplicationSectionStatusDisplay {

  /**
   * The provider name.
   */
  private String providerName;

  /**
   * The provider case reference.
   */
  private String providerCaseReferenceNumber;

  /**
   * The provider contact name.
   */
  private String providerContactName;

  /**
   * The provider's office name.
   */
  private String officeName;

  /**
   * The provider fee earner name.
   */
  private String feeEarner;

  /**
   * The provider's supervisor name.
   */
  private String supervisorName;

}
