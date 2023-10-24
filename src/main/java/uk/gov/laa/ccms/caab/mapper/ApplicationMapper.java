package uk.gov.laa.ccms.caab.mapper;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST_DESCRIPTION;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL_DESCRIPTION;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_LAND;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_OTHER_ASSET;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
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

  @Mapping(target = "caseReferenceNumber", source = "caseDetail.caseReferenceNumber")
  @Mapping(target = "certificate.id", source = "caseDetail.certificateType")
  @Mapping(target = "applicationType.id",
      source = "caseDetail.applicationDetails.applicationAmendmentType")
  @Mapping(target = "applicationType.displayValue", source = "applicationTypeLookup.description")
  @Mapping(target = "dateCreated", source = "caseDetail.recordHistory.dateCreated")
  @Mapping(target = "providerCaseReference",
      source = "caseDetail.applicationDetails.providerDetails.providerCaseReferenceNumber")
  @Mapping(target = "provider.id",
      source = "caseDetail.applicationDetails.providerDetails.providerFirmId")
  @Mapping(target = "provider.displayValue", source = "ebsProvider.name")
  @Mapping(target = "providerContact.id",
      source = "caseDetail.applicationDetails.providerDetails.contactUserId.userLoginId")
  @Mapping(target = "providerContact.displayValue",
      source = "caseDetail.applicationDetails.providerDetails.contactUserId.userName")
  @Mapping(target = "office.id", source = "providerOffice.id")
  @Mapping(target = "office.displayValue", source = "providerOffice.name")
  @Mapping(target = "supervisor.id", source = "supervisorContact.id")
  @Mapping(target = "supervisor.displayValue", source = "supervisorContact.name")
  @Mapping(target = "feeEarner.id", source = "feeEarnerContact.id")
  @Mapping(target = "feeEarner.displayValue", source = "feeEarnerContact.name")
  @Mapping(target = "correspondenceAddress",
      source = "caseDetail.applicationDetails.correspondenceAddress")
//  @Mapping(target = "correspondenceAddress.preferredAddress",
//    source = "caseDetail.applicationDetails.preferredAddress")
//  @Mapping(target = "correspondenceAddress.noFixedAbode", constant = "false")
//  @Mapping(target = "client", source = "caseDetail.applicationDetails.client")
//  @Mapping(target = "client.reference",
  //  source = "caseDetail.applicationDetails.client.clientReferenceNumber")
  @Mapping(target = "categoryOfLaw.id",
      source = "caseDetail.applicationDetails.categoryOfLaw.categoryOfLawCode")
  @Mapping(target = "categoryOfLaw.displayValue",
      source = "caseDetail.applicationDetails.categoryOfLaw.categoryOfLawDescription")
  @Mapping(target = "costs.grantedCostLimitation",
      source = "caseDetail.applicationDetails.categoryOfLaw.grantedAmount")
  @Mapping(target = "costs.requestedCostLimitation",
      source = "caseDetail.applicationDetails.categoryOfLaw.requestedAmount")
  @Mapping(target = "costs.defaultCostLimitation", ignore = true)
  @Mapping(target = "costs.costEntries",
      source = "caseDetail.applicationDetails.categoryOfLaw.costLimitations")
  @Mapping(target = "costs.currentProviderBilledAmount", ignore = true)
  @Mapping(target = "costs.auditTrail", ignore = true)
  @Mapping(target = "larScopeFlag",
      source = "caseDetail.applicationDetails.larDetails.larScopeFlag", defaultValue = "false")
  @Mapping(target = "status.id", source = "caseDetail.caseStatus.actualCaseStatus")
  @Mapping(target = "status.displayValue", source = "caseDetail.caseStatus.displayCaseStatus")
  @Mapping(target = "availableFunctions", source = "caseDetail.availableFunctions")
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
  @Mapping(
      target = "clientInvolvement.displayValue",
      source = "clientInvolvementLookup.description")
  @Mapping(target = "status.id", source = "proceedingStatus.code")
  @Mapping(target = "status.displayValue", source = "proceedingStatus.description")
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
      CommonLookupValueDetail proceedingStatus);

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


  @Mapping(target = "preferredAddress", ignore = true)
  @Mapping(target = "careOf", source = "careOfName")
  @Mapping(target = "houseNameOrNumber", source = "house")
  @Mapping(target = "postcode", source = "postalCode")
  @Mapping(target = "noFixedAbode", constant = "false")
  Address toAddress(AddressDetail soaAddress);

  @Mapping(target = "reference", source = "clientReferenceNumber")
  Client toClient(BaseClient soaClient);

  @Mapping(target = "type", constant = "Individual")
