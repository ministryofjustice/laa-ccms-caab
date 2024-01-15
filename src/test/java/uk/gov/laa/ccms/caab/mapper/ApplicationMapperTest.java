package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST_DESCRIPTION;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL_DESCRIPTION;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_LAND;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_OTHER_ASSET;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_INDIVIDUAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_ORGANISATION;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildBaseApplication;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildPriorAuthorityDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildPriorAuthorityTypeDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildAddressDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildAssessmentResult;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildBaseClient;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildCaseDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildCaseSummary;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildCostAward;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildCostLimitation;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildFinancialAward;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildLandAward;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildLinkedCase;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildOtherAssetAward;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildOtherPartyOrganisation;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildOtherPartyPerson;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildPriorAuthority;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildProceedingDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildRecovery;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildScopeLimitation;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildTimeRelatedAward;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import uk.gov.laa.ccms.caab.mapper.context.ApplicationMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.CaseOutcomeMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.PriorAuthorityMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.ProceedingMappingContext;
import uk.gov.laa.ccms.caab.model.Address;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.AssessmentResult;
import uk.gov.laa.ccms.caab.model.BaseApplication;
import uk.gov.laa.ccms.caab.model.CaseOutcome;
import uk.gov.laa.ccms.caab.model.Client;
import uk.gov.laa.ccms.caab.model.CostAward;
import uk.gov.laa.ccms.caab.model.CostEntry;
import uk.gov.laa.ccms.caab.model.FinancialAward;
import uk.gov.laa.ccms.caab.model.LiableParty;
import uk.gov.laa.ccms.caab.model.LinkedCase;
import uk.gov.laa.ccms.caab.model.Opponent;
import uk.gov.laa.ccms.caab.model.OtherAssetAward;
import uk.gov.laa.ccms.caab.model.PriorAuthority;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ProceedingOutcome;
import uk.gov.laa.ccms.caab.model.ReferenceDataItem;
import uk.gov.laa.ccms.caab.model.TimeRecovery;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;
import uk.gov.laa.ccms.soa.gateway.model.AddressDetail;
import uk.gov.laa.ccms.soa.gateway.model.ApplicationDetails;
import uk.gov.laa.ccms.soa.gateway.model.Award;
import uk.gov.laa.ccms.soa.gateway.model.BaseClient;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseSummary;
import uk.gov.laa.ccms.soa.gateway.model.CostLimitation;
import uk.gov.laa.ccms.soa.gateway.model.OtherParty;
import uk.gov.laa.ccms.soa.gateway.model.OutcomeDetail;
import uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail;
import uk.gov.laa.ccms.soa.gateway.model.Recovery;
import uk.gov.laa.ccms.soa.gateway.model.ScopeLimitation;
import uk.gov.laa.ccms.soa.gateway.model.TimeRelatedAward;

public class ApplicationMapperTest {

  private final ApplicationMapper applicationMapper = new ApplicationMapperImpl();

  @Test
  void testToApplicationDetailDevolvedPowers() {
    CaseDetail soaCaseDetail = buildCaseDetail(APP_TYPE_EMERGENCY_DEVOLVED_POWERS);
    ApplicationMappingContext applicationMappingContext =
        buildApplicationMappingContext(
            soaCaseDetail,
            true,
            soaCaseDetail.getApplicationDetails().getDevolvedPowersDate());
    ApplicationDetail result = applicationMapper.toApplicationDetail(applicationMappingContext);

    assertNotNull(result);
    assertEquals(soaCaseDetail.getCaseReferenceNumber(), result.getCaseReferenceNumber());
    assertEquals(soaCaseDetail.getCertificateType(), result.getCertificate().getId());
    assertEquals(soaCaseDetail.getApplicationDetails().getApplicationAmendmentType(),
        result.getApplicationType().getId());
    assertEquals(applicationMappingContext.getApplicationType().getDescription(),
        result.getApplicationType().getDisplayValue());
    assertEquals(soaCaseDetail.getRecordHistory().getDateCreated(),
        result.getDateCreated());
    assertEquals(
        soaCaseDetail.getApplicationDetails().getProviderDetails().getProviderCaseReferenceNumber(),
        result.getProviderDetails().getProviderCaseReference());
    assertEquals(
        applicationMappingContext.getProviderDetail().getId(),
        result.getProviderDetails().getProvider().getId());
    assertEquals(
        applicationMappingContext.getProviderDetail().getName(),
        result.getProviderDetails().getProvider().getDisplayValue());
    assertEquals(
        soaCaseDetail.getApplicationDetails().getProviderDetails()
            .getContactUserId().getUserLoginId(),
        result.getProviderDetails().getProviderContact().getId());
    assertEquals(
        soaCaseDetail.getApplicationDetails().getProviderDetails()
            .getContactUserId().getUserName(),
        result.getProviderDetails().getProviderContact().getDisplayValue());
    assertEquals(applicationMappingContext.getProviderOffice().getId(),
        result.getProviderDetails().getOffice().getId());
    assertEquals(applicationMappingContext.getProviderOffice().getName(),
        result.getProviderDetails().getOffice().getDisplayValue());
    assertEquals(applicationMappingContext.getSupervisorContact().getId().toString(),
        result.getProviderDetails().getSupervisor().getId());
    assertEquals(applicationMappingContext.getSupervisorContact().getName(),
        result.getProviderDetails().getSupervisor().getDisplayValue());
    assertEquals(applicationMappingContext.getFeeEarnerContact().getId().toString(),
        result.getProviderDetails().getFeeEarner().getId());
    assertEquals(applicationMappingContext.getFeeEarnerContact().getName(),
        result.getProviderDetails().getFeeEarner().getDisplayValue());
    assertNotNull(result.getCorrespondenceAddress());  // Detail tested in specific test case
    assertNotNull(result.getClient()); // Detail tested in specific test case
    assertEquals(
        soaCaseDetail.getApplicationDetails().getCategoryOfLaw().getCategoryOfLawCode(),
        result.getCategoryOfLaw().getId());
    assertEquals(
        soaCaseDetail.getApplicationDetails().getCategoryOfLaw().getCategoryOfLawDescription(),
        result.getCategoryOfLaw().getDisplayValue());
    assertEquals(
        soaCaseDetail.getApplicationDetails().getCategoryOfLaw().getGrantedAmount(),
        result.getCosts().getGrantedCostLimitation());
    assertEquals(
        soaCaseDetail.getApplicationDetails().getCategoryOfLaw().getRequestedAmount(),
        result.getCosts().getRequestedCostLimitation());
    assertEquals(
        soaCaseDetail.getApplicationDetails().getCategoryOfLaw().getCategoryOfLawCode(),
        result.getCategoryOfLaw().getId());
    assertNull(result.getCosts().getDefaultCostLimitation());
    assertNotNull(result.getCosts().getCostEntries()); // Detail tested in specific test case
    assertEquals(
        soaCaseDetail.getApplicationDetails().getCategoryOfLaw().getCostLimitations().size(),
        result.getCosts().getCostEntries().size());
    assertNull(result.getCosts().getAuditTrail());
    assertEquals(
        soaCaseDetail.getApplicationDetails().getLarDetails().isLarScopeFlag(),
        result.getLarScopeFlag());
    assertEquals(
        soaCaseDetail.getCaseStatus().getActualCaseStatus(),
        result.getStatus().getId());
    assertEquals(
        soaCaseDetail.getCaseStatus().getDisplayCaseStatus(),
        result.getStatus().getDisplayValue());
    assertEquals(
        soaCaseDetail.getAvailableFunctions(),
        result.getAvailableFunctions());
    assertTrue(result.getApplicationType().getDevolvedPowers().getUsed());
    assertEquals(soaCaseDetail.getApplicationDetails().getDevolvedPowersDate(),
        result.getApplicationType().getDevolvedPowers().getDateUsed());
    assertNotNull(result.getCorrespondenceAddress());
    assertNotNull(result.getMeansAssessment());
    assertNotNull(result.getMeritsAssessment());
    assertNotNull(result.getOpponents());
    assertNotNull(result.getOpponents());
    assertEquals(2, result.getOpponents().size());
    assertEquals(OPPONENT_TYPE_INDIVIDUAL, result.getOpponents().get(0).getType());
    assertEquals(OPPONENT_TYPE_ORGANISATION, result.getOpponents().get(1).getType());

  }

