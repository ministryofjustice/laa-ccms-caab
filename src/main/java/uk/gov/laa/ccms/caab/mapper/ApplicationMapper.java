package uk.gov.laa.ccms.caab.mapper;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST_DESCRIPTION;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL_DESCRIPTION;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_LAND;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_OTHER_ASSET;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_INDIVIDUAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_ORGANISATION;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.data.domain.Page;
import uk.gov.laa.ccms.caab.mapper.context.ApplicationMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.CaseOutcomeMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.PriorAuthorityMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.ProceedingMappingContext;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.AssessmentResult;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.BaseAwardDetail;
import uk.gov.laa.ccms.caab.model.BooleanDisplayValue;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.ClientDetail;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.DevolvedPowersDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.LandAwardDetail;
import uk.gov.laa.ccms.caab.model.LiablePartyDetail;
import uk.gov.laa.ccms.caab.model.LinkedCaseDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ProceedingOutcomeDetail;
import uk.gov.laa.ccms.caab.model.RecoveryDetail;
import uk.gov.laa.ccms.caab.model.ReferenceDataItemDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.model.TimeRecoveryDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;
import uk.gov.laa.ccms.soa.gateway.model.Award;
import uk.gov.laa.ccms.soa.gateway.model.BaseClient;
import uk.gov.laa.ccms.soa.gateway.model.CaseStatus;
import uk.gov.laa.ccms.soa.gateway.model.CaseSummary;
import uk.gov.laa.ccms.soa.gateway.model.CategoryOfLaw;
import uk.gov.laa.ccms.soa.gateway.model.CostLimitation;
import uk.gov.laa.ccms.soa.gateway.model.LinkedCase;
import uk.gov.laa.ccms.soa.gateway.model.OtherParty;
import uk.gov.laa.ccms.soa.gateway.model.Recovery;
import uk.gov.laa.ccms.soa.gateway.model.ScopeLimitation;
import uk.gov.laa.ccms.soa.gateway.model.TimeRelatedAward;
import uk.gov.laa.ccms.soa.gateway.model.UserDetail;

/**
 * Mapper class to convert a SoaApi Case into a CAAB ApplicationDetail.
 */
@Mapper(componentModel = "spring")
public interface ApplicationMapper {

  ApplicationDetails toApplicationDetails(Page<BaseApplicationDetail> applicationPage);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", source = "caseStatusDisplay")
  @Mapping(target = "providerDetails", source = ".")
  BaseApplicationDetail toBaseApplication(CaseSummary soaCaseSummary);

  @Mapping(target = "providerCaseReference", source = "providerCaseReferenceNumber")
  @Mapping(target = "feeEarner.displayValue", source = "feeEarnerName")
  @Mapping(target = "provider", ignore = true)
  @Mapping(target = "office", ignore = true)
  @Mapping(target = "supervisor", ignore = true)
  @Mapping(target = "providerContact", ignore = true)
  ApplicationProviderDetails toApplicationProviderDetails(CaseSummary soaCaseSummary);

