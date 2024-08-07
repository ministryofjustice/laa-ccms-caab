package uk.gov.laa.ccms.caab.mapper;

import java.util.List;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import uk.gov.laa.ccms.caab.mapper.context.submission.OpponentSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.submission.ProceedingSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.caab.model.summary.GeneralDetailsSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.OpponentSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.OpponentsAndOtherPartiesSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ProceedingAndCostSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ProceedingSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ProviderSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ScopeLimitationSubmissionSummaryDisplay;

@Mapper(componentModel = "spring")
public interface SubmissionSummaryDisplayMapper {
  CommonMapper COMMON_MAPPER = Mappers.getMapper(CommonMapper.class);
  @Mapping(target = "office", source =
      "application.providerDetails.office.displayValue")
  @Mapping(target = "feeEarner", source =
      "application.providerDetails.feeEarner.displayValue")
  @Mapping(target = "supervisor", source =
      "application.providerDetails.supervisor.displayValue")
  @Mapping(target = "providerCaseReference", source =
      "application.providerDetails.providerCaseReference")
  @Mapping(target = "contactName", source =
      "application.providerDetails.providerContact.displayValue")
  ProviderSubmissionSummaryDisplay toProviderSummaryDisplay(
      ApplicationDetail application);

  @Mapping(target = "categoryOfLaw", source = "application.categoryOfLaw.displayValue")
  @Mapping(target = "applicationType", source = "application.applicationType.displayValue")
  @Mapping(target = "delegatedFunctionsDate", source = "application.applicationType.devolvedPowers.dateUsed")
  @Mapping(target = "lookups", ignore = true)
  GeneralDetailsSubmissionSummaryDisplay toGeneralDetailsSummaryDisplay(
      ApplicationDetail application);

  @Mapping(target = "proceedings",
      source = "application.proceedings",
      qualifiedByName = "toProceedingSummaryDisplayList")
  @Mapping(target = "caseCostLimitation",
      source = "application.costs.requestedCostLimitation")
  ProceedingAndCostSubmissionSummaryDisplay toProceedingAndCostSummaryDisplay(
      ApplicationDetail application,
      @Context ProceedingSubmissionSummaryMappingContext context);

  @Named("toProceedingSummaryDisplayList")
  List<ProceedingSubmissionSummaryDisplay> toProceedingSummaryDisplayList(
      List<ProceedingDetail> proceedings,
      @Context ProceedingSubmissionSummaryMappingContext context);

  @Mapping(target = "matterType", source = "matterType.displayValue")
  @Mapping(target = "proceeding", source = "proceedingType.displayValue")
  @Mapping(target = "clientInvolvementType", source = "clientInvolvement.displayValue")
  @Mapping(target = "formOfCivilLegalService", source = "levelOfService.displayValue")
  @Mapping(target = "typeOfOrder", source = "typeOfOrder.id", qualifiedByName = "toTypeOfOrder")
  @Mapping(target = "scopeLimitations",
      source = "scopeLimitations",
      qualifiedByName = "toScopeLimitationSummaryDisplayList")
  ProceedingSubmissionSummaryDisplay toProceedingSummaryDisplay(
      final ProceedingDetail proceeding,
      @Context final ProceedingSubmissionSummaryMappingContext context);

  @Named("toTypeOfOrder")
  default String toTypeOfOrder(
      final String code,
      @Context final ProceedingSubmissionSummaryMappingContext context) {
    return COMMON_MAPPER.toDisplayValue(code, context.getTypeOfOrder());
  }

  @Named("toScopeLimitationSummaryDisplayList")
  List<ScopeLimitationSubmissionSummaryDisplay> toScopeLimitationSummaryDisplayList(
      List<ScopeLimitationDetail> scopeLimitations);

  @Mapping(target = "scopeLimitation", source = "scopeLimitation.displayValue")
  @Mapping(target = "scopeLimitationWording", source = "scopeLimitationWording")
  ScopeLimitationSubmissionSummaryDisplay toScopeLimitationSummaryDisplay(
      ScopeLimitationDetail scopeLimitation);

  @Mapping(target = "opponents",
      source = "application.opponents",
      qualifiedByName = "toOpponentSummaryDisplayList")
  OpponentsAndOtherPartiesSubmissionSummaryDisplay toOpponentsAndOtherPartiesSummaryDisplay(
      ApplicationDetail application,
      @Context OpponentSubmissionSummaryMappingContext context);

  @Named("toOpponentSummaryDisplayList")
  List<OpponentSubmissionSummaryDisplay> toOpponentSummaryDisplayList(
      List<OpponentDetail> opponents,
      @Context OpponentSubmissionSummaryMappingContext context);

  @Mapping(target = "title", source = "title", qualifiedByName = "toTitle")
  @Mapping(target = "relationshipToCase", source = "opponent", qualifiedByName = "toRelationshipToCase")
  @Mapping(target = "relationshipToClient", source = "relationshipToClient", qualifiedByName = "toRelationshipToClient")
  @Mapping(target = "houseNameOrNumber", source = "address.houseNameOrNumber")
  @Mapping(target = "addressLine1", source = "address.addressLine1")
  @Mapping(target = "addressLine2", source = "address.addressLine2")
  @Mapping(target = "city", source = "address.city")
  @Mapping(target = "county", source = "address.county")
  @Mapping(target = "country", source = "address.country")
  @Mapping(target = "postcode", source = "address.postcode")
  OpponentSubmissionSummaryDisplay toOpponentSummaryDisplay(
      final OpponentDetail opponent,
      @Context final OpponentSubmissionSummaryMappingContext context);

  @Named("toTitle")
  default String toTitle(
      final String code,
      @Context final OpponentSubmissionSummaryMappingContext context) {
    return COMMON_MAPPER.toDisplayValue(code, context.getContactTitle());
  }

  @Named("toRelationshipToCase")
  default String toRelationshipToCase(
      final OpponentDetail opponent,
      @Context final OpponentSubmissionSummaryMappingContext context) {

    if (opponent.getType().equalsIgnoreCase("Organisation")) {
      return COMMON_MAPPER.toRelationshipDisplayValue(
          opponent.getRelationshipToCase(), context.getOrganisationRelationshipsToCase());
    } else {
      return COMMON_MAPPER.toRelationshipDisplayValue(
          opponent.getRelationshipToCase(), context.getIndividualRelationshipsToCase());
    }
  }

  @Named("toRelationshipToClient")
  default String toRelationshipToClient(
      final String code,
      @Context final OpponentSubmissionSummaryMappingContext context) {
    return COMMON_MAPPER.toDisplayValue(code, context.getRelationshipToClient());
  }


}