  @Test
  void testToApplicationDetailNonDevolvedPowers() {
    ApplicationMappingContext applicationMappingContext =
        buildApplicationMappingContext(
            buildCaseDetail(APP_TYPE_EMERGENCY),
            false,
            null);

    ApplicationDetail result = applicationMapper.toApplicationDetail(applicationMappingContext);

    // Check the devolved powers
    assertFalse(result.getApplicationType().getDevolvedPowers().getUsed());
    assertNull(result.getApplicationType().getDevolvedPowers().getDateUsed());
  }

  @Test
  void testToCorrespondenceAddress() {
    ApplicationMappingContext applicationMappingContext =
        buildApplicationMappingContext(
            buildCaseDetail(APP_TYPE_EMERGENCY),
            false,
            null);
    ApplicationDetails soaApplicationDetails =
        applicationMappingContext.getSoaCaseDetail().getApplicationDetails();

    Address result = applicationMapper.toCorrespondenceAddress(applicationMappingContext);

    assertNotNull(result);
    assertEquals(soaApplicationDetails.getCorrespondenceAddress().getAddressLine1(),
        result.getAddressLine1());
    assertEquals(soaApplicationDetails.getCorrespondenceAddress().getAddressLine2(),
        result.getAddressLine2());
    assertEquals(soaApplicationDetails.getCorrespondenceAddress().getCareOfName(),
        result.getCareOf());
    assertEquals(soaApplicationDetails.getCorrespondenceAddress().getCity(),
        result.getCity());
    assertEquals(soaApplicationDetails.getCorrespondenceAddress().getCountry(),
        result.getCountry());
    assertEquals(soaApplicationDetails.getCorrespondenceAddress().getCounty(),
        result.getCounty());
    assertEquals(soaApplicationDetails.getCorrespondenceAddress().getHouse(),
        result.getHouseNameOrNumber());
    assertFalse(result.getNoFixedAbode());
    assertEquals(soaApplicationDetails.getPreferredAddress(), result.getPreferredAddress());
  }

    @Test
  void testToCostEntry() {
    CostLimitation soaCostLimitation = buildCostLimitation("");

    CostEntry result = applicationMapper.toCostEntry(soaCostLimitation);

    assertNotNull(result);
    assertEquals(soaCostLimitation.getBillingProviderId(), result.getLscResourceId());
    assertEquals(soaCostLimitation.getPaidToDate(), result.getAmountBilled());
    assertEquals(soaCostLimitation.getAmount(), result.getRequestedCosts());
    assertEquals(soaCostLimitation.getBillingProviderName(), result.getResourceName());
    assertEquals(soaCostLimitation.getCostLimitId(), result.getEbsId());
    assertEquals(soaCostLimitation.getCostCategory(), result.getCostCategory());
    assertFalse(result.getNewEntry());
    assertNull(result.getSubmitted());
  }

  @Test
  void testToProceeding() {
    ProceedingDetail soaProceeding = buildProceedingDetail(STATUS_DRAFT);
    ProceedingMappingContext proceedingMappingContext =
        buildProceedingMappingContext(soaProceeding);

    Proceeding result = applicationMapper.toProceeding(proceedingMappingContext);

    assertNotNull(result);
    assertFalse(result.getEdited());
    assertEquals(soaProceeding.getAvailableFunctions(),
        result.getAvailableFunctions());
    assertEquals(soaProceeding.getStage(),
        result.getStage());
    assertEquals(soaProceeding.getDateGranted(),
        result.getDateGranted());
    assertEquals(soaProceeding.getDateCostsValid(),
        result.getDateCostsValid());
    assertEquals(soaProceeding.getAvailableFunctions(),
        result.getAvailableFunctions());

    assertEquals(soaProceeding.getProceedingCaseId(),
        result.getEbsId());
    assertEquals(soaProceeding.isLeadProceedingIndicator(),
        result.getLeadProceedingInd());
    assertEquals(proceedingMappingContext.getMatterType().getCode(),
        result.getMatterType().getId());
    assertEquals(proceedingMappingContext.getMatterType().getDescription(),
        result.getMatterType().getDisplayValue());
    assertEquals(proceedingMappingContext.getProceedingLookup().getCode(),
        result.getProceedingType().getId());
    assertEquals(proceedingMappingContext.getProceedingLookup().getName(),
        result.getProceedingType().getDisplayValue());
    assertEquals(soaProceeding.getProceedingDescription(),
        result.getDescription());
    assertEquals(proceedingMappingContext.getProceedingLookup().getLarScope(),
        result.getLarScope());
    assertEquals(proceedingMappingContext.getLevelOfService().getCode(),
        result.getLevelOfService().getId());
    assertEquals(proceedingMappingContext.getLevelOfService().getDescription(),
        result.getLevelOfService().getDisplayValue());
    assertEquals(proceedingMappingContext.getClientInvolvement().getCode(),
        result.getClientInvolvement().getId());
    assertEquals(proceedingMappingContext.getClientInvolvement().getDescription(),
        result.getClientInvolvement().getDisplayValue());
    assertEquals(proceedingMappingContext.getProceedingStatusLookup().getCode(),
        result.getStatus().getId());
    assertEquals(proceedingMappingContext.getProceedingStatusLookup().getDescription(),
        result.getStatus().getDisplayValue());
    assertEquals(soaProceeding.getOrderType(),
        result.getTypeOfOrder().getId());

    assertNotNull(result.getScopeLimitations());
    assertEquals(1, result.getScopeLimitations().size());
    assertNotNull(result.getOutcome());
    assertNotNull(result.getCostLimitation());

    assertNull(result.getDefaultScopeLimitation());
    assertNull(result.getGrantedUsingDevolvedPowers());
    assertNull(result.getOrderTypeReqFlag());
    assertNull(result.getOrderTypeDisplayFlag());
    assertNull(result.getDeleteScopeLimitationFlag());
    assertNull(result.getAuditTrail());
  }