  @Mapping(target = ".", source = "soaCaseDetail")
  @Mapping(target = "certificate", source = "certificate")
  @Mapping(target = "applicationType", source = "applicationMappingContext")
  @Mapping(target = "dateCreated", source = "soaCaseDetail.recordHistory.dateCreated")
  @Mapping(target = "providerDetails.providerCaseReference",
      source = "soaCaseDetail.applicationDetails.providerDetails.providerCaseReferenceNumber")
  @Mapping(target = "providerDetails.provider", source = "providerDetail")
  @Mapping(target = "providerDetails.providerContact",
      source = "soaCaseDetail.applicationDetails.providerDetails.contactUserId")
  @Mapping(target = "providerDetails.office", source = "providerOffice")
  @Mapping(target = "providerDetails.supervisor", source = "supervisorContact")
  @Mapping(target = "providerDetails.feeEarner", source = "feeEarnerContact")
  @Mapping(target = "correspondenceAddress", source = "applicationMappingContext")
  @Mapping(target = "client", source = "soaCaseDetail.applicationDetails.client")
  @Mapping(target = "categoryOfLaw", source = "soaCaseDetail.applicationDetails.categoryOfLaw")
  @Mapping(target = "costs", source = "applicationMappingContext")
  @Mapping(target = "larScopeFlag",
      source = "soaCaseDetail.applicationDetails.larDetails.larScopeFlag", defaultValue = "false")
  @Mapping(target = "status", source = "soaCaseDetail.caseStatus")
  @Mapping(target = "opponents", source = "soaCaseDetail.applicationDetails.otherParties")
  @Mapping(target = "proceedings", source = "proceedings")
  @Mapping(target = "amendmentProceedingsInEbs", source = "amendmentProceedingsInEbs")
  @Mapping(target = "submitted", source = "caseWithOnlyDraftProceedings")
  @Mapping(target = "priorAuthorities", source = "priorAuthorities")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "relationToLinkedCase", ignore = true)
  @Mapping(target = "quickEditType", ignore = true)
  @Mapping(target = "opponentMode", ignore = true)
  @Mapping(target = "opponentAppliedForFunding", ignore = true)
  @Mapping(target = "meritsReassessmentRequired", ignore = true)
  @Mapping(target = "meritsAssessmentStatus", ignore = true)
  @Mapping(target = "meritsAssessmentAmended", ignore = true)
  @Mapping(target = "meansAssessmentStatus", ignore = true)
  @Mapping(target = "meansAssessmentAmended", ignore = true)
  @Mapping(target = "leadProceedingChanged", ignore = true)
  @Mapping(target = "editProceedingsAndCostsAllowed", ignore = true)
  @Mapping(target = "costLimit", ignore = true)
  @Mapping(target = "award", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "appMode", ignore = true)
  @Mapping(target = "amendment", ignore = true)
  @Mapping(target = "allSectionsComplete", ignore = true)
  ApplicationDetail toApplicationDetail(ApplicationMappingContext applicationMappingContext);

  @Mapping(target = "id", source = "soaCaseDetail.applicationDetails.applicationAmendmentType")
  @Mapping(target = "displayValue", source = "applicationType.description")
  @Mapping(target = "devolvedPowers", source = "devolvedPowers")
  ApplicationType toApplicationType(ApplicationMappingContext applicationMappingContext);

  @Mapping(target = ".", source = "soaCaseDetail.applicationDetails.correspondenceAddress")
  @Mapping(target = "careOf",
      source = "soaCaseDetail.applicationDetails.correspondenceAddress.careOfName")
  @Mapping(target = "houseNameOrNumber",
      source = "soaCaseDetail.applicationDetails.correspondenceAddress.house")
  @Mapping(target = "postcode",
      source = "soaCaseDetail.applicationDetails.correspondenceAddress.postalCode")
  @Mapping(target = "noFixedAbode", constant = "false")
  @Mapping(target = "preferredAddress",
      source = "soaCaseDetail.applicationDetails.preferredAddress")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  AddressDetail toCorrespondenceAddress(ApplicationMappingContext mappingContext);


  @Mapping(target = "edited", constant = "false")
  @Mapping(target = ".", source = "soaProceeding")
  @Mapping(target = "ebsId", source = "soaProceeding.proceedingCaseId")
  @Mapping(target = "leadProceedingInd", source = "soaProceeding.leadProceedingIndicator")
  @Mapping(target = "proceedingType", source = "proceedingLookup")
  @Mapping(target = "matterType", source = "matterType")
  @Mapping(target = "levelOfService", source = "levelOfService")
  @Mapping(target = "larScope", source = "proceedingLookup.larScope")
  @Mapping(target = "description", source = "soaProceeding.proceedingDescription")
  @Mapping(target = "status", source = "proceedingStatusLookup")
  @Mapping(target = "typeOfOrder", source = "soaProceeding.orderType")
  @Mapping(target = "costLimitation", source = "proceedingCostLimitation")
  @Mapping(target = "outcome", source = "proceedingContext",
      conditionExpression = "java(proceedingContext.getSoaProceeding().getOutcome() != null)")
  @Mapping(target = "scopeLimitations", source = "scopeLimitations")
  @Mapping(target = "defaultScopeLimitation", ignore = true)
  @Mapping(target = "grantedUsingDevolvedPowers", ignore = true)
  @Mapping(target = "orderTypeReqFlag", ignore = true)
  @Mapping(target = "orderTypeDisplayFlag", ignore = true)
  @Mapping(target = "deleteScopeLimitationFlag", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "id", ignore = true)
  ProceedingDetail toProceeding(ProceedingMappingContext proceedingContext);

  @Mapping(target = "grantedCostLimitation",
      source = "soaCaseDetail.applicationDetails.categoryOfLaw.grantedAmount")
  @Mapping(target = "requestedCostLimitation",
      source = "soaCaseDetail.applicationDetails.categoryOfLaw.requestedAmount")
  @Mapping(target = "defaultCostLimitation", ignore = true)
  @Mapping(target = "costEntries",
      source = "soaCaseDetail.applicationDetails.categoryOfLaw.costLimitations")
  @Mapping(target = "currentProviderBilledAmount", source = "currentProviderBilledAmount")
  @Mapping(target = "auditTrail", ignore = true)
  CostStructureDetail toCostStructure(ApplicationMappingContext mappingContext);

  @Mapping(target = "lscResourceId", source = "billingProviderId")
  @Mapping(target = "amountBilled", source = "paidToDate")
  @Mapping(target = "requestedCosts", source = "amount")
  @Mapping(target = "resourceName", source = "billingProviderName")
  @Mapping(target = "ebsId", source = "costLimitId")
  @Mapping(target = "newEntry", constant = "false")
  @Mapping(target = "submitted", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  CostEntryDetail toCostEntry(CostLimitation costLimitation);

  @Mapping(target = "ebsId", source = "key.scopeLimitationId")
  @Mapping(target = "scopeLimitation", source = "value")
  @Mapping(target = "scopeLimitationWording", source = "key.scopeLimitationWording")
  @Mapping(target = "delegatedFuncApplyInd", source = "key.delegatedFunctionsApply")
  @Mapping(target = "defaultInd", ignore = true)
  @Mapping(target = "nonDefaultWordingReqd", ignore = true)
  @Mapping(target = "stage", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "id", ignore = true)
  ScopeLimitationDetail toScopeLimitation(
      Pair<ScopeLimitation, CommonLookupValueDetail> scopeLimitation);

  @Mapping(target = "description", source = "soaProceeding.proceedingDescription")
  @Mapping(target = "proceedingCaseId", source = "soaProceeding.proceedingCaseId")
  @Mapping(target = "proceedingType", source = "proceedingLookup")
  @Mapping(target = "adrInfo", source = "soaProceeding.outcome.altAcceptanceReason")
  @Mapping(target = "alternativeResolution", source = "soaProceeding.outcome.altDisputeResolution")
  @Mapping(target = "courtCode", source = "courtLookup.code")
  @Mapping(target = "courtName", source = "courtLookup.description")
  @Mapping(target = "dateOfFinalWork", source = "soaProceeding.outcome.finalWorkDate")
  @Mapping(target = "dateOfIssue", ignore = true)
  @Mapping(target = "resolutionMethod", source = "soaProceeding.outcome.resolutionMethod")
  @Mapping(target = "result", source = "outcomeResultLookup")
  @Mapping(target = "resultInfo", source = "soaProceeding.outcome.additionalResultInfo")
  @Mapping(target = "stageEnd", source = "stageEndLookup")
  @Mapping(target = "widerBenefits", source = "soaProceeding.outcome.widerBenefits")
  @Mapping(target = "outcomeCourtCaseNo", source = "soaProceeding.outcome.outcomeCourtCaseNumber")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  ProceedingOutcomeDetail toProceedingOutcome(ProceedingMappingContext proceedingContext);

  AssessmentResult toAssessmentResult(
      uk.gov.laa.ccms.soa.gateway.model.AssessmentResult assessmentResult);

  @Mapping(target = "careOf", source = "careOfName")
  @Mapping(target = "houseNameOrNumber", source = "house")
  @Mapping(target = "postcode", source = "postalCode")
  @Mapping(target = "noFixedAbode", constant = "false")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "preferredAddress", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  AddressDetail toAddress(uk.gov.laa.ccms.soa.gateway.model.AddressDetail soaAddress);

  @Mapping(target = "reference", source = "clientReferenceNumber")
  ClientDetail toClient(BaseClient soaClient);

  @Mapping(target = "type", constant = OPPONENT_TYPE_INDIVIDUAL)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "ebsId", source = "otherPartyId")
  @Mapping(target = ".", source = "person")
  @Mapping(target = "courtOrderedMeansAssessment", source = "person.courtOrderedMeansAssesment")
  @Mapping(target = "employerAddress", source = "person.organizationAddress")
  @Mapping(target = "employerName", source = "person.organizationName")
  @Mapping(target = ".", source = "person.name")
  @Mapping(target = "middleNames", source = "person.name.middleName")
  @Mapping(target = "legalAided", source = "person.partyLegalAidedInd")
  @Mapping(target = "nationalInsuranceNumber", source = "person.niNumber")
  @Mapping(target = "relationshipToCase", source = "person.relationToCase")
  @Mapping(target = "relationshipToClient", source = "person.relationToClient")
  @Mapping(target = ".", source = "person.contactDetails")
  @Mapping(target = "telephoneMobile", source = "person.contactDetails.mobileNumber")
  @Mapping(target = "faxNumber", source = "person.contactDetails.fax")
  @Mapping(target = "publicFundingApplied", source = "person.publicFundingAppliedInd")
  @Mapping(target = "deleteInd", constant = "false")
  @Mapping(target = "amendment", ignore = true)
  @Mapping(target = "appMode", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "award", ignore = true)
  @Mapping(target = "confirmed", ignore = true)
  @Mapping(target = "contactNameRole", ignore = true)
  @Mapping(target = "currentlyTrading", ignore = true)
  @Mapping(target = "displayAddress", ignore = true)
  @Mapping(target = "displayName", ignore = true)
  @Mapping(target = "organisationName", ignore = true)
  @Mapping(target = "organisationType", ignore = true)
  @Mapping(target = "partyId", ignore = true)
  OpponentDetail toIndividualOpponent(OtherParty otherParty);


  @Mapping(target = "type", constant = OPPONENT_TYPE_ORGANISATION)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "ebsId", source = "otherPartyId")
  @Mapping(target = ".", source = "organisation")
  @Mapping(target = "address", source = "organisation.address")
  @Mapping(target = "organisationName", source = "organisation.organizationName")
  @Mapping(target = "organisationType", source = "organisation.organizationType")
  @Mapping(target = "contactNameRole", source = "organisation.contactName")
  @Mapping(target = "relationshipToCase", source = "organisation.relationToCase")
  @Mapping(target = "relationshipToClient", source = "organisation.relationToClient")
  @Mapping(target = ".", source = "organisation.contactDetails")
  @Mapping(target = "telephoneMobile", source = "organisation.contactDetails.mobileNumber")
  @Mapping(target = "faxNumber", source = "organisation.contactDetails.fax")
  @Mapping(target = "deleteInd", constant = "false")
  @Mapping(target = "amendment", ignore = true)
  @Mapping(target = "appMode", ignore = true)
  @Mapping(target = "assessedAssets", ignore = true)
  @Mapping(target = "assessedIncome", ignore = true)
  @Mapping(target = "assessedIncomeFrequency", ignore = true)
  @Mapping(target = "assessmentDate", ignore = true)
  @Mapping(target = "title", ignore = true)
  @Mapping(target = "surname", ignore = true)
  @Mapping(target = "publicFundingApplied", ignore = true)
  @Mapping(target = "partyId", ignore = true)
  @Mapping(target = "nationalInsuranceNumber", ignore = true)
  @Mapping(target = "middleNames", ignore = true)
  @Mapping(target = "legalAided", ignore = true)
  @Mapping(target = "firstName", ignore = true)
  @Mapping(target = "employmentStatus", ignore = true)
  @Mapping(target = "employerAddress", ignore = true)
  @Mapping(target = "employerName", ignore = true)
  @Mapping(target = "displayName", ignore = true)
  @Mapping(target = "displayAddress", ignore = true)
  @Mapping(target = "dateOfBirth", ignore = true)
  @Mapping(target = "courtOrderedMeansAssessment", ignore = true)
  @Mapping(target = "confirmed", ignore = true)
  @Mapping(target = "certificateNumber", ignore = true)
  @Mapping(target = "award", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  OpponentDetail toOrganisationOpponent(OtherParty otherParty);

  @Mapping(target = "lscCaseReference", source = "caseReferenceNumber")
  @Mapping(target = "categoryOfLaw", source = "categoryOfLawDesc")
  @Mapping(target = "providerCaseReference", source = "providerReferenceNumber")
  @Mapping(target = "feeEarner", source = "feeEarnerName")
  @Mapping(target = "status", source = "caseStatus")
  @Mapping(target = "relationToCase", source = "linkType")
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "id", ignore = true)
  LinkedCaseDetail toLinkedCase(LinkedCase soaLinkedCase);

  @Mapping(target = "status", source = "soaPriorAuthority.decisionStatus")
  @Mapping(target = "summary", source = "soaPriorAuthority.description")
  @Mapping(target = "type", source = "priorAuthorityTypeLookup")
  @Mapping(target = "justification", source = "soaPriorAuthority.reasonForRequest")
  @Mapping(target = "amountRequested", source = "soaPriorAuthority.requestAmount")
  @Mapping(target = "valueRequired", source = "priorAuthorityTypeLookup.valueRequired")
  @Mapping(target = "ebsId", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "id", ignore = true)
  PriorAuthorityDetail toPriorAuthority(PriorAuthorityMappingContext priorAuthorityMappingContext);

  @Mapping(target = "code.id", source = "key.code")
  @Mapping(target = "code.displayValue", source = "key.description")
  @Mapping(target = "type", source = "key.dataType")
  @Mapping(target = "lovLookUp", source = "key.lovCode")
  @Mapping(target = "mandatory", source = "key.mandatoryFlag")
  @Mapping(target = "value", source = "value")
  @Mapping(target = "id", ignore = true)
  ReferenceDataItemDetail toReferenceDataItem(Pair<uk.gov.laa.ccms.data.model.PriorAuthorityDetail,
      CommonLookupValueDetail> priorAuthorityDetail);

  @Mapping(target = ".", source = "soaCase")
  @Mapping(target = "legalCosts", source = "soaCase.legalHelpCosts")
  @Mapping(target = "officeCode",
      source = "soaCase.applicationDetails.larDetails.legalHelpOfficeCode")
  @Mapping(target = "uniqueFileNo", source = "soaCase.applicationDetails.larDetails.legalHelpUfn")
  @Mapping(target = "otherDetails", source = "soaCase.dischargeStatus.otherDetails")
  @Mapping(target = "dischargeReason", source = "soaCase.dischargeStatus.reason")
  @Mapping(target = "clientContinueInd", source = "soaCase.dischargeStatus.clientContinuePvtInd")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "dischargeCaseInd", ignore = true)
  @Mapping(target = "providerId", ignore = true)
  @Mapping(target = "opponentIds", ignore = true)
  @Mapping(target = "caseReferenceNumber", ignore = true)
  CaseOutcomeDetail toCaseOutcome(CaseOutcomeMappingContext caseOutcome);

  @Mapping(target = "ebsId", source = "awardId")
  @Mapping(target = "awardType", constant = AWARD_TYPE_COST)
  @Mapping(target = "description", constant = AWARD_TYPE_COST_DESCRIPTION)
  @Mapping(target = "awardCode", source = "awardType")
  @Mapping(target = ".", source = "costAward")
  @Mapping(target = "dateOfOrder", source = "costAward.orderDate")
  @Mapping(target = "preCertificateLscCost",
      source = "costAward.preCertificateAwardLsc", defaultValue = "0.00")
  @Mapping(target = "preCertificateOtherCost",
      source = "costAward.preCertificateAwardOth", defaultValue = "0.00")
  @Mapping(target = "certificateCostLsc",
      source = "costAward.certificateCostRateLsc", defaultValue = "0.00")
  @Mapping(target = "certificateCostMarket",
      source = "costAward.certificateCostRateMarket", defaultValue = "0.00")
  @Mapping(target = "orderServedDate", source = "costAward.orderDateServed")
  @Mapping(target = "interestStartDate", source = "costAward.interestAwardedStartDate")
  @Mapping(target = "addressLine1", source = "costAward.serviceAddress.addressLine1")
  @Mapping(target = "addressLine2", source = "costAward.serviceAddress.addressLine2")
  @Mapping(target = "addressLine3", source = "costAward.serviceAddress.addressLine3")
  @Mapping(target = "recovery", source = "costAward.recovery")
  @Mapping(target = "liableParties", source = "costAward.liableParties")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "opponentsToSelect", ignore = true)
  @Mapping(target = "costOrFinancial", ignore = true)
  @Mapping(target = "effectiveDate", ignore = true)
  @Mapping(target = "totalCertCostsAwarded", ignore = true)
  @Mapping(target = "awardAmount", ignore = true)
  CostAwardDetail toCostAward(Award soaAward);

  /**
   * AfterMapping method to finalise a CostAwardDetail.
   *
   * @param costAward - the target for extra mapping.
   */
  @AfterMapping
  default void finaliseCostAward(@MappingTarget final CostAwardDetail costAward) {
    if (costAward.getRecovery() != null) {
      costAward.getRecovery().setAwardType(AWARD_TYPE_COST);
      costAward.getRecovery().setDescription(AWARD_TYPE_COST_DESCRIPTION);
    }

    // Calculate the total costs awarded by summing LSC and Market
    costAward.setTotalCertCostsAwarded(costAward.getCertificateCostLsc()
        .add(costAward.getCertificateCostMarket()));
  }

  @Mapping(target = "ebsId", source = "awardId")
  @Mapping(target = "awardType", constant = AWARD_TYPE_FINANCIAL)
  @Mapping(target = "description", constant = AWARD_TYPE_FINANCIAL_DESCRIPTION)
  @Mapping(target = "awardCode", source = "awardType")
  @Mapping(target = ".", source = "financialAward")
  @Mapping(target = "dateOfOrder", source = "financialAward.orderDate")
  @Mapping(target = "awardAmount", source = "financialAward.amount")
  @Mapping(target = "orderServedDate", source = "financialAward.orderDateServed")
  @Mapping(target = "statutoryChargeExemptReason",
      source = "financialAward.statutoryChangeReason")
  @Mapping(target = "addressLine1", source = "financialAward.serviceAddress.addressLine1")
  @Mapping(target = "addressLine2", source = "financialAward.serviceAddress.addressLine2")
  @Mapping(target = "addressLine3", source = "financialAward.serviceAddress.addressLine3")
  @Mapping(target = "recovery", source = "financialAward.recovery")
  @Mapping(target = "liableParties", source = "financialAward.liableParties")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "opponentsToSelect", ignore = true)
  @Mapping(target = "costOrFinancial", ignore = true)
  @Mapping(target = "effectiveDate", ignore = true)
  FinancialAwardDetail toFinancialAward(Award soaAward);

  /**
   * AfterMapping method to finalise a FinancialAwardDetail.
   *
   * @param financialAward - the target for extra mapping.
   */
  @AfterMapping
  default void finaliseFinancialAward(@MappingTarget final FinancialAwardDetail financialAward) {
    if (financialAward.getRecovery() != null) {
      financialAward.getRecovery().setAwardType(AWARD_TYPE_FINANCIAL);
      financialAward.getRecovery().setDescription(AWARD_TYPE_FINANCIAL_DESCRIPTION);
    }
  }

  @Mapping(target = "awardCode", source = "awardType")
  @Mapping(target = "ebsId", source = "awardId")
  @Mapping(target = "dateOfOrder", source = "landAward.orderDate")
  @Mapping(target = "awardType", constant = AWARD_TYPE_LAND)
  @Mapping(target = ".", source = "landAward")
  @Mapping(target = "addressLine1", source = "landAward.propertyAddress.addressLine1")
  @Mapping(target = "addressLine2", source = "landAward.propertyAddress.addressLine2")
  @Mapping(target = "addressLine3", source = "landAward.propertyAddress.addressLine3")
  @Mapping(target = "disputedPercentage",
      source = "landAward.disputedPercentage", defaultValue = "0.00")
  @Mapping(target = "awardedPercentage",
      source = "landAward.awardedPercentage", defaultValue = "0.00")
  @Mapping(target = "mortgageAmountDue",
      source = "landAward.mortgageAmountDue", defaultValue = "0.00")
  @Mapping(target = "valuationAmount", source = "landAward.valuation.amount", defaultValue = "0.00")
  @Mapping(target = "valuationCriteria", source = "landAward.valuation.criteria")
  @Mapping(target = "valuationDate", source = "landAward.valuation.date")
  @Mapping(target = "timeRecovery", source = "landAward.timeRelatedAward")
  @Mapping(target = "liableParties", source = "landAward.otherProprietors")
  @Mapping(target = "statutoryChargeExemptReason", source = "landAward.statChargeExemptReason")
  @Mapping(target = "titleNumber", source = "landAward.titleNo")
  @Mapping(target = "registrationReference", source = "landAward.registrationRef")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "equity", ignore = true)
  @Mapping(target = "awardAmount", ignore = true)
  @Mapping(target = "opponentsToSelect", ignore = true)
  @Mapping(target = "costOrFinancial", ignore = true)
  @Mapping(target = "effectiveDate", ignore = true)
  @Mapping(target = "recoveryOfAwardTimeRelated", ignore = true)
  LandAwardDetail toLandAward(Award soaAward);

  /**
   * Complete the mapping for a LandAwardDetail.
   *
   * @param landAward - the mapping target
   */
  @AfterMapping
  default void finaliseLandAward(@MappingTarget final LandAwardDetail landAward) {
    final TimeRecoveryDetail timeRecovery = landAward.getTimeRecovery();
    landAward.setRecoveryOfAwardTimeRelated(timeRecovery != null);
    if (timeRecovery != null) {
      timeRecovery.setAwardType(AWARD_TYPE_LAND);
    }

    // Initialise the equity value based on Valuation Amount vs Mortgage Amount Due
    landAward.setEquity(landAward.getValuationAmount().subtract(landAward.getMortgageAmountDue()));
  }

  @Mapping(target = "ebsId", source = "awardId")
  @Mapping(target = "dateOfOrder", source = "otherAsset.orderDate")
  @Mapping(target = "awardType", constant = AWARD_TYPE_OTHER_ASSET)
  @Mapping(target = "awardCode", source = "awardType")
  @Mapping(target = ".", source = "otherAsset")
  @Mapping(target = "valuationAmount",
      source = "otherAsset.valuation.amount", defaultValue = "0.00")
  @Mapping(target = "valuationCriteria", source = "otherAsset.valuation.criteria")
  @Mapping(target = "valuationDate", source = "otherAsset.valuation.date")
  @Mapping(target = "timeRecovery", source = "otherAsset.timeRelatedAward")
  @Mapping(target = "liableParties", source = "otherAsset.heldBy")
  @Mapping(target = "statutoryChargeExemptReason", source = "otherAsset.statChargeExemptReason")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "awardAmount", ignore = true)
  @Mapping(target = "costOrFinancial", ignore = true)
  @Mapping(target = "effectiveDate", ignore = true)
  @Mapping(target = "opponentsToSelect", ignore = true)
  @Mapping(target = "recoveryOfAwardTimeRelated", ignore = true)
  OtherAssetAwardDetail toOtherAssetAward(Award soaAward);

  /**
   * Complete the mapping for an OtherAssetAwardDetail.
   *
   * @param otherAssetAward - the mapping target
   */
  @AfterMapping
  default void finaliseOtherAssetAward(@MappingTarget final OtherAssetAwardDetail otherAssetAward) {
    final TimeRecoveryDetail timeRecovery = otherAssetAward.getTimeRecovery();
    otherAssetAward.setRecoveryOfAwardTimeRelated(timeRecovery != null);
    if (timeRecovery != null) {
      timeRecovery.setAwardType(AWARD_TYPE_OTHER_ASSET);
    }
  }

  /**
   * Complete the mapping for a BaseAwardDetail.
   *
   * @param baseAward - the mapping target
   */
  @AfterMapping
  default void finaliseAward(@MappingTarget final BaseAwardDetail baseAward) {
    baseAward.getLiableParties().forEach(
        liableParty -> liableParty.setAwardType(baseAward.getAwardType()));
  }

  @Mapping(target = "opponentId", source = ".")
  @Mapping(target = "awardType", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  LiablePartyDetail toLiableParty(String partyId);

  @Mapping(target = "awardAmount", source = "awardValue", defaultValue = "0.00")
  @Mapping(target = "clientAmountPaidToLsc",
      source = "recoveredAmount.client.paidToLsc", defaultValue = "0.00")
  @Mapping(target = "clientRecoveryAmount",
      source = "recoveredAmount.client.amount", defaultValue = "0.00")
  @Mapping(target = "clientRecoveryDate", source = "recoveredAmount.client.dateReceived")
  @Mapping(target = "courtAmountPaidToLsc",
      source = "recoveredAmount.court.paidToLsc", defaultValue = "0.00")
  @Mapping(target = "courtRecoveryAmount",
      source = "recoveredAmount.court.amount", defaultValue = "0.00")
  @Mapping(target = "courtRecoveryDate", source = "recoveredAmount.court.dateReceived")
  @Mapping(target = "solicitorAmountPaidToLsc",
      source = "recoveredAmount.solicitor.paidToLsc", defaultValue = "0.00")
  @Mapping(target = "solicitorRecoveryAmount",
      source = "recoveredAmount.solicitor.amount", defaultValue = "0.00")
  @Mapping(target = "solicitorRecoveryDate", source = "recoveredAmount.solicitor.dateReceived")
  @Mapping(target = "offeredAmount", source = "offeredAmount.amount", defaultValue = "0.00")
  @Mapping(target = "conditionsOfOffer", source = "offeredAmount.conditionsOfOffer")
  @Mapping(target = "offerDetails", source = "offeredAmount.conditionsOfOffer")
  @Mapping(target = "leaveOfCourtRequiredInd", source = "leaveOfCourtReqdInd")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "unrecoveredAmount", ignore = true)
  @Mapping(target = "recoveredAmount", ignore = true)
  @Mapping(target = "awardType", ignore = true)
  @Mapping(target = "description", ignore = true)
  RecoveryDetail toRecovery(Recovery soaRecovery);

  /**
   * Perform after-mapping for a RecoveryDetail.
   *
   * @param recovery - the mapping target
   */
  @AfterMapping
  default void finaliseRecovery(@MappingTarget final RecoveryDetail recovery) {
    // Set the total recovered amount to be a sum of solicitor, court and client.
    recovery.setRecoveredAmount(
        recovery.getSolicitorRecoveryAmount()
            .add(recovery.getCourtRecoveryAmount())
            .add(recovery.getClientRecoveryAmount()));

    // Now set the unrecovered amount accordingly
    recovery.setUnrecoveredAmount(
        recovery.getAwardAmount().subtract(recovery.getRecoveredAmount()));
  }

  @Mapping(target = "awardAmount", source = "amount")
  @Mapping(target = "triggeringEvent", source = "awardTriggeringEvent")
  @Mapping(target = "effectiveDate", source = "awardDate")
  @Mapping(target = "timeRelatedRecoveryDetails", source = "otherDetails")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  TimeRecoveryDetail toTimeRecovery(TimeRelatedAward timeRelatedAward);

  @Mapping(target = "id", source = "id")
  @Mapping(target = "displayValue", source = "name")
  IntDisplayValue toIntDisplayValue(OfficeDetail officeDetail);

  @Mapping(target = "id", source = "id")
  @Mapping(target = "displayValue", source = "name")
  IntDisplayValue toIntDisplayValue(ProviderDetail providerDetail);

  @Mapping(target = "id", source = "code")
  @Mapping(target = "displayValue", source = "code")
  StringDisplayValue toStringDisplayValue(String code);

  @Mapping(target = "id", source = "code")
  @Mapping(target = "displayValue", source = "description")
  StringDisplayValue toStringDisplayValue(CommonLookupValueDetail commonLookupValueDetail);

  @Mapping(target = "id", source = "id")
  @Mapping(target = "displayValue", source = "name")
  StringDisplayValue toStringDisplayValue(ContactDetail contactDetail);

  @Mapping(target = "id", source = "userLoginId")
  @Mapping(target = "displayValue", source = "userName")
  StringDisplayValue toStringDisplayValue(UserDetail userDetail);

  @Mapping(target = "id", source = "actualCaseStatus")
  @Mapping(target = "displayValue", source = "displayCaseStatus")
  StringDisplayValue toStringDisplayValue(CaseStatus caseStatus);

  @Mapping(target = "id", source = "categoryOfLawCode")
  @Mapping(target = "displayValue", source = "categoryOfLawDescription")
  StringDisplayValue toStringDisplayValue(CategoryOfLaw categoryOfLaw);

  @Mapping(target = "id", source = "code")
  @Mapping(target = "displayValue", source = "description")
  StringDisplayValue toStringDisplayValue(PriorAuthorityTypeDetail priorAuthorityTypeDetail);

  @Mapping(target = "id", source = "code")
  @Mapping(target = "displayValue", source = "name")
  StringDisplayValue toStringDisplayValue(
      uk.gov.laa.ccms.data.model.ProceedingDetail proceedingDetail);

  @Mapping(target = "id", source = "outcomeResult")
  @Mapping(target = "displayValue", source = "outcomeResultDescription")
  StringDisplayValue toStringDisplayValue(
      OutcomeResultLookupValueDetail outcomeResultLookupValueDetail);

  @Mapping(target = "id", source = "stageEnd")
  @Mapping(target = "displayValue", source = "description")
  StringDisplayValue toStringDisplayValue(
      StageEndLookupValueDetail stageEndLookupValueDetail);

  @Mapping(target = "flag", source = "flag")
  @Mapping(target = "displayValue", source = "flag")
  BooleanDisplayValue toBooleanDisplayValue(Boolean flag);

  @Mapping(target = "used", source = "key")
  @Mapping(target = "dateUsed", source = "value")
  @Mapping(target = "contractFlag", ignore = true)
  DevolvedPowersDetail toDevolvedPowers(Pair<Boolean, Date> devolvedPowersInfo);

  /**
   * Map a List of OtherParty conditionally to an Individual or Organisation OpponentDetail.
   *
   * @param otherParties - List of OtherParty.
   * @return Mapped List of OpponentDetail.
   */
  default List<OpponentDetail> convertOpponents(final List<OtherParty> otherParties) {
    return otherParties != null ? otherParties.stream()
        .map(otherParty -> otherParty.getPerson() != null
            ? toIndividualOpponent(otherParty) :
            toOrganisationOpponent(otherParty))
        .toList() : null;
  }
}