//  @Mapping(target = "id", source = "otherPartyId")
  @Mapping(target = "ebsId", source = "otherPartyId")
  @Mapping(target = ".", source = "person")
  @Mapping(target = "courtOrderedMeansAssessment", source = "person.courtOrderedMeansAssesment")
  @Mapping(target = "employmentAddress", source = "person.organizationAddress")
  @Mapping(target = "employerName", source = "person.organizationName")
  @Mapping(target = ".", source = "person.name")
  @Mapping(target = "legalAided", source = "person.partyLegalAidedInd")
  @Mapping(target = "nationalInsuranceNumber", source = "person.niNumber")
  @Mapping(target = "relationshipToCase", source = "person.relationToCase")
  @Mapping(target = "relationshipToClient", source = "person.relationToClient")
  @Mapping(target = ".", source = "person.contactDetails")
  @Mapping(target = "telephoneMobile", source = "person.contactDetails.mobileNumber")
  @Mapping(target = "faxNumber", source = "person.contactDetails.fax")
  @Mapping(target = "publicFundingApplied", source = "person.publicFundingAppliedInd")
  @Mapping(target = "deleteInd", constant = "false")
  Opponent toIndividualOpponent(OtherParty otherParty);

  @Mapping(target = "type", constant = "Organisation")
//  @Mapping(target = "id", source = "otherPartyId")
  @Mapping(target = "ebsId", source = "otherPartyId")
  @Mapping(target = ".", source = "organisation")
  @Mapping(target = ".", source = "organisation.address")
  @Mapping(target = "contactNameRole", source = "organisation.contactName")
  @Mapping(target = "relationshipToCase", source = "organisation.relationToCase")
  @Mapping(target = "relationshipToClient", source = "organisation.relationToClient")
  @Mapping(target = ".", source = "organisation.contactDetails")
  @Mapping(target = "telephoneMobile", source = "organisation.contactDetails.mobileNumber")
  @Mapping(target = "faxNumber", source = "organisation.contactDetails.fax")
  @Mapping(target = "deleteInd", constant = "false")
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
  PriorAuthority toPriorAuthority(uk.gov.laa.ccms.soa.gateway.model.PriorAuthority soaPriorAuthority,
      PriorAuthorityTypeDetail priorAuthTypeLookup);

  @Mapping(target = "code.id", source = "code")
  @Mapping(target = "code.displayValue", source = "description")
  @Mapping(target = "type", source = "dataType")
//  @Mapping(target = "mandatory", source = "mandatoryFlag")
  @Mapping(target = "lovLookUp", source = "lovCode")
  @Mapping(target = "value", ignore = true)
  ReferenceDataItem toReferenceDataItem(PriorAuthorityDetail priorAuthorityDetail);

  @Mapping(target = "legalCosts", source = "legalHelpCosts")
  @Mapping(target = "officeCode", source = "applicationDetails.larDetails.legalHelpOfficeCode")
  @Mapping(target = "uniqueFileNo", source = "applicationDetails.larDetails.legalHelpUfn")
  @Mapping(target = "otherDetails", source = "dischargeStatus.otherDetails")
  @Mapping(target = "dischargeReason", source = "dischargeStatus.reason")
  @Mapping(target = "clientContinueInd", source = "dischargeStatus.clientContinuePvtInd")

  CaseOutcome toCaseOutcome(CaseDetail caseDetail);
  /* TODO: 24/10/23 - Is this below needed?
   * The ProceedingOutcome has already been converted. Is this actually changing any
   * of the values?
   */
