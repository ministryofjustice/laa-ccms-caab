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
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildApplicationDetail;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildBaseApplication;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildCaseSummary;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildPriorAuthorityDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildPriorAuthorityTypeDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildAddressDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildAssessmentResult;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildBaseClient;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildCaseDetail;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.PageImpl;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.mapper.context.CaseMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.SoaApplicationMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.SoaCaseOutcomeMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.SoaPriorAuthorityMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.SoaProceedingMappingContext;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.AssessmentResult;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.BaseEvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.BooleanDisplayValue;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.ClientDetail;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
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
import uk.gov.laa.ccms.data.model.CaseSummary;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.Award;
import uk.gov.laa.ccms.soa.gateway.model.BaseClient;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseDoc;
import uk.gov.laa.ccms.soa.gateway.model.CategoryOfLaw;
import uk.gov.laa.ccms.soa.gateway.model.CostLimitation;
import uk.gov.laa.ccms.soa.gateway.model.LinkedCase;
import uk.gov.laa.ccms.soa.gateway.model.OpaAttribute;
import uk.gov.laa.ccms.soa.gateway.model.OtherParty;
import uk.gov.laa.ccms.soa.gateway.model.OtherPartyOrganisation;
import uk.gov.laa.ccms.soa.gateway.model.OtherPartyPerson;
import uk.gov.laa.ccms.soa.gateway.model.OutcomeDetail;
import uk.gov.laa.ccms.soa.gateway.model.PriorAuthority;
import uk.gov.laa.ccms.soa.gateway.model.PriorAuthorityAttribute;
import uk.gov.laa.ccms.soa.gateway.model.RecordHistory;
import uk.gov.laa.ccms.soa.gateway.model.Recovery;
import uk.gov.laa.ccms.soa.gateway.model.ScopeLimitation;
import uk.gov.laa.ccms.soa.gateway.model.SubmittedApplicationDetails;
import uk.gov.laa.ccms.soa.gateway.model.TimeRelatedAward;

@DisplayName("SOA Application Mapper Test")
class SoaApplicationMapperTest {

  private final SoaApplicationMapper applicationMapper = new SoaApplicationMapperImpl() {
  };

  private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  @Test
  void testToApplicationDetailDevolvedPowers() {
    CaseDetail soaCaseDetail = buildCaseDetail(APP_TYPE_EMERGENCY_DEVOLVED_POWERS);
    SoaApplicationMappingContext applicationMappingContext =
        buildApplicationMappingContext(
            soaCaseDetail,
            true,
            soaCaseDetail.getApplicationDetails().getDevolvedPowersDate());
    ApplicationDetail result = applicationMapper.toApplicationDetail(applicationMappingContext);

    assertNotNull(result);
    assertEquals(soaCaseDetail.getCaseReferenceNumber(), result.getCaseReferenceNumber());
    assertEquals(applicationMappingContext.getCertificate().getCode(),
        result.getCertificate().getId());
    assertEquals(applicationMappingContext.getCertificate().getDescription(),
        result.getCertificate().getDisplayValue());
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
    assertEquals(OPPONENT_TYPE_INDIVIDUAL, result.getOpponents().getFirst().getType());
    assertEquals(OPPONENT_TYPE_ORGANISATION, result.getOpponents().get(1).getType());

  }

  @Test
  void testToApplicationDetailNonDevolvedPowers() {
    SoaApplicationMappingContext applicationMappingContext =
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
    SoaApplicationMappingContext applicationMappingContext =
        buildApplicationMappingContext(
            buildCaseDetail(APP_TYPE_EMERGENCY),
            false,
            null);
    SubmittedApplicationDetails soaApplicationDetails =
        applicationMappingContext.getSoaCaseDetail().getApplicationDetails();

    AddressDetail result = applicationMapper.toCorrespondenceAddress(applicationMappingContext);

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

    CostEntryDetail result = applicationMapper.toCostEntry(soaCostLimitation);

    assertNotNull(result);
    assertEquals(soaCostLimitation.getBillingProviderId(), result.getLscResourceId());
    assertEquals(soaCostLimitation.getPaidToDate(), result.getAmountBilled());
    assertEquals(soaCostLimitation.getAmount(), result.getRequestedCosts());
    assertEquals(soaCostLimitation.getBillingProviderName(), result.getResourceName());
    assertEquals(soaCostLimitation.getCostLimitId(), result.getEbsId());
    assertEquals(soaCostLimitation.getCostCategory(), result.getCostCategory());
    assertFalse(result.getNewEntry());
    assertTrue(result.getSubmitted()); // Defaults to true in caab-api model
  }

