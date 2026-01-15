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
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;

/** Mapper class to copy a subset of attributes from one CAAB ApplicationDetail to another. */
@Mapper(componentModel = "spring")
public interface CopyApplicationMapper {

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "applicationType", source = "applicationType")
  @Mapping(target = "applicationType.displayValue", source = "applicationType.displayValue")
  @Mapping(target = "applicationType.id", source = "applicationType.id")
  @Mapping(target = "applicationType.devolvedPowers.contractFlag", ignore = true)
  @Mapping(
      target = "applicationType.devolvedPowers.dateUsed",
      source = "applicationType.devolvedPowers.dateUsed")
  @Mapping(
      target = "applicationType.devolvedPowers.used",
      source = "applicationType.devolvedPowers.used")
  @Mapping(target = "providerDetails", source = "providerDetails")
  @Mapping(target = "categoryOfLaw", source = "categoryOfLaw")
  @Mapping(target = "correspondenceAddress", source = "correspondenceAddress")
  @Mapping(target = "larScopeFlag", source = "larScopeFlag")
  @Mapping(target = "proceedings", source = "proceedings")
  @Mapping(target = "opponents", source = "opponents")
  @Mapping(target = "costs", source = "costs")
  ApplicationDetail copyApplication(
      @MappingTarget ApplicationDetail newApplication, ApplicationDetail applicationToCopy);

  @Mapping(target = "providerCaseReference", ignore = true)
  ApplicationProviderDetails copyApplicationProviderDetails(
      ApplicationProviderDetails providerDetailsToCopy);

  List<ProceedingDetail> copyProceedingList(List<ProceedingDetail> proceedingList);

  @Mapping(target = "ebsId", ignore = true)
  @Mapping(target = "status.id", constant = STATUS_DRAFT)
  @Mapping(target = "status.displayValue", constant = PROCEEDING_STATUS_UNCHANGED_DISPLAY)
  ProceedingDetail copyProceeding(ProceedingDetail proceedingToCopy);

  List<ScopeLimitationDetail> copyScopeLimitationList(
      List<ScopeLimitationDetail> scopeLimitationList);

  @Mapping(target = "ebsId", ignore = true)
  @Mapping(target = "defaultInd", ignore = true)
  @Mapping(target = "nonDefaultWordingReqd", ignore = true)
  @Mapping(target = "stage", ignore = true)
  ScopeLimitationDetail copyScopeLimitation(ScopeLimitationDetail scopeLimitationToCopy);

  List<OpponentDetail> copyOpponentList(List<OpponentDetail> opponentList);

  @Mapping(target = "confirmed", constant = "true")
  @Mapping(target = "deleteInd", constant = "true")
  @Mapping(target = "amendment", constant = "false")
  @Mapping(target = "appMode", constant = "true")
  @Mapping(target = "award", constant = "false")
  @Mapping(
      target = "address",
      defaultExpression = "java(new uk.gov.laa.ccms.caab.model.AddressDetail())")
  OpponentDetail copyOpponent(OpponentDetail opponentToCopy);

  List<CostEntryDetail> copyCostEntryList(List<CostEntryDetail> costEntryList);

  CostEntryDetail copyCostEntry(CostEntryDetail costEntryToCopy);

  CostStructureDetail copyCostStructure(CostStructureDetail costStructureToCopy);
}
