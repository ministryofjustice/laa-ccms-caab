package uk.gov.laa.ccms.caab.mapper;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_INDIVIDUAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.PROCEEDING_STATUS_UNCHANGED_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;

import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.Opponent;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ScopeLimitation;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;

/**
 * Mapper class to copy a subset of attributes from one CAAB ApplicationDetail to another.
 */
@Mapper(componentModel = "spring")
public interface CopyApplicationMapper {

  @Mapping(target = "applicationType", source = "applicationToCopy.applicationType")
  @Mapping(target = "provider", source = "applicationToCopy.provider")
  @Mapping(target = "office", source = "applicationToCopy.office")
  @Mapping(target = "supervisor", source = "applicationToCopy.supervisor")
  @Mapping(target = "feeEarner", source = "applicationToCopy.feeEarner")
  @Mapping(target = "providerContact", source = "applicationToCopy.providerContact")
  @Mapping(target = "categoryOfLaw", source = "applicationToCopy.categoryOfLaw")
  @Mapping(target = "correspondenceAddress", source = "applicationToCopy.correspondenceAddress")
  @Mapping(target = "larScopeFlag", source = "applicationToCopy.larScopeFlag")
  @Mapping(target = "proceedings", source = "applicationToCopy.proceedings")
  @Mapping(target = "opponents", source = "applicationToCopy.opponents")
  @Mapping(target = "costs.requestedCostLimitation", source = "requestedCostLimitation")
  @Mapping(target = "costs.defaultCostLimitation", source = "defaultCostLimitation")
  ApplicationDetail copyApplication(@MappingTarget ApplicationDetail application,
      ApplicationDetail applicationToCopy,
      BigDecimal requestedCostLimitation,
      BigDecimal defaultCostLimitation,
      List<RelationshipToCaseLookupValueDetail> copyPartyRelationships);

  /**
   * AfterMapping logic to finalise the copying of an ApplicationDetail.
   *
   * @param applicationDetail - the mapping target.
   * @param copyPartyRelationships - List of relationships.
   */
  @AfterMapping
  default void afterMappingApplication(@MappingTarget ApplicationDetail applicationDetail,
      List<RelationshipToCaseLookupValueDetail> copyPartyRelationships) {

    // Clear the ebsId for an opponent if it is of type INDIVIDUAL AND it is shared AND
    // the relationship to case for the opponent is of type Copy Party.
    if (applicationDetail.getOpponents() != null) {
      applicationDetail.getOpponents().forEach(opponent -> {
        if (OPPONENT_TYPE_INDIVIDUAL.equals(opponent.getType()) && opponent.getSharedInd()) {
          copyPartyRelationships.stream()
              .filter(
                  relationshipToCase -> relationshipToCase.getCode().equals(
                      opponent.getRelationshipToCase()))
              .findFirst()
              .ifPresent(copyPartyLookup -> opponent.setEbsId(null));
        }
      });
    }
  }

  List<Proceeding> copyProceedingList(List<Proceeding> proceedingList);

  @Mapping(target = "ebsId", ignore = true)
  @Mapping(target = "status.id", constant = STATUS_DRAFT)
  @Mapping(target = "status.displayValue", constant = PROCEEDING_STATUS_UNCHANGED_DISPLAY)
  Proceeding copyProceeding(Proceeding proceedingToCopy);

  List<ScopeLimitation> copyScopeLimitationList(List<ScopeLimitation> scopeLimitationList);

  @Mapping(target = "ebsId", ignore = true)
  @Mapping(target = "defaultInd", ignore = true)
  uk.gov.laa.ccms.caab.model.ScopeLimitation copyScopeLimitation(
      uk.gov.laa.ccms.caab.model.ScopeLimitation scopeLimitationToCopy);

  List<Opponent> copyOpponentList(List<Opponent> opponentList);

  @Mapping(target = "confirmed", constant = "true")
  @Mapping(target = "deleteInd", constant = "true")
  @Mapping(target = "amendment", constant = "false")
  @Mapping(target = "appMode", constant = "true")
  @Mapping(target = "award", constant = "false")
  Opponent copyOpponent(Opponent opponentToCopy);
}