  @Test
  void testToProceeding() {
    uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail soaProceeding =
        buildProceedingDetail(STATUS_DRAFT);
    SoaProceedingMappingContext proceedingMappingContext =
        buildProceedingMappingContext(soaProceeding);

    ProceedingDetail result = applicationMapper.toProceeding(proceedingMappingContext);

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

    ScopeLimitationDetail result =
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
    uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail soaProceeding =
        buildProceedingDetail(STATUS_DRAFT);
    SoaProceedingMappingContext proceedingMappingContext =
        buildProceedingMappingContext(soaProceeding);
    OutcomeDetail soaOutcomeDetail = soaProceeding.getOutcome();

    ProceedingOutcomeDetail result = applicationMapper.toProceedingOutcome(proceedingMappingContext);

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
    assertEquals(soaAssessmentResult.getAssessmentDetails().getFirst().getCaption(),
        result.getAssessmentDetails().getFirst().getCaption());
    assertEquals(soaAssessmentResult.getAssessmentDetails().getFirst().getScreenName(),
        result.getAssessmentDetails().getFirst().getScreenName());
    assertEquals(soaAssessmentResult.getAssessmentDetails().getFirst().getEntity().getFirst().getCaption(),
        result.getAssessmentDetails().getFirst().getEntity().getFirst().getCaption());
    assertEquals(
        soaAssessmentResult.getAssessmentDetails().getFirst().getEntity().getFirst().getEntityName(),
        result.getAssessmentDetails().getFirst().getEntity().getFirst().getEntityName());
    assertEquals(
        soaAssessmentResult.getAssessmentDetails().getFirst().getEntity().getFirst().getSequenceNumber(),
        result.getAssessmentDetails().getFirst().getEntity().getFirst().getSequenceNumber());
    assertEquals(
        soaAssessmentResult.getAssessmentDetails().getFirst().getEntity().getFirst().getInstances().getFirst()
            .getCaption(),
        result.getAssessmentDetails().getFirst().getEntity().getFirst().getInstances().getFirst().getCaption());
    assertEquals(
        soaAssessmentResult.getAssessmentDetails().getFirst().getEntity().getFirst().getInstances().getFirst()
            .getInstanceLabel(),
        result.getAssessmentDetails().getFirst().getEntity().getFirst().getInstances().getFirst()
            .getInstanceLabel());
    assertEquals(
        soaAssessmentResult.getAssessmentDetails().getFirst().getEntity().getFirst().getInstances().getFirst()
            .getAttributes().getFirst().getAttribute(),
        result.getAssessmentDetails().getFirst().getEntity().getFirst().getInstances().getFirst()
            .getAttributes().getFirst().getAttribute());
    assertEquals(
        soaAssessmentResult.getAssessmentDetails().getFirst().getEntity().getFirst().getInstances().getFirst()
            .getAttributes().getFirst().getCaption(),
        result.getAssessmentDetails().getFirst().getEntity().getFirst().getInstances().getFirst()
            .getAttributes().getFirst().getCaption());
    assertEquals(
        soaAssessmentResult.getAssessmentDetails().getFirst().getEntity().getFirst().getInstances().getFirst()
            .getAttributes().getFirst().getResponseText(),
        result.getAssessmentDetails().getFirst().getEntity().getFirst().getInstances().getFirst()
            .getAttributes().getFirst().getResponseText());
    assertEquals(
        soaAssessmentResult.getAssessmentDetails().getFirst().getEntity().getFirst().getInstances().getFirst()
            .getAttributes().getFirst().getResponseType(),
        result.getAssessmentDetails().getFirst().getEntity().getFirst().getInstances().getFirst()
            .getAttributes().getFirst().getResponseType());
    assertEquals(
        soaAssessmentResult.getAssessmentDetails().getFirst().getEntity().getFirst().getInstances().getFirst()
            .getAttributes().getFirst().getResponseValue(),
        result.getAssessmentDetails().getFirst().getEntity().getFirst().getInstances().getFirst()
            .getAttributes().getFirst().getResponseValue());
    assertEquals(soaAssessmentResult.getResults().getFirst().getAttribute(),
        result.getResults().getFirst().getAttribute());
    assertEquals(soaAssessmentResult.getResults().getFirst().getAttributeValue(),
        result.getResults().getFirst().getAttributeValue());
  }

  @Test
  void testToAddress() {
    uk.gov.laa.ccms.soa.gateway.model.AddressDetail soaAddress = buildAddressDetail("");

    AddressDetail result = applicationMapper.toAddress(soaAddress);
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

    ClientDetail result = applicationMapper.toClient(soaBaseClient);

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

    OpponentDetail result = applicationMapper.toIndividualOpponent(soaOtherParty);

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

    OpponentDetail result = applicationMapper.toOrganisationOpponent(soaOtherParty);

    assertNotNull(result);
    assertEquals(OPPONENT_TYPE_ORGANISATION, result.getType());
    assertEquals(soaOtherParty.getOtherPartyId(), result.getEbsId());
    assertEquals(soaOtherParty.getOrganisation().getOrganizationName(),
        result.getOrganisationName());
    assertEquals(soaOtherParty.getOrganisation().getOrganizationType(),
        result.getOrganisationType());
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

    assertEquals(soaOtherParty.getOrganisation().isCurrentlyTrading(),
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
    LinkedCase soaLinkedCase = buildLinkedCase();

    LinkedCaseDetail result = applicationMapper.toLinkedCase(soaLinkedCase);

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
    PriorAuthority soaPriorAuthority =
        buildPriorAuthority();
    SoaPriorAuthorityMappingContext priorAuthorityMappingContext =
        buildPriorAuthorityMappingContext(soaPriorAuthority);
    PriorAuthorityTypeDetail priorAuthorityTypeDetail =
        priorAuthorityMappingContext.getPriorAuthorityTypeLookup();

    PriorAuthorityDetail result = applicationMapper.toPriorAuthority(priorAuthorityMappingContext);

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
    uk.gov.laa.ccms.data.model.PriorAuthorityDetail priorAuthorityDetail =
        buildPriorAuthorityDetail("dataType");
    CommonLookupValueDetail priorAuthLookup = new CommonLookupValueDetail();

    ReferenceDataItemDetail result = applicationMapper.toReferenceDataItem(
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
    SoaCaseOutcomeMappingContext caseOutcomeMappingContext = buildCaseOutcomeMappingContext(soaCaseDetail);

    CaseOutcomeDetail result = applicationMapper.toCaseOutcome(caseOutcomeMappingContext);

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

    TimeRecoveryDetail result = applicationMapper.toTimeRecovery(timeRelatedAward);

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

    RecoveryDetail result = applicationMapper.toRecovery(soaRecovery);

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
    assertEquals(soaRecovery.getOfferedAmount().getConditionsOfOffer(), result.getOfferDetails());
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
    String partyId = "123";

    LiablePartyDetail result = applicationMapper.toLiableParty(partyId);

    assertNotNull(result);
    assertEquals(Integer.valueOf(partyId), result.getOpponentId());
    assertNull(result.getAuditTrail());
    assertNull(result.getAwardType());
  }

  @Test
  void testToCostAward() {
    Award soaAward = buildCostAward();

    CostAwardDetail result = applicationMapper.toCostAward(soaAward);

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

    FinancialAwardDetail result = applicationMapper.toFinancialAward(soaAward);

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
        result.getStatutoryChargeExemptReason());

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

    LandAwardDetail result = applicationMapper.toLandAward(soaAward);

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
    assertEquals(soaAward.getLandAward().getTitleNo(), result.getTitleNumber());
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
        result.getStatutoryChargeExemptReason());
    assertEquals(soaAward.getLandAward().getLandChargeRegistration(),
        result.getLandChargeRegistration());
    assertEquals(soaAward.getLandAward().getRegistrationRef(),
        result.getRegistrationReference());

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

    OtherAssetAwardDetail result = applicationMapper.toOtherAssetAward(soaAward);

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
        result.getStatutoryChargeExemptReason());

