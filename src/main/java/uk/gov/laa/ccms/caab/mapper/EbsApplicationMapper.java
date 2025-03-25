package uk.gov.laa.ccms.caab.mapper;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST_DESCRIPTION;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL_DESCRIPTION;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_LAND;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_OTHER_ASSET;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_INDIVIDUAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_ORGANISATION;
import static uk.gov.laa.ccms.caab.util.OpponentUtil.getAssessmentMappingId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.tuple.Pair;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.mapper.context.CaseMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.EbsApplicationMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.EbsCaseOutcomeMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.EbsPriorAuthorityMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.EbsProceedingMappingContext;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.AssessmentResult;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.BaseAwardDetail;
import uk.gov.laa.ccms.caab.model.BaseEvidenceDocumentDetail;
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
import uk.gov.laa.ccms.data.model.AssessmentScreen;
import uk.gov.laa.ccms.data.model.Award;
import uk.gov.laa.ccms.data.model.BaseClient;
import uk.gov.laa.ccms.data.model.CaseDoc;
import uk.gov.laa.ccms.data.model.CaseStatus;
import uk.gov.laa.ccms.data.model.CaseSummary;
import uk.gov.laa.ccms.data.model.CategoryOfLaw;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.CostLimitation;
import uk.gov.laa.ccms.data.model.LinkedCase;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.OpaAttribute;
import uk.gov.laa.ccms.data.model.OpaEntity;
import uk.gov.laa.ccms.data.model.OpaGoal;
import uk.gov.laa.ccms.data.model.OpaInstance;
import uk.gov.laa.ccms.data.model.OtherParty;
import uk.gov.laa.ccms.data.model.OtherPartyOrganisation;
import uk.gov.laa.ccms.data.model.OtherPartyPerson;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.PriorAuthority;
import uk.gov.laa.ccms.data.model.PriorAuthorityAttribute;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.Proceeding;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.RecordHistory;
import uk.gov.laa.ccms.data.model.Recovery;
import uk.gov.laa.ccms.data.model.ScopeLimitation;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;
import uk.gov.laa.ccms.data.model.SubmittedApplicationDetails;
import uk.gov.laa.ccms.data.model.TimeRelatedAward;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Mapper class to convert a EbsApi Case into a CAAB ApplicationDetail.
 */
@Mapper(componentModel = "spring")
public interface EbsApplicationMapper {

