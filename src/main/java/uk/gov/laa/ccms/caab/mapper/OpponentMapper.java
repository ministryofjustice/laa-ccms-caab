package uk.gov.laa.ccms.caab.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.laa.ccms.caab.bean.OpponentFormData;
import uk.gov.laa.ccms.caab.model.Opponent;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.soa.gateway.model.OrganisationDetail;

/**
 * Mapper class to convert Opponents between various formats.
 */
@Mapper(componentModel = "spring", config = IgnoreUnmappedMapperConfig.class)
public interface OpponentMapper {

  @Mapping(target = "type", constant = "Organisation")
  @Mapping(target = ".", source = "organisation")
  @Mapping(target = "houseNameOrNumber", source = "organisation.address.house")
  @Mapping(target = "addressLine1", source = "organisation.address.addressLine1")
  @Mapping(target = "addressLine2", source = "organisation.address.addressLine2")
  @Mapping(target = "city", source = "organisation.address.city")
  @Mapping(target = "county", source = "organisation.address.county")
  @Mapping(target = "country", source = "organisation.address.country")
  @Mapping(target = "postcode", source = "organisation.address.postalCode")
  @Mapping(target = "organisationName", source = "organisation.name")
  @Mapping(target = "organisationType", source = "orgTypeLookup.code")
  @Mapping(target = "organisationTypeDisplayValue", source = "orgTypeLookup.description")
  @Mapping(target = "telephoneHome", source = "organisation.contactDetails.telephoneHome")
  @Mapping(target = "telephoneWork", source = "organisation.contactDetails.telephoneWork")
  @Mapping(target = "telephoneMobile", source = "organisation.contactDetails.mobileNumber")
  @Mapping(target = "faxNumber", source = "organisation.contactDetails.fax")
  @Mapping(target = "emailAddress", source = "organisation.contactDetails.emailAddress")
  OpponentFormData toOpponentFormData(OrganisationDetail organisation,
      CommonLookupValueDetail orgTypeLookup);

  @Mapping(target = "ebsId", source = "partyId")
  @Mapping(target = "organisationType.id", source = "organisationType")
  @Mapping(target = "organisationType.displayValue", source = "organisationTypeDisplayValue")
  @Mapping(target = "address", source = ".")
  Opponent toOpponent(OpponentFormData opponentFormData);
}