  @Test
  void testToScopeLimitation() {
    ScopeLimitation soaScopeLimitation = buildScopeLimitation("");
    CommonLookupValueDetail scopeLimitationLookup = new CommonLookupValueDetail()
        .code("scopecode")
        .description("scopedesc");

    uk.gov.laa.ccms.caab.model.ScopeLimitation result =
        applicationMapper.toScopeLimitation(
            Pair.of(soaScopeLimitation, scopeLimitationLookup));

    assertNotNull(result);
    assertEquals(soaScopeLimitation.getScopeLimitationId(), result.getEbsId());
    assertEquals(scopeLimitationLookup.getCode(), result.getScopeLimitation().getId());
    assertEquals(scopeLimitationLookup.getDescription(),
        result.getScopeLimitation().getDisplayValue());
    assertEquals(soaScopeLimitation.getScopeLimitationWording(),
        result.getScopeLimitationWording());
    assertEquals(soaScopeLimitation.isDelegatedFunctionsApply(),
        result.getDelegatedFuncApplyInd().getFlag());
    assertNull(result.getDefaultInd());
    assertNull(result.getNonDefaultWordingReqd());
    assertNull(result.getStage());
  }

  @Test
  void testToProceedingOutcome() {
    ProceedingDetail soaProceeding = buildProceedingDetail(STATUS_DRAFT);
    ProceedingMappingContext proceedingMappingContext =
        buildProceedingMappingContext(soaProceeding);
    OutcomeDetail soaOutcomeDetail = soaProceeding.getOutcome();

    ProceedingOutcome result = applicationMapper.toProceedingOutcome(proceedingMappingContext);

    assertNotNull(result);
    assertEquals(soaProceeding.getProceedingDescription(), result.getDescription());
    assertEquals(proceedingMappingContext.getMatterType().getCode(),
        result.getMatterType().getId());
    assertEquals(proceedingMappingContext.getMatterType().getDescription(),
        result.getMatterType().getDisplayValue());
    assertEquals(soaProceeding.getProceedingCaseId(), result.getProceedingCaseId());
    assertEquals(proceedingMappingContext.getProceedingLookup().getCode(),
        result.getProceedingType().getId());
    assertEquals(proceedingMappingContext.getProceedingLookup().getName(),
        result.getProceedingType().getDisplayValue());
    assertEquals(soaOutcomeDetail.getAltAcceptanceReason(), result.getAdrInfo());
    assertEquals(soaOutcomeDetail.getAltDisputeResolution(), result.getAlternativeResolution());
    assertEquals(proceedingMappingContext.getCourtLookup().getCode(), result.getCourtCode());
    assertEquals(proceedingMappingContext.getCourtLookup().getDescription(), result.getCourtName());
    assertEquals(soaOutcomeDetail.getFinalWorkDate(), result.getDateOfFinalWork());
    assertNull(result.getDateOfIssue());
    assertEquals(soaOutcomeDetail.getResolutionMethod(), result.getResolutionMethod());
    assertEquals(proceedingMappingContext.getOutcomeResultLookup().getOutcomeResult(),
        result.getResult().getId());
    assertEquals(proceedingMappingContext.getOutcomeResultLookup().getOutcomeResultDescription(),
        result.getResult().getDisplayValue());
    assertEquals(soaOutcomeDetail.getAdditionalResultInfo(), result.getResultInfo());
    assertEquals(soaOutcomeDetail.getStageEnd(), result.getStageEnd().getId());
    assertEquals(proceedingMappingContext.getStageEndLookup().getDescription(),
        result.getStageEnd().getDisplayValue());
    assertEquals(soaOutcomeDetail.getWiderBenefits(), result.getWiderBenefits());
    assertEquals(soaOutcomeDetail.getOutcomeCourtCaseNumber(), result.getOutcomeCourtCaseNo());
  }

  @Test
  void testMapMostRecentAssessment_SingleAssessment() {
    uk.gov.laa.ccms.soa.gateway.model.AssessmentResult soaAssessmentResult =
        buildAssessmentResult("");

    AssessmentResult result = applicationMapper.toAssessmentResult(soaAssessmentResult);

    assertNotNull(result);
    assertEquals(soaAssessmentResult.getAssessmentId(),
        result.getAssessmentId());
    assertEquals(soaAssessmentResult.getDate(),
        result.getDate());
    assertEquals(soaAssessmentResult.getAssessmentDetails().get(0).getCaption(),
        result.getAssessmentDetails().get(0).getCaption());
    assertEquals(soaAssessmentResult.getAssessmentDetails().get(0).getScreenName(),
        result.getAssessmentDetails().get(0).getScreenName());
    assertEquals(soaAssessmentResult.getAssessmentDetails().get(0).getEntity().get(0).getCaption(),
        result.getAssessmentDetails().get(0).getEntity().get(0).getCaption());
    assertEquals(
        soaAssessmentResult.getAssessmentDetails().get(0).getEntity().get(0).getEntityName(),
        result.getAssessmentDetails().get(0).getEntity().get(0).getEntityName());
    assertEquals(
        soaAssessmentResult.getAssessmentDetails().get(0).getEntity().get(0).getSequenceNumber(),
        result.getAssessmentDetails().get(0).getEntity().get(0).getSequenceNumber());
    assertEquals(
        soaAssessmentResult.getAssessmentDetails().get(0).getEntity().get(0).getInstances().get(0)
            .getCaption(),
        result.getAssessmentDetails().get(0).getEntity().get(0).getInstances().get(0).getCaption());
    assertEquals(
        soaAssessmentResult.getAssessmentDetails().get(0).getEntity().get(0).getInstances().get(0)
            .getInstanceLabel(),
        result.getAssessmentDetails().get(0).getEntity().get(0).getInstances().get(0)
            .getInstanceLabel());
    assertEquals(
        soaAssessmentResult.getAssessmentDetails().get(0).getEntity().get(0).getInstances().get(0)
            .getAttributes().get(0).getAttribute(),
        result.getAssessmentDetails().get(0).getEntity().get(0).getInstances().get(0)
            .getAttributes().get(0).getAttribute());
    assertEquals(
        soaAssessmentResult.getAssessmentDetails().get(0).getEntity().get(0).getInstances().get(0)
            .getAttributes().get(0).getCaption(),
        result.getAssessmentDetails().get(0).getEntity().get(0).getInstances().get(0)
            .getAttributes().get(0).getCaption());
    assertEquals(
        soaAssessmentResult.getAssessmentDetails().get(0).getEntity().get(0).getInstances().get(0)
            .getAttributes().get(0).getResponseText(),
        result.getAssessmentDetails().get(0).getEntity().get(0).getInstances().get(0)
            .getAttributes().get(0).getResponseText());
    assertEquals(
        soaAssessmentResult.getAssessmentDetails().get(0).getEntity().get(0).getInstances().get(0)
            .getAttributes().get(0).getResponseType(),
        result.getAssessmentDetails().get(0).getEntity().get(0).getInstances().get(0)
            .getAttributes().get(0).getResponseType());
    assertEquals(
        soaAssessmentResult.getAssessmentDetails().get(0).getEntity().get(0).getInstances().get(0)
            .getAttributes().get(0).getResponseValue(),
        result.getAssessmentDetails().get(0).getEntity().get(0).getInstances().get(0)
            .getAttributes().get(0).getResponseValue());
    assertEquals(soaAssessmentResult.getResults().get(0).getAttribute(),
        result.getResults().get(0).getAttribute());
    assertEquals(soaAssessmentResult.getResults().get(0).getAttributeValue(),
        result.getResults().get(0).getAttributeValue());
  }