  ApplicationDetails toApplicationDetails(Page<BaseApplicationDetail> applicationPage);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "amendment", ignore = true)
  @Mapping(target = "status", source = "caseStatusDisplay")
  @Mapping(target = "providerDetails", source = ".")
  @Mapping(target = "client.reference", source = "client.clientReferenceNumber")
  BaseApplicationDetail toBaseApplication(CaseSummary ebsCaseSummary);

  @Mapping(target = "providerCaseReference", source = "providerCaseReferenceNumber")
  @Mapping(target = "feeEarner.displayValue", source = "feeEarnerName")
  @Mapping(target = "provider", ignore = true)
  @Mapping(target = "office", ignore = true)
  @Mapping(target = "supervisor", ignore = true)
  @Mapping(target = "providerContact", ignore = true)
  ApplicationProviderDetails toApplicationProviderDetails(CaseSummary ebsCaseSummary);

  @Mapping(target = ".", source = "ebsCaseDetail")
  @Mapping(target = "certificate", source = "certificate")
  @Mapping(target = "applicationType", source = "applicationMappingContext")
  @Mapping(target = "dateCreated", source = "ebsCaseDetail.recordHistory.dateCreated")
  @Mapping(target = "providerDetails.providerCaseReference",
      source = "ebsCaseDetail.applicationDetails.providerDetails.providerCaseReferenceNumber")
  @Mapping(target = "providerDetails.provider", source = "providerDetail")
  @Mapping(target = "providerDetails.providerContact",
      source = "ebsCaseDetail.applicationDetails.providerDetails.contactUserId")
  @Mapping(target = "providerDetails.office", source = "providerOffice")
  @Mapping(target = "providerDetails.supervisor", source = "supervisorContact")
  @Mapping(target = "providerDetails.feeEarner", source = "feeEarnerContact")
  @Mapping(target = "correspondenceAddress", source = "applicationMappingContext")
  @Mapping(target = "client", source = "ebsCaseDetail.applicationDetails.client")
  @Mapping(target = "categoryOfLaw", source = "ebsCaseDetail.applicationDetails.categoryOfLaw")
  @Mapping(target = "costs", source = "applicationMappingContext")
  @Mapping(target = "larScopeFlag",
      source = "ebsCaseDetail.applicationDetails.larDetails.larScopeFlag", defaultValue = "false")
  @Mapping(target = "status", source = "ebsCaseDetail.caseStatus")
  @Mapping(target = "opponents", source = "ebsCaseDetail.applicationDetails.otherParties")
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
  ApplicationDetail toApplicationDetail(EbsApplicationMappingContext applicationMappingContext);

  @Mapping(target = "id", source = "ebsCaseDetail.applicationDetails.applicationAmendmentType")
  @Mapping(target = "displayValue", source = "applicationType.description")
  @Mapping(target = "devolvedPowers", source = "devolvedPowers")
  ApplicationType toApplicationType(EbsApplicationMappingContext applicationMappingContext);

  @Mapping(target = ".", source = "ebsCaseDetail.applicationDetails.correspondenceAddress")
  @Mapping(target = "careOf",
      source = "ebsCaseDetail.applicationDetails.correspondenceAddress.careOfName")
  @Mapping(target = "houseNameOrNumber",
      source = "ebsCaseDetail.applicationDetails.correspondenceAddress.house")
  @Mapping(target = "postcode",
      source = "ebsCaseDetail.applicationDetails.correspondenceAddress.postalCode")
  @Mapping(target = "noFixedAbode", constant = "false")
  @Mapping(target = "preferredAddress",
      source = "ebsCaseDetail.applicationDetails.preferredAddress")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  AddressDetail toCorrespondenceAddress(EbsApplicationMappingContext mappingContext);


  @Mapping(target = "edited", constant = "false")
  @Mapping(target = ".", source = "ebsProceeding")
  @Mapping(target = "ebsId", source = "ebsProceeding.proceedingCaseId")
  @Mapping(target = "leadProceedingInd", source = "ebsProceeding.leadProceedingIndicator")
  @Mapping(target = "proceedingType", source = "proceedingLookup")
  @Mapping(target = "matterType", source = "matterType")
  @Mapping(target = "levelOfService", source = "levelOfService")
  @Mapping(target = "larScope", source = "proceedingLookup.larScope")
  @Mapping(target = "description", source = "ebsProceeding.proceedingDescription")
  @Mapping(target = "status", source = "proceedingStatusLookup")
  @Mapping(target = "typeOfOrder", source = "ebsProceeding.orderType")
  @Mapping(target = "costLimitation", source = "proceedingCostLimitation")
  @Mapping(target = "outcome", source = "proceedingContext",
      conditionExpression = "java(proceedingContext.getEbsProceeding().getOutcome() != null)")
  @Mapping(target = "scopeLimitations", source = "scopeLimitations")
  @Mapping(target = "defaultScopeLimitation", ignore = true)
  @Mapping(target = "grantedUsingDevolvedPowers", ignore = true)
  @Mapping(target = "orderTypeReqFlag", ignore = true)
  @Mapping(target = "orderTypeDisplayFlag", ignore = true)
  @Mapping(target = "deleteScopeLimitationFlag", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "id", ignore = true)
  ProceedingDetail toProceeding(EbsProceedingMappingContext proceedingContext);

  @Mapping(target = "grantedCostLimitation",
      source = "ebsCaseDetail.applicationDetails.categoryOfLaw.grantedAmount")
  @Mapping(target = "requestedCostLimitation",
      source = "ebsCaseDetail.applicationDetails.categoryOfLaw.requestedAmount")
  @Mapping(target = "defaultCostLimitation", ignore = true)
  @Mapping(target = "costEntries",
      source = "ebsCaseDetail.applicationDetails.categoryOfLaw.costLimitations")
  @Mapping(target = "currentProviderBilledAmount", source = "currentProviderBilledAmount")
  @Mapping(target = "auditTrail", ignore = true)
  CostStructureDetail toCostStructure(EbsApplicationMappingContext mappingContext);

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

  @Mapping(target = "description", source = "ebsProceeding.proceedingDescription")
  @Mapping(target = "proceedingCaseId", source = "ebsProceeding.proceedingCaseId")
  @Mapping(target = "proceedingType", source = "proceedingLookup")
  @Mapping(target = "adrInfo", source = "ebsProceeding.outcome.altAcceptanceReason")
  @Mapping(target = "alternativeResolution", source = "ebsProceeding.outcome.altDisputeResolution")
  @Mapping(target = "courtCode", source = "courtLookup.code")
  @Mapping(target = "courtName", source = "courtLookup.description")
  @Mapping(target = "dateOfFinalWork", source = "ebsProceeding.outcome.finalWorkDate")
  @Mapping(target = "dateOfIssue", ignore = true)
  @Mapping(target = "resolutionMethod", source = "ebsProceeding.outcome.resolutionMethod")
  @Mapping(target = "result", source = "outcomeResultLookup")
  @Mapping(target = "resultInfo", source = "ebsProceeding.outcome.additionalResultInfo")
  @Mapping(target = "stageEnd", source = "stageEndLookup")
  @Mapping(target = "widerBenefits", source = "ebsProceeding.outcome.widerBenefits")
  @Mapping(target = "outcomeCourtCaseNo", source = "ebsProceeding.outcome.outcomeCourtCaseNumber")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  ProceedingOutcomeDetail toProceedingOutcome(EbsProceedingMappingContext proceedingContext);

  AssessmentResult toAssessmentResult(
      uk.gov.laa.ccms.data.model.AssessmentResult assessmentResult);

  @Mapping(target = "careOf", source = "careOfName")
  @Mapping(target = "houseNameOrNumber", source = "house")
  @Mapping(target = "postcode", source = "postalCode")
  @Mapping(target = "noFixedAbode", constant = "false")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "preferredAddress", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  AddressDetail toAddress(uk.gov.laa.ccms.data.model.AddressDetail ebsAddress);

  @Mapping(target = "reference", source = "clientReferenceNumber")
  ClientDetail toClient(BaseClient ebsClient);

  @Mapping(target = "type", constant = OPPONENT_TYPE_INDIVIDUAL)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "ebsId", source = "otherPartyId")
  @Mapping(target = ".", source = "person")
  @Mapping(target = "courtOrderedMeansAssessment", source = "person.courtOrderedMeansAssessment")
  @Mapping(target = "employerAddress", source = "person.organisationAddress")
  @Mapping(target = "employerName", source = "person.organisationName")
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
  @Mapping(target = "organisationName", source = "organisation.organisationName")
  @Mapping(target = "organisationType", source = "organisation.organisationType")
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
  LinkedCaseDetail toLinkedCase(LinkedCase ebsLinkedCase);

  @Mapping(target = "status", source = "ebsPriorAuthority.decisionStatus")
  @Mapping(target = "summary", source = "ebsPriorAuthority.description")
  @Mapping(target = "type", source = "priorAuthorityTypeLookup")
  @Mapping(target = "justification", source = "ebsPriorAuthority.reasonForRequest")
  @Mapping(target = "amountRequested", source = "ebsPriorAuthority.requestAmount")
  @Mapping(target = "valueRequired", source = "priorAuthorityTypeLookup.valueRequired")
  @Mapping(target = "ebsId", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "id", ignore = true)
  PriorAuthorityDetail toPriorAuthority(EbsPriorAuthorityMappingContext priorAuthorityMappingContext);

  @Mapping(target = "code.id", source = "key.code")
  @Mapping(target = "code.displayValue", source = "key.description")
  @Mapping(target = "type", source = "key.dataType")
  @Mapping(target = "lovLookUp", source = "key.lovCode")
  @Mapping(target = "mandatory", source = "key.mandatoryFlag")
  @Mapping(target = "value", source = "value")
  @Mapping(target = "id", ignore = true)
  ReferenceDataItemDetail toReferenceDataItem(Pair<uk.gov.laa.ccms.data.model.PriorAuthorityDetail,
      CommonLookupValueDetail> priorAuthorityDetail);

  @Mapping(target = ".", source = "ebsCase")
  @Mapping(target = "legalCosts", source = "ebsCase.legalHelpCosts")
  @Mapping(target = "officeCode",
      source = "ebsCase.applicationDetails.larDetails.legalHelpOfficeCode")
  @Mapping(target = "uniqueFileNo", source = "ebsCase.applicationDetails.larDetails.legalHelpUfn")
  @Mapping(target = "otherDetails", source = "ebsCase.dischargeStatus.otherDetails")
  @Mapping(target = "dischargeReason", source = "ebsCase.dischargeStatus.reason")
  @Mapping(target = "clientContinueInd", source = "ebsCase.dischargeStatus.clientContinuePvtInd")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "dischargeCaseInd", ignore = true)
  @Mapping(target = "providerId", ignore = true)
  @Mapping(target = "opponentIds", ignore = true)
  @Mapping(target = "caseReferenceNumber", ignore = true)
  CaseOutcomeDetail toCaseOutcome(EbsCaseOutcomeMappingContext caseOutcome);

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
  CostAwardDetail toCostAward(Award sebsAward);

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
  FinancialAwardDetail toFinancialAward(Award ebsAward);

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
  LandAwardDetail toLandAward(Award ebsAward);

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
  OtherAssetAwardDetail toOtherAssetAward(Award ebsAward);

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
  RecoveryDetail toRecovery(Recovery ebsRecovery);

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

  @Mapping(target = "id", source = "loginId")
  @Mapping(target = "displayValue", source = "username")
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


  @Mapping(target = ".", source = "tdsApplication")
  @Mapping(target = "caseReferenceNumber", source = "tdsApplication.caseReferenceNumber")
  @Mapping(target = "applicationDetails", source = "context")
  @Mapping(target = "linkedCases", source = "tdsApplication.linkedCases")
  @Mapping(target = "recordHistory", source = ".", qualifiedByName = "toEbsRecordHistory")
  //costs are not required as no awards will have been granted at this stage
  @Mapping(target = "certificateType", ignore = true)
  @Mapping(target = "certificateDate", ignore = true)
  @Mapping(target = "preCertificateCosts", ignore = true)
  @Mapping(target = "legalHelpCosts", ignore = true)
  @Mapping(target = "undertakingAmount", ignore = true)
  @Mapping(target = "awards", ignore = true)
  @Mapping(target = "dischargeStatus", ignore = true)
  @Mapping(target = "caseStatus", ignore = true)
  @Mapping(target = "availableFunctions", ignore = true)
  uk.gov.laa.ccms.data.model.CaseDetail toEbsCaseDetail(CaseMappingContext context);

  @Mapping(target = "ccmsDocumentId", source = "registeredDocumentId")
  @Mapping(target = "documentSubject", source = "documentType.displayValue")
  CaseDoc toEbsCaseDoc(BaseEvidenceDocumentDetail evidenceDocumentDetail);

  @Mapping(target = "priorAuthorityType", source = "type.id")
  @Mapping(target = "description", source = "summary")
  @Mapping(target = "reasonForRequest", source = "justification")
  @Mapping(target = "requestAmount", source = "amountRequested",
      qualifiedByName = "mapBigDecimalDefault")
  @Mapping(target = "decisionStatus", source = "status")
  @Mapping(target = "details", source = "items")
  @Mapping(target = "assessedAmount", ignore = true)
  PriorAuthority toEbsPriorAuthority(PriorAuthorityDetail priorAuthorityDetail);

  @Mapping(target = "name", source = "code.id")
  @Mapping(target = "value", source = "value.id")
  PriorAuthorityAttribute toEbsPriorAuthorityAttribute(
      ReferenceDataItemDetail referenceDataItemDetail);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "caseReferenceNumber", source = "lscCaseReference")
  @Mapping(target = "linkType", source = "relationToCase")
  LinkedCase toEbsLinkedCase(LinkedCaseDetail linkedCase);

  @Mapping(target = "client",
      source = "tdsApplication.client")
  @Mapping(target = "preferredAddress",
      source = "tdsApplication.correspondenceAddress.preferredAddress")
  @Mapping(target = "providerDetails",
      source = "tdsApplication.providerDetails")
  @Mapping(target = "categoryOfLaw",
      source = "tdsApplication")
  @Mapping(target = "applicationAmendmentType",
      source = "tdsApplication.applicationType.id")
  @Mapping(target = "correspondenceAddress",
      source = "tdsApplication.correspondenceAddress")
  @Mapping(target = "proceedings", source = "tdsApplication.proceedings")
  @Mapping(target = "larDetails.larScopeFlag", source = "tdsApplication.larScopeFlag")
  @Mapping(target = "otherParties", source = "tdsApplication.opponents")
  @Mapping(target = "meansAssessments", source = "meansAssessment",
      qualifiedByName = "mapMeansAssessment")
  @Mapping(target = "meritsAssessments", source = "meritsAssessment",
      qualifiedByName = "mapMeritsAssessment")
  @Mapping(target = "devolvedPowersDate", source = "tdsApplication",
      qualifiedByName = "mapDevolvedPowersDate")
  @Mapping(target = "larDetails.legalHelpOfficeCode", ignore = true)
  @Mapping(target = "larDetails.legalHelpUfn", ignore = true)
  @Mapping(target = "externalResources", ignore = true)
  @Mapping(target = "dateOfFirstAttendance", ignore = true)
  @Mapping(target = "purposeOfApplication", ignore = true)
  @Mapping(target = "fixedHearingDateInd", ignore = true)
  @Mapping(target = "dateOfHearing", ignore = true)
  @Mapping(target = "purposeOfHearing", ignore = true)
  @Mapping(target = "highProfileCaseInd", ignore = true)
  @Mapping(target = "certificateType", ignore = true)
  SubmittedApplicationDetails toSubmittedApplicationDetails(CaseMappingContext context);

  /**
   * Maps and returns the devolved powers date from the provided application details.
   * The date is retrieved based on the application type being either emergency or substantive
   * devolved powers.
   *
   * @param app the application details
   * @return the devolved powers date, or {@code null} if the application or devolved powers
   *         details are not present
   */
  @Named("mapDevolvedPowersDate")
  default Date mapDevolvedPowersDate(final ApplicationDetail app) {
    if (app != null && app.getApplicationType() != null) {
      final ApplicationType appType = app.getApplicationType();
      final DevolvedPowersDetail devolvedPowers = appType.getDevolvedPowers();

      if (devolvedPowers != null) {
        if (APP_TYPE_EMERGENCY_DEVOLVED_POWERS.equalsIgnoreCase(appType.getId())) {
          return devolvedPowers.getDateUsed();
        } else if (APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS.equalsIgnoreCase(appType.getId())) {
          return devolvedPowers.getDateUsed();
        }
      }
    }
    return null;
  }

  @Mapping(target = "clientReferenceNumber", source = "reference")
  BaseClient toBaseClient(ClientDetail client);

  @Mapping(target = "providerCaseReferenceNumber", source = "providerCaseReference")
  @Mapping(target = "providerFirmId", source = "provider.id")
  @Mapping(target = "providerOfficeId", source = "office.id")
  @Mapping(target = "contactUserId.loginId", source = "providerContact.id")
  @Mapping(target = "supervisorContactId", source = "supervisor.id")
  @Mapping(target = "feeEarnerContactId", source = "feeEarner.id")
  @Mapping(target = "contactDetails", ignore = true)
  @Mapping(target = "contactUserId.username", ignore = true)
  @Mapping(target = "contactUserId.userType", ignore = true)
  uk.gov.laa.ccms.data.model.ProviderDetails toEbsProviderDetail(
      ApplicationProviderDetails providerDetail);

  @Mapping(target = "categoryOfLawCode", source = "categoryOfLaw.id")
  @Mapping(target = "categoryOfLawDescription", source = "categoryOfLaw.displayValue")
  @Mapping(target = "requestedAmount", source = "costs", qualifiedByName = "mapRequestedAmount")
  @Mapping(target = "grantedAmount", ignore = true)
  @Mapping(target = "totalPaidToDate", ignore = true)
  @Mapping(target = "costLimitations", ignore = true)
  CategoryOfLaw toEbsCategoryOfLaw(ApplicationDetail applicationDetail);

  @Mapping(target = "addressId", source = "id")
  @Mapping(target = "house", source = "houseNameOrNumber")
  @Mapping(target = "careOfName", source = "careOf")
  @Mapping(target = "postalCode", source = "postcode")
  @Mapping(target = "addressLine3", ignore = true)
  @Mapping(target = "addressLine4", ignore = true)
  @Mapping(target = "province", ignore = true)
  @Mapping(target = "state", ignore = true)
  uk.gov.laa.ccms.data.model.AddressDetail toEbsAddressDetail(AddressDetail addressDetail);

  @Mapping(target = "proceedingCaseId", source = "id", qualifiedByName = "mapProceedingId")
  @Mapping(target = "status", source = "status.id")
  @Mapping(target = "leadProceedingIndicator", source = "leadProceedingInd")
  @Mapping(target = "proceedingType", source = "proceedingType.id")
  @Mapping(target = "proceedingDescription", source = "description")
  @Mapping(target = "orderType", source = "typeOfOrder.id")
  @Mapping(target = "matterType", source = "matterType.id")
  @Mapping(target = "levelOfService", source = "levelOfService.id")
  @Mapping(target = "clientInvolvementType", source = "clientInvolvement.id")
  @Mapping(target = "outcome", ignore = true)
  @Mapping(target = "dateApplied", ignore = true)
  @Mapping(target = "outcomeCourtCaseNumber", ignore = true)
  @Mapping(target = "scopeLimitationApplied", ignore = true)
  @Mapping(target = "devolvedPowersInd", ignore = true)
  @Mapping(target = "availableFunctions", ignore = true)
  Proceeding toEbsProceedingDetail(
      ProceedingDetail proceedingDetail);

  @Mapping(target = "scopeLimitation", source = "scopeLimitation.id")
  @Mapping(target = "scopeLimitationWording", source = "scopeLimitationWording")
  @Mapping(target = "delegatedFunctionsApply", source = "delegatedFuncApplyInd.flag")
  @Mapping(target = "scopeLimitationId", ignore = true)
  ScopeLimitation toEbsScopeLimitation(ScopeLimitationDetail scopeLimitationDetail);

  @Mapping(target = "otherPartyId", source = ".", qualifiedByName = "mapOpponentId")
  @Mapping(target = "person", source = ".", qualifiedByName = "mapPerson")
  @Mapping(target = "organisation", source = ".", qualifiedByName = "mapOrganisation")
  OtherParty toEbsOtherParty(OpponentDetail opponentDetail);

  @Mapping(target = "name.title", source = "title")
  @Mapping(target = "name.surname", source = "surname")
  @Mapping(target = "name.middleName", source = "middleNames")
  @Mapping(target = "name.firstName", source = "firstName")
  @Mapping(target = "relationToClient", source = "relationshipToClient")
  @Mapping(target = "relationToCase", source = "relationshipToCase")
  @Mapping(target = "niNumber", source = "nationalInsuranceNumber")
  @Mapping(target = "partyLegalAidedInd", source = "legalAided")
  @Mapping(target = "courtOrderedMeansAssessment", source = "courtOrderedMeansAssessment")
  @Mapping(target = "contactName", source = "contactNameRole")
  @Mapping(target = "employersName", source = "employerName")
  @Mapping(target = "organisationAddress", source = "employerAddress")
  @Mapping(target = "assessedIncome", source = "assessedIncome",
      qualifiedByName = "mapBigDecimalDefault")
  @Mapping(target = "assessedAssets", source = "assessedAssets",
      qualifiedByName = "mapBigDecimalDefault")
  @Mapping(target = "contactDetails", ignore = true)
  @Mapping(target = "address", ignore = true)
  @Mapping(target = "publicFundingAppliedInd", ignore = true)
  @Mapping(target = "organisationName", ignore = true)
  OtherPartyPerson toEbsPerson(OpponentDetail opponentDetail);

  /**
   * Maps the provided {@link BigDecimal} to a scaled value with 2 decimal places.
   *
   * @param value the {@link BigDecimal} value to be mapped
   * @return the scaled {@link BigDecimal} value, or {@code BigDecimal.ZERO} if the value is
   *         {@code null}
   */
  @Named("mapBigDecimalDefault")
  default BigDecimal mapBigDecimalDefault(final BigDecimal value) {
    return value != null
        ? value.setScale(2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
  }

  @Mapping(target = "relationToClient", source = "relationshipToClient")
  @Mapping(target = "relationToCase", source = "relationshipToCase")
  @Mapping(target = "organisationName", source = "organisationName")
  @Mapping(target = "organisationType", source = "organisationType")
  @Mapping(target = "contactName", source = "contactNameRole")
  @Mapping(target = "contactDetails", ignore = true)
  OtherPartyOrganisation toEbsOrganisation(OpponentDetail opponentDetail);

  /**
   * Maps the provided assessment details into the given
   * {@link uk.gov.laa.ccms.data.model.AssessmentResult}. It organizes entity types,
   * entities, and their attributes into an assessment screen summary.
   *
   * @param assessmentResult the assessment result to which the mapped data will be added
   * @param assessment the assessment details containing entities and their attributes
   */
  @Named("mapIntoAssessment")
  default void mapIntoAssessment(
      final uk.gov.laa.ccms.data.model.AssessmentResult assessmentResult,
      final AssessmentDetail assessment) {

    final AssessmentScreen assessmentScreen = new AssessmentScreen()
        .screenName("SUMMARY");

    final AtomicInteger sequenceNumber = new AtomicInteger(1);

    // Use a map to track existing OpaEntity by entityName
    final Map<String, OpaEntity> entityMap = new HashMap<>();

    assessment.getEntityTypes().stream()
        .filter(entityType -> entityType.getEntities() != null)
        .forEach(entityType -> entityType.getEntities().forEach(entity -> {

          // Find existing OpaEntity or create a new one
          final OpaEntity opaEntity = entityMap.computeIfAbsent(entityType.getName(), name ->
              new OpaEntity()
                  .sequenceNumber(sequenceNumber.getAndIncrement())
                  .entityName(entityType.getName()));

          final OpaInstance opaInstance = new OpaInstance()
              .instanceLabel(entity.getName());

          // Only add the attributes if they have values
          Optional.ofNullable(entity.getAttributes()).stream()
              .flatMap(Collection::stream)
              .filter(attribute -> attribute.getValue() != null)
              .map(this::toOpaAttribute)
              .forEach(opaInstance::addAttributesItem);

          // Only add the instance if it has attributes
          Optional.of(opaInstance)
              .filter(inst -> inst.getAttributes() != null && !inst.getAttributes().isEmpty())
              .ifPresent(inst -> opaEntity.addInstancesItem(opaInstance));

        }));

    // Add each unique OpaEntity from the map to the assessmentScreen
    entityMap.values().stream()
        .filter(ent -> ent.getInstances() != null && !ent.getInstances().isEmpty())
        .forEach(assessmentScreen::addEntityItem);

    assessmentResult.addAssessmentDetailsItem(assessmentScreen);
  }

  /**
   * Maps the means assessment details into a list of
   * {@link uk.gov.laa.ccms.data.model.AssessmentResult} using the goal attribute from the
   * means rule base.
   *
   * @param meansAssessment the means assessment details to be mapped
   * @return a list containing the mapped means assessment result
   */
  @Named("mapMeansAssessment")
  default List<uk.gov.laa.ccms.data.model.AssessmentResult> mapMeansAssessment(
      final AssessmentDetail meansAssessment) {
    return mapAssessment(meansAssessment, AssessmentRulebase.MEANS.getGoalAttributeName());
  }

  /**
   * Maps the merits assessment details into a list of
   * {@link uk.gov.laa.ccms.data.model.AssessmentResult} using the goal attribute from the
   * merits rule base.
   *
   * @param meritsAssessment the merits assessment details to be mapped
   * @return a list containing the mapped merits assessment result
   */
  @Named("mapMeritsAssessment")
  default List<uk.gov.laa.ccms.data.model.AssessmentResult> mapMeritsAssessment(
      final AssessmentDetail meritsAssessment) {
    return mapAssessment(meritsAssessment, AssessmentRulebase.MERITS.getGoalAttributeName());
  }

  /**
   * Maps the provided assessment details into a list of
   * {@link uk.gov.laa.ccms.data.model.AssessmentResult}. An {@link OpaGoal} with the
   * specified goal attribute is created and added to the result.
   *
   * @param assessmentDetail the assessment details to be mapped
   * @param goalAttribute the goal attribute to be set in the assessment result
   * @return a list containing the mapped assessment result, or an empty list if the assessment
   *         detail is null
   */
  default List<uk.gov.laa.ccms.data.model.AssessmentResult> mapAssessment(
      final AssessmentDetail assessmentDetail, final String goalAttribute) {
    if (assessmentDetail == null) {
      return Collections.emptyList();
    }

    final uk.gov.laa.ccms.data.model.AssessmentResult assessmentResult =
        new uk.gov.laa.ccms.data.model.AssessmentResult();

    final OpaGoal opaGoal = new OpaGoal()
        .attribute(goalAttribute)
        .attributeValue("true");

    assessmentResult.addResultsItem(opaGoal);
    mapIntoAssessment(assessmentResult, assessmentDetail);

    return List.of(assessmentResult);
  }

  // MapStruct mappings for attributes
  @Mapping(target = "attribute", source = "name")
  @Mapping(target = "responseType", source = "type")
  @Mapping(target = "responseValue", source = "value")
  @Mapping(target = "userDefinedInd", source = ".",
      qualifiedByName = "mapUserDefinedInd")
  @Mapping(target = "caption", ignore = true)
  @Mapping(target = "responseText", ignore = true)
  OpaAttribute toOpaAttribute(AssessmentAttributeDetail attribute);


  @Named("toEbsRecordHistory")
  @Mapping(target = "dateCreated", source = "tdsApplication.auditTrail.created")
  @Mapping(target = "dateLastUpdated", source = "tdsApplication.auditTrail.lastSaved")
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "lastUpdatedBy.loginId", source = "user.loginId")
  @Mapping(target = "lastUpdatedBy.username", source = "user.username")
  @Mapping(target = "lastUpdatedBy.userType", source = "user.userType")
  RecordHistory toEbsRecordHistory(CaseMappingContext context);

  /**
   * Maps and returns a boolean indicating if the assessment attribute is user-defined.
   *
   * @param attribute the assessment attribute detail
   * @return {@code true} if the attribute is user-defined, {@code false} otherwise,
   *        or {@code null} if the attribute is null
   */
  @Named("mapUserDefinedInd")
  default Boolean mapUserDefinedInd(final AssessmentAttributeDetail attribute) {
    if (attribute == null) {
      return null;
    }
    return !"intermediate".equalsIgnoreCase(attribute.getInferencingType())
        && (!attribute.getName().startsWith("SA_"))
        && (!Boolean.TRUE.equals(attribute.getPrepopulated()));
  }

  /**
   * Maps and returns the person details from the provided opponent detail if it is of type
   * "individual".
   *
   * @param opponentDetail the opponent detail
   * @return the mapped person, or {@code null} if the opponent is not an individual
   */
  @Named("mapPerson")
  default OtherPartyPerson mapPerson(final OpponentDetail opponentDetail) {
    if (opponentDetail == null || opponentDetail.getType() == null) {
      return null;
    }
    return opponentDetail.getType().equalsIgnoreCase(OPPONENT_TYPE_INDIVIDUAL)
        ? toEbsPerson(opponentDetail)
        : null;
  }

  /**
   * Maps and returns the organisation details from the provided opponent detail if it is of type
   * "organisation".
   *
   * @param opponentDetail the opponent detail
   * @return the mapped organisation, or {@code null} if the opponent is not an organisation
   */
  @Named("mapOrganisation")
  default OtherPartyOrganisation mapOrganisation(final OpponentDetail opponentDetail) {
    if (opponentDetail == null || opponentDetail.getType() == null) {
      return null;
    }
    return opponentDetail.getType().equalsIgnoreCase(OPPONENT_TYPE_ORGANISATION)
        ? toEbsOrganisation(opponentDetail)
        : null;
  }


  /**
   * Maps and returns the proceeding ID by formatting the provided ID.
   * If the ID is not null, it is prefixed with "P_".
   *
   * @param id the proceeding ID to be formatted
   * @return the formatted proceeding ID, or {@code null} if the input ID is {@code null}
   */
  @Named("mapProceedingId")
  default String proceedingId(final Integer id) {
    return id != null
        ? String.format("P_%s", id)
        : null;
  }

  /**
   * Maps and returns the opponent ID from the provided opponent details.
   *
   * @param opponent the opponent details
   * @return the mapped opponent ID, or {@code null} if the opponent is {@code null}
   */
  @Named("mapOpponentId")
  default String opponentId(final OpponentDetail opponent) {
    if (opponent == null) {
      return null;
    }
    return getAssessmentMappingId(opponent);
  }

  /**
   * Maps the requested amount from the provided cost structure details.
   * If the requested cost limitation is not available, the default cost limitation is returned.
   *
   * @param costs the cost structure details
   * @return the requested or default cost limitation, or {@code null} if the cost structure is null
   */
  @Named("mapRequestedAmount")
  default BigDecimal mapRequestedAmount(final CostStructureDetail costs) {
    if (costs == null) {
      return null;
    }
    return costs.getRequestedCostLimitation() != null
        ? costs.getRequestedCostLimitation()
        : costs.getDefaultCostLimitation();
  }

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
