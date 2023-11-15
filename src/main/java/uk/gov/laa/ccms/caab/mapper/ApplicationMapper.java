package uk.gov.laa.ccms.caab.mapper;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST_DESCRIPTION;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL_DESCRIPTION;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_LAND;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_OTHER_ASSET;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_INDIVIDUAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_ORGANISATION;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.laa.ccms.caab.model.Address;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.AssessmentResult;
import uk.gov.laa.ccms.caab.model.BaseAward;
import uk.gov.laa.ccms.caab.model.CaseOutcome;
import uk.gov.laa.ccms.caab.model.Client;
import uk.gov.laa.ccms.caab.model.CostAward;
import uk.gov.laa.ccms.caab.model.CostEntry;
import uk.gov.laa.ccms.caab.model.FinancialAward;
import uk.gov.laa.ccms.caab.model.LandAward;
import uk.gov.laa.ccms.caab.model.LiableParty;
import uk.gov.laa.ccms.caab.model.LinkedCase;
import uk.gov.laa.ccms.caab.model.Opponent;
import uk.gov.laa.ccms.caab.model.OtherAssetAward;
import uk.gov.laa.ccms.caab.model.PriorAuthority;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ProceedingOutcome;
import uk.gov.laa.ccms.caab.model.Recovery;
import uk.gov.laa.ccms.caab.model.ReferenceDataItem;
import uk.gov.laa.ccms.caab.model.ScopeLimitation;
import uk.gov.laa.ccms.caab.model.TimeRecovery;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.ProceedingDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;
import uk.gov.laa.ccms.soa.gateway.model.AddressDetail;
import uk.gov.laa.ccms.soa.gateway.model.Award;
import uk.gov.laa.ccms.soa.gateway.model.BaseClient;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;
import uk.gov.laa.ccms.soa.gateway.model.CostLimitation;
import uk.gov.laa.ccms.soa.gateway.model.OtherParty;
import uk.gov.laa.ccms.soa.gateway.model.OutcomeDetail;
import uk.gov.laa.ccms.soa.gateway.model.TimeRelatedAward;

/**
 * Mapper class to convert a SoaApi Case into a CAAB ApplicationDetail.
 */
@Mapper(componentModel = "spring")
public interface ApplicationMapper {
  
