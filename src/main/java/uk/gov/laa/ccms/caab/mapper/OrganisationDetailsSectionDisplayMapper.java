package uk.gov.laa.ccms.caab.mapper;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ORGANISATION_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_RELATIONSHIP_TO_CLIENT;

import java.util.Collections;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.sections.OrganisationAddressDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OrganisationDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OrganisationOrganisationDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.util.OpponentUtil;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;

/**
 * Mapper interface for converting ebs application data to an OrganisationDetailsSectionDisplay
 * object.
 *
 * @author Geoff Murley
 */
@Mapper(
    componentModel = "spring",
    uses = {LookupService.class})
public abstract class OrganisationDetailsSectionDisplayMapper {
  @Autowired protected LookupService lookupService;

  @Mapping(target = "organisationDetails", source = "opponentDetail")
  @Mapping(target = "addressDetails", source = "opponentDetail")
  public abstract OrganisationDetailsSectionDisplay toOrganisationDetailsSectionDisplay(
      OpponentDetail opponentDetail);

  @Mapping(
      target = "relationshipToClient",
      source = "opponentDetail",
      qualifiedByName = "mapRelationshipToClient")
  @Mapping(
      target = "relationshipToCase",
      source = "opponentDetail",
      qualifiedByName = "mapRelationshipToCase")
  @Mapping(
      target = "organisationType",
      source = "opponentDetail",
      qualifiedByName = "mapOrganisationType")
  public abstract OrganisationOrganisationDetailsSectionDisplay toIndividualDetailsSectionDisplay(
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
  public abstract OrganisationAddressDetailsSectionDisplay toOrganisationAddressDetails(
      OpponentDetail opponentDetail);

  @Named("mapRelationshipToClient")
  protected String mapRelationshipToClient(OpponentDetail opponentDetail) {
    final Mono<CommonLookupDetail> relationshipsToClientMono =
        lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT);

    return relationshipsToClientMono
        .blockOptional()
        .map(x -> OpponentUtil.getRelationshipToClient(opponentDetail, x.getContent()))
        .map(CommonLookupValueDetail::getDescription)
        .orElse("");
  }

  @Named("mapOrganisationType")
  protected String mapOrganisationType(OpponentDetail opponentDetail) {
    final Mono<CommonLookupDetail> organisationTypeMono =
        lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES);

    return organisationTypeMono
        .blockOptional()
        .map(x -> OpponentUtil.getOrganisationType(opponentDetail, x.getContent()))
        .map(CommonLookupValueDetail::getDescription)
        .orElse("");
  }

  @Named("mapRelationshipToCase")
  protected String mapRelationshipToCase(OpponentDetail opponentDetail) {
    final Mono<RelationshipToCaseLookupDetail> organisationRelationshipsToCaseMono =
        lookupService.getOrganisationToCaseRelationships();

    return organisationRelationshipsToCaseMono
        .map(
            organisationRelationshipsToCase ->
                OpponentUtil.getRelationshipToCase(
                    opponentDetail,
                    organisationRelationshipsToCase.getContent(),
                    Collections.emptyList()))
        .map(RelationshipToCaseLookupValueDetail::getDescription)
        .blockOptional()
        .orElse("");
  }
}