  @Test
  void testToAddress() {
    AddressDetail soaAddress = buildAddressDetail("");

    Address result = applicationMapper.toAddress(soaAddress);
    assertNotNull(result);
    assertEquals(soaAddress.getCareOfName(), result.getCareOf());
    assertEquals(soaAddress.getHouse(), result.getHouseNameOrNumber());
    assertEquals(soaAddress.getPostalCode(), result.getPostcode());
    assertFalse(result.getNoFixedAbode());
    assertNull(result.getPreferredAddress());
  }

  @Test
  void testToClient() {
    BaseClient soaBaseClient = buildBaseClient();

    Client result = applicationMapper.toClient(soaBaseClient);

    assertNotNull(result);
    assertEquals(soaBaseClient.getClientReferenceNumber(),
        result.getReference());

    assertEquals(soaBaseClient.getFirstName(),
        result.getFirstName());
    assertEquals(soaBaseClient.getSurname(),
        result.getSurname());
  }

  @Test
  void testToIndividualOpponent() {
    OtherParty soaOtherParty = buildOtherPartyPerson();

    Opponent result = applicationMapper.toIndividualOpponent(soaOtherParty);

    assertNotNull(result);
    assertEquals(OPPONENT_TYPE_INDIVIDUAL, result.getType());
    assertEquals(soaOtherParty.getOtherPartyId(), result.getEbsId());
    assertEquals(soaOtherParty.getPerson().isCourtOrderedMeansAssesment(),
        result.getCourtOrderedMeansAssessment());
    assertEquals(soaOtherParty.getPerson().getOrganizationAddress(),
        result.getEmployerAddress());
    assertEquals(soaOtherParty.getPerson().getOrganizationName(),
        result.getEmployerName());
    assertEquals(soaOtherParty.getPerson().isPartyLegalAidedInd(),
        result.getLegalAided());
    assertEquals(soaOtherParty.getPerson().getNiNumber(),
        result.getNationalInsuranceNumber());
    assertEquals(soaOtherParty.getPerson().getRelationToCase(),
        result.getRelationshipToCase());
    assertEquals(soaOtherParty.getPerson().getRelationToClient(),
        result.getRelationshipToClient());
    assertEquals(soaOtherParty.getPerson().getContactDetails().getMobileNumber(),
        result.getTelephoneMobile());
    assertEquals(soaOtherParty.getPerson().getContactDetails().getFax(),
        result.getFaxNumber());
    assertEquals(soaOtherParty.getPerson().isPublicFundingAppliedInd(),
        result.getPublicFundingApplied());
    assertFalse(result.getDeleteInd());

    assertEquals(soaOtherParty.getPerson().getDateOfBirth(),
        result.getDateOfBirth());
    assertNotNull(result.getAddress()); // Detail tested elsewhere
    assertEquals(soaOtherParty.getPerson().getEmploymentStatus(),
        result.getEmploymentStatus());
    assertEquals(soaOtherParty.getPerson().getCertificateNumber(),
        result.getCertificateNumber());
    assertEquals(soaOtherParty.getPerson().getAssessedIncomeFrequency(),
        result.getAssessedIncomeFrequency());
    assertEquals(soaOtherParty.getPerson().getAssessedIncome(),
        result.getAssessedIncome());
    assertEquals(soaOtherParty.getPerson().getAssessedAssets(),
        result.getAssessedAssets());
    assertEquals(soaOtherParty.getPerson().getAssessmentDate(),
        result.getAssessmentDate());
    assertEquals(soaOtherParty.getPerson().getOtherInformation(),
        result.getOtherInformation());

    assertEquals(soaOtherParty.getPerson().getContactDetails().getTelephoneHome(),
        result.getTelephoneHome());
    assertEquals(soaOtherParty.getPerson().getContactDetails().getTelephoneWork(),
        result.getTelephoneWork());
    assertEquals(soaOtherParty.getPerson().getContactDetails().getEmailAddress(),
        result.getEmailAddress());

    assertEquals(soaOtherParty.getPerson().getName().getTitle(),
        result.getTitle());
    assertEquals(soaOtherParty.getPerson().getName().getFirstName(),
        result.getFirstName());
    assertEquals(soaOtherParty.getPerson().getName().getSurname(),
        result.getSurname());
    assertEquals(soaOtherParty.getPerson().getName().getMiddleName(),
        result.getMiddleNames());
  }

  @Test
  void testToOrganisationOpponent() {
    OtherParty soaOtherParty = buildOtherPartyOrganisation();

    Opponent result = applicationMapper.toOrganisationOpponent(soaOtherParty);

    assertNotNull(result);
    assertEquals(OPPONENT_TYPE_ORGANISATION, result.getType());
    assertEquals(soaOtherParty.getOtherPartyId(), result.getEbsId());
    assertEquals(soaOtherParty.getOrganisation().getOrganizationName(),
        result.getOrganisationName());
    assertEquals(soaOtherParty.getOrganisation().getOrganizationType(),
        result.getOrganisationType().getId());
    assertEquals(soaOtherParty.getOrganisation().getContactName(),
        result.getContactNameRole());
    assertEquals(soaOtherParty.getOrganisation().getRelationToCase(),
        result.getRelationshipToCase());
    assertEquals(soaOtherParty.getOrganisation().getRelationToClient(),
        result.getRelationshipToClient());
    assertEquals(soaOtherParty.getOrganisation().getContactDetails().getMobileNumber(),
        result.getTelephoneMobile());
    assertEquals(soaOtherParty.getOrganisation().getContactDetails().getFax(),
        result.getFaxNumber());
    assertEquals(soaOtherParty.getOrganisation().getRelationToCase(),
        result.getRelationshipToCase());
    assertFalse(result.getDeleteInd());

    assertEquals(Boolean.parseBoolean(soaOtherParty.getOrganisation().getCurrentlyTrading()),
        result.getCurrentlyTrading());
    assertEquals(soaOtherParty.getOrganisation().getOtherInformation(),
        result.getOtherInformation());
    assertNotNull(result.getAddress()); // Detail tested elsewhere
    assertEquals(soaOtherParty.getOrganisation().getContactDetails().getEmailAddress(),
        result.getEmailAddress());
    assertEquals(soaOtherParty.getOrganisation().getContactDetails().getTelephoneHome(),
        result.getTelephoneHome());
    assertEquals(soaOtherParty.getOrganisation().getContactDetails().getTelephoneWork(),
        result.getTelephoneWork());
  }


