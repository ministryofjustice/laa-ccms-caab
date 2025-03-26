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
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildAddressDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildAssessmentResult;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildBaseClient;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildCaseDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildCaseSummary;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildCostAward;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildCostLimitation;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildFinancialAward;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildLandAward;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildLinkedCase;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildOtherAssetAward;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildOtherPartyOrganisation;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildOtherPartyPerson;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildPriorAuthority;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildPriorAuthorityDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildPriorAuthorityTypeDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildProceedingDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildRecovery;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildScopeLimitation;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildTimeRelatedAward;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import uk.gov.laa.ccms.caab.mapper.context.EbsApplicationMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.EbsCaseOutcomeMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.EbsPriorAuthorityMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.EbsProceedingMappingContext;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
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
import uk.gov.laa.ccms.data.model.Award;
import uk.gov.laa.ccms.data.model.BaseClient;
import uk.gov.laa.ccms.data.model.CaseDetail;
import uk.gov.laa.ccms.data.model.CaseDoc;
import uk.gov.laa.ccms.data.model.CaseSummary;
import uk.gov.laa.ccms.data.model.CategoryOfLaw;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.CostLimitation;
import uk.gov.laa.ccms.data.model.LinkedCase;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.OpaAttribute;
import uk.gov.laa.ccms.data.model.OtherParty;
import uk.gov.laa.ccms.data.model.OtherPartyOrganisation;
import uk.gov.laa.ccms.data.model.OtherPartyPerson;
import uk.gov.laa.ccms.data.model.OutcomeDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.PriorAuthority;
import uk.gov.laa.ccms.data.model.PriorAuthorityAttribute;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.Proceeding;
import uk.gov.laa.ccms.data.model.RecordHistory;
import uk.gov.laa.ccms.data.model.Recovery;
import uk.gov.laa.ccms.data.model.ScopeLimitation;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;
import uk.gov.laa.ccms.data.model.SubmittedApplicationDetails;
import uk.gov.laa.ccms.data.model.TimeRelatedAward;
import uk.gov.laa.ccms.data.model.UserDetail;

@DisplayName("EBS Application mapper test")
class EbsApplicationMapperTest {

  private final EbsApplicationMapper applicationMapper = new EbsApplicationMapperImpl() {
  };

