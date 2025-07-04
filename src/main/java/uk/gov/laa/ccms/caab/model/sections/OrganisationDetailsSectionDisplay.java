package uk.gov.laa.ccms.caab.model.sections;

/**
 * Represents the display structure for an organisation's details section.
 *
 * <p>This record encapsulates two sub-sections:
 *
 * <ol>
 *   <li>Details about the organisation, such as organisation name, currently trading, and
 *       organisation type.
 *   <li>Address details and other information for the organisation. *
 * </ol>
 *
 * @author Geoff Murley
 */
public record OrganisationDetailsSectionDisplay(
    OrganisationOrganisationDetailsSectionDisplay organisationDetails,
    OrganisationAddressDetailsSectionDisplay addressDetails) {}