    // afterMapping
    assertTrue(result.getRecoveryOfAwardTimeRelated());
    assertEquals(AWARD_TYPE_OTHER_ASSET, result.getTimeRecovery().getAwardType());

    // afterMapping (baseAward)
    result.getLiableParties().forEach(
        liableParty -> assertEquals(AWARD_TYPE_OTHER_ASSET, liableParty.getAwardType()));
  }

  @Test
  public void testToApplicationDetails() {
    List<BaseApplicationDetail> baseApplicationList = List.of(
        buildBaseApplication(1),
        buildBaseApplication(2));

    ApplicationDetails result =
        applicationMapper.toApplicationDetails(new PageImpl<>(baseApplicationList));

    assertNotNull(result);
    assertEquals(2, result.getSize());
    assertNotNull(result.getContent());
    assertEquals(baseApplicationList, result.getContent());
  }

  @Test
  public void testToBaseApplication() {
    CaseSummary soaCaseSummary = buildCaseSummary();

    BaseApplicationDetail result = applicationMapper.toBaseApplication(soaCaseSummary);

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

  private SoaApplicationMappingContext buildApplicationMappingContext(
      CaseDetail soaCase,
      Boolean devolvedPowers,
      Date devolvedPowersDate) {
    return SoaApplicationMappingContext.builder()
        .soaCaseDetail(soaCase)
        .applicationType(new CommonLookupValueDetail()
            .code("apptypecode")
            .description("apptypedesc"))
        .amendmentProceedingsInEbs(Collections.singletonList(
            buildProceedingMappingContext(soaCase.getApplicationDetails().getProceedings().getFirst())))
        .caseOutcome(buildCaseOutcomeMappingContext(soaCase))
        .caseWithOnlyDraftProceedings(Boolean.TRUE)
        .certificate(new CommonLookupValueDetail()
            .code("certcode")
            .description("certificate descr"))
        .currentProviderBilledAmount(BigDecimal.ONE)
        .devolvedPowers(Pair.of(devolvedPowers, devolvedPowersDate))
        .feeEarnerContact(new ContactDetail()
            .id(100)
            .name("feeEarnerName"))
        .supervisorContact(new ContactDetail()
            .id(101)
            .name("supName"))
        .meansAssessment(soaCase.getApplicationDetails().getMeansAssesments().getFirst())
        .meritsAssessment(soaCase.getApplicationDetails().getMeritsAssesments().getFirst())
        .priorAuthorities(Collections.singletonList(buildPriorAuthorityMappingContext(
            soaCase.getPriorAuthorities().getFirst())))
        .proceedings(Collections.singletonList(
            buildProceedingMappingContext(soaCase.getApplicationDetails().getProceedings().getFirst())))
        .providerDetail(new ProviderDetail()
            .id(1)
            .name("provname"))
        .providerOffice(new OfficeDetail().id(1000).name("offName"))
        .build();
  }

  private SoaProceedingMappingContext buildProceedingMappingContext(
      uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail soaProceeding) {
    return SoaProceedingMappingContext.builder()
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

  private SoaCaseOutcomeMappingContext buildCaseOutcomeMappingContext(CaseDetail soaCase) {
    return SoaCaseOutcomeMappingContext.builder()
        .soaCase(soaCase)
        .costAwards(Collections.singletonList(soaCase.getAwards().getFirst()))
        .financialAwards(Collections.singletonList(soaCase.getAwards().get(1)))
        .landAwards(Collections.singletonList(soaCase.getAwards().get(2)))
        .otherAssetAwards(Collections.singletonList(soaCase.getAwards().get(3)))
        .proceedingOutcomes(Collections.singletonList(buildProceedingMappingContext(
            soaCase.getApplicationDetails().getProceedings().getFirst())))
        .build();
  }

  private SoaPriorAuthorityMappingContext buildPriorAuthorityMappingContext(
      PriorAuthority soaPriorAuthority) {
    return SoaPriorAuthorityMappingContext.builder()
        .soaPriorAuthority(soaPriorAuthority)
        .priorAuthorityTypeLookup(buildPriorAuthorityTypeDetail("dataType"))
        .items(Collections.singletonList(Pair.of(buildPriorAuthorityDetail("dataType"),
            new CommonLookupValueDetail()
                .code("priorAuthCode")
                .description("priorAuthDesc"))))
        .build();
  }

  @Test
  @DisplayName("Test toCaseDetail with valid CaseMappingContext")
  void testToCaseDetail_Valid() {
    final ApplicationDetail applicationDetail = buildApplicationDetail(1, false, new Date());
    final LinkedCaseDetail linkedCaseDetail = new LinkedCaseDetail().lscCaseReference("LC123").relationToCase("Related");
    final PriorAuthorityDetail priorAuthorityDetail = new PriorAuthorityDetail().summary("Test Prior Authority");
    final BaseEvidenceDocumentDetail caseDocDetail = new BaseEvidenceDocumentDetail().registeredDocumentId("DOC123");
    applicationDetail.setLinkedCases(Collections.singletonList(linkedCaseDetail));
    applicationDetail.setPriorAuthorities(Collections.singletonList(priorAuthorityDetail));

    final CaseMappingContext context = CaseMappingContext.builder()
        .tdsApplication(applicationDetail)
        .caseDocs(Collections.singletonList(caseDocDetail))
        .build();

    final CaseDetail result = applicationMapper.toCaseDetail(context);

    assertNotNull(result);
    assertEquals(applicationDetail.getCaseReferenceNumber(), result.getCaseReferenceNumber());
    assertNotNull(result.getApplicationDetails());
    assertNotNull(result.getLinkedCases());
    assertEquals(1, result.getLinkedCases().size());
    assertEquals("LC123", result.getLinkedCases().getFirst().getCaseReferenceNumber());
    assertNotNull(result.getPriorAuthorities());
    assertEquals(1, result.getPriorAuthorities().size());
    assertEquals("Test Prior Authority", result.getPriorAuthorities().getFirst().getDescription());
    assertNotNull(result.getCaseDocs());
    assertEquals(1, result.getCaseDocs().size());
    assertEquals("DOC123", result.getCaseDocs().getFirst().getCcmsDocumentId());
    assertNotNull(result.getRecordHistory());
  }

  @Test
  @DisplayName("Test toCaseDetail with null CaseMappingContext")
  void testToCaseDetail_Null() {
    assertNull(applicationMapper.toCaseDetail(null));
  }


  @Test
  @DisplayName("Test toSoaCaseDoc with valid BaseEvidenceDocumentDetail")
  void testToSoaCaseDoc_Valid() {
    final BaseEvidenceDocumentDetail evidenceDocumentDetail
        = new BaseEvidenceDocumentDetail()
        .documentType(new StringDisplayValue()
            .displayValue("Test Document"))
        .registeredDocumentId("12345");

    final CaseDoc result = applicationMapper.toSoaCaseDoc(evidenceDocumentDetail);

    assertNotNull(result);
    assertEquals("12345", result.getCcmsDocumentId());
    assertEquals("Test Document", result.getDocumentSubject());
  }

  @Test
  @DisplayName("Test toSoaCaseDoc with null BaseEvidenceDocumentDetail")
  void testToSoaCaseDoc_Null() {
    assertNull(applicationMapper.toSoaCaseDoc(null));
  }

  @Test
  @DisplayName("Test toSoaPriorAuthority with valid PriorAuthorityDetail")
  void testToSoaPriorAuthority_Valid() {
    final PriorAuthorityDetail priorAuthorityDetail = new PriorAuthorityDetail()
        .summary("Test Summary")
        .justification("Test Justification")
        .amountRequested(new BigDecimal("1000.00"))
        .status("Approved")
        .items(Collections.singletonList(new ReferenceDataItemDetail()
            .code(new StringDisplayValue().id("PA123").displayValue("Test Attribute"))));

    final PriorAuthority result = applicationMapper.toSoaPriorAuthority(priorAuthorityDetail);

    assertNotNull(result);
    assertEquals("Test Summary", result.getDescription());
    assertEquals("Test Justification", result.getReasonForRequest());
    assertEquals(new BigDecimal("1000.00"), result.getRequestAmount());
    assertEquals("Approved", result.getDecisionStatus());
    assertNotNull(result.getDetails());
    assertEquals(1, result.getDetails().size());
    assertEquals("PA123", result.getDetails().getFirst().getName());
  }

  @Test
  @DisplayName("Test toSoaPriorAuthority with null PriorAuthorityDetail")
  void testToSoaPriorAuthority_Null() {
    assertNull(applicationMapper.toSoaPriorAuthority(null));
  }


  @Test
  @DisplayName("Test toSoaPriorAuthorityAttribute with valid ReferenceDataItemDetail")
  void testToSoaPriorAuthorityAttribute_Valid() {
    final ReferenceDataItemDetail referenceDataItemDetail = new ReferenceDataItemDetail()
        .code(new StringDisplayValue().id("AttributeName"))
        .value(new StringDisplayValue().id("AttributeValue"));

    final PriorAuthorityAttribute result = applicationMapper.toSoaPriorAuthorityAttribute(referenceDataItemDetail);

    assertNotNull(result);
    assertEquals("AttributeName", result.getName());
    assertEquals("AttributeValue", result.getValue());
  }

  @Test
  @DisplayName("Test toSoaPriorAuthorityAttribute with null ReferenceDataItemDetail")
  void testToSoaPriorAuthorityAttribute_Null() {
    assertNull(applicationMapper.toSoaPriorAuthorityAttribute(null));
  }


  @Test
  @DisplayName("Test toSoaLinkedCase with valid LinkedCaseDetail")
  void testToSoaLinkedCase_Valid() {

    final LinkedCaseDetail linkedCaseDetail = new LinkedCaseDetail()
        .lscCaseReference("LSC123")
        .relationToCase("Related Case");

    final LinkedCase result = applicationMapper.toSoaLinkedCase(linkedCaseDetail);

    assertNotNull(result);
    assertEquals("LSC123", result.getCaseReferenceNumber());
    assertEquals("Related Case", result.getLinkType());
  }

  @Test
  @DisplayName("Test toSoaLinkedCase with null LinkedCaseDetail")
  void testToSoaLinkedCase_Null() {
    assertNull(applicationMapper.toSoaLinkedCase(null));
  }


  @Test
  @DisplayName("Test toSubmittedApplicationDetails with valid CaseMappingContext")
  void testToSubmittedApplicationDetails_Valid() {
    final ApplicationDetail applicationDetail = new ApplicationDetail();
    applicationDetail.setApplicationType(new ApplicationType().id("type123"));
    applicationDetail.setCorrespondenceAddress(new AddressDetail().preferredAddress("preferred"));
    applicationDetail.setClient(new ClientDetail().reference("client123"));
    applicationDetail.setProviderDetails(new ApplicationProviderDetails().providerCaseReference("CASE123"));
    applicationDetail.setCategoryOfLaw(new StringDisplayValue().id("category123"));
    applicationDetail.setCosts(new CostStructureDetail().requestedCostLimitation(new BigDecimal("1000.00")));

    final CaseMappingContext context = CaseMappingContext.builder()
        .tdsApplication(applicationDetail)
        .meansAssessment(new AssessmentDetail())
        .meritsAssessment(new AssessmentDetail())
        .caseDocs(Collections.singletonList(new BaseEvidenceDocumentDetail()))
        .build();

    final SubmittedApplicationDetails result = applicationMapper.toSubmittedApplicationDetails(context);

    assertNotNull(result);
    assertNotNull(result.getLarDetails());
    assertEquals("client123", result.getClient().getClientReferenceNumber());
    assertEquals("preferred", result.getPreferredAddress());
    assertEquals("CASE123", result.getProviderDetails().getProviderCaseReferenceNumber());
    assertEquals("category123", result.getCategoryOfLaw().getCategoryOfLawCode());
    assertEquals(new BigDecimal("1000.00"), result.getCategoryOfLaw().getRequestedAmount());
  }

  @Test
  @DisplayName("Test toSubmittedApplicationDetails with null CaseMappingContext")
  void testToSubmittedApplicationDetails_Null() {
    assertNull(applicationMapper.toSubmittedApplicationDetails(null));
  }


  @Test
  @DisplayName("Test toBaseClient with valid ClientDetail")
  void testToBaseClient_Valid() {
    final ClientDetail clientDetail = new ClientDetail();
    clientDetail.setReference("12345");
    clientDetail.setFirstName("John");
    clientDetail.setSurname("Doe");

    final BaseClient result = applicationMapper.toBaseClient(clientDetail);

    assertNotNull(result);
    assertEquals("12345", result.getClientReferenceNumber());
    assertEquals("John", result.getFirstName());
    assertEquals("Doe", result.getSurname());
  }

  @Test
  @DisplayName("Test toBaseClient with null ClientDetail")
  void testToBaseClient_Null() {
    assertNull(applicationMapper.toBaseClient(null));
  }

  @Test
  @DisplayName("Test toSoaProviderDetail with valid ApplicationProviderDetails")
  void testToSoaProviderDetail_Valid() {
    final StringDisplayValue providerContact = new StringDisplayValue();
    providerContact.setId("user123");

    final ApplicationProviderDetails providerDetails = new ApplicationProviderDetails();
    providerDetails.setProviderContact(providerContact);
    providerDetails.setProviderCaseReference("CASE123");
    providerDetails.provider(new IntDisplayValue().id(100));
    providerDetails.office(new IntDisplayValue().id(200));
    providerDetails.supervisor(new StringDisplayValue().id("300"));
    providerDetails.feeEarner(new StringDisplayValue().id("400"));

    final uk.gov.laa.ccms.soa.gateway.model.ProviderDetail result = applicationMapper.toSoaProviderDetail(providerDetails);

    assertNotNull(result);
    assertEquals("user123", result.getContactUserId().getUserLoginId());
    assertEquals("CASE123", result.getProviderCaseReferenceNumber());
    assertEquals("100", result.getProviderFirmId());
    assertEquals("200", result.getProviderOfficeId());
    assertEquals("300", result.getSupervisorContactId());
    assertEquals("400", result.getFeeEarnerContactId());
  }

  @Test
  @DisplayName("Test toSoaProviderDetail with null ApplicationProviderDetails")
  void testToSoaProviderDetail_Null() {
    assertNull(applicationMapper.toSoaProviderDetail(null));
  }


  @ParameterizedTest
  @CsvSource({
      "'LAW123', 'Test Law Description', '1000', '500', '1000'",  // Case with requested amount
      "'LAW124', 'Another Law Description', '', '500', '500'",    // Case with default amount (requested amount is null)
      "'LAW125', 'Third Law Description', '', '', ''"             // Case with no costs (null amounts)
  })
  @DisplayName("Parameterized Test toSoaCategoryOfLaw with different ApplicationDetail inputs")
  void testToSoaCategoryOfLaw_Parameterized(
      final String lawCode,
      final String lawDescription,
      final String requestedCostStr,
      final String defaultCostStr,
      final String expectedCostStr) {

    // Setup: Create mock objects for ApplicationDetail and related fields
    final StringDisplayValue categoryOfLaw = new StringDisplayValue();
    categoryOfLaw.setId(lawCode);
    categoryOfLaw.setDisplayValue(lawDescription);

    final CostStructureDetail costStructureDetail = new CostStructureDetail();
    costStructureDetail.setRequestedCostLimitation(requestedCostStr.isEmpty() ? null : new BigDecimal(requestedCostStr));
    costStructureDetail.setDefaultCostLimitation(defaultCostStr.isEmpty() ? null : new BigDecimal(defaultCostStr));

    final ApplicationDetail applicationDetail = new ApplicationDetail();
    applicationDetail.setCategoryOfLaw(categoryOfLaw);
    applicationDetail.setCosts(costStructureDetail);

    // Call the method under test
    final CategoryOfLaw result = applicationMapper.toSoaCategoryOfLaw(applicationDetail);

    // Assertions
    if (expectedCostStr.isEmpty()) {
      assertNull(result.getRequestedAmount());  // No costs provided, so requested amount should be null
    } else {
      assertNotNull(result);
      assertEquals(lawCode, result.getCategoryOfLawCode());
      assertEquals(lawDescription, result.getCategoryOfLawDescription());
      assertEquals(new BigDecimal(expectedCostStr), result.getRequestedAmount());
    }
  }

  @Test
  @DisplayName("Test toSoaCategoryOfLaw with null ApplicationDetail")
  void testToSoaCategoryOfLaw_Null() {
    assertNull(applicationMapper.toSoaCategoryOfLaw(null));
  }


  @Test
  @DisplayName("Test toSoaAddressDetail with valid AddressDetail")
  void testToSoaAddressDetail_Valid() {
    final AddressDetail addressDetail = new AddressDetail();
    addressDetail.setId(123);
    addressDetail.setHouseNameOrNumber("1234 House");
    addressDetail.setCareOf("Care Of Name");
    addressDetail.setPostcode("12345");
    addressDetail.setAddressLine1("Line 1");
    addressDetail.setAddressLine2("Line 2");
    addressDetail.setCity("Test City");
    addressDetail.setCountry("Test Country");
    addressDetail.setCounty("Test County");

    final uk.gov.laa.ccms.soa.gateway.model.AddressDetail result = applicationMapper.toSoaAddressDetail(addressDetail);

    assertNotNull(result);
    assertEquals("123", result.getAddressId());
    assertEquals("1234 House", result.getHouse());
    assertEquals("Care Of Name", result.getCareOfName());
    assertEquals("12345", result.getPostalCode());
    assertEquals("Line 1", result.getAddressLine1());
    assertEquals("Line 2", result.getAddressLine2());
    assertEquals("Test City", result.getCity());
    assertEquals("Test Country", result.getCountry());
    assertEquals("Test County", result.getCounty());
  }

  @Test
  @DisplayName("Test toSoaAddressDetail with null AddressDetail")
  void testToSoaAddressDetail_Null() {
    assertNull(applicationMapper.toSoaAddressDetail(null));
  }

  @Test
  @DisplayName("Test toSoaProceedingDetail with valid ProceedingDetail")
  void testToSoaProceedingDetail_Valid() {
    final ProceedingDetail proceedingDetail = new ProceedingDetail();
    proceedingDetail.setId(123);
    proceedingDetail.setStatus(new StringDisplayValue().id("Status"));
    proceedingDetail.setTypeOfOrder(new StringDisplayValue().id("Order"));
    proceedingDetail.setMatterType(new StringDisplayValue().id("Matter"));
    proceedingDetail.setLevelOfService(new StringDisplayValue().id("Service"));
    proceedingDetail.setClientInvolvement(new StringDisplayValue().id("Involvement"));
    proceedingDetail.setLeadProceedingInd(true);
    proceedingDetail.setDescription("Test Description");
    proceedingDetail.setDateCostsValid(new Date());
    proceedingDetail.setStage("Test Stage");
    proceedingDetail.setDateDevolvedPowersUsed(new Date());
    proceedingDetail.setDateGranted(new Date());

    final uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail result = applicationMapper.toSoaProceedingDetail(proceedingDetail);

    assertNotNull(result);
    assertEquals("P_123", result.getProceedingCaseId());
    assertEquals("Status", result.getStatus());
    assertTrue(result.isLeadProceedingIndicator());
    assertEquals("Test Description", result.getProceedingDescription());
    assertEquals("Order", result.getOrderType());
    assertEquals("Matter", result.getMatterType());
    assertEquals("Service", result.getLevelOfService());
    assertEquals("Involvement", result.getClientInvolvementType());
    assertNotNull(result.getDateCostsValid());
    assertEquals("Test Stage", result.getStage());
    assertNotNull(result.getDateDevolvedPowersUsed());
    assertNotNull(result.getDateGranted());
  }

  @Test
  @DisplayName("Test toSoaProceedingDetail with null ProceedingDetail")
  void testToSoaProceedingDetail_Null() {
    assertNull(applicationMapper.toSoaProceedingDetail(null));
  }

  @Test
  @DisplayName("Test toSoaScopeLimitation with valid ScopeLimitationDetail")
  void testToSoaScopeLimitation_Valid() {
    final ScopeLimitationDetail scopeLimitationDetail = new ScopeLimitationDetail();
    scopeLimitationDetail.setScopeLimitation(new StringDisplayValue().id("scopeId").displayValue("Scope Description"));
    scopeLimitationDetail.setScopeLimitationWording("Scope Limitation Wording");
    scopeLimitationDetail.setDelegatedFuncApplyInd(new BooleanDisplayValue().flag(true));

    final ScopeLimitation result = applicationMapper.toSoaScopeLimitation(scopeLimitationDetail);

    assertNotNull(result);
    assertEquals("scopeId", result.getScopeLimitation());
    assertEquals("Scope Limitation Wording", result.getScopeLimitationWording());
    assertTrue(result.isDelegatedFunctionsApply());
  }

  @Test
  @DisplayName("Test toSoaScopeLimitation with null ScopeLimitationDetail")
  void testToSoaScopeLimitation_Null() {
    assertNull(applicationMapper.toSoaScopeLimitation(null));
  }


  @Test
  @DisplayName("Test toSoaOtherParty with individual type OpponentDetail")
  void testToSoaOtherParty_Individual() {
    final OpponentDetail opponentDetail = new OpponentDetail();
    opponentDetail.setType(OPPONENT_TYPE_INDIVIDUAL);
    opponentDetail.setRelationshipToClient("ClientRelInd");
    opponentDetail.setRelationshipToCase("CaseRelInd");
    opponentDetail.setNationalInsuranceNumber("NI12345");
    opponentDetail.setLegalAided(true);

    final OtherParty result = applicationMapper.toSoaOtherParty(opponentDetail);

    assertNotNull(result);
    assertNotNull(result.getPerson());
    assertNull(result.getOrganisation());
    assertEquals("ClientRelInd", result.getPerson().getRelationToClient());
    assertEquals("CaseRelInd", result.getPerson().getRelationToCase());
    assertEquals("NI12345", result.getPerson().getNiNumber());
    assertTrue(result.getPerson().isPartyLegalAidedInd());
  }

  @Test
  @DisplayName("Test toSoaOtherParty with organisation type OpponentDetail")
  void testToSoaOtherParty_Organisation() {
    final OpponentDetail opponentDetail = new OpponentDetail();
    opponentDetail.setType(OPPONENT_TYPE_ORGANISATION);
    opponentDetail.setRelationshipToClient("ClientRelOrg");
    opponentDetail.setRelationshipToCase("CaseRelOrg");
    opponentDetail.setOrganisationName("OrgName");
    opponentDetail.setOrganisationType("Private");

    final OtherParty result = applicationMapper.toSoaOtherParty(opponentDetail);

    assertNotNull(result);
    assertNull(result.getPerson());
    assertNotNull(result.getOrganisation());
    assertEquals("ClientRelOrg", result.getOrganisation().getRelationToClient());
    assertEquals("CaseRelOrg", result.getOrganisation().getRelationToCase());
    assertEquals("OrgName", result.getOrganisation().getOrganizationName());
    assertEquals("Private", result.getOrganisation().getOrganizationType());
  }

  @Test
  @DisplayName("Test toSoaOtherParty with null or unknown type OpponentDetail")
  void testToSoaOtherParty_NullOrUnknownType() {
    final OpponentDetail opponentDetail = new OpponentDetail();
    opponentDetail.setType(null);

    final OtherParty result = applicationMapper.toSoaOtherParty(opponentDetail);

    assertNotNull(result);
    assertNull(result.getPerson());
    assertNull(result.getOrganisation());
  }

  @Test
  @DisplayName("Test toSoaOtherParty with null OpponentDetail")
  void testToSoaOtherParty_Null() {
    assertNull(applicationMapper.toSoaOtherParty(null));
  }

  @ParameterizedTest
  @DisplayName("Test toSoaPerson with valid OpponentDetail inputs")
  @CsvSource({
      "'TestClientRel', 'TestCaseRel', '123456', 'true', 'true', 'John Doe', 'Test Employer', 'Employer Address', '1000', '500', '1990-01-01', 'Employed', 'Cert123', 'Monthly', '2023-10-12', 'OtherInfo'",
      "'ClientRel2', 'CaseRel2', '987654', 'false', 'false', 'Jane Smith', 'Another Employer', 'Another Address', '2000', '1000', '1985-05-05', 'Unemployed', 'Cert456', 'Weekly', '2023-08-20', 'Info2'"
  })
  void testToSoaPerson_Valid(final String relationToClient, final String relationToCase, final String niNumber, final boolean legalAided,
      final boolean courtOrderedMeansAssesment, final String contactName, final String employerName,
      final String employerAddress, final String assessedIncome, final String assessedAssets, final String dateOfBirth,
      final String employmentStatus, final String certificateNumber, final String assessedIncomeFrequency,
      final String assessmentDate, final String otherInformation) throws ParseException {

    final Date parsedDateOfBirth = dateFormat.parse(dateOfBirth);
    final Date parsedAssessmentDate = dateFormat.parse(assessmentDate);

    final OpponentDetail opponentDetail = new OpponentDetail();
    opponentDetail.setRelationshipToClient(relationToClient);
    opponentDetail.setRelationshipToCase(relationToCase);
    opponentDetail.setNationalInsuranceNumber(niNumber);
    opponentDetail.setLegalAided(legalAided);
    opponentDetail.setCourtOrderedMeansAssessment(courtOrderedMeansAssesment);
    opponentDetail.setContactNameRole(contactName);
    opponentDetail.setEmployerName(employerName);
    opponentDetail.setEmployerAddress(employerAddress);
    opponentDetail.setAssessedIncome(new BigDecimal(assessedIncome));
    opponentDetail.setAssessedAssets(new BigDecimal(assessedAssets));
    opponentDetail.setDateOfBirth(parsedDateOfBirth);
    opponentDetail.setEmploymentStatus(employmentStatus);
    opponentDetail.setCertificateNumber(certificateNumber);
    opponentDetail.setAssessedIncomeFrequency(assessedIncomeFrequency);
    opponentDetail.setAssessmentDate(parsedAssessmentDate);
    opponentDetail.setOtherInformation(otherInformation);

    // Call the method under test
    final OtherPartyPerson result = applicationMapper.toSoaPerson(opponentDetail);

    assertNotNull(result);
    assertEquals(relationToClient, result.getRelationToClient());
    assertEquals(relationToCase, result.getRelationToCase());
    assertEquals(niNumber, result.getNiNumber());
    assertEquals(legalAided, result.isPartyLegalAidedInd());
    assertEquals(courtOrderedMeansAssesment, result.isCourtOrderedMeansAssesment());
    assertEquals(contactName, result.getContactName());
    assertEquals(employerName, result.getEmployersName());
    assertEquals(employerAddress, result.getOrganizationAddress());
    assertEquals(new BigDecimal(assessedIncome).setScale(2), result.getAssessedIncome());
    assertEquals(new BigDecimal(assessedAssets).setScale(2), result.getAssessedAssets());
    assertEquals(parsedDateOfBirth, result.getDateOfBirth());
    assertEquals(employmentStatus, result.getEmploymentStatus());
    assertEquals(certificateNumber, result.getCertificateNumber());
    assertEquals(assessedIncomeFrequency, result.getAssessedIncomeFrequency());
    assertEquals(parsedAssessmentDate, result.getAssessmentDate());
    assertEquals(otherInformation, result.getOtherInformation());
  }

  @Test
  @DisplayName("Test toSoaPerson with null OpponentDetail")
  void testToSoaPerson_NullOpponentDetail() {
    assertNull(applicationMapper.toSoaPerson(null));
  }

  @ParameterizedTest
  @DisplayName("Test toSoaOrganisation with valid OpponentDetail inputs")
  @CsvSource({
      "'ClientRelation', 'CaseRelation', 'OrgName', 'OrgType', 'ContactName', 'true', 'OtherInfo'",
      "'ClientRelation2', 'CaseRelation2', 'OrgName2', 'OrgType2', 'ContactName2', 'false', 'OtherInfo2'"
  })
  void testToSoaOrganisation_Valid(final String relationToClient, final String relationToCase, final String organisationName, final String organisationType,
      final String contactName, final boolean currentlyTrading, final String otherInformation) {
    final OpponentDetail opponentDetail = new OpponentDetail();
    opponentDetail.setRelationshipToClient(relationToClient);
    opponentDetail.setRelationshipToCase(relationToCase);
    opponentDetail.setOrganisationName(organisationName);
    opponentDetail.setOrganisationType(organisationType);
    opponentDetail.setContactNameRole(contactName);
    opponentDetail.setCurrentlyTrading(currentlyTrading);
    opponentDetail.setOtherInformation(otherInformation);

    final OtherPartyOrganisation result = applicationMapper.toSoaOrganisation(opponentDetail);

    assertNotNull(result);
    assertEquals(relationToClient, result.getRelationToClient());
    assertEquals(relationToCase, result.getRelationToCase());
    assertEquals(organisationName, result.getOrganizationName());
    assertEquals(organisationType, result.getOrganizationType());
    assertEquals(contactName, result.getContactName());
    assertEquals(currentlyTrading, result.isCurrentlyTrading());
    assertEquals(otherInformation, result.getOtherInformation());
    assertNull(result.getAddress());
  }

  @Test
  @DisplayName("Test toSoaOrganisation with null OpponentDetail")
  void testToSoaOrganisation_NullOpponentDetail() {
    assertNull(applicationMapper.toSoaOrganisation(null));
  }

  @ParameterizedTest
  @DisplayName("Test toOpaAttribute with different AssessmentAttributeDetail inputs, with different user defined indicator values")
  @CsvSource({
      "'TestName', 'TestType', 'TestValue', 'nonIntermediate', true",
      "'SA_TestName', 'TestType', 'TestValue', 'intermediate', false",
      "'TestName', 'TestType', 'TestValue', 'intermediate', false",
      "'SA_TestName', 'TestType', 'TestValue', 'nonIntermediate', false",
      "'TestName', 'TestType', 'TestValue', 'nonIntermediate', true"
  })
  void testToOpaAttribute_Valid_userDefinedIndicator(final String name, final String type, final String value,
      final String inferencingType, final boolean expectedUserDefinedInd) {
    final AssessmentAttributeDetail assessmentAttributeDetail =
        new AssessmentAttributeDetail()
            .name(name)
            .type(type)
            .value(value)
            .inferencingType(inferencingType)
            .prepopulated(false);

    final OpaAttribute result = applicationMapper.toOpaAttribute(assessmentAttributeDetail);

    assertNotNull(result);
    assertEquals(name, result.getAttribute());
    assertEquals(type, result.getResponseType());
    assertEquals(value, result.getResponseValue());
    assertEquals(expectedUserDefinedInd, result.isUserDefinedInd());
  }

  @Test
  @DisplayName("Test toOpaAttribute with null AssessmentAttributeDetail")
  void testToOpaAttribute_Null() {
    assertNull(applicationMapper.toOpaAttribute(null));
  }

  @Test
  @DisplayName("Test toSoaRecordHistory with valid CaseMappingContext")
  void testToSoaRecordHistory_ValidContext() {
    final Date testDate = new Date();
    final ApplicationDetail application = buildApplicationDetail(1, false, testDate);
    application.getAuditTrail().created(testDate);
    application.getAuditTrail().lastSaved(testDate);

    final UserDetail user = buildUserDetail();

    final CaseMappingContext caseMappingContext = CaseMappingContext.builder()
        .tdsApplication(application)
        .user(user)
        .build();

    final RecordHistory result = applicationMapper.toSoaRecordHistory(caseMappingContext);

    assertNotNull(result);
    assertEquals("testLoginId", result.getLastUpdatedBy().getUserLoginId());
    assertEquals(testDate, result.getDateLastUpdated());
    assertEquals(testDate, result.getDateCreated());
  }

  @Test
  @DisplayName("Test toSoaRecordHistory with null context")
  void testToSoaRecordHistory_NullContext() {
    assertNull(applicationMapper.toSoaRecordHistory(null));
  }




}
