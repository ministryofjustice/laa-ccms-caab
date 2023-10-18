package uk.gov.laa.ccms.caab.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.laa.ccms.caab.model.Address;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.AssessmentResult;
import uk.gov.laa.ccms.caab.model.Client;
import uk.gov.laa.ccms.caab.model.CostEntry;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ProceedingOutcome;
import uk.gov.laa.ccms.caab.model.ScopeLimitation;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.ProceedingDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;
import uk.gov.laa.ccms.soa.gateway.model.AddressDetail;
import uk.gov.laa.ccms.soa.gateway.model.BaseClient;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.CostLimitation;
import uk.gov.laa.ccms.soa.gateway.model.OutcomeDetail;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

  @Mapping(target = "caseReferenceNumber", source = "caseDetail.caseReferenceNumber")
  @Mapping(target = "certificate.id", source = "caseDetail.certificateType")
  @Mapping(target = "applicationType.id", source = "caseDetail.applicationDetails.applicationAmendmentType")
  @Mapping(target = "applicationType.displayValue", source = "applicationTypeLookup.description")
  @Mapping(target = "dateCreated", source = "caseDetail.recordHistory.dateCreated")
  @Mapping(target = "providerCaseReference", source = "caseDetail.applicationDetails.providerDetails.providerCaseReferenceNumber")
  @Mapping(target = "provider.id", source = "caseDetail.applicationDetails.providerDetails.providerFirmId")
  @Mapping(target = "provider.displayValue", source = "ebsProvider.name")
  @Mapping(target = "providerContact.id", source = "caseDetail.applicationDetails.providerDetails.contactUserId.userLoginId")
  @Mapping(target = "providerContact.displayValue", source = "caseDetail.applicationDetails.providerDetails.contactUserId.userName")
  @Mapping(target = "office.id", source = "providerOffice.id")
  @Mapping(target = "office.displayValue", source = "providerOffice.name")
  @Mapping(target = "supervisor.id", source = "supervisorContact.id")
  @Mapping(target = "supervisor.displayValue", source = "supervisorContact.name")
  @Mapping(target = "feeEarner.id", source = "feeEarnerContact.id")
  @Mapping(target = "feeEarner.displayValue", source = "feeEarnerContact.name")
