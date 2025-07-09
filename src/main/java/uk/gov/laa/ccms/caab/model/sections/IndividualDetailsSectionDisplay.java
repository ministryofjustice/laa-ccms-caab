package uk.gov.laa.ccms.caab.model.sections;

/**
 * Represents the display structure for an individual's details section.
 *
 * <p>This record encapsulates three sub-sections:
 *
 * <ol>
 *   <li>General details about the individual, such as name, date of birth, and relationships.
 *   <li>Address and contact information for the individual.
 *   <li>Employment-related information.
 * </ol>
 *
 * @author Jamie Briggs
 */
public record IndividualDetailsSectionDisplay(
    IndividualGeneralDetailsSectionDisplay generalDetails,
    IndividualAddressContactDetailsSectionDisplay addressContactDetails,
    IndividualEmploymentDetailsSectionDisplay employmentDetails) {}