  @Test
  void testToLinkedCase() {
    uk.gov.laa.ccms.soa.gateway.model.LinkedCase soaLinkedCase = buildLinkedCase();

    LinkedCase result = applicationMapper.toLinkedCase(soaLinkedCase);

    assertNotNull(result);
    assertEquals(soaLinkedCase.getCaseReferenceNumber(), result.getLscCaseReference());
    assertEquals(soaLinkedCase.getClient().getClientReferenceNumber(),
        result.getClient().getReference());
    assertEquals(soaLinkedCase.getClient().getFirstName(),
        result.getClient().getFirstName());
    assertEquals(soaLinkedCase.getClient().getSurname(),
        result.getClient().getSurname());
    assertEquals(soaLinkedCase.getCategoryOfLawDesc(), result.getCategoryOfLaw());
    assertEquals(soaLinkedCase.getProviderReferenceNumber(), result.getProviderCaseReference());
    assertEquals(soaLinkedCase.getFeeEarnerName(), result.getFeeEarner());
    assertEquals(soaLinkedCase.getCaseStatus(), result.getStatus());
    assertEquals(soaLinkedCase.getLinkType(), result.getRelationToCase());
  }

  @Test
  void testToPriorAuthority() {
    uk.gov.laa.ccms.soa.gateway.model.PriorAuthority soaPriorAuthority =
        buildPriorAuthority();
    PriorAuthorityMappingContext priorAuthorityMappingContext =
        buildPriorAuthorityMappingContext(soaPriorAuthority);
    PriorAuthorityTypeDetail priorAuthorityTypeDetail =
        priorAuthorityMappingContext.getPriorAuthorityTypeLookup();

    PriorAuthority result = applicationMapper.toPriorAuthority(priorAuthorityMappingContext);

    assertNotNull(result);
    assertEquals(soaPriorAuthority.getDecisionStatus(), result.getStatus());
    assertEquals(soaPriorAuthority.getDescription(), result.getSummary());
    assertEquals(priorAuthorityTypeDetail.getCode(), result.getType().getId());
    assertEquals(priorAuthorityTypeDetail.getDescription(), result.getType().getDisplayValue());
    assertEquals(soaPriorAuthority.getReasonForRequest(), result.getJustification());
    assertEquals(soaPriorAuthority.getRequestAmount(), result.getAmountRequested());
    assertEquals(priorAuthorityTypeDetail.getValueRequired(), result.getValueRequired());
    assertNotNull(result.getItems());
    assertEquals(1, result.getItems().size()); // Detail tested elsewhere
  }

  @Test
  void testToReferenceDataItem() {
    PriorAuthorityDetail priorAuthorityDetail = buildPriorAuthorityDetail("dataType");
    CommonLookupValueDetail priorAuthLookup = new CommonLookupValueDetail();

    ReferenceDataItem result = applicationMapper.toReferenceDataItem(
        Pair.of(priorAuthorityDetail, priorAuthLookup));

    assertNotNull(result);
    assertEquals(priorAuthorityDetail.getCode(), result.getCode().getId());
    assertEquals(priorAuthorityDetail.getDescription(), result.getCode().getDisplayValue());
    assertEquals(priorAuthorityDetail.getDataType(), result.getType());
    assertEquals(priorAuthorityDetail.getLovCode(), result.getLovLookUp());
    assertEquals(priorAuthorityDetail.getMandatoryFlag(), result.getMandatory());
  }

  @Test
  void testToCaseOutcome() {
    CaseDetail soaCaseDetail = buildCaseDetail(APP_TYPE_EMERGENCY);
    CaseOutcomeMappingContext caseOutcomeMappingContext = buildCaseOutcomeMappingContext(soaCaseDetail);

    CaseOutcome result = applicationMapper.toCaseOutcome(caseOutcomeMappingContext);

    assertNotNull(result);
    assertEquals(soaCaseDetail.getLegalHelpCosts(), result.getLegalCosts());
    assertEquals(soaCaseDetail.getApplicationDetails().getLarDetails().getLegalHelpOfficeCode(),
        result.getOfficeCode());
    assertEquals(soaCaseDetail.getApplicationDetails().getLarDetails().getLegalHelpUfn(),
        result.getUniqueFileNo());
    assertEquals(soaCaseDetail.getDischargeStatus().getOtherDetails(),
        result.getOtherDetails());
    assertEquals(soaCaseDetail.getDischargeStatus().getReason(),
        result.getDischargeReason());
    assertEquals(soaCaseDetail.getDischargeStatus().isClientContinuePvtInd(),
        result.getClientContinueInd());
  }


  @Test
  void testToTimeRecovery() {
    // Create a TimeRelatedAward object for testing
    TimeRelatedAward timeRelatedAward = buildTimeRelatedAward();

    TimeRecovery result = applicationMapper.toTimeRecovery(timeRelatedAward);

    assertNotNull(result);
    assertEquals(timeRelatedAward.getDescription(), result.getDescription());
    assertEquals(timeRelatedAward.getAwardDate(), result.getEffectiveDate());
    assertEquals(timeRelatedAward.getAwardType(), result.getAwardType());
    assertEquals(timeRelatedAward.getAmount(), result.getAwardAmount());
    assertEquals(timeRelatedAward.getAwardTriggeringEvent(), result.getTriggeringEvent());
    assertEquals(timeRelatedAward.getOtherDetails(), result.getTimeRelatedRecoveryDetails());
  }