  private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  @Test
  void testToApplicationDetailDevolvedPowers() {
    CaseDetail ebsCaseDetail = buildCaseDetail(APP_TYPE_EMERGENCY_DEVOLVED_POWERS);
    EbsApplicationMappingContext applicationMappingContext =
        buildApplicationMappingContext(
            ebsCaseDetail,
            true,
            ebsCaseDetail.getApplicationDetails().getDevolvedPowersDate());
    ApplicationDetail result = applicationMapper.toApplicationDetail(applicationMappingContext);

    assertNotNull(result);
    assertEquals(ebsCaseDetail.getCaseReferenceNumber(), result.getCaseReferenceNumber());
    assertEquals(applicationMappingContext.getCertificate().getCode(),
        result.getCertificate().getId());
    assertEquals(applicationMappingContext.getCertificate().getDescription(),
        result.getCertificate().getDisplayValue());
    assertEquals(ebsCaseDetail.getApplicationDetails().getApplicationAmendmentType(),
        result.getApplicationType().getId());
    assertEquals(applicationMappingContext.getApplicationType().getDescription(),
        result.getApplicationType().getDisplayValue());
    assertEquals(toDate(ebsCaseDetail.getRecordHistory().getDateCreated()),
        result.getDateCreated());
    assertEquals(
        ebsCaseDetail.getApplicationDetails().getProviderDetails().getProviderCaseReferenceNumber(),
        result.getProviderDetails().getProviderCaseReference());
    assertEquals(
        applicationMappingContext.getProviderDetail().getId(),
        result.getProviderDetails().getProvider().getId());
    assertEquals(
        applicationMappingContext.getProviderDetail().getName(),
        result.getProviderDetails().getProvider().getDisplayValue());
    assertEquals(
        ebsCaseDetail.getApplicationDetails().getProviderDetails()
            .getContactUserId().getLoginId(),
        result.getProviderDetails().getProviderContact().getId());
    assertEquals(
        ebsCaseDetail.getApplicationDetails().getProviderDetails()
            .getContactUserId().getUsername(),
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
        ebsCaseDetail.getApplicationDetails().getCategoryOfLaw().getCategoryOfLawCode(),
        result.getCategoryOfLaw().getId());
    assertEquals(
        ebsCaseDetail.getApplicationDetails().getCategoryOfLaw().getCategoryOfLawDescription(),
        result.getCategoryOfLaw().getDisplayValue());
    assertEquals(
        ebsCaseDetail.getApplicationDetails().getCategoryOfLaw().getGrantedAmount(),
        result.getCosts().getGrantedCostLimitation());
    assertEquals(
        ebsCaseDetail.getApplicationDetails().getCategoryOfLaw().getRequestedAmount(),
        result.getCosts().getRequestedCostLimitation());
    assertEquals(
        ebsCaseDetail.getApplicationDetails().getCategoryOfLaw().getCategoryOfLawCode(),
        result.getCategoryOfLaw().getId());
    assertNull(result.getCosts().getDefaultCostLimitation());
    assertNotNull(result.getCosts().getCostEntries()); // Detail tested in specific test case
    assertEquals(
        ebsCaseDetail.getApplicationDetails().getCategoryOfLaw().getCostLimitations().size(),
        result.getCosts().getCostEntries().size());
    assertNull(result.getCosts().getAuditTrail());
    assertEquals(
        ebsCaseDetail.getApplicationDetails().getLarDetails().getLarScopeFlag(),
        result.getLarScopeFlag());
    assertEquals(
        ebsCaseDetail.getCaseStatus().getActualCaseStatus(),
        result.getStatus().getId());
    assertEquals(
        ebsCaseDetail.getCaseStatus().getDisplayCaseStatus(),
        result.getStatus().getDisplayValue());
    assertEquals(
        ebsCaseDetail.getAvailableFunctions(),
        result.getAvailableFunctions());
    assertTrue(result.getApplicationType().getDevolvedPowers().getUsed());
    assertEquals(toDate(ebsCaseDetail.getApplicationDetails().getDevolvedPowersDate()),
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
    EbsApplicationMappingContext applicationMappingContext =
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
    EbsApplicationMappingContext applicationMappingContext =
        buildApplicationMappingContext(
            buildCaseDetail(APP_TYPE_EMERGENCY),
            false,
            null);
    uk.gov.laa.ccms.data.model.@Valid SubmittedApplicationDetails soaApplicationDetails =
        applicationMappingContext.getEbsCaseDetail().getApplicationDetails();

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
    CostLimitation ebsCostLimitation = buildCostLimitation("");

    CostEntryDetail result = applicationMapper.toCostEntry(ebsCostLimitation);

    assertNotNull(result);
    assertEquals(ebsCostLimitation.getBillingProviderId(), result.getLscResourceId());
    assertEquals(ebsCostLimitation.getPaidToDate(), result.getAmountBilled());
    assertEquals(ebsCostLimitation.getAmount(), result.getRequestedCosts());
    assertEquals(ebsCostLimitation.getBillingProviderName(), result.getResourceName());
    assertEquals(ebsCostLimitation.getCostLimitId(), result.getEbsId());
    assertEquals(ebsCostLimitation.getCostCategory(), result.getCostCategory());
    assertFalse(result.getNewEntry());
    assertTrue(result.getSubmitted()); // Defaults to true in caab-api model
  }

  @Test
  void testToProceeding() {
    uk.gov.laa.ccms.data.model.Proceeding ebsProceeding =
        buildProceedingDetail(STATUS_DRAFT);
    EbsProceedingMappingContext proceedingMappingContext =
        buildProceedingMappingContext(ebsProceeding);

    ProceedingDetail result = applicationMapper.toProceeding(proceedingMappingContext);

    assertNotNull(result);
    assertFalse(result.getEdited());
    assertEquals(ebsProceeding.getAvailableFunctions(),
        result.getAvailableFunctions());
    assertEquals(ebsProceeding.getStage(),
        result.getStage());
    assertEquals(toDate(ebsProceeding.getDateGranted()),
        result.getDateGranted());
    assertEquals(toDate(ebsProceeding.getDateCostsValid()),
        result.getDateCostsValid());
    assertEquals(ebsProceeding.getAvailableFunctions(),
        result.getAvailableFunctions());

    assertEquals(ebsProceeding.getProceedingCaseId(),
        result.getEbsId());
    assertEquals(ebsProceeding.getLeadProceedingIndicator(),
        result.getLeadProceedingInd());
    assertEquals(proceedingMappingContext.getMatterType().getCode(),
        result.getMatterType().getId());
    assertEquals(proceedingMappingContext.getMatterType().getDescription(),
        result.getMatterType().getDisplayValue());
    assertEquals(proceedingMappingContext.getProceedingLookup().getCode(),
        result.getProceedingType().getId());
    assertEquals(proceedingMappingContext.getProceedingLookup().getName(),
        result.getProceedingType().getDisplayValue());
    assertEquals(ebsProceeding.getProceedingDescription(),
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
    assertEquals(ebsProceeding.getOrderType(),
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
    assertEquals(soaScopeLimitation.getDelegatedFunctionsApply(),
        result.getDelegatedFuncApplyInd().getFlag());
    assertNull(result.getDefaultInd());
    assertNull(result.getNonDefaultWordingReqd());
    assertNull(result.getStage());
  }

  @Test
  void testToProceedingOutcome() {
    uk.gov.laa.ccms.data.model.Proceeding ebsProceeding =
        buildProceedingDetail(STATUS_DRAFT);
    EbsProceedingMappingContext proceedingMappingContext =
        buildProceedingMappingContext(ebsProceeding);
    OutcomeDetail soaOutcomeDetail = ebsProceeding.getOutcome();

    ProceedingOutcomeDetail result = applicationMapper.toProceedingOutcome(proceedingMappingContext);

    assertNotNull(result);
    assertEquals(ebsProceeding.getProceedingDescription(), result.getDescription());
    assertEquals(proceedingMappingContext.getMatterType().getCode(),
        result.getMatterType().getId());
    assertEquals(proceedingMappingContext.getMatterType().getDescription(),
        result.getMatterType().getDisplayValue());
    assertEquals(ebsProceeding.getProceedingCaseId(), result.getProceedingCaseId());
    assertEquals(proceedingMappingContext.getProceedingLookup().getCode(),
        result.getProceedingType().getId());
    assertEquals(proceedingMappingContext.getProceedingLookup().getName(),
        result.getProceedingType().getDisplayValue());
    assertEquals(soaOutcomeDetail.getAltAcceptanceReason(), result.getAdrInfo());
    assertEquals(soaOutcomeDetail.getAltDisputeResolution(), result.getAlternativeResolution());
    assertEquals(proceedingMappingContext.getCourtLookup().getCode(), result.getCourtCode());
    assertEquals(proceedingMappingContext.getCourtLookup().getDescription(), result.getCourtName());
    assertEquals(toDate(soaOutcomeDetail.getFinalWorkDate()), result.getDateOfFinalWork());
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
    uk.gov.laa.ccms.data.model.AssessmentResult soaAssessmentResult =
        buildAssessmentResult("");

    AssessmentResult result = applicationMapper.toAssessmentResult(soaAssessmentResult);

    assertNotNull(result);
    assertEquals(soaAssessmentResult.getAssessmentId(),
        result.getAssessmentId());
    assertEquals(toDate(soaAssessmentResult.getDate()),
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
    uk.gov.laa.ccms.data.model.AddressDetail soaAddress = buildAddressDetail("");

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
    assertEquals(soaOtherParty.getPerson().getCourtOrderedMeansAssessment(),
        result.getCourtOrderedMeansAssessment());
    assertEquals(soaOtherParty.getPerson().getOrganisationAddress(),
        result.getEmployerAddress());
    assertEquals(soaOtherParty.getPerson().getOrganisationName(),
        result.getEmployerName());
    assertEquals(soaOtherParty.getPerson().getPartyLegalAidedInd(),
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
    assertEquals(soaOtherParty.getPerson().getPublicFundingAppliedInd(),
        result.getPublicFundingApplied());
    assertFalse(result.getDeleteInd());

    assertEquals(toDate(soaOtherParty.getPerson().getDateOfBirth()),
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
    assertEquals(toDate(soaOtherParty.getPerson().getAssessmentDate()),
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
    assertEquals(soaOtherParty.getOrganisation().getOrganisationName(),
        result.getOrganisationName());
    assertEquals(soaOtherParty.getOrganisation().getOrganisationType(),
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

    assertEquals(soaOtherParty.getOrganisation().getCurrentlyTrading(),
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
    EbsPriorAuthorityMappingContext priorAuthorityMappingContext =
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
    EbsCaseOutcomeMappingContext caseOutcomeMappingContext = buildCaseOutcomeMappingContext(soaCaseDetail);

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
    assertEquals(soaCaseDetail.getDischargeStatus().getClientContinuePvtInd(),
        result.getClientContinueInd());
  }


  @Test
  void testToTimeRecovery() {
    // Create a TimeRelatedAward object for testing
    TimeRelatedAward timeRelatedAward = buildTimeRelatedAward();

    TimeRecoveryDetail result = applicationMapper.toTimeRecovery(timeRelatedAward);

    assertNotNull(result);
    assertEquals(timeRelatedAward.getDescription(), result.getDescription());
    assertEquals(toDate(timeRelatedAward.getAwardDate()), result.getEffectiveDate());
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
    assertEquals(toDate(soaRecovery.getRecoveredAmount().getClient().getDateReceived()),
        result.getClientRecoveryDate());
    assertEquals(soaRecovery.getRecoveredAmount().getCourt().getPaidToLsc(),
        result.getCourtAmountPaidToLsc());
    assertEquals(soaRecovery.getRecoveredAmount().getCourt().getAmount(),
        result.getCourtRecoveryAmount());
    assertEquals(toDate(soaRecovery.getRecoveredAmount().getCourt().getDateReceived()),
        result.getCourtRecoveryDate());
    assertEquals(soaRecovery.getRecoveredAmount().getSolicitor().getPaidToLsc(),
        result.getSolicitorAmountPaidToLsc());
    assertEquals(soaRecovery.getRecoveredAmount().getSolicitor().getAmount(),
        result.getSolicitorRecoveryAmount());
    assertEquals(toDate(soaRecovery.getRecoveredAmount().getSolicitor().getDateReceived()),
        result.getSolicitorRecoveryDate());
    assertEquals(soaRecovery.getOfferedAmount().getAmount(), result.getOfferedAmount());
    assertEquals(soaRecovery.getOfferedAmount().getConditionsOfOffer(),
        result.getConditionsOfOffer());
    assertEquals(soaRecovery.getOfferedAmount().getConditionsOfOffer(), result.getOfferDetails());
    assertEquals(soaRecovery.getLeaveOfCourtReqdInd(), result.getLeaveOfCourtRequiredInd());
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
    assertEquals(toDate(soaAward.getCostAward().getOrderDate()), result.getDateOfOrder());
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
    assertEquals(soaAward.getDeleteAllowed(), result.getDeleteAllowed());
    assertEquals(soaAward.getUpdateAllowed(), result.getUpdateAllowed());
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
    assertEquals(toDate(soaAward.getCostAward().getOrderDateServed()),
        result.getOrderServedDate());
    assertEquals(soaAward.getCostAward().getInterestAwardedRate(),
        result.getInterestAwardedRate());
    assertEquals(toDate(soaAward.getCostAward().getInterestAwardedStartDate()),
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
    assertEquals(toDate(soaAward.getFinancialAward().getOrderDate()), result.getDateOfOrder());
    assertEquals(soaAward.getFinancialAward().getAmount(),
        result.getAwardAmount());
    assertEquals(toDate(soaAward.getFinancialAward().getOrderDateServed()),
        result.getOrderServedDate());
    assertEquals(soaAward.getFinancialAward().getStatutoryChangeReason(),
        result.getStatutoryChargeExemptReason());

    assertNotNull(result.getRecovery()); // detail tested separately

    assertNotNull(result.getLiableParties());
    assertEquals(soaAward.getFinancialAward().getLiableParties().size(),
        result.getLiableParties().size());

    // Like for like mappings
    assertEquals(soaAward.getDeleteAllowed(), result.getDeleteAllowed());
    assertEquals(soaAward.getUpdateAllowed(), result.getUpdateAllowed());
    assertEquals(soaAward.getFinancialAward().getServiceAddress().getAddressLine1(),
        result.getAddressLine1());
    assertEquals(soaAward.getFinancialAward().getServiceAddress().getAddressLine2(),
        result.getAddressLine2());
    assertEquals(soaAward.getFinancialAward().getServiceAddress().getAddressLine3(),
        result.getAddressLine3());
    assertEquals(soaAward.getFinancialAward().getInterimAward().toString(),
        result.getInterimAward());
    assertEquals(soaAward.getFinancialAward().getAwardedBy(), result.getAwardedBy());
    assertEquals(toDate(soaAward.getFinancialAward().getOrderDateServed()),
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
    assertEquals(toDate(soaAward.getLandAward().getOrderDate()), result.getDateOfOrder());
    assertEquals(AWARD_TYPE_LAND, result.getAwardType());
    assertEquals(soaAward.getLandAward().getValuation().getAmount(),
        result.getValuationAmount());
    assertEquals(soaAward.getLandAward().getValuation().getCriteria(),
        result.getValuationCriteria());
    assertEquals(toDate(soaAward.getLandAward().getValuation().getDate()),
        result.getValuationDate());
    assertNotNull(result.getTimeRecovery());
    assertNotNull(result.getLiableParties());
    assertEquals(soaAward.getLandAward().getOtherProprietors().size(),
        result.getLiableParties().size());

    // Like for like mappings
    assertEquals(soaAward.getDeleteAllowed(), result.getDeleteAllowed());
    assertEquals(soaAward.getUpdateAllowed(), result.getUpdateAllowed());
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
    assertEquals(toDate(soaAward.getOtherAsset().getOrderDate()), result.getDateOfOrder());
    assertEquals(AWARD_TYPE_OTHER_ASSET, result.getAwardType());
    assertEquals(soaAward.getAwardType(), result.getAwardCode());
    assertEquals(soaAward.getOtherAsset().getValuation().getAmount(),
        result.getValuationAmount());
    assertEquals(soaAward.getOtherAsset().getValuation().getCriteria(),
        result.getValuationCriteria());
    assertEquals(toDate(soaAward.getOtherAsset().getValuation().getDate()),
        result.getValuationDate());
    assertNotNull(result.getTimeRecovery());
    assertNotNull(result.getLiableParties());
    assertEquals(soaAward.getOtherAsset().getHeldBy().size(),
        result.getLiableParties().size());

    // Like for like mappings
    assertEquals(soaAward.getDeleteAllowed(), result.getDeleteAllowed());
    assertEquals(soaAward.getUpdateAllowed(), result.getUpdateAllowed());
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

  private EbsApplicationMappingContext buildApplicationMappingContext(
      uk.gov.laa.ccms.data.model.CaseDetail ebsCaseDetail,
      Boolean devolvedPowers,
      LocalDate devolvedPowersDate) {
    return EbsApplicationMappingContext.builder()
        .ebsCaseDetail(ebsCaseDetail)
        .applicationType(new CommonLookupValueDetail()
            .code("apptypecode")
            .description("apptypedesc"))
        .amendmentProceedingsInEbs(Collections.singletonList(
            buildProceedingMappingContext(ebsCaseDetail.getApplicationDetails().getProceedings().get(0))))
        .caseOutcome(buildCaseOutcomeMappingContext(ebsCaseDetail))
        .caseWithOnlyDraftProceedings(Boolean.TRUE)
        .certificate(new CommonLookupValueDetail()
            .code("certcode")
            .description("certificate descr"))
        .currentProviderBilledAmount(BigDecimal.ONE)
        .devolvedPowers(Pair.of(devolvedPowers, devolvedPowersDate))
        .feeEarnerContact(new uk.gov.laa.ccms.data.model.ContactDetail()
            .id(100)
            .name("feeEarnerName"))
        .supervisorContact(new uk.gov.laa.ccms.data.model.ContactDetail()
            .id(101)
            .name("supName"))
        .meansAssessment(ebsCaseDetail.getApplicationDetails().getMeansAssessments().get(0))
        .meritsAssessment(ebsCaseDetail.getApplicationDetails().getMeritsAssessments().get(0))
        .priorAuthorities(Collections.singletonList(buildPriorAuthorityMappingContext(
            ebsCaseDetail.getPriorAuthorities().get(0))))
        .proceedings(Collections.singletonList(
            buildProceedingMappingContext(ebsCaseDetail.getApplicationDetails().getProceedings().get(0))))
        .providerDetail(new uk.gov.laa.ccms.data.model.ProviderDetail()
            .id(1)
            .name("provname"))
        .providerOffice(new OfficeDetail().id(1000).name("offName"))
        .build();
  }

  private EbsProceedingMappingContext buildProceedingMappingContext(
      Proceeding ebsProceeding) {
    return EbsProceedingMappingContext.builder()
        .ebsProceeding(ebsProceeding)
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

  private EbsCaseOutcomeMappingContext buildCaseOutcomeMappingContext(CaseDetail soaCase) {
    return EbsCaseOutcomeMappingContext.builder()
        .ebsCase(soaCase)
        .costAwards(Collections.singletonList(soaCase.getAwards().get(0)))
        .financialAwards(Collections.singletonList(soaCase.getAwards().get(1)))
        .landAwards(Collections.singletonList(soaCase.getAwards().get(2)))
        .otherAssetAwards(Collections.singletonList(soaCase.getAwards().get(3)))
        .proceedingOutcomes(Collections.singletonList(buildProceedingMappingContext(
            soaCase.getApplicationDetails().getProceedings().get(0))))
        .build();
  }

  private EbsPriorAuthorityMappingContext buildPriorAuthorityMappingContext(
      PriorAuthority soaPriorAuthority) {
    return EbsPriorAuthorityMappingContext.builder()
        .ebsPriorAuthority(soaPriorAuthority)
        .priorAuthorityTypeLookup(buildPriorAuthorityTypeDetail("dataType"))
        .items(Collections.singletonList(Pair.of(buildPriorAuthorityDetail("dataType"),
            new CommonLookupValueDetail()
                .code("priorAuthCode")
                .description("priorAuthDesc"))))
        .build();
  }

  @Test
  @DisplayName("Test toCaseDetail with valid CaseMappingContext")
  void testToSoaCaseDetail_Valid() {
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

    final CaseDetail result = applicationMapper.toEbsCaseDetail(context);

    assertNotNull(result);
    assertEquals(applicationDetail.getCaseReferenceNumber(), result.getCaseReferenceNumber());
    assertNotNull(result.getApplicationDetails());
    assertNotNull(result.getLinkedCases());
    assertEquals(1, result.getLinkedCases().size());
    assertEquals("LC123", result.getLinkedCases().get(0).getCaseReferenceNumber());
    assertNotNull(result.getPriorAuthorities());
    assertEquals(1, result.getPriorAuthorities().size());
    assertEquals("Test Prior Authority", result.getPriorAuthorities().get(0).getDescription());
    assertNotNull(result.getCaseDocs());
    assertEquals(1, result.getCaseDocs().size());
    assertEquals("DOC123", result.getCaseDocs().get(0).getCcmsDocumentId());
    assertNotNull(result.getRecordHistory());
  }

  @Test
  @DisplayName("Test toCaseDetail with null CaseMappingContext")
  void testToSoaCaseDetail_Null() {
    assertNull(applicationMapper.toEbsCaseDetail(null));
  }


  @Test
  @DisplayName("Test toSoaCaseDoc with valid BaseEvidenceDocumentDetail")
  void testToSoaCaseDoc_Valid() {
    final BaseEvidenceDocumentDetail evidenceDocumentDetail
        = new BaseEvidenceDocumentDetail()
        .documentType(new StringDisplayValue()
            .displayValue("Test Document"))
        .registeredDocumentId("12345");

    final CaseDoc result = applicationMapper.toEbsCaseDoc(evidenceDocumentDetail);

    assertNotNull(result);
    assertEquals("12345", result.getCcmsDocumentId());
    assertEquals("Test Document", result.getDocumentSubject());
  }

  @Test
  @DisplayName("Test toSoaCaseDoc with null BaseEvidenceDocumentDetail")
  void testToSoaCaseDoc_Null() {
    assertNull(applicationMapper.toEbsCaseDoc(null));
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

    final PriorAuthority result = applicationMapper.toEbsPriorAuthority(priorAuthorityDetail);

    assertNotNull(result);
    assertEquals("Test Summary", result.getDescription());
    assertEquals("Test Justification", result.getReasonForRequest());
    assertEquals(new BigDecimal("1000.00"), result.getRequestAmount());
    assertEquals("Approved", result.getDecisionStatus());
    assertNotNull(result.getDetails());
    assertEquals(1, result.getDetails().size());
    assertEquals("PA123", result.getDetails().get(0).getName());
  }

  @Test
  @DisplayName("Test toSoaPriorAuthority with null PriorAuthorityDetail")
  void testToSoaPriorAuthority_Null() {
    assertNull(applicationMapper.toEbsPriorAuthority(null));
  }


  @Test
  @DisplayName("Test toSoaPriorAuthorityAttribute with valid ReferenceDataItemDetail")
  void testToSoaPriorAuthorityAttribute_Valid() {
    final ReferenceDataItemDetail referenceDataItemDetail = new ReferenceDataItemDetail()
        .code(new StringDisplayValue().id("AttributeName"))
        .value(new StringDisplayValue().id("AttributeValue"));

    final PriorAuthorityAttribute result = applicationMapper.toEbsPriorAuthorityAttribute(referenceDataItemDetail);

    assertNotNull(result);
    assertEquals("AttributeName", result.getName());
    assertEquals("AttributeValue", result.getValue());
  }

  @Test
  @DisplayName("Test toSoaPriorAuthorityAttribute with null ReferenceDataItemDetail")
  void testToSoaPriorAuthorityAttribute_Null() {
    assertNull(applicationMapper.toEbsPriorAuthorityAttribute(null));
  }


  @Test
  @DisplayName("Test toSoaLinkedCase with valid LinkedCaseDetail")
  void testToSoaLinkedCase_Valid() {

    final LinkedCaseDetail linkedCaseDetail = new LinkedCaseDetail()
        .lscCaseReference("LSC123")
        .relationToCase("Related Case");

    final LinkedCase result = applicationMapper.toEbsLinkedCase(linkedCaseDetail);

    assertNotNull(result);
    assertEquals("LSC123", result.getCaseReferenceNumber());
    assertEquals("Related Case", result.getLinkType());
  }

  @Test
  @DisplayName("Test toSoaLinkedCase with null LinkedCaseDetail")
  void testToSoaLinkedCase_Null() {
    assertNull(applicationMapper.toEbsLinkedCase(null));
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
  void testToEbsProviderDetail_Valid() {
    final StringDisplayValue providerContact = new StringDisplayValue();
    providerContact.setId("user123");

    final ApplicationProviderDetails providerDetails = new ApplicationProviderDetails();
    providerDetails.setProviderContact(providerContact);
    providerDetails.setProviderCaseReference("CASE123");
    providerDetails.provider(new IntDisplayValue().id(100));
    providerDetails.office(new IntDisplayValue().id(200));
    providerDetails.supervisor(new StringDisplayValue().id("300"));
    providerDetails.feeEarner(new StringDisplayValue().id("400"));

    final uk.gov.laa.ccms.data.model.ProviderDetails result = applicationMapper.toEbsProviderDetail(providerDetails);

    assertNotNull(result);
    assertEquals("user123", result.getContactUserId().getLoginId());
    assertEquals("CASE123", result.getProviderCaseReferenceNumber());
    assertEquals("100", result.getProviderFirmId());
    assertEquals("200", result.getProviderOfficeId());
    assertEquals("300", result.getSupervisorContactId());
    assertEquals("400", result.getFeeEarnerContactId());
  }

  @Test
  @DisplayName("Test toSoaProviderDetail with null ApplicationProviderDetails")
  void testToEbsProviderDetail_Null() {
    assertNull(applicationMapper.toEbsProviderDetail(null));
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
    final CategoryOfLaw result = applicationMapper.toEbsCategoryOfLaw(applicationDetail);

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
    assertNull(applicationMapper.toEbsCategoryOfLaw(null));
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

    final uk.gov.laa.ccms.data.model.AddressDetail result = applicationMapper.toEbsAddressDetail(addressDetail);

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
    assertNull(applicationMapper.toEbsAddressDetail(null));
  }

  @Test
  @DisplayName("Test toSoaProceedingDetail with valid ProceedingDetail")
  void testToEbsProceedingDetail_Valid() {
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

    final uk.gov.laa.ccms.data.model.Proceeding result = applicationMapper.toEbsProceedingDetail(proceedingDetail);

    assertNotNull(result);
    assertEquals("P_123", result.getProceedingCaseId());
    assertEquals("Status", result.getStatus());
    assertTrue(result.getLeadProceedingIndicator());
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
  void testToEbsProceedingDetail_Null() {
    assertNull(applicationMapper.toEbsProceedingDetail(null));
  }

  @Test
  @DisplayName("Test toSoaScopeLimitation with valid ScopeLimitationDetail")
  void testToSoaScopeLimitation_Valid() {
    final ScopeLimitationDetail scopeLimitationDetail = new ScopeLimitationDetail();
    scopeLimitationDetail.setScopeLimitation(new StringDisplayValue().id("scopeId").displayValue("Scope Description"));
    scopeLimitationDetail.setScopeLimitationWording("Scope Limitation Wording");
    scopeLimitationDetail.setDelegatedFuncApplyInd(new BooleanDisplayValue().flag(true));

    final ScopeLimitation result = applicationMapper.toEbsScopeLimitation(scopeLimitationDetail);

    assertNotNull(result);
    assertEquals("scopeId", result.getScopeLimitation());
    assertEquals("Scope Limitation Wording", result.getScopeLimitationWording());
    assertTrue(result.getDelegatedFunctionsApply());
  }

  @Test
  @DisplayName("Test toSoaScopeLimitation with null ScopeLimitationDetail")
  void testToSoaScopeLimitation_Null() {
    assertNull(applicationMapper.toEbsScopeLimitation(null));
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

    final OtherParty result = applicationMapper.toEbsOtherParty(opponentDetail);

    assertNotNull(result);
    assertNotNull(result.getPerson());
    assertNull(result.getOrganisation());
    assertEquals("ClientRelInd", result.getPerson().getRelationToClient());
    assertEquals("CaseRelInd", result.getPerson().getRelationToCase());
    assertEquals("NI12345", result.getPerson().getNiNumber());
    assertTrue(result.getPerson().getPartyLegalAidedInd());
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

    final OtherParty result = applicationMapper.toEbsOtherParty(opponentDetail);

    assertNotNull(result);
    assertNull(result.getPerson());
    assertNotNull(result.getOrganisation());
    assertEquals("ClientRelOrg", result.getOrganisation().getRelationToClient());
    assertEquals("CaseRelOrg", result.getOrganisation().getRelationToCase());
    assertEquals("OrgName", result.getOrganisation().getOrganisationName());
    assertEquals("Private", result.getOrganisation().getOrganisationType());
  }

  @Test
  @DisplayName("Test toSoaOtherParty with null or unknown type OpponentDetail")
  void testToSoaOtherParty_NullOrUnknownType() {
    final OpponentDetail opponentDetail = new OpponentDetail();
    opponentDetail.setType(null);

    final OtherParty result = applicationMapper.toEbsOtherParty(opponentDetail);

    assertNotNull(result);
    assertNull(result.getPerson());
    assertNull(result.getOrganisation());
  }

  @Test
  @DisplayName("Test toSoaOtherParty with null OpponentDetail")
  void testToSoaOtherParty_Null() {
    assertNull(applicationMapper.toEbsOtherParty(null));
  }

  @ParameterizedTest
  @DisplayName("Test toSoaPerson with valid OpponentDetail inputs")
  @CsvSource({
      "'TestClientRel', 'TestCaseRel', '123456', 'true', 'true', 'John Doe', 'Test Employer', 'Employer Address', '1000', '500', '1990-01-01', 'Employed', 'Cert123', 'Monthly', '2023-10-12', 'OtherInfo'",
      "'ClientRel2', 'CaseRel2', '987654', 'false', 'false', 'Jane Smith', 'Another Employer', 'Another Address', '2000', '1000', '1985-05-05', 'Unemployed', 'Cert456', 'Weekly', '2023-08-20', 'Info2'"
  })
  void testToEbsPerson_Valid(final String relationToClient, final String relationToCase, final String niNumber, final boolean legalAided,
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
    final OtherPartyPerson result = applicationMapper.toEbsPerson(opponentDetail);

    assertNotNull(result);
    assertEquals(relationToClient, result.getRelationToClient());
    assertEquals(relationToCase, result.getRelationToCase());
    assertEquals(niNumber, result.getNiNumber());
    assertEquals(legalAided, result.getPartyLegalAidedInd());
    assertEquals(courtOrderedMeansAssesment, result.getCourtOrderedMeansAssessment());
    assertEquals(contactName, result.getContactName());
    assertEquals(employerName, result.getEmployersName());
    assertEquals(employerAddress, result.getOrganisationAddress());
    assertEquals(new BigDecimal(assessedIncome).setScale(2), result.getAssessedIncome());
    assertEquals(new BigDecimal(assessedAssets).setScale(2), result.getAssessedAssets());
    assertEquals(toDate(parsedDateOfBirth), result.getDateOfBirth());
    assertEquals(employmentStatus, result.getEmploymentStatus());
    assertEquals(certificateNumber, result.getCertificateNumber());
    assertEquals(assessedIncomeFrequency, result.getAssessedIncomeFrequency());
    assertEquals(toDate(parsedAssessmentDate), result.getAssessmentDate());
    assertEquals(otherInformation, result.getOtherInformation());
  }

  @Test
  @DisplayName("Test toSoaPerson with null OpponentDetail")
  void testToEbsPerson_NullOpponentDetail() {
    assertNull(applicationMapper.toEbsPerson(null));
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

    final OtherPartyOrganisation result = applicationMapper.toEbsOrganisation(opponentDetail);

    assertNotNull(result);
    assertEquals(relationToClient, result.getRelationToClient());
    assertEquals(relationToCase, result.getRelationToCase());
    assertEquals(organisationName, result.getOrganisationName());
    assertEquals(organisationType, result.getOrganisationType());
    assertEquals(contactName, result.getContactName());
    assertEquals(currentlyTrading, result.getCurrentlyTrading());
    assertEquals(otherInformation, result.getOtherInformation());
    assertNull(result.getAddress());
  }

  @Test
  @DisplayName("Test toSoaOrganisation with null OpponentDetail")
  void testToSoaOrganisation_NullOpponentDetail() {
    assertNull(applicationMapper.toEbsOrganisation(null));
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
    assertEquals(expectedUserDefinedInd, result.getUserDefinedInd());
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

    final RecordHistory result = applicationMapper.toEbsRecordHistory(caseMappingContext);

    assertNotNull(result);
    assertEquals("testLoginId", result.getLastUpdatedBy().getLoginId());
    assertEquals(toLocalDateTime(testDate), result.getDateLastUpdated());
    assertEquals(toLocalDateTime(testDate), result.getDateCreated());
  }

  @Test
  @DisplayName("Test toSoaRecordHistory with null context")
  void testToSoaRecordHistory_NullContext() {
    assertNull( applicationMapper.toEbsRecordHistory(null));
  }

  private LocalDateTime toLocalDateTime(Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
  }

  private Date toDate(LocalDateTime localDateTime) {
    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

  private Date toDate(LocalDate localDate) {
    return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  private LocalDate toDate(Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }


}