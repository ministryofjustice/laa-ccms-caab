package uk.gov.laa.ccms.caab.model.sections;

/**
 * Represents the display structure for an individual's details section.
 *
 * <p>This record encapsulates three sub-sections:</p>
 * <ol>
 *   <li>General details about the individual, such as name, date of birth, and relationships.</li>
 *   <li>Address and contact information for the individual.</li>
 *   <li>Employment-related information.</li>
 * </ol>
 *
 * @author Jamie Briggs
 */
public record IndividualDetailsSectionDisplay(
    IndividualGeneralDetailsSectionDisplay generalDetails,
    IndividualAddressContactDetailsSectionDisplay addressContactDetails,
    IndividualEmploymentDetailsSectionDisplay employmentDetails) { }
