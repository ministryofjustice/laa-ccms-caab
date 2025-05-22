package uk.gov.laa.ccms.caab.model.sections;

public record IndividualDetailsSectionDisplay(
    IndividualGeneralDetailsSectionDisplay generalDetails,
    AddressContactDetailsSectionDisplay addressContactDetails,
    IndividualEmploymentDetailsSectionDisplay employmentDetails) { }
