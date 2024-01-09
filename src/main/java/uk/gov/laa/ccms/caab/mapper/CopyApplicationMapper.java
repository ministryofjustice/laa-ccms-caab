package uk.gov.laa.ccms.caab.mapper;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.PROCEEDING_STATUS_UNCHANGED_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;

import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.Client;
import uk.gov.laa.ccms.caab.model.Opponent;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ScopeLimitation;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

/**
 * Mapper class to copy a subset of attributes from one CAAB ApplicationDetail to another.
 */
@Mapper(componentModel = "spring")
public interface CopyApplicationMapper {

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "caseReferenceNumber", source = "caseReferenceSummary.caseReferenceNumber")
  @Mapping(target = "applicationType", source = "applicationToCopy.applicationType")
  @Mapping(target = "providerDetails.provider",
      source = "applicationToCopy.providerDetails.provider")
  @Mapping(target = "providerDetails.office",
      source = "applicationToCopy.providerDetails.office")
  @Mapping(target = "providerDetails.supervisor",
      source = "applicationToCopy.providerDetails.supervisor")
  @Mapping(target = "providerDetails.feeEarner",
      source = "applicationToCopy.providerDetails.feeEarner")
  @Mapping(target = "providerDetails.providerContact",
      source = "applicationToCopy.providerDetails.providerContact")
  @Mapping(target = "categoryOfLaw", source = "applicationToCopy.categoryOfLaw")
  @Mapping(target = "correspondenceAddress", source = "applicationToCopy.correspondenceAddress")
  @Mapping(target = "larScopeFlag", source = "applicationToCopy.larScopeFlag")
  @Mapping(target = "proceedings", source = "applicationToCopy.proceedings")
  @Mapping(target = "opponents", source = "applicationToCopy.opponents")
  @Mapping(target = "client", source = "clientDetail")
  @Mapping(target = "costs.requestedCostLimitation", source = "requestedCostLimitation")
  @Mapping(target = "costs.defaultCostLimitation", source = "defaultCostLimitation")
  ApplicationDetail copyApplication(
      ApplicationDetail applicationToCopy,
      CaseReferenceSummary caseReferenceSummary,
      ClientDetail clientDetail,
      BigDecimal requestedCostLimitation,
      BigDecimal defaultCostLimitation);

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

  @Mapping(target = "firstName", source = "details.name.firstName")
  @Mapping(target = "surname", source = "details.name.surname")
  @Mapping(target = "reference", source = "clientReferenceNumber")
  Client copyClient(ClientDetail soaClient);
}
