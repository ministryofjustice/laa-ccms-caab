package uk.gov.laa.ccms.caab.model.sections;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents the display details for an organisation's details.
 *
 * @author Geoff Murley
 */
@Getter
@Setter
public class OrganisationOrganisationDetailsSectionDisplay {

  private String organisationName;
  private Boolean currentlyTrading;
  private String contactNameRole;
  private String organisationType;
  private String relationshipToCase;
  private String relationshipToClient;
}