  @Test
  void testToRecovery() {
    Recovery soaRecovery = buildRecovery();

    uk.gov.laa.ccms.caab.model.Recovery result = applicationMapper.toRecovery(soaRecovery);

    assertNotNull(result);
    assertEquals(soaRecovery.getAwardValue(), result.getAwardAmount());
    assertEquals(soaRecovery.getRecoveredAmount().getClient().getPaidToLsc(),
        result.getClientAmountPaidToLsc());
    assertEquals(soaRecovery.getRecoveredAmount().getClient().getAmount(),
        result.getClientRecoveryAmount());
    assertEquals(soaRecovery.getRecoveredAmount().getClient().getDateReceived(),
        result.getClientRecoveryDate());
    assertEquals(soaRecovery.getRecoveredAmount().getCourt().getPaidToLsc(),
        result.getCourtAmountPaidToLsc());
    assertEquals(soaRecovery.getRecoveredAmount().getCourt().getAmount(),
        result.getCourtRecoveryAmount());
    assertEquals(soaRecovery.getRecoveredAmount().getCourt().getDateReceived(),
        result.getCourtRecoveryDate());
    assertEquals(soaRecovery.getRecoveredAmount().getSolicitor().getPaidToLsc(),
        result.getSolicitorAmountPaidToLsc());
    assertEquals(soaRecovery.getRecoveredAmount().getSolicitor().getAmount(),
        result.getSolicitorRecoveryAmount());
    assertEquals(soaRecovery.getRecoveredAmount().getSolicitor().getDateReceived(),
        result.getSolicitorRecoveryDate());
    assertEquals(soaRecovery.getOfferedAmount().getAmount(), result.getOfferedAmount());
    assertEquals(soaRecovery.getOfferedAmount().getConditionsOfOffer(),
        result.getConditionsOfOffer());
    assertEquals(soaRecovery.getOfferedAmount().getConditionsOfOffer(), result.getDetailsOfOffer());
    assertEquals(soaRecovery.isLeaveOfCourtReqdInd(), result.getLeaveOfCourtRequiredInd());
    assertNull(result.getAwardType()); // Populated by a specific award mapper method
    assertNull(result.getDescription()); // Populated by a specific award mapper method

    // From the afterMapping, the sum of recovered amounts
    assertEquals(
        soaRecovery.getRecoveredAmount().getClient().getAmount()
            .add(soaRecovery.getRecoveredAmount().getCourt().getAmount())
            .add(soaRecovery.getRecoveredAmount().getSolicitor().getAmount()),
        result.getRecoveredAmount());
    assertEquals(result.getAwardAmount().subtract(result.getRecoveredAmount()),
        result.getUnrecoveredAmount());
  }

  @Test
  void testToLiableParty() {
    String partyId = "party1";

    LiableParty result = applicationMapper.toLiableParty(partyId);

    assertNotNull(result);
    assertEquals(partyId, result.getOpponentId());
    assertNull(result.getAuditTrail());
    assertNull(result.getAwardType());
  }

  @Test
  void testToCostAward() {
    Award soaAward = buildCostAward();

    CostAward result = applicationMapper.toCostAward(soaAward);

    assertNotNull(result);
    assertEquals(soaAward.getAwardId(), result.getEbsId());
    assertEquals(AWARD_TYPE_COST, result.getAwardType());
    assertEquals(AWARD_TYPE_COST_DESCRIPTION, result.getDescription());
    assertEquals(soaAward.getAwardType(), result.getAwardCode());
    assertEquals(soaAward.getCostAward().getOrderDate(), result.getDateOfOrder());
    assertEquals(soaAward.getCostAward().getPreCertificateAwardLsc(),
        result.getPreCertificateLscCost());
    assertEquals(soaAward.getCostAward().getPreCertificateAwardOth(),
        result.getPreCertificateOtherCost());
    assertEquals(soaAward.getCostAward().getCertificateCostRateLsc(),
        result.getCertificateCostLsc());
    assertNotNull(result.getRecovery()); // detail tested separately
    assertNotNull(result.getLiableParties()); // detail tested separately
    assertEquals(soaAward.getCostAward().getLiableParties().size(),
        result.getLiableParties().size());

    // Like for like mappings
    assertEquals(soaAward.isDeleteAllowed(), result.getDeleteAllowed());
    assertEquals(soaAward.isUpdateAllowed(), result.getUpdateAllowed());
    assertEquals(soaAward.getCostAward().getServiceAddress().getAddressLine1(),
        result.getAddressLine1());
    assertEquals(soaAward.getCostAward().getServiceAddress().getAddressLine2(),
        result.getAddressLine2());
    assertEquals(soaAward.getCostAward().getServiceAddress().getAddressLine3(),
        result.getAddressLine3());
    assertEquals(soaAward.getCostAward().getCertificateCostRateMarket(),
        result.getCertificateCostMarket());
    assertEquals(soaAward.getCostAward().getCourtAssessmentStatus(),
        result.getCourtAssessmentStatus());
    assertEquals(soaAward.getCostAward().getOrderDateServed(),
        result.getOrderServedDate());
    assertEquals(soaAward.getCostAward().getInterestAwardedRate(),
        result.getInterestAwardedRate());
    assertEquals(soaAward.getCostAward().getInterestAwardedStartDate(),
        result.getInterestStartDate());
    assertEquals(soaAward.getCostAward().getAwardedBy(),
        result.getAwardedBy());

    assertEquals(soaAward.getCostAward().getOtherDetails(), result.getOtherDetails());

    // afterMapping
    assertEquals(AWARD_TYPE_COST, result.getRecovery().getAwardType());
    assertEquals(AWARD_TYPE_COST_DESCRIPTION, result.getRecovery().getDescription());
    assertEquals(soaAward.getCostAward().getCertificateCostRateLsc()
            .add(soaAward.getCostAward().getCertificateCostRateMarket()),
        result.getTotalCertCostsAwarded());

    // afterMapping (baseAward)
    result.getLiableParties().forEach(
        liableParty -> assertEquals(AWARD_TYPE_COST, liableParty.getAwardType()));
  }