  @Mapping(target = "caseReferenceNumber", source = "soaCaseDetail.caseReferenceNumber")
  @Mapping(target = "certificate.id", source = "soaCaseDetail.certificateType")
  @Mapping(target = "applicationType.id",
      source = "soaCaseDetail.applicationDetails.applicationAmendmentType")
  @Mapping(target = "applicationType.displayValue", source = "applicationTypeLookup.description")
  @Mapping(target = "dateCreated", source = "soaCaseDetail.recordHistory.dateCreated")
  @Mapping(target = "providerCaseReference",
      source = "soaCaseDetail.applicationDetails.providerDetails.providerCaseReferenceNumber")
  @Mapping(target = "provider.id",
      source = "soaCaseDetail.applicationDetails.providerDetails.providerFirmId")
  @Mapping(target = "provider.displayValue", source = "ebsProvider.name")
  @Mapping(target = "providerContact.id",
      source = "soaCaseDetail.applicationDetails.providerDetails.contactUserId.userLoginId")
  @Mapping(target = "providerContact.displayValue",
      source = "soaCaseDetail.applicationDetails.providerDetails.contactUserId.userName")
  @Mapping(target = "office.id", source = "providerOffice.id")
  @Mapping(target = "office.displayValue", source = "providerOffice.name")
  @Mapping(target = "supervisor.id", source = "supervisorContact.id")
  @Mapping(target = "supervisor.displayValue", source = "supervisorContact.name")
  @Mapping(target = "feeEarner.id", source = "feeEarnerContact.id")
  @Mapping(target = "feeEarner.displayValue", source = "feeEarnerContact.name")
  @Mapping(target = "correspondenceAddress",
      source = "soaCaseDetail.applicationDetails.correspondenceAddress")
  @Mapping(target = "client", source = "soaCaseDetail.applicationDetails.client")
  @Mapping(target = "categoryOfLaw.id",
      source = "soaCaseDetail.applicationDetails.categoryOfLaw.categoryOfLawCode")
  @Mapping(target = "categoryOfLaw.displayValue",
      source = "soaCaseDetail.applicationDetails.categoryOfLaw.categoryOfLawDescription")
  @Mapping(target = "costs.grantedCostLimitation",
      source = "soaCaseDetail.applicationDetails.categoryOfLaw.grantedAmount")
  @Mapping(target = "costs.requestedCostLimitation",
      source = "soaCaseDetail.applicationDetails.categoryOfLaw.requestedAmount")
  @Mapping(target = "costs.defaultCostLimitation", ignore = true)
  @Mapping(target = "costs.costEntries",
      source = "soaCaseDetail.applicationDetails.categoryOfLaw.costLimitations")
  @Mapping(target = "costs.currentProviderBilledAmount", ignore = true)
  @Mapping(target = "costs.auditTrail", ignore = true)
  @Mapping(target = "larScopeFlag",
      source = "soaCaseDetail.applicationDetails.larDetails.larScopeFlag", defaultValue = "false")
  @Mapping(target = "status.id", source = "soaCaseDetail.caseStatus.actualCaseStatus")
  @Mapping(target = "status.displayValue", source = "soaCaseDetail.caseStatus.displayCaseStatus")
  @Mapping(target = "availableFunctions", source = "soaCaseDetail.availableFunctions")
  @Mapping(target = "priorAuthorities", ignore = true)
  @Mapping(target = "submitted", ignore = true)
  @Mapping(target = "relationToLinkedCase", ignore = true)
  @Mapping(target = "quickEditType", ignore = true)
  @Mapping(target = "proceedings", ignore = true)
  @Mapping(target = "opponents", ignore = true)
  @Mapping(target = "opponentMode", ignore = true)
  @Mapping(target = "opponentAppliedForFunding", ignore = true)
  @Mapping(target = "meritsReassessmentRequired", ignore = true)
  @Mapping(target = "meritsAssessmentStatus", ignore = true)
  @Mapping(target = "meritsAssessmentAmended", ignore = true)
  @Mapping(target = "meritsAssessment", ignore = true)
  @Mapping(target = "meansAssessmentStatus", ignore = true)
  @Mapping(target = "meansAssessmentAmended", ignore = true)
  @Mapping(target = "meansAssessment", ignore = true)
  @Mapping(target = "leadProceedingChanged", ignore = true)
  @Mapping(target = "editProceedingsAndCostsAllowed", ignore = true)
  @Mapping(target = "costLimit", ignore = true)
  @Mapping(target = "caseOutcome", ignore = true)
  @Mapping(target = "award", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "appMode", ignore = true)
  @Mapping(target = "amendmentProceedingsInEbs", ignore = true)
  @Mapping(target = "amendment", ignore = true)
  @Mapping(target = "allSectionsComplete", ignore = true)
  ApplicationDetail toApplicationDetail(
      CaseDetail soaCaseDetail,
      CommonLookupValueDetail applicationTypeLookup,
      uk.gov.laa.ccms.data.model.ProviderDetail ebsProvider,
      OfficeDetail providerOffice,
      ContactDetail supervisorContact,
      ContactDetail feeEarnerContact);

  /**
   * AfterMapping method to finalise the ApplicationDetail.
   *
   * @param applicationDetail - the mapping target
   * @param soaCase - the source CaseDetail
   */
  @AfterMapping
  default void finaliseApplicationDetails(@MappingTarget ApplicationDetail applicationDetail,
      CaseDetail soaCase) {

    if (soaCase.getApplicationDetails() != null) {
      if (applicationDetail.getCorrespondenceAddress() != null) {
        applicationDetail.getCorrespondenceAddress().setPreferredAddress(
            soaCase.getApplicationDetails().getPreferredAddress());
      }
    }
  }