//    if (ebsCase != null && ebsCase.getProceedings() != null) {
//      for (Proceeding proceeding : ebsCase.getProceedings()) {
//        if (proceeding.hasOutcome()) {
//          ProceedingOutcome proceedingOutcome = proceeding.getOutcome();
//          proceedingOutcome.setProceedingCaseId(proceeding.getProceedingCaseId());
//          proceedingOutcome.setMatterTypeDisplayValue(proceeding.getMatterTypeDisplayValue());
//          proceedingOutcome
//              .setProceedingTypeDisplayValue(proceeding.getProceedingTypeDisplayValue());
//          proceedingOutcome.setDescription(proceeding.getDescription());
//          proceedingOutcome.setProceedingType(proceeding.getProceedingType());
//          outcome.addProceedingOutcome(proceedingOutcome);
//        }
//      }
//    }

  @Mapping(target = "ebsId", source = "awardId")
  @Mapping(target = "awardType", constant = AWARD_TYPE_COST)
  @Mapping(target = "description", constant = AWARD_TYPE_COST_DESCRIPTION)
  @Mapping(target = "awardCode", source = "awardType")
  @Mapping(target = ".", source = "costAward")
  @Mapping(target = "dateOfOrder", source = "costAward.orderDate")
  @Mapping(target = "preCertificateLscCost", source = "costAward.preCertificateAwardLsc")
  @Mapping(target = "preCertificateOtherCost", source = "costAward.preCertificateAwardOth")
  @Mapping(target = ".", source = "costAward.serviceAddress")
  @Mapping(target = "recovery", source = "costAward.recovery")
  @Mapping(target = "liableParties", source = "costAward.liableParties")
  CostAward toCostAward(Award soaAward);

  @AfterMapping
  default void afterMapping(@MappingTarget CostAward costAward) {
    costAward.getRecovery().setAwardType(AWARD_TYPE_COST);
    costAward.getRecovery().setDescription(AWARD_TYPE_COST_DESCRIPTION);
  }

  @Mapping(target = "ebsId", source = "awardId")
  @Mapping(target = "awardType", constant = AWARD_TYPE_FINANCIAL)
  @Mapping(target = "description", constant = AWARD_TYPE_FINANCIAL_DESCRIPTION)
  @Mapping(target = "awardCode", source = "awardType")
  @Mapping(target = ".", source = "financialAward")
  @Mapping(target = "dateOfOrder", source = "financialAward.orderDate")
  @Mapping(target = "awardAmount", source = "financialAward.amount")
  @Mapping(target = "orderServedDate", source = "financialAward.orderDateServed")
  @Mapping(target = "statutoryChargeExemptionReason", source = "financialAward.statutoryChangeReason")
  @Mapping(target = ".", source = "financialAward.serviceAddress")
  @Mapping(target = "recovery", source = "financialAward.recovery")
  @Mapping(target = "liableParties", source = "financialAward.liableParties")
  FinancialAward toFinancialAward(Award soaAward);

  @AfterMapping
  default void afterMapping(@MappingTarget FinancialAward financialAward) {
    financialAward.getRecovery().setAwardType(AWARD_TYPE_FINANCIAL);
    financialAward.getRecovery().setDescription(AWARD_TYPE_FINANCIAL_DESCRIPTION);
  }

  @Mapping(target = "awardCode", source = "awardType")
  @Mapping(target = "ebsId", source = "awardId")
  @Mapping(target = "dateOfOrder", source = "landAward.orderDate")
  @Mapping(target = "awardType", constant = AWARD_TYPE_LAND)
  @Mapping(target = ".", source = "landAward")
  @Mapping(target = ".", source = "landAward.propertyAddress")
  @Mapping(target = "valuationAmount", source = "landAward.valuation.amount")
  @Mapping(target = "valuationCriteria", source = "landAward.valuation.criteria")
  @Mapping(target = "valuationDate", source = "landAward.valuation.date")
  @Mapping(target = "timeRecovery", source = "landAward.timeRelatedAward")
  @Mapping(target = "liableParties", source = "landAward.otherProprietors")
  LandAward toLandAward(Award soaAward);

  @AfterMapping
  default void afterMapping(@MappingTarget LandAward landAward) {
    TimeRecovery timeRecovery = landAward.getTimeRecovery();
    landAward.setRecoveryOfAwardTimeRelated(timeRecovery != null); // To be changed once api updated
    if (timeRecovery != null) {
      timeRecovery.setAwardType(AWARD_TYPE_LAND);
    }
  }

  @Mapping(target = "ebsId", source = "awardId")
  @Mapping(target = "dateOfOrder", source = "otherAsset.orderDate")
  @Mapping(target = "awardType", constant = AWARD_TYPE_OTHER_ASSET)
  @Mapping(target = "awardCode", source = "awardType")
  @Mapping(target = ".", source = "otherAsset")
  @Mapping(target = "valuationAmount", source = "otherAsset.valuation.amount")
  @Mapping(target = "valuationCriteria", source = "otherAsset.valuation.criteria")
  @Mapping(target = "valuationDate", source = "otherAsset.valuation.date")
  @Mapping(target = "timeRecovery", source = "otherAsset.timeRelatedAward")
  @Mapping(target = "liableParties", source = "otherAsset.heldBy")
  OtherAssetAward toOtherAssetAward(Award soaAward);

  @AfterMapping
  default void afterMapping(@MappingTarget OtherAssetAward otherAssetAward) {
    TimeRecovery timeRecovery = otherAssetAward.getTimeRecovery();
    otherAssetAward.setRecoveryOfAwardTimeRelated(timeRecovery != null ? "true" : "false"); // To be changed once api updated
    if (timeRecovery != null) {
      timeRecovery.setAwardType(AWARD_TYPE_OTHER_ASSET);
    }
  }

  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "opponentId", source = ".")
  LiableParty toLiableParty(String partyId);

  @AfterMapping
  default void afterMapping(@MappingTarget BaseAward baseAward, Award soaAward) {
    baseAward.getLiableParties().forEach(
        liableParty -> liableParty.setAwardType(soaAward.getAwardType()));
  }

  @Mapping(target = "awardAmount", source = "awardValue")
  @Mapping(target = "clientAmountPaidToLsc", source = "recoveredAmount.client.paidToLsc")
  @Mapping(target = "clientRecoveryAmount", source = "recoveredAmount.client.amount")
  @Mapping(target = "clientRecoveryDate", source = "recoveredAmount.client.dateReceived")
  @Mapping(target = "courtAmountPaidToLsc", source = "recoveredAmount.court.paidToLsc")
  @Mapping(target = "courtRecoveryAmount", source = "recoveredAmount.court.amount")
  @Mapping(target = "courtRecoveryDate", source = "recoveredAmount.court.dateReceived")
  @Mapping(target = "solicitorAmountPaidToLsc", source = "recoveredAmount.solicitor.paidToLsc")
  @Mapping(target = "solicitorRecoveryAmount", source = "recoveredAmount.solicitor.amount")
  @Mapping(target = "solicitorRecoveryDate", source = "recoveredAmount.solicitor.dateReceived")
  @Mapping(target = "offeredAmount", source = "offeredAmount.amount")
  @Mapping(target = "conditionsOfOffer", source = "offeredAmount.conditionsOfOffer")
  @Mapping(target = "detailsOfOffer", source = "offeredAmount.conditionsOfOffer")
  @Mapping(target = "leaveOfCourtRequiredInd", source = "leaveOfCourtReqdInd")
  @Mapping(target = "recoveredAmount", ignore = true)
  Recovery toRecovery(uk.gov.laa.ccms.soa.gateway.model.Recovery soaRecovery);

  @AfterMapping
  default void calculateRecoveredAmount(@MappingTarget Recovery recovery,
      uk.gov.laa.ccms.soa.gateway.model.Recovery soaRecovery) {
    recovery.setRecoveredAmount(Stream.of(
        soaRecovery.getRecoveredAmount().getSolicitor().getAmount(),
        soaRecovery.getRecoveredAmount().getCourt().getAmount(),
        soaRecovery.getRecoveredAmount().getClient().getAmount())
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  @Mapping(target = "awardAmount", source = "amount")
  @Mapping(target = "triggeringEvent", source = "awardTriggeringEvent")
  @Mapping(target = "effectiveDate", source = "awardDate")
  @Mapping(target = "timeRelatedRecoveryDetails", source = "otherDetails")
  TimeRecovery toTimeRecovery(TimeRelatedAward timeRelatedAward);
}
