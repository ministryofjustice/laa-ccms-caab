package uk.gov.laa.ccms.caab.mapper;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.PROCEEDING_STATUS_UNCHANGED_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;

import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.Opponent;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ScopeLimitation;

/**
 * Mapper class to copy a subset of attributes from one CAAB ApplicationDetail to another.
 */
@Mapper(componentModel = "spring")
public interface CopyApplicationMapper {

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "applicationType", source = "applicationType")
  @Mapping(target = "applicationType.devolvedPowers.contractFlag", ignore = true)
  @Mapping(target = "providerDetails", source = "providerDetails")
  @Mapping(target = "categoryOfLaw", source = "categoryOfLaw")
  @Mapping(target = "correspondenceAddress", source = "correspondenceAddress")
  @Mapping(target = "larScopeFlag", source = "larScopeFlag")
  @Mapping(target = "proceedings", source = "proceedings")
  @Mapping(target = "opponents", source = "opponents")
  ApplicationDetail copyApplication(
      @MappingTarget ApplicationDetail newApplication,
      ApplicationDetail applicationToCopy);

  @Mapping(target = "providerCaseReference", ignore = true)
  ApplicationProviderDetails copyApplicationProviderDetails(
      ApplicationProviderDetails providerDetailsToCopy);

  List<Proceeding> copyProceedingList(List<Proceeding> proceedingList);

  @Mapping(target = "ebsId", ignore = true)
  @Mapping(target = "status.id", constant = STATUS_DRAFT)
  @Mapping(target = "status.displayValue", constant = PROCEEDING_STATUS_UNCHANGED_DISPLAY)
  Proceeding copyProceeding(Proceeding proceedingToCopy);

  List<ScopeLimitation> copyScopeLimitationList(List<ScopeLimitation> scopeLimitationList);

  @Mapping(target = "ebsId", ignore = true)
  @Mapping(target = "defaultInd", ignore = true)
  @Mapping(target = "nonDefaultWordingReqd", ignore = true)
  @Mapping(target = "stage", ignore = true)
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
