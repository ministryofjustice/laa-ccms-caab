package uk.gov.laa.ccms.caab.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.sections.IndividualAddressContactDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualEmploymentDetailsSectionDisplay;

/**
 * Mapper interface for converting ebs application data to an IndividualDetailsSectionDisplay
 * object.
 *
 * @author Jamie Briggs
 */
@Mapper(componentModel = "spring")
public interface IndividualDetailsSectionDisplayMapper {

  @Mapping(target = "generalDetails", source = "opponentDetail")
  @Mapping(target = "employmentDetails", source = "opponentDetail")
  @Mapping(target = "addressContactDetails", source = "opponentDetail")
  IndividualDetailsSectionDisplay toIndividualDetailsSectionDisplay(OpponentDetail opponentDetail);

  @Mapping(target = "houseNameNumber", source = "opponentDetail.address.houseNameOrNumber")
  @Mapping(target = "addressLineOne", source = "opponentDetail.address.addressLine1")
  @Mapping(target = "addressLineTwo", source = "opponentDetail.address.addressLine2")
  @Mapping(target = "cityTown", source = "opponentDetail.address.city")
  @Mapping(target = "county", source = "opponentDetail.address.county")
  @Mapping(target = "country", source = "opponentDetail.address.country")
  @Mapping(target = "postcode", source = "opponentDetail.address.postcode")
  @Mapping(target = "telephoneHome", source = "opponentDetail.telephoneHome")
  @Mapping(target = "telephoneWork", source = "opponentDetail.telephoneWork")
  @Mapping(target = "telephoneMobile", source = "opponentDetail.telephoneMobile")
  @Mapping(target = "email", source = "opponentDetail.emailAddress")
  @Mapping(target = "fax", source = "opponentDetail.faxNumber")
  IndividualAddressContactDetailsSectionDisplay toAddressDetails(OpponentDetail opponentDetail);

  @Mapping(target = "employersName", source = "opponentDetail.employerName")
  @Mapping(target = "employersAddress", source = "opponentDetail.employerAddress")
  @Mapping(target = "hadCourtOrderedMeansAssessment",
      source = "opponentDetail.courtOrderedMeansAssessment")
  @Mapping(target = "partyIsLegalAided", source = "opponentDetail.legalAided")
  IndividualEmploymentDetailsSectionDisplay toIndividualEmploymentDetailsSectionDisplay(
      OpponentDetail opponentDetail);

}
