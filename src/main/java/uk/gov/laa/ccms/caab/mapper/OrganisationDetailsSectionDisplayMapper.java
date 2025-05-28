package uk.gov.laa.ccms.caab.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.sections.OrganisationAddressDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OrganisationDetailsSectionDisplay;

/**
 * Mapper interface for converting ebs application data to an OrganisationDetailsSectionDisplay
 * object.
 *
 * @author Geoff Murley
 */
@Mapper(componentModel = "spring")
public interface OrganisationDetailsSectionDisplayMapper {

  @Mapping(target = "organisationDetails", source = "opponentDetail")
  @Mapping(target = "addressDetails", source = "opponentDetail")
  OrganisationDetailsSectionDisplay toOrganisationDetailsSectionDisplay(
      OpponentDetail opponentDetail);

  @Mapping(target = "houseNameNumber", source = "opponentDetail.address.houseNameOrNumber")
  @Mapping(target = "addressLineOne", source = "opponentDetail.address.addressLine1")
  @Mapping(target = "addressLineTwo", source = "opponentDetail.address.addressLine2")
  @Mapping(target = "cityTown", source = "opponentDetail.address.city")
  @Mapping(target = "county", source = "opponentDetail.address.county")
  @Mapping(target = "country", source = "opponentDetail.address.country")
  @Mapping(target = "postcode", source = "opponentDetail.address.postcode")
  @Mapping(target = "telephone", source = "opponentDetail.telephoneHome")
  @Mapping(target = "email", source = "opponentDetail.emailAddress")
  @Mapping(target = "fax", source = "opponentDetail.faxNumber")
  @Mapping(target = "otherInformation", source = "opponentDetail.otherInformation")
  OrganisationAddressDetailsSectionDisplay toOrganisationAddressDetails(
      OpponentDetail opponentDetail);

}