//  @Mapping(target = "correspondenceAddress", source = "caseDetail.applicationDetails.correspondenceAddress")
//  @Mapping(target = "correspondenceAddress.preferredAddress", source = "caseDetail.applicationDetails.preferredAddress")
//  @Mapping(target = "correspondenceAddress.noFixedAbode", constant = "false")
//  @Mapping(target = "client", source = "caseDetail.applicationDetails.client")
//  @Mapping(target = "client.reference", source = "caseDetail.applicationDetails.client.clientReferenceNumber")
  @Mapping(target = "categoryOfLaw.id", source = "caseDetail.applicationDetails.categoryOfLaw.categoryOfLawCode")
  @Mapping(target = "categoryOfLaw.displayValue", source = "caseDetail.applicationDetails.categoryOfLaw.categoryOfLawDescription")
  @Mapping(target = "costs.grantedCostLimitation", source = "caseDetail.applicationDetails.categoryOfLaw.grantedAmount")
  @Mapping(target = "costs.requestedCostLimitation", source = "caseDetail.applicationDetails.categoryOfLaw.requestedAmount")
  @Mapping(target = "costs.defaultCostLimitation", ignore = true)
  @Mapping(target = "costs.costEntries", source = "caseDetail.applicationDetails.categoryOfLaw.costLimitations")
  @Mapping(target = "costs.currentProviderBilledAmount", ignore = true)
  @Mapping(target = "costs.auditTrail", ignore = true)
  ApplicationDetail toApplicationDetail(
      CaseDetail caseDetail,
      CommonLookupValueDetail applicationTypeLookup,
      uk.gov.laa.ccms.data.model.ProviderDetail ebsProvider,
      OfficeDetail providerOffice,
      ContactDetail supervisorContact,
      ContactDetail feeEarnerContact);

  List<CostEntry> toCostEntryList(List<CostLimitation> costLimitationList);

  @Mapping(target = "lscResourceId", source = "billingProviderId")
  @Mapping(target = "amountBilled", source = "paidToDate")
  @Mapping(target = "requestedCosts", source = "amount")
  @Mapping(target = "resourceName", source = "billingProviderName")
  @Mapping(target = "ebsId", source = "costLimitId")
  @Mapping(target = "newEntry", constant = "false")
  CostEntry toCostEntry(CostLimitation costLimitation);

  @Mapping(target = "edited", constant = "false")
  @Mapping(target = ".", source = "soaProceeding")
  @Mapping(target = "ebsId", source = "soaProceeding.proceedingCaseId")
  @Mapping(target = "leadProceedingInd", source = "soaProceeding.leadProceedingIndicator")
  @Mapping(target = "matterType.id", source = "matterTypeLookup.code")
  @Mapping(target = "matterType.displayValue", source = "matterTypeLookup.description")
  @Mapping(target = "proceedingType.id", source = "proceedingLookup.code")
  @Mapping(target = "proceedingType.displayValue", source = "proceedingLookup.name")
  @Mapping(target = "description", source = "soaProceeding.proceedingDescription")
  @Mapping(target = "larScope", source = "proceedingLookup.larScope")
  @Mapping(target = "levelOfService.id", source = "levelOfServiceLookup.code")
  @Mapping(target = "levelOfService.displayValue", source = "levelOfServiceLookup.description")
  @Mapping(target = "clientInvolvement.id", source = "clientInvolvementLookup.code")
  @Mapping(target = "clientInvolvement.displayValue", source = "clientInvolvementLookup.description")
  @Mapping(target = "status.id", source = "proceedingStatus.code")
  @Mapping(target = "status.displayValue", source = "proceedingStatus.description")
  @Mapping(target = "typeOfOrder.id", source = "soaProceeding.orderType")
  @Mapping(target = "scopeLimitations", ignore = true)
  @Mapping(target = "outcome", ignore = true)
  Proceeding toProceeding(uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail soaProceeding,
      ProceedingDetail proceedingLookup,
      CommonLookupValueDetail matterTypeLookup,
      CommonLookupValueDetail levelOfServiceLookup,
      CommonLookupValueDetail clientInvolvementLookup,
      CommonLookupValueDetail proceedingStatus);

  @Mapping(target = "ebsId", source = "soaScopeLimitation.scopeLimitationId")
  @Mapping(target = "scopeLimitation.id", source = "scopeLimitationLookup.code")
  @Mapping(target = "scopeLimitation.displayValue", source = "scopeLimitationLookup.description")
  @Mapping(target = "scopeLimitationWording.id", source = "soaScopeLimitation.scopeLimitationWording")
  @Mapping(target = "delegatedFuncApplyInd.flag", source = "soaScopeLimitation.delegatedFunctionsApply")
  ScopeLimitation toScopeLimitation(
      uk.gov.laa.ccms.soa.gateway.model.ScopeLimitation soaScopeLimitation,
      CommonLookupValueDetail scopeLimitationLookup);

  @Mapping(target = "description", source = "caabProceeding.description")
  @Mapping(target = "matterType", source = "caabProceeding.matterType")
  @Mapping(target = "proceedingCaseId", source = "caabProceeding.proceedingCaseId")
  @Mapping(target = "proceedingType", source = "caabProceeding.proceedingType")
  @Mapping(target = "adrInfo", source = "soaOutcome.altAcceptanceReason")
  @Mapping(target = "alternativeResolution", source = "soaOutcome.altDisputeResolution")
  @Mapping(target = "courtCode", source = "soaOutcome.courtCode")
  @Mapping(target = "courtName", source = "courtLookup.description")
  @Mapping(target = "dateOfFinalWork", source = "soaOutcome.finalWorkDate")
  @Mapping(target = "resolutionMethod", source = "soaOutcome.resolutionMethod")
  @Mapping(target = "result.id", source = "soaOutcome.result")
  @Mapping(target = "result.displayValue", source = "outcomeResultLookup.outcomeResultDescription")
  @Mapping(target = "resultInfo", source = "soaOutcome.additionalResultInfo")
  @Mapping(target = "stageEnd.id", source = "soaOutcome.stageEnd")
  @Mapping(target = "stageEnd.displayValue", source = "stageEndLookup.description")
  @Mapping(target = "widerBenefits", source = "soaOutcome.widerBenefits")
  @Mapping(target = "outcomeCourtCaseNo", source = "soaOutcome.outcomeCourtCaseNumber")
  ProceedingOutcome toProceedingOutcome(
      Proceeding caabProceeding,
      OutcomeDetail soaOutcome,
      CommonLookupValueDetail courtLookup,
      OutcomeResultLookupValueDetail outcomeResultLookup,
      StageEndLookupValueDetail stageEndLookup);

  AssessmentResult toAssessmentResult(uk.gov.laa.ccms.soa.gateway.model.AssessmentResult assessmentResult);


  @Mapping(target = "preferredAddress", ignore = true)
  @Mapping(target = "careOf", source = "careOfName")
  @Mapping(target = "houseNameOrNumber", source = "house")
  @Mapping(target = "postcode", source = "postalCode")
  Address toAddress(AddressDetail soaAddress);

  @Mapping(target = "reference", source = "clientReferenceNumber")
  Client toClient(BaseClient soaClient);

}