  @Test
  void testToFinancialAward() {
    Award soaAward = buildFinancialAward();

    FinancialAward result = applicationMapper.toFinancialAward(soaAward);

    assertNotNull(result);
    assertEquals(soaAward.getAwardId(), result.getEbsId());
    assertEquals(AWARD_TYPE_FINANCIAL, result.getAwardType());
    assertEquals(AWARD_TYPE_FINANCIAL_DESCRIPTION, result.getDescription());
    assertEquals(soaAward.getAwardType(), result.getAwardCode());
    assertEquals(soaAward.getFinancialAward().getOrderDate(), result.getDateOfOrder());
    assertEquals(soaAward.getFinancialAward().getAmount(),
        result.getAwardAmount());
    assertEquals(soaAward.getFinancialAward().getOrderDateServed(),
        result.getOrderServedDate());
    assertEquals(soaAward.getFinancialAward().getStatutoryChangeReason(),
        result.getStatutoryChargeExemptionReason());

    assertNotNull(result.getRecovery()); // detail tested separately

    assertNotNull(result.getLiableParties());
    assertEquals(soaAward.getFinancialAward().getLiableParties().size(),
        result.getLiableParties().size());

    // Like for like mappings
    assertEquals(soaAward.isDeleteAllowed(), result.getDeleteAllowed());
    assertEquals(soaAward.isUpdateAllowed(), result.getUpdateAllowed());
    assertEquals(soaAward.getFinancialAward().getServiceAddress().getAddressLine1(),
        result.getAddressLine1());
    assertEquals(soaAward.getFinancialAward().getServiceAddress().getAddressLine2(),
        result.getAddressLine2());
    assertEquals(soaAward.getFinancialAward().getServiceAddress().getAddressLine3(),
        result.getAddressLine3());
    assertEquals(soaAward.getFinancialAward().getInterimAward().toString(),
        result.getInterimAward());
    assertEquals(soaAward.getFinancialAward().getAwardedBy(), result.getAwardedBy());
    assertEquals(soaAward.getFinancialAward().getOrderDateServed(),
        result.getOrderServedDate());
    assertEquals(soaAward.getFinancialAward().getAwardJustifications(),
        result.getAwardJustifications());
    assertEquals(soaAward.getFinancialAward().getOtherDetails(), result.getOtherDetails());

    // afterMapping
    assertEquals(AWARD_TYPE_FINANCIAL, result.getRecovery().getAwardType());
    assertEquals(AWARD_TYPE_FINANCIAL_DESCRIPTION, result.getRecovery().getDescription());

    // afterMapping (baseAward)
    result.getLiableParties().forEach(
        liableParty -> assertEquals(AWARD_TYPE_FINANCIAL, liableParty.getAwardType()));
  }

  @Test
  void testToLandAward() {
    Award soaAward = buildLandAward();

    uk.gov.laa.ccms.caab.model.LandAward result = applicationMapper.toLandAward(soaAward);

    assertNotNull(result);
    assertEquals(soaAward.getAwardType(), result.getAwardCode());
    assertEquals(soaAward.getAwardId(), result.getEbsId());
    assertEquals(soaAward.getLandAward().getOrderDate(), result.getDateOfOrder());
    assertEquals(AWARD_TYPE_LAND, result.getAwardType());
    assertEquals(soaAward.getLandAward().getValuation().getAmount(),
        result.getValuationAmount());
    assertEquals(soaAward.getLandAward().getValuation().getCriteria(),
        result.getValuationCriteria());
    assertEquals(soaAward.getLandAward().getValuation().getDate(),
        result.getValuationDate());
    assertNotNull(result.getTimeRecovery());
    assertNotNull(result.getLiableParties());
    assertEquals(soaAward.getLandAward().getOtherProprietors().size(),
        result.getLiableParties().size());

    // Like for like mappings
    assertEquals(soaAward.isDeleteAllowed(), result.getDeleteAllowed());
    assertEquals(soaAward.isUpdateAllowed(), result.getUpdateAllowed());
    assertEquals(soaAward.getLandAward().getDescription(), result.getDescription());
    assertEquals(soaAward.getLandAward().getTitleNo(), result.getTitleNo());
    assertEquals(soaAward.getLandAward().getPropertyAddress().getAddressLine1(),
        result.getAddressLine1());
    assertEquals(soaAward.getLandAward().getPropertyAddress().getAddressLine2(),
        result.getAddressLine2());
    assertEquals(soaAward.getLandAward().getPropertyAddress().getAddressLine3(),
        result.getAddressLine3());
    assertEquals(soaAward.getLandAward().getDisputedPercentage(), result.getDisputedPercentage());
    assertEquals(soaAward.getLandAward().getAwardedPercentage(), result.getAwardedPercentage());
    assertEquals(soaAward.getLandAward().getMortgageAmountDue(), result.getMortgageAmountDue());
    assertEquals(soaAward.getLandAward().getAwardedBy(), result.getAwardedBy());
    assertEquals(soaAward.getLandAward().getRecovery(), result.getRecovery());
    assertEquals(soaAward.getLandAward().getNoRecoveryDetails(), result.getNoRecoveryDetails());
    assertEquals(soaAward.getLandAward().getStatChargeExemptReason(),
        result.getStatChargeExemptReason());
    assertEquals(soaAward.getLandAward().getLandChargeRegistration(),
        result.getLandChargeRegistration());
    assertEquals(soaAward.getLandAward().getRegistrationRef(),
        result.getRegistrationRef());

    // afterMapping
    assertTrue(result.getRecoveryOfAwardTimeRelated());
    assertEquals(AWARD_TYPE_LAND, result.getTimeRecovery().getAwardType());
    assertEquals(result.getValuationAmount().subtract(result.getMortgageAmountDue()),
        result.getEquity());

    // afterMapping (baseAward)
    result.getLiableParties().forEach(
        liableParty -> assertEquals(AWARD_TYPE_LAND, liableParty.getAwardType()));
  }

  @Test
  void testToOtherAssetAward() {
    Award soaAward = buildOtherAssetAward();

    OtherAssetAward result = applicationMapper.toOtherAssetAward(soaAward);

    assertNotNull(result);
    assertEquals(soaAward.getAwardId(), result.getEbsId());
    assertEquals(soaAward.getOtherAsset().getOrderDate(), result.getDateOfOrder());
    assertEquals(AWARD_TYPE_OTHER_ASSET, result.getAwardType());
    assertEquals(soaAward.getAwardType(), result.getAwardCode());
    assertEquals(soaAward.getOtherAsset().getValuation().getAmount(),
        result.getValuationAmount());
    assertEquals(soaAward.getOtherAsset().getValuation().getCriteria(),
        result.getValuationCriteria());
    assertEquals(soaAward.getOtherAsset().getValuation().getDate(),
        result.getValuationDate());
    assertNotNull(result.getTimeRecovery());
    assertNotNull(result.getLiableParties());
    assertEquals(soaAward.getOtherAsset().getHeldBy().size(),
        result.getLiableParties().size());

    // Like for like mappings
    assertEquals(soaAward.isDeleteAllowed(), result.getDeleteAllowed());
    assertEquals(soaAward.isUpdateAllowed(), result.getUpdateAllowed());
    assertEquals(soaAward.getOtherAsset().getDescription(), result.getDescription());
    assertEquals(soaAward.getOtherAsset().getAwardedBy(), result.getAwardedBy());
    assertEquals(soaAward.getOtherAsset().getAwardedAmount(), result.getAwardedAmount());
    assertEquals(soaAward.getOtherAsset().getAwardedPercentage(), result.getAwardedPercentage());
    assertEquals(soaAward.getOtherAsset().getRecoveredAmount(), result.getRecoveredAmount());
    assertEquals(soaAward.getOtherAsset().getRecoveredPercentage(),
        result.getRecoveredPercentage());
    assertEquals(soaAward.getOtherAsset().getDisputedAmount(), result.getDisputedAmount());
    assertEquals(soaAward.getOtherAsset().getDisputedPercentage(), result.getDisputedPercentage());
    assertEquals(soaAward.getOtherAsset().getRecovery(), result.getRecovery());
    assertEquals(soaAward.getOtherAsset().getNoRecoveryDetails(), result.getNoRecoveryDetails());
    assertEquals(soaAward.getOtherAsset().getStatChargeExemptReason(),
        result.getStatChargeExemptReason());

    // afterMapping
    assertTrue(result.getRecoveryOfAwardTimeRelated());
    assertEquals(AWARD_TYPE_OTHER_ASSET, result.getTimeRecovery().getAwardType());

    // afterMapping (baseAward)
    result.getLiableParties().forEach(
        liableParty -> assertEquals(AWARD_TYPE_OTHER_ASSET, liableParty.getAwardType()));
  }

