package uk.gov.laa.ccms.caab.mapper;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_RELATIONSHIP_TO_CLIENT;

import java.util.Collections;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.sections.IndividualAddressContactDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualEmploymentDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualGeneralDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.util.OpponentUtil;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;

/**
 * Abstract mapper class for converting ebs application data to an IndividualDetailsSectionDisplay
 * object.
 *
 * @author Jamie Briggs
 */
@Mapper(
    componentModel = "spring",
    uses = {LookupService.class})
public abstract class IndividualDetailsSectionDisplayMapper {

  @Autowired protected LookupService lookupService;

  @Mapping(target = "generalDetails", source = "opponentDetail")
  @Mapping(target = "employmentDetails", source = "opponentDetail")
  @Mapping(target = "addressContactDetails", source = "opponentDetail")
  public abstract IndividualDetailsSectionDisplay toIndividualDetailsSectionDisplay(
      OpponentDetail opponentDetail);

  @Mapping(
      target = "relationshipToClient",
      source = "opponentDetail",
      qualifiedByName = "mapRelationshipToClient")
  @Mapping(
      target = "relationshipToCase",
      source = "opponentDetail",
      qualifiedByName = "mapRelationshipToCase")
  public abstract IndividualGeneralDetailsSectionDisplay toIndividualGeneralDetailsSectionDisplay(
      OpponentDetail opponentDetail);

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
  public abstract IndividualAddressContactDetailsSectionDisplay toAddressDetails(
      OpponentDetail opponentDetail);

  @Mapping(target = "employersName", source = "opponentDetail.employerName")
  @Mapping(target = "employersAddress", source = "opponentDetail.employerAddress")
  @Mapping(
      target = "hadCourtOrderedMeansAssessment",
      source = "opponentDetail.courtOrderedMeansAssessment")
  @Mapping(target = "partyIsLegalAided", source = "opponentDetail.legalAided")
  public abstract IndividualEmploymentDetailsSectionDisplay
      toIndividualEmploymentDetailsSectionDisplay(OpponentDetail opponentDetail);

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

  @Named("mapRelationshipToCase")
  protected String mapRelationshipToCase(OpponentDetail opponentDetail) {
    final Mono<RelationshipToCaseLookupDetail> personRelationshipsToCaseMono =
        lookupService.getPersonToCaseRelationships();

    return personRelationshipsToCaseMono
        .map(
            personRelationshipsToCase ->
                OpponentUtil.getRelationshipToCase(
                    opponentDetail,
                    Collections.emptyList(),
                    personRelationshipsToCase.getContent()))
        .map(RelationshipToCaseLookupValueDetail::getDescription)
        .blockOptional()
        .orElse("");
  }
}