  @Mapping(target = "lscResourceId", source = "billingProviderId")
  @Mapping(target = "amountBilled", source = "paidToDate")
  @Mapping(target = "requestedCosts", source = "amount")
  @Mapping(target = "resourceName", source = "billingProviderName")
  @Mapping(target = "ebsId", source = "costLimitId")
  @Mapping(target = "newEntry", constant = "false")
  @Mapping(target = "submitted", ignore = true)
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
  @Mapping(target = "clientInvolvement.displayValue",
      source = "clientInvolvementLookup.description")
  @Mapping(target = "status.id", source = "proceedingStatusLookup.code")
  @Mapping(target = "status.displayValue", source = "proceedingStatusLookup.description")
  @Mapping(target = "typeOfOrder.id", source = "soaProceeding.orderType")
  @Mapping(target = "scopeLimitations", ignore = true)
  @Mapping(target = "outcome", ignore = true)
  @Mapping(target = "costLimitation", ignore = true)
  @Mapping(target = "defaultScopeLimitation", ignore = true)
  @Mapping(target = "grantedUsingDevolvedPowers", ignore = true)
  @Mapping(target = "orderTypeReqFlag", ignore = true)
  @Mapping(target = "orderTypeDisplayFlag", ignore = true)
  @Mapping(target = "deleteScopeLimitationFlag", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  Proceeding toProceeding(uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail soaProceeding,
      ProceedingDetail proceedingLookup,
      CommonLookupValueDetail matterTypeLookup,
      CommonLookupValueDetail levelOfServiceLookup,
      CommonLookupValueDetail clientInvolvementLookup,
      CommonLookupValueDetail proceedingStatusLookup);

  @Mapping(target = "ebsId", source = "soaScopeLimitation.scopeLimitationId")
  @Mapping(target = "scopeLimitation.id", source = "scopeLimitationLookup.code")
  @Mapping(target = "scopeLimitation.displayValue", source = "scopeLimitationLookup.description")
  @Mapping(
      target = "scopeLimitationWording", source = "soaScopeLimitation.scopeLimitationWording")
  @Mapping(
      target = "delegatedFuncApplyInd.flag", source = "soaScopeLimitation.delegatedFunctionsApply")
  @Mapping(target = "defaultInd", ignore = true)
  @Mapping(target = "nonDefaultWordingReqd", ignore = true)
  @Mapping(target = "stage", ignore = true)
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
  @Mapping(target = "dateOfIssue", ignore = true)
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

  AssessmentResult toAssessmentResult(
      uk.gov.laa.ccms.soa.gateway.model.AssessmentResult assessmentResult);

  @Mapping(target = "careOf", source = "careOfName")
  @Mapping(target = "houseNameOrNumber", source = "house")
  @Mapping(target = "postcode", source = "postalCode")
  @Mapping(target = "noFixedAbode", constant = "false")
  @Mapping(target = "preferredAddress", ignore = true)
  Address toAddress(AddressDetail soaAddress);

  @Mapping(target = "reference", source = "clientReferenceNumber")
  Client toClient(BaseClient soaClient);

  @Mapping(target = "type", constant = OPPONENT_TYPE_INDIVIDUAL)
  @Mapping(target = "ebsId", source = "otherPartyId")
  @Mapping(target = ".", source = "person")
  @Mapping(target = "courtOrderedMeansAssessment", source = "person.courtOrderedMeansAssesment")
  @Mapping(target = "employmentAddress", source = "person.organizationAddress")
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
  Opponent toIndividualOpponent(OtherParty otherParty);


  @Mapping(target = "type", constant = OPPONENT_TYPE_ORGANISATION)
  @Mapping(target = "ebsId", source = "otherPartyId")
  @Mapping(target = ".", source = "organisation")
  @Mapping(target = "address", source = "organisation.address")
  @Mapping(target = "organisationName", source = "organisation.organizationName")
  @Mapping(target = "organisationType.id", source = "organisation.organizationType")
  @Mapping(target = "organisationType.displayValue", ignore = true)
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
  @Mapping(target = "employmentAddress", ignore = true)
  @Mapping(target = "employerName", ignore = true)
  @Mapping(target = "displayName", ignore = true)
  @Mapping(target = "displayAddress", ignore = true)
  @Mapping(target = "dateOfBirth", ignore = true)
  @Mapping(target = "courtOrderedMeansAssessment", ignore = true)
  @Mapping(target = "confirmed", ignore = true)
  @Mapping(target = "certificateNumber", ignore = true)
  @Mapping(target = "award", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  Opponent toOrganisationOpponent(OtherParty otherParty);

  @Mapping(target = "lscCaseReference", source = "caseReferenceNumber")
  @Mapping(target = "clientReference", source = "client.clientReferenceNumber")
  @Mapping(target = "clientFirstName", source = "client.firstName")
  @Mapping(target = "clientSurname", source = "client.surname")
  @Mapping(target = "categoryOfLaw", source = "categoryOfLawDesc")
  @Mapping(target = "providerCaseReference", source = "providerReferenceNumber")
  @Mapping(target = "feeEarner", source = "feeEarnerName")
  @Mapping(target = "status", source = "caseStatus")
  @Mapping(target = "relationToCase", source = "linkType")
  LinkedCase toLinkedCase(uk.gov.laa.ccms.soa.gateway.model.LinkedCase soaLinkedCase);

  @Mapping(target = "status", source = "soaPriorAuthority.decisionStatus")
  @Mapping(target = "summary", source = "soaPriorAuthority.description")
  @Mapping(target = "type.id", source = "soaPriorAuthority.priorAuthorityType")
  @Mapping(target = "type.displayValue", source = "priorAuthTypeLookup.description")
  @Mapping(target = "justification", source = "soaPriorAuthority.reasonForRequest")
  @Mapping(target = "amountRequested", source = "soaPriorAuthority.requestAmount")
  @Mapping(target = "valueRequired", source = "priorAuthTypeLookup.valueRequired")
  @Mapping(target = "items", source = "priorAuthTypeLookup.priorAuthorities")
  @Mapping(target = "ebsId", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  PriorAuthority toPriorAuthority(
      uk.gov.laa.ccms.soa.gateway.model.PriorAuthority soaPriorAuthority,
      PriorAuthorityTypeDetail priorAuthTypeLookup);

  @Mapping(target = "code.id", source = "code")
  @Mapping(target = "code.displayValue", source = "description")
  @Mapping(target = "type", source = "dataType")
  @Mapping(target = "lovLookUp", source = "lovCode")
  //  @Mapping(target = "mandatory", source = "mandatoryFlag")
  @Mapping(target = "mandatory", ignore = true)
  @Mapping(target = "value", ignore = true)
  ReferenceDataItem toReferenceDataItem(PriorAuthorityDetail priorAuthorityDetail);

  @Mapping(target = "legalCosts", source = "legalHelpCosts")
  @Mapping(target = "officeCode", source = "applicationDetails.larDetails.legalHelpOfficeCode")
  @Mapping(target = "uniqueFileNo", source = "applicationDetails.larDetails.legalHelpUfn")
  @Mapping(target = "otherDetails", source = "dischargeStatus.otherDetails")
  @Mapping(target = "dischargeReason", source = "dischargeStatus.reason")
  @Mapping(target = "clientContinueInd", source = "dischargeStatus.clientContinuePvtInd")
  @Mapping(target = "costAwards", ignore = true)
  @Mapping(target = "financialAwards", ignore = true)
  @Mapping(target = "landAwards", ignore = true)
  @Mapping(target = "otherAssetAwards", ignore = true)
  @Mapping(target = "proceedingOutcomes", ignore = true)
  @Mapping(target = "dischargeCaseInd", ignore = true)
  @Mapping(target = "providerId", ignore = true)
  @Mapping(target = "caseReferenceNumber", ignore = true)
  CaseOutcome toCaseOutcome(CaseDetail caseDetail);

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
  @Mapping(target = "certificateCostRateMarket",
      source = "costAward.certificateCostRateMarket", defaultValue = "0.00")
  @Mapping(target = "orderServedDate", source = "costAward.orderDateServed")
  @Mapping(target = "interestStartDate", source = "costAward.interestAwardedStartDate")
  @Mapping(target = "addressLine1", source = "costAward.serviceAddress.addressLine1")
  @Mapping(target = "addressLine2", source = "costAward.serviceAddress.addressLine2")
  @Mapping(target = "addressLine3", source = "costAward.serviceAddress.addressLine3")
  @Mapping(target = "recovery", source = "costAward.recovery")
  @Mapping(target = "liableParties", source = "costAward.liableParties")
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "opponentId", ignore = true)
  @Mapping(target = "opponentsToSelect", ignore = true)
  @Mapping(target = "costOrFinancial", ignore = true)
  @Mapping(target = "effectiveDate", ignore = true)
  @Mapping(target = "timeRelatedRecoveryDetails", ignore = true)
  @Mapping(target = "totalCertCostsAwarded", ignore = true)
  @Mapping(target = "triggeringEvent", ignore = true)
  @Mapping(target = "awardAmount", ignore = true)
  CostAward toCostAward(Award soaAward);

  /**
   * AfterMapping method to finalise a CostAward.
   *
   * @param costAward - the target for extra mapping.
   */
  @AfterMapping
  default void finaliseCostAward(@MappingTarget CostAward costAward) {
    if (costAward.getRecovery() != null) {
      costAward.getRecovery().setAwardType(AWARD_TYPE_COST);
      costAward.getRecovery().setDescription(AWARD_TYPE_COST_DESCRIPTION);
    }

    // Calculate the total costs awarded by summing LSC and Market
    costAward.setTotalCertCostsAwarded(costAward.getCertificateCostLsc()
        .add(costAward.getCertificateCostRateMarket()));
  }

  @Mapping(target = "ebsId", source = "awardId")
  @Mapping(target = "awardType", constant = AWARD_TYPE_FINANCIAL)
  @Mapping(target = "description", constant = AWARD_TYPE_FINANCIAL_DESCRIPTION)
  @Mapping(target = "awardCode", source = "awardType")
  @Mapping(target = ".", source = "financialAward")
  @Mapping(target = "dateOfOrder", source = "financialAward.orderDate")
  @Mapping(target = "awardAmount", source = "financialAward.amount")
  @Mapping(target = "orderServedDate", source = "financialAward.orderDateServed")
  @Mapping(target = "statutoryChargeExemptionReason",
      source = "financialAward.statutoryChangeReason")
  @Mapping(target = "addressLine1", source = "financialAward.serviceAddress.addressLine1")
  @Mapping(target = "addressLine2", source = "financialAward.serviceAddress.addressLine2")
  @Mapping(target = "addressLine3", source = "financialAward.serviceAddress.addressLine3")
  @Mapping(target = "recovery", source = "financialAward.recovery")
  @Mapping(target = "liableParties", source = "financialAward.liableParties")
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "opponentId", ignore = true)
  @Mapping(target = "opponentsToSelect", ignore = true)
  @Mapping(target = "costOrFinancial", ignore = true)
  @Mapping(target = "effectiveDate", ignore = true)
  @Mapping(target = "timeRelatedRecoveryDetails", ignore = true)
  @Mapping(target = "triggeringEvent", ignore = true)
  FinancialAward toFinancialAward(Award soaAward);

  /**
   * AfterMapping method to finalise a FinancialAward.
   *
   * @param financialAward - the target for extra mapping.
   */
  @AfterMapping
  default void finaliseFinancialAward(@MappingTarget FinancialAward financialAward) {
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
  @Mapping(target = "equity", ignore = true)
  @Mapping(target = "awardAmount", ignore = true)
  @Mapping(target = "opponentId", ignore = true)
  @Mapping(target = "opponentsToSelect", ignore = true)
  @Mapping(target = "costOrFinancial", ignore = true)
  @Mapping(target = "effectiveDate", ignore = true)
  @Mapping(target = "recoveryOfAwardTimeRelated", ignore = true)
  @Mapping(target = "timeRelatedRecoveryDetails", ignore = true)
  @Mapping(target = "triggeringEvent", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  LandAward toLandAward(Award soaAward);

  /**
   * Complete the mapping for a LandAward.
   *
   * @param landAward - the mapping target
   */
  @AfterMapping
  default void finaliseLandAward(@MappingTarget LandAward landAward) {
    TimeRecovery timeRecovery = landAward.getTimeRecovery();
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
  @Mapping(target = "awardAmount", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "costOrFinancial", ignore = true)
  @Mapping(target = "effectiveDate", ignore = true)
  @Mapping(target = "opponentId", ignore = true)
  @Mapping(target = "opponentsToSelect", ignore = true)
  @Mapping(target = "recoveryOfAwardTimeRelated", ignore = true)
  @Mapping(target = "timeRelatedRecoveryDetails", ignore = true)
  @Mapping(target = "triggeringEvent", ignore = true)
  OtherAssetAward toOtherAssetAward(Award soaAward);

  /**
   * Complete the mapping for an OtherAssetAward.
   *
   * @param otherAssetAward - the mapping target
   */
  @AfterMapping
  default void finaliseOtherAssetAward(@MappingTarget OtherAssetAward otherAssetAward) {
    TimeRecovery timeRecovery = otherAssetAward.getTimeRecovery();
    /* TODO: To be changed once api updated */
    otherAssetAward.setRecoveryOfAwardTimeRelated(timeRecovery != null ? "true" : "false");
    if (timeRecovery != null) {
      timeRecovery.setAwardType(AWARD_TYPE_OTHER_ASSET);
    }
  }

  /**
   * Complete the mapping for a BaseAward.
   *
   * @param baseAward - the mapping target
   */
  @AfterMapping
  default void finaliseAward(@MappingTarget BaseAward baseAward) {
    baseAward.getLiableParties().forEach(
        liableParty -> liableParty.setAwardType(baseAward.getAwardType()));
  }

  @Mapping(target = "opponentId", source = ".")
  @Mapping(target = "awardType", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  LiableParty toLiableParty(String partyId);

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
  @Mapping(target = "detailsOfOffer", source = "offeredAmount.conditionsOfOffer")
  @Mapping(target = "leaveOfCourtRequiredInd", source = "leaveOfCourtReqdInd")
  @Mapping(target = "unrecoveredAmount", ignore = true)
  @Mapping(target = "recoveredAmount", ignore = true)
  @Mapping(target = "awardType", ignore = true)
  @Mapping(target = "description", ignore = true)
  Recovery toRecovery(uk.gov.laa.ccms.soa.gateway.model.Recovery soaRecovery);

  /**
   * Perform after-mapping for a Recovery.
   *
   * @param recovery - the mapping target
   */
  @AfterMapping
  default void finaliseRecovery(@MappingTarget Recovery recovery) {
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
  TimeRecovery toTimeRecovery(TimeRelatedAward timeRelatedAward);
}