  @Test
  public void testToApplicationDetails() {
    List<BaseApplication> baseApplicationList = List.of(
        buildBaseApplication(1),
        buildBaseApplication(2));

    uk.gov.laa.ccms.caab.model.ApplicationDetails result =
        applicationMapper.toApplicationDetails(new PageImpl<>(baseApplicationList));

    assertNotNull(result);
    assertEquals(2, result.getSize());
    assertNotNull(result.getContent());
    assertEquals(baseApplicationList, result.getContent());
  }

  @Test
  public void testToBaseApplication() {
    CaseSummary soaCaseSummary = buildCaseSummary();

    BaseApplication result = applicationMapper.toBaseApplication(soaCaseSummary);

    assertNotNull(result);
    assertEquals(soaCaseSummary.getCaseReferenceNumber(), result.getCaseReferenceNumber());
    assertEquals(soaCaseSummary.getCaseStatusDisplay(), result.getStatus().getDisplayValue());
    assertEquals(soaCaseSummary.getCategoryOfLaw(), result.getCategoryOfLaw().getDisplayValue());
    assertEquals(soaCaseSummary.getClient().getClientReferenceNumber(), result.getClient().getReference());
    assertEquals(soaCaseSummary.getClient().getFirstName(), result.getClient().getFirstName());
    assertEquals(soaCaseSummary.getClient().getSurname(), result.getClient().getSurname());
    assertNotNull(result.getProviderDetails());
    assertEquals(soaCaseSummary.getFeeEarnerName(), result.getProviderDetails().getFeeEarner().getDisplayValue());
    assertEquals(soaCaseSummary.getProviderCaseReferenceNumber(), result.getProviderDetails().getProviderCaseReference());
  }

  private ApplicationMappingContext buildApplicationMappingContext(
      CaseDetail soaCase,
      Boolean devolvedPowers,
      Date devolvedPowersDate) {
    return ApplicationMappingContext.builder()
        .soaCaseDetail(soaCase)
        .applicationType(new CommonLookupValueDetail()
            .code("apptypecode")
            .description("apptypedesc"))
        .amendmentProceedingsInEbs(Collections.singletonList(
            buildProceedingMappingContext(soaCase.getApplicationDetails().getProceedings().get(0))))
        .caseOutcome(buildCaseOutcomeMappingContext(soaCase))
        .caseWithOnlyDraftProceedings(Boolean.TRUE)
        .currentProviderBilledAmount(BigDecimal.ONE)
        .devolvedPowers(Pair.of(devolvedPowers, devolvedPowersDate))
        .feeEarnerContact(new uk.gov.laa.ccms.data.model.ContactDetail()
            .id(100)
            .name("feeEarnerName"))
        .supervisorContact(new uk.gov.laa.ccms.data.model.ContactDetail()
            .id(101)
            .name("supName"))
        .meansAssessment(soaCase.getApplicationDetails().getMeansAssesments().get(0))
        .meritsAssessment(soaCase.getApplicationDetails().getMeritsAssesments().get(0))
        .priorAuthorities(Collections.singletonList(buildPriorAuthorityMappingContext(
            soaCase.getPriorAuthorities().get(0))))
        .proceedings(Collections.singletonList(
            buildProceedingMappingContext(soaCase.getApplicationDetails().getProceedings().get(0))))
        .providerDetail(new uk.gov.laa.ccms.data.model.ProviderDetail()
            .id(1)
            .name("provname"))
        .providerOffice(new OfficeDetail().id(1000).name("offName"))
        .build();
  }

  private ProceedingMappingContext buildProceedingMappingContext(ProceedingDetail soaProceeding) {
    return ProceedingMappingContext.builder()
        .soaProceeding(soaProceeding)
        .clientInvolvement(new CommonLookupValueDetail()
            .code("clientInv")
            .description("clientDesc"))
        .proceedingCostLimitation(BigDecimal.TEN)
        .proceedingStatusLookup(new CommonLookupValueDetail()
            .code("procStatCode")
            .description("procStatDesc"))
        .levelOfService(new CommonLookupValueDetail()
            .code("losCode")
            .description("losDescr"))
        .proceedingLookup(new uk.gov.laa.ccms.data.model.ProceedingDetail()
            .code("procCode")
            .name("procName")
            .larScope("procLarScope"))
        .scopeLimitations(Collections.singletonList(Pair.of(buildScopeLimitation(""),
            new CommonLookupValueDetail()
                .code("scopeLimitCode")
                .description("scopeLimitDescr"))))
        .outcomeResultLookup(new OutcomeResultLookupValueDetail()
            .outcomeResult("or")
            .outcomeResultDescription("orDesc"))
        .courtLookup(new CommonLookupValueDetail()
            .code("crt")
            .description("crtDescr"))
        .stageEndLookup(new StageEndLookupValueDetail()
            .stageEnd("se")
            .description("seDescr"))
        .matterType(new CommonLookupValueDetail()
            .code("mat")
            .description("matDescr"))
        .build();
  }

  private CaseOutcomeMappingContext buildCaseOutcomeMappingContext(CaseDetail soaCase) {
    return CaseOutcomeMappingContext.builder()
        .soaCase(soaCase)
        .costAwards(Collections.singletonList(soaCase.getAwards().get(0)))
        .financialAwards(Collections.singletonList(soaCase.getAwards().get(1)))
        .landAwards(Collections.singletonList(soaCase.getAwards().get(2)))
        .otherAssetAwards(Collections.singletonList(soaCase.getAwards().get(3)))
        .proceedingOutcomes(Collections.singletonList(buildProceedingMappingContext(
            soaCase.getApplicationDetails().getProceedings().get(0))))
        .build();
  }

  private PriorAuthorityMappingContext buildPriorAuthorityMappingContext(
      uk.gov.laa.ccms.soa.gateway.model.PriorAuthority soaPriorAuthority) {
    return PriorAuthorityMappingContext.builder()
        .soaPriorAuthority(soaPriorAuthority)
        .priorAuthorityTypeLookup(buildPriorAuthorityTypeDetail("dataType"))
        .items(Collections.singletonList(Pair.of(buildPriorAuthorityDetail("dataType"),
            new CommonLookupValueDetail()
                .code("priorAuthCode")
                .description("priorAuthDesc"))))
        .build();
  }




}