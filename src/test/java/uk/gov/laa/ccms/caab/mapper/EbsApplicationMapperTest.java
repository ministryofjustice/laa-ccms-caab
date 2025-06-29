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
import uk.gov.laa.ccms.data.model.Award;
import uk.gov.laa.ccms.data.model.BaseClient;
import uk.gov.laa.ccms.data.model.CaseDetail;
import uk.gov.laa.ccms.data.model.CaseDoc;
import uk.gov.laa.ccms.data.model.CaseSummary;
import uk.gov.laa.ccms.data.model.CategoryOfLaw;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
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
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.ProviderDetails;
import uk.gov.laa.ccms.data.model.RecordHistory;
import uk.gov.laa.ccms.data.model.Recovery;
import uk.gov.laa.ccms.data.model.ScopeLimitation;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;
import uk.gov.laa.ccms.data.model.SubmittedApplicationDetails;
import uk.gov.laa.ccms.data.model.TimeRelatedAward;
import uk.gov.laa.ccms.data.model.UserDetail;

@DisplayName("EBS Application mapper test")
class EbsApplicationMapperTest {

  private final EbsApplicationMapper applicationMapper = new EbsApplicationMapperImpl() {};

  private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  @Test
  void testToApplicationDetailDevolvedPowers() {
    CaseDetail ebsCaseDetail = buildCaseDetail(APP_TYPE_EMERGENCY_DEVOLVED_POWERS);
    EbsApplicationMappingContext applicationMappingContext =
        buildApplicationMappingContext(
            ebsCaseDetail, true, ebsCaseDetail.getApplicationDetails().getDevolvedPowersDate());
    ApplicationDetail result = applicationMapper.toApplicationDetail(applicationMappingContext);

    assertNotNull(result);
    assertEquals(ebsCaseDetail.getCaseReferenceNumber(), result.getCaseReferenceNumber());
    assertEquals(
        applicationMappingContext.getCertificate().getCode(), result.getCertificate().getId());
    assertEquals(
        applicationMappingContext.getCertificate().getDescription(),
        result.getCertificate().getDisplayValue());
    assertEquals(
        ebsCaseDetail.getApplicationDetails().getApplicationAmendmentType(),
        result.getApplicationType().getId());
    assertEquals(
        applicationMappingContext.getApplicationType().getDescription(),
        result.getApplicationType().getDisplayValue());
    assertEquals(
        toDate(ebsCaseDetail.getRecordHistory().getDateCreated()), result.getDateCreated());
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
        ebsCaseDetail.getApplicationDetails().getProviderDetails().getContactUserId().getLoginId(),
        result.getProviderDetails().getProviderContact().getId());
    assertEquals(
        ebsCaseDetail.getApplicationDetails().getProviderDetails().getContactUserId().getUsername(),
        result.getProviderDetails().getProviderContact().getDisplayValue());
    assertEquals(
        applicationMappingContext.getProviderOffice().getId(),
        result.getProviderDetails().getOffice().getId());
    assertEquals(
        applicationMappingContext.getProviderOffice().getName(),
        result.getProviderDetails().getOffice().getDisplayValue());
    assertEquals(
        applicationMappingContext.getSupervisorContact().getId().toString(),
        result.getProviderDetails().getSupervisor().getId());
    assertEquals(
        applicationMappingContext.getSupervisorContact().getName(),
        result.getProviderDetails().getSupervisor().getDisplayValue());
    assertEquals(
        applicationMappingContext.getFeeEarnerContact().getId().toString(),
        result.getProviderDetails().getFeeEarner().getId());
    assertEquals(
        applicationMappingContext.getFeeEarnerContact().getName(),
        result.getProviderDetails().getFeeEarner().getDisplayValue());
    assertNotNull(result.getCorrespondenceAddress()); // Detail tested in specific test case
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
    assertEquals(ebsCaseDetail.getCaseStatus().getActualCaseStatus(), result.getStatus().getId());
    assertEquals(
        ebsCaseDetail.getCaseStatus().getDisplayCaseStatus(), result.getStatus().getDisplayValue());
    assertEquals(ebsCaseDetail.getAvailableFunctions(), result.getAvailableFunctions());
    assertTrue(result.getApplicationType().getDevolvedPowers().getUsed());
    assertEquals(
        toDate(ebsCaseDetail.getApplicationDetails().getDevolvedPowersDate()),
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
    EbsApplicationMappingContext applicationMappingContext =
        buildApplicationMappingContext(buildCaseDetail(APP_TYPE_EMERGENCY), false, null);

    ApplicationDetail result = applicationMapper.toApplicationDetail(applicationMappingContext);

    // Check the devolved powers
    assertFalse(result.getApplicationType().getDevolvedPowers().getUsed());
    assertNull(result.getApplicationType().getDevolvedPowers().getDateUsed());
  }

  @Test
  void testToCorrespondenceAddress() {
    EbsApplicationMappingContext applicationMappingContext =
        buildApplicationMappingContext(buildCaseDetail(APP_TYPE_EMERGENCY), false, null);
    @Valid
    SubmittedApplicationDetails ebsApplicationDetails =
        applicationMappingContext.getEbsCaseDetail().getApplicationDetails();

    AddressDetail result = applicationMapper.toCorrespondenceAddress(applicationMappingContext);

    assertNotNull(result);
    assertEquals(
        ebsApplicationDetails.getCorrespondenceAddress().getAddressLine1(),
        result.getAddressLine1());
    assertEquals(
        ebsApplicationDetails.getCorrespondenceAddress().getAddressLine2(),
        result.getAddressLine2());
    assertEquals(
        ebsApplicationDetails.getCorrespondenceAddress().getCareOfName(), result.getCareOf());
    assertEquals(ebsApplicationDetails.getCorrespondenceAddress().getCity(), result.getCity());
    assertEquals(
        ebsApplicationDetails.getCorrespondenceAddress().getCountry(), result.getCountry());
    assertEquals(ebsApplicationDetails.getCorrespondenceAddress().getCounty(), result.getCounty());
    assertEquals(
        ebsApplicationDetails.getCorrespondenceAddress().getHouse(), result.getHouseNameOrNumber());
    assertFalse(result.getNoFixedAbode());
    assertEquals(ebsApplicationDetails.getPreferredAddress(), result.getPreferredAddress());
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
    Proceeding ebsProceeding = buildProceedingDetail(STATUS_DRAFT);
    EbsProceedingMappingContext proceedingMappingContext =
        buildProceedingMappingContext(ebsProceeding);

    ProceedingDetail result = applicationMapper.toProceeding(proceedingMappingContext);

    assertNotNull(result);
    assertFalse(result.getEdited());
    assertEquals(ebsProceeding.getAvailableFunctions(), result.getAvailableFunctions());
    assertEquals(ebsProceeding.getStage(), result.getStage());
    assertEquals(toDate(ebsProceeding.getDateGranted()), result.getDateGranted());
    assertEquals(toDate(ebsProceeding.getDateCostsValid()), result.getDateCostsValid());
    assertEquals(ebsProceeding.getAvailableFunctions(), result.getAvailableFunctions());

    assertEquals(ebsProceeding.getProceedingCaseId(), result.getEbsId());
    assertEquals(ebsProceeding.getLeadProceedingIndicator(), result.getLeadProceedingInd());
    assertEquals(
        proceedingMappingContext.getMatterType().getCode(), result.getMatterType().getId());
    assertEquals(
        proceedingMappingContext.getMatterType().getDescription(),
        result.getMatterType().getDisplayValue());
    assertEquals(
        proceedingMappingContext.getProceedingLookup().getCode(),
        result.getProceedingType().getId());
    assertEquals(
        proceedingMappingContext.getProceedingLookup().getName(),
        result.getProceedingType().getDisplayValue());
    assertEquals(ebsProceeding.getProceedingDescription(), result.getDescription());
    assertEquals(
        proceedingMappingContext.getProceedingLookup().getLarScope(), result.getLarScope());
    assertEquals(
        proceedingMappingContext.getLevelOfService().getCode(), result.getLevelOfService().getId());
    assertEquals(
        proceedingMappingContext.getLevelOfService().getDescription(),
        result.getLevelOfService().getDisplayValue());
    assertEquals(
        proceedingMappingContext.getClientInvolvement().getCode(),
        result.getClientInvolvement().getId());
    assertEquals(
        proceedingMappingContext.getClientInvolvement().getDescription(),
        result.getClientInvolvement().getDisplayValue());
    assertEquals(
        proceedingMappingContext.getProceedingStatusLookup().getCode(), result.getStatus().getId());
    assertEquals(
        proceedingMappingContext.getProceedingStatusLookup().getDescription(),
        result.getStatus().getDisplayValue());
    assertEquals(ebsProceeding.getOrderType(), result.getTypeOfOrder().getId());

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
    ScopeLimitation ebsScopeLimitation = buildScopeLimitation("");
    CommonLookupValueDetail scopeLimitationLookup =
        new CommonLookupValueDetail().code("scopecode").description("scopedesc");

    ScopeLimitationDetail result =
        applicationMapper.toScopeLimitation(Pair.of(ebsScopeLimitation, scopeLimitationLookup));

    assertNotNull(result);
    assertEquals(ebsScopeLimitation.getScopeLimitationId(), result.getEbsId());
    assertEquals(scopeLimitationLookup.getCode(), result.getScopeLimitation().getId());
    assertEquals(
        scopeLimitationLookup.getDescription(), result.getScopeLimitation().getDisplayValue());
    assertEquals(
        ebsScopeLimitation.getScopeLimitationWording(), result.getScopeLimitationWording());
    assertEquals(
        ebsScopeLimitation.getDelegatedFunctionsApply(),
        result.getDelegatedFuncApplyInd().getFlag());
    assertNull(result.getDefaultInd());
    assertNull(result.getNonDefaultWordingReqd());
    assertNull(result.getStage());
  }

  @Test
  void testToProceedingOutcome() {
    Proceeding ebsProceeding = buildProceedingDetail(STATUS_DRAFT);
    EbsProceedingMappingContext proceedingMappingContext =
        buildProceedingMappingContext(ebsProceeding);
    OutcomeDetail ebsOutcomeDetail = ebsProceeding.getOutcome();

    ProceedingOutcomeDetail result =
        applicationMapper.toProceedingOutcome(proceedingMappingContext);

    assertNotNull(result);
    assertEquals(ebsProceeding.getProceedingDescription(), result.getDescription());
    assertEquals(
        proceedingMappingContext.getMatterType().getCode(), result.getMatterType().getId());
    assertEquals(
        proceedingMappingContext.getMatterType().getDescription(),
        result.getMatterType().getDisplayValue());
    assertEquals(ebsProceeding.getProceedingCaseId(), result.getProceedingCaseId());
    assertEquals(
        proceedingMappingContext.getProceedingLookup().getCode(),
        result.getProceedingType().getId());
    assertEquals(
        proceedingMappingContext.getProceedingLookup().getName(),
        result.getProceedingType().getDisplayValue());
    assertEquals(ebsOutcomeDetail.getAltAcceptanceReason(), result.getAdrInfo());
    assertEquals(ebsOutcomeDetail.getAltDisputeResolution(), result.getAlternativeResolution());
    assertEquals(proceedingMappingContext.getCourtLookup().getCode(), result.getCourtCode());
    assertEquals(proceedingMappingContext.getCourtLookup().getDescription(), result.getCourtName());
    assertEquals(toDate(ebsOutcomeDetail.getFinalWorkDate()), result.getDateOfFinalWork());
    assertNull(result.getDateOfIssue());
    assertEquals(ebsOutcomeDetail.getResolutionMethod(), result.getResolutionMethod());
    assertEquals(
        proceedingMappingContext.getOutcomeResultLookup().getOutcomeResult(),
        result.getResult().getId());
    assertEquals(
        proceedingMappingContext.getOutcomeResultLookup().getOutcomeResultDescription(),
        result.getResult().getDisplayValue());
    assertEquals(ebsOutcomeDetail.getAdditionalResultInfo(), result.getResultInfo());
    assertEquals(ebsOutcomeDetail.getStageEnd(), result.getStageEnd().getId());
    assertEquals(
        proceedingMappingContext.getStageEndLookup().getDescription(),
        result.getStageEnd().getDisplayValue());
    assertEquals(ebsOutcomeDetail.getWiderBenefits(), result.getWiderBenefits());
    assertEquals(ebsOutcomeDetail.getOutcomeCourtCaseNumber(), result.getOutcomeCourtCaseNo());
  }

  @Test
  void testMapMostRecentAssessment_SingleAssessment() {
    uk.gov.laa.ccms.data.model.AssessmentResult ebsAssessmentResult = buildAssessmentResult("");

    AssessmentResult result = applicationMapper.toAssessmentResult(ebsAssessmentResult);

    assertNotNull(result);
    assertEquals(ebsAssessmentResult.getAssessmentId(), result.getAssessmentId());
    assertEquals(toDate(ebsAssessmentResult.getDate()), result.getDate());
    assertEquals(
        ebsAssessmentResult.getAssessmentDetails().getFirst().getCaption(),
        result.getAssessmentDetails().getFirst().getCaption());
    assertEquals(
        ebsAssessmentResult.getAssessmentDetails().getFirst().getScreenName(),
        result.getAssessmentDetails().getFirst().getScreenName());
    assertEquals(
        ebsAssessmentResult.getAssessmentDetails().getFirst().getEntity().getFirst().getCaption(),
        result.getAssessmentDetails().getFirst().getEntity().getFirst().getCaption());
    assertEquals(
        ebsAssessmentResult
            .getAssessmentDetails()
            .getFirst()
            .getEntity()
            .getFirst()
            .getEntityName(),
        result.getAssessmentDetails().getFirst().getEntity().getFirst().getEntityName());
    assertEquals(
        ebsAssessmentResult
            .getAssessmentDetails()
            .getFirst()
            .getEntity()
            .getFirst()
            .getSequenceNumber(),
        result.getAssessmentDetails().getFirst().getEntity().getFirst().getSequenceNumber());
    assertEquals(
        ebsAssessmentResult
            .getAssessmentDetails()
            .getFirst()
            .getEntity()
            .getFirst()
            .getInstances()
            .getFirst()
            .getCaption(),
        result
            .getAssessmentDetails()
            .getFirst()
            .getEntity()
            .getFirst()
            .getInstances()
            .getFirst()
            .getCaption());
    assertEquals(
        ebsAssessmentResult
            .getAssessmentDetails()
            .getFirst()
            .getEntity()
            .getFirst()
            .getInstances()
            .getFirst()
            .getInstanceLabel(),
        result
            .getAssessmentDetails()
            .getFirst()
            .getEntity()
            .getFirst()
            .getInstances()
            .getFirst()
            .getInstanceLabel());
    assertEquals(
        ebsAssessmentResult
            .getAssessmentDetails()
            .getFirst()
            .getEntity()
            .getFirst()
            .getInstances()
            .getFirst()
            .getAttributes()
            .getFirst()
            .getAttribute(),
        result
            .getAssessmentDetails()
            .getFirst()
            .getEntity()
            .getFirst()
            .getInstances()
            .getFirst()
            .getAttributes()
            .getFirst()
            .getAttribute());
    assertEquals(
        ebsAssessmentResult
            .getAssessmentDetails()
            .getFirst()
            .getEntity()
            .getFirst()
            .getInstances()
            .getFirst()
            .getAttributes()
            .getFirst()
            .getCaption(),
        result
            .getAssessmentDetails()
            .getFirst()
            .getEntity()
            .getFirst()
            .getInstances()
            .getFirst()
            .getAttributes()
            .getFirst()
            .getCaption());
    assertEquals(
        ebsAssessmentResult
            .getAssessmentDetails()
            .getFirst()
            .getEntity()
            .getFirst()
            .getInstances()
            .getFirst()
            .getAttributes()
            .getFirst()
            .getResponseText(),
        result
            .getAssessmentDetails()
            .getFirst()
            .getEntity()
            .getFirst()
            .getInstances()
            .getFirst()
            .getAttributes()
            .getFirst()
            .getResponseText());
    assertEquals(
        ebsAssessmentResult
            .getAssessmentDetails()
            .getFirst()
            .getEntity()
            .getFirst()
            .getInstances()
            .getFirst()
            .getAttributes()
            .getFirst()
            .getResponseType(),
        result
            .getAssessmentDetails()
            .getFirst()
            .getEntity()
            .getFirst()
            .getInstances()
            .getFirst()
            .getAttributes()
            .getFirst()
            .getResponseType());
    assertEquals(
        ebsAssessmentResult
            .getAssessmentDetails()
            .getFirst()
            .getEntity()
            .getFirst()
            .getInstances()
            .getFirst()
            .getAttributes()
            .getFirst()
            .getResponseValue(),
        result
            .getAssessmentDetails()
            .getFirst()
            .getEntity()
            .getFirst()
            .getInstances()
            .getFirst()
            .getAttributes()
            .getFirst()
            .getResponseValue());
    assertEquals(
        ebsAssessmentResult.getResults().getFirst().getAttribute(),
        result.getResults().getFirst().getAttribute());
    assertEquals(
        ebsAssessmentResult.getResults().getFirst().getAttributeValue(),
        result.getResults().getFirst().getAttributeValue());
  }

  @Test
  void testToAddress() {
    uk.gov.laa.ccms.data.model.AddressDetail ebsAddress = buildAddressDetail("");

    AddressDetail result = applicationMapper.toAddress(ebsAddress);
    assertNotNull(result);
    assertEquals(ebsAddress.getCareOfName(), result.getCareOf());
    assertEquals(ebsAddress.getHouse(), result.getHouseNameOrNumber());
    assertEquals(ebsAddress.getPostalCode(), result.getPostcode());
    assertFalse(result.getNoFixedAbode());
    assertNull(result.getPreferredAddress());
  }

  @Test
  void testToClient() {
    BaseClient ebsBaseClient = buildBaseClient();

    ClientDetail result = applicationMapper.toClient(ebsBaseClient);

    assertNotNull(result);
    assertEquals(ebsBaseClient.getClientReferenceNumber(), result.getReference());

    assertEquals(ebsBaseClient.getFirstName(), result.getFirstName());
    assertEquals(ebsBaseClient.getSurname(), result.getSurname());
  }

  @Test
  void testToIndividualOpponent() {
    OtherParty ebsOtherParty = buildOtherPartyPerson();

    OpponentDetail result = applicationMapper.toIndividualOpponent(ebsOtherParty);

    assertNotNull(result);
    assertEquals(OPPONENT_TYPE_INDIVIDUAL, result.getType());
    assertEquals(ebsOtherParty.getOtherPartyId(), result.getEbsId());
    assertEquals(
        ebsOtherParty.getPerson().getCourtOrderedMeansAssessment(),
        result.getCourtOrderedMeansAssessment());
    assertEquals(ebsOtherParty.getPerson().getOrganisationAddress(), result.getEmployerAddress());
    assertEquals(ebsOtherParty.getPerson().getOrganisationName(), result.getEmployerName());
    assertEquals(ebsOtherParty.getPerson().getPartyLegalAidedInd(), result.getLegalAided());
    assertEquals(ebsOtherParty.getPerson().getNiNumber(), result.getNationalInsuranceNumber());
    assertEquals(ebsOtherParty.getPerson().getRelationToCase(), result.getRelationshipToCase());
    assertEquals(ebsOtherParty.getPerson().getRelationToClient(), result.getRelationshipToClient());
    assertEquals(
        ebsOtherParty.getPerson().getContactDetails().getMobileNumber(),
        result.getTelephoneMobile());
    assertEquals(ebsOtherParty.getPerson().getContactDetails().getFax(), result.getFaxNumber());
    assertEquals(
        ebsOtherParty.getPerson().getPublicFundingAppliedInd(), result.getPublicFundingApplied());
    assertFalse(result.getDeleteInd());

    assertEquals(toDate(ebsOtherParty.getPerson().getDateOfBirth()), result.getDateOfBirth());
    assertNotNull(result.getAddress()); // Detail tested elsewhere
    assertEquals(ebsOtherParty.getPerson().getEmploymentStatus(), result.getEmploymentStatus());
    assertEquals(ebsOtherParty.getPerson().getCertificateNumber(), result.getCertificateNumber());
    assertEquals(
        ebsOtherParty.getPerson().getAssessedIncomeFrequency(),
        result.getAssessedIncomeFrequency());
    assertEquals(ebsOtherParty.getPerson().getAssessedIncome(), result.getAssessedIncome());
    assertEquals(ebsOtherParty.getPerson().getAssessedAssets(), result.getAssessedAssets());
    assertEquals(toDate(ebsOtherParty.getPerson().getAssessmentDate()), result.getAssessmentDate());
    assertEquals(ebsOtherParty.getPerson().getOtherInformation(), result.getOtherInformation());

    assertEquals(
        ebsOtherParty.getPerson().getContactDetails().getTelephoneHome(),
        result.getTelephoneHome());
    assertEquals(
        ebsOtherParty.getPerson().getContactDetails().getTelephoneWork(),
        result.getTelephoneWork());
    assertEquals(
        ebsOtherParty.getPerson().getContactDetails().getEmailAddress(), result.getEmailAddress());

    assertEquals(ebsOtherParty.getPerson().getName().getTitle(), result.getTitle());
    assertEquals(ebsOtherParty.getPerson().getName().getFirstName(), result.getFirstName());
    assertEquals(ebsOtherParty.getPerson().getName().getSurname(), result.getSurname());
    assertEquals(ebsOtherParty.getPerson().getName().getMiddleName(), result.getMiddleNames());
  }

  @Test
  void testToOrganisationOpponent() {
    OtherParty ebsOtherParty = buildOtherPartyOrganisation();

    OpponentDetail result = applicationMapper.toOrganisationOpponent(ebsOtherParty);

    assertNotNull(result);
    assertEquals(OPPONENT_TYPE_ORGANISATION, result.getType());
    assertEquals(ebsOtherParty.getOtherPartyId(), result.getEbsId());
    assertEquals(
        ebsOtherParty.getOrganisation().getOrganisationName(), result.getOrganisationName());
    assertEquals(
        ebsOtherParty.getOrganisation().getOrganisationType(), result.getOrganisationType());
    assertEquals(ebsOtherParty.getOrganisation().getContactName(), result.getContactNameRole());
    assertEquals(
        ebsOtherParty.getOrganisation().getRelationToCase(), result.getRelationshipToCase());
    assertEquals(
        ebsOtherParty.getOrganisation().getRelationToClient(), result.getRelationshipToClient());
    assertEquals(
        ebsOtherParty.getOrganisation().getContactDetails().getMobileNumber(),
        result.getTelephoneMobile());
    assertEquals(
        ebsOtherParty.getOrganisation().getContactDetails().getFax(), result.getFaxNumber());
    assertEquals(
        ebsOtherParty.getOrganisation().getRelationToCase(), result.getRelationshipToCase());
    assertFalse(result.getDeleteInd());

    assertEquals(
        ebsOtherParty.getOrganisation().getCurrentlyTrading(), result.getCurrentlyTrading());
    assertEquals(
        ebsOtherParty.getOrganisation().getOtherInformation(), result.getOtherInformation());
    assertNotNull(result.getAddress()); // Detail tested elsewhere
    assertEquals(
        ebsOtherParty.getOrganisation().getContactDetails().getEmailAddress(),
        result.getEmailAddress());
    assertEquals(
        ebsOtherParty.getOrganisation().getContactDetails().getTelephoneHome(),
        result.getTelephoneHome());
    assertEquals(
        ebsOtherParty.getOrganisation().getContactDetails().getTelephoneWork(),
        result.getTelephoneWork());
  }

  @Test
  void testToLinkedCase() {
    LinkedCase ebsLinkedCase = buildLinkedCase();

    LinkedCaseDetail result = applicationMapper.toLinkedCase(ebsLinkedCase);

    assertNotNull(result);
    assertEquals(ebsLinkedCase.getCaseReferenceNumber(), result.getLscCaseReference());
    assertEquals(
        ebsLinkedCase.getClient().getClientReferenceNumber(), result.getClient().getReference());
    assertEquals(ebsLinkedCase.getClient().getFirstName(), result.getClient().getFirstName());
    assertEquals(ebsLinkedCase.getClient().getSurname(), result.getClient().getSurname());
    assertEquals(ebsLinkedCase.getCategoryOfLawDesc(), result.getCategoryOfLaw());
    assertEquals(ebsLinkedCase.getProviderReferenceNumber(), result.getProviderCaseReference());
    assertEquals(ebsLinkedCase.getFeeEarnerName(), result.getFeeEarner());
    assertEquals(ebsLinkedCase.getCaseStatus(), result.getStatus());
    assertEquals(ebsLinkedCase.getLinkType(), result.getRelationToCase());
  }

  @Test
  void testToPriorAuthority() {
    PriorAuthority ebsPriorAuthority = buildPriorAuthority();
    EbsPriorAuthorityMappingContext priorAuthorityMappingContext =
        buildPriorAuthorityMappingContext(ebsPriorAuthority);
    PriorAuthorityTypeDetail priorAuthorityTypeDetail =
        priorAuthorityMappingContext.getPriorAuthorityTypeLookup();

    PriorAuthorityDetail result = applicationMapper.toPriorAuthority(priorAuthorityMappingContext);

    assertNotNull(result);
    assertEquals(ebsPriorAuthority.getDecisionStatus(), result.getStatus());
    assertEquals(ebsPriorAuthority.getDescription(), result.getSummary());
    assertEquals(priorAuthorityTypeDetail.getCode(), result.getType().getId());
    assertEquals(priorAuthorityTypeDetail.getDescription(), result.getType().getDisplayValue());
    assertEquals(ebsPriorAuthority.getReasonForRequest(), result.getJustification());
    assertEquals(ebsPriorAuthority.getRequestAmount(), result.getAmountRequested());
    assertEquals(priorAuthorityTypeDetail.getValueRequired(), result.getValueRequired());
    assertNotNull(result.getItems());
    assertEquals(1, result.getItems().size()); // Detail tested elsewhere
  }

  @Test
  void testToReferenceDataItem() {
    uk.gov.laa.ccms.data.model.PriorAuthorityDetail priorAuthorityDetail =
        buildPriorAuthorityDetail("dataType");
    CommonLookupValueDetail priorAuthLookup = new CommonLookupValueDetail();

    ReferenceDataItemDetail result =
        applicationMapper.toReferenceDataItem(Pair.of(priorAuthorityDetail, priorAuthLookup));

    assertNotNull(result);
    assertEquals(priorAuthorityDetail.getCode(), result.getCode().getId());
    assertEquals(priorAuthorityDetail.getDescription(), result.getCode().getDisplayValue());
    assertEquals(priorAuthorityDetail.getDataType(), result.getType());
    assertEquals(priorAuthorityDetail.getLovCode(), result.getLovLookUp());
    assertEquals(priorAuthorityDetail.getMandatoryFlag(), result.getMandatory());
  }

  @Test
  void testToCaseOutcome() {
    CaseDetail ebsCaseDetail = buildCaseDetail(APP_TYPE_EMERGENCY);
    EbsCaseOutcomeMappingContext caseOutcomeMappingContext =
        buildCaseOutcomeMappingContext(ebsCaseDetail);

    CaseOutcomeDetail result = applicationMapper.toCaseOutcome(caseOutcomeMappingContext);

    assertNotNull(result);
    assertEquals(ebsCaseDetail.getLegalHelpCosts(), result.getLegalCosts());
    assertEquals(
        ebsCaseDetail.getApplicationDetails().getLarDetails().getLegalHelpOfficeCode(),
        result.getOfficeCode());
    assertEquals(
        ebsCaseDetail.getApplicationDetails().getLarDetails().getLegalHelpUfn(),
        result.getUniqueFileNo());
    assertEquals(ebsCaseDetail.getDischargeStatus().getOtherDetails(), result.getOtherDetails());
    assertEquals(ebsCaseDetail.getDischargeStatus().getReason(), result.getDischargeReason());
    assertEquals(
        ebsCaseDetail.getDischargeStatus().getClientContinuePvtInd(),
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
    Recovery ebsRecovery = buildRecovery();

    RecoveryDetail result = applicationMapper.toRecovery(ebsRecovery);

    assertNotNull(result);
    assertEquals(ebsRecovery.getAwardValue(), result.getAwardAmount());
    assertEquals(
        ebsRecovery.getRecoveredAmount().getClient().getPaidToLsc(),
        result.getClientAmountPaidToLsc());
    assertEquals(
        ebsRecovery.getRecoveredAmount().getClient().getAmount(), result.getClientRecoveryAmount());
    assertEquals(
        toDate(ebsRecovery.getRecoveredAmount().getClient().getDateReceived()),
        result.getClientRecoveryDate());
    assertEquals(
        ebsRecovery.getRecoveredAmount().getCourt().getPaidToLsc(),
        result.getCourtAmountPaidToLsc());
    assertEquals(
        ebsRecovery.getRecoveredAmount().getCourt().getAmount(), result.getCourtRecoveryAmount());
    assertEquals(
        toDate(ebsRecovery.getRecoveredAmount().getCourt().getDateReceived()),
        result.getCourtRecoveryDate());
    assertEquals(
        ebsRecovery.getRecoveredAmount().getSolicitor().getPaidToLsc(),
        result.getSolicitorAmountPaidToLsc());
    assertEquals(
        ebsRecovery.getRecoveredAmount().getSolicitor().getAmount(),
        result.getSolicitorRecoveryAmount());
    assertEquals(
        toDate(ebsRecovery.getRecoveredAmount().getSolicitor().getDateReceived()),
        result.getSolicitorRecoveryDate());
    assertEquals(ebsRecovery.getOfferedAmount().getAmount(), result.getOfferedAmount());
    assertEquals(
        ebsRecovery.getOfferedAmount().getConditionsOfOffer(), result.getConditionsOfOffer());
    assertEquals(ebsRecovery.getOfferedAmount().getConditionsOfOffer(), result.getOfferDetails());
    assertEquals(ebsRecovery.getLeaveOfCourtReqdInd(), result.getLeaveOfCourtRequiredInd());
    assertNull(result.getAwardType()); // Populated by a specific award mapper method
    assertNull(result.getDescription()); // Populated by a specific award mapper method

    // From the afterMapping, the sum of recovered amounts
    assertEquals(
        ebsRecovery
            .getRecoveredAmount()
            .getClient()
            .getAmount()
            .add(ebsRecovery.getRecoveredAmount().getCourt().getAmount())
            .add(ebsRecovery.getRecoveredAmount().getSolicitor().getAmount()),
        result.getRecoveredAmount());
    assertEquals(
        result.getAwardAmount().subtract(result.getRecoveredAmount()),
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
    Award ebsAward = buildCostAward();

    CostAwardDetail result = applicationMapper.toCostAward(ebsAward);

    assertNotNull(result);
    assertEquals(ebsAward.getAwardId(), result.getEbsId());
    assertEquals(AWARD_TYPE_COST, result.getAwardType());
    assertEquals(AWARD_TYPE_COST_DESCRIPTION, result.getDescription());
    assertEquals(ebsAward.getAwardType(), result.getAwardCode());
    assertEquals(toDate(ebsAward.getCostAward().getOrderDate()), result.getDateOfOrder());
    assertEquals(
        ebsAward.getCostAward().getPreCertificateAwardLsc(), result.getPreCertificateLscCost());
    assertEquals(
        ebsAward.getCostAward().getPreCertificateAwardOth(), result.getPreCertificateOtherCost());
    assertEquals(
        ebsAward.getCostAward().getCertificateCostRateLsc(), result.getCertificateCostLsc());
    assertNotNull(result.getRecovery()); // detail tested separately
    assertNotNull(result.getLiableParties()); // detail tested separately
    assertEquals(
        ebsAward.getCostAward().getLiableParties().size(), result.getLiableParties().size());

    // Like for like mappings
    assertEquals(ebsAward.getDeleteAllowed(), result.getDeleteAllowed());
    assertEquals(ebsAward.getUpdateAllowed(), result.getUpdateAllowed());
    assertEquals(
        ebsAward.getCostAward().getServiceAddress().getAddressLine1(), result.getAddressLine1());
    assertEquals(
        ebsAward.getCostAward().getServiceAddress().getAddressLine2(), result.getAddressLine2());
    assertEquals(
        ebsAward.getCostAward().getServiceAddress().getAddressLine3(), result.getAddressLine3());
    assertEquals(
        ebsAward.getCostAward().getCertificateCostRateMarket(), result.getCertificateCostMarket());
    assertEquals(
        ebsAward.getCostAward().getCourtAssessmentStatus(), result.getCourtAssessmentStatus());
    assertEquals(toDate(ebsAward.getCostAward().getOrderDateServed()), result.getOrderServedDate());
    assertEquals(ebsAward.getCostAward().getInterestAwardedRate(), result.getInterestAwardedRate());
    assertEquals(
        toDate(ebsAward.getCostAward().getInterestAwardedStartDate()),
        result.getInterestStartDate());
    assertEquals(ebsAward.getCostAward().getAwardedBy(), result.getAwardedBy());

    assertEquals(ebsAward.getCostAward().getOtherDetails(), result.getOtherDetails());

    // afterMapping
    assertEquals(AWARD_TYPE_COST, result.getRecovery().getAwardType());
    assertEquals(AWARD_TYPE_COST_DESCRIPTION, result.getRecovery().getDescription());
    assertEquals(
        ebsAward
            .getCostAward()
            .getCertificateCostRateLsc()
            .add(ebsAward.getCostAward().getCertificateCostRateMarket()),
        result.getTotalCertCostsAwarded());

    // afterMapping (baseAward)
    result
        .getLiableParties()
        .forEach(liableParty -> assertEquals(AWARD_TYPE_COST, liableParty.getAwardType()));
  }

  @Test
  void testToFinancialAward() {
    Award ebsAward = buildFinancialAward();

    FinancialAwardDetail result = applicationMapper.toFinancialAward(ebsAward);

    assertNotNull(result);
    assertEquals(ebsAward.getAwardId(), result.getEbsId());
    assertEquals(AWARD_TYPE_FINANCIAL, result.getAwardType());
    assertEquals(AWARD_TYPE_FINANCIAL_DESCRIPTION, result.getDescription());
    assertEquals(ebsAward.getAwardType(), result.getAwardCode());
    assertEquals(toDate(ebsAward.getFinancialAward().getOrderDate()), result.getDateOfOrder());
    assertEquals(ebsAward.getFinancialAward().getAmount(), result.getAwardAmount());
    assertEquals(
        toDate(ebsAward.getFinancialAward().getOrderDateServed()), result.getOrderServedDate());
    assertEquals(
        ebsAward.getFinancialAward().getStatutoryChangeReason(),
        result.getStatutoryChargeExemptReason());

    assertNotNull(result.getRecovery()); // detail tested separately

    assertNotNull(result.getLiableParties());
    assertEquals(
        ebsAward.getFinancialAward().getLiableParties().size(), result.getLiableParties().size());

    // Like for like mappings
    assertEquals(ebsAward.getDeleteAllowed(), result.getDeleteAllowed());
    assertEquals(ebsAward.getUpdateAllowed(), result.getUpdateAllowed());
    assertEquals(
        ebsAward.getFinancialAward().getServiceAddress().getAddressLine1(),
        result.getAddressLine1());
    assertEquals(
        ebsAward.getFinancialAward().getServiceAddress().getAddressLine2(),
        result.getAddressLine2());
    assertEquals(
        ebsAward.getFinancialAward().getServiceAddress().getAddressLine3(),
        result.getAddressLine3());
    assertEquals(
        ebsAward.getFinancialAward().getInterimAward().toString(), result.getInterimAward());
    assertEquals(ebsAward.getFinancialAward().getAwardedBy(), result.getAwardedBy());
    assertEquals(
        toDate(ebsAward.getFinancialAward().getOrderDateServed()), result.getOrderServedDate());
    assertEquals(
        ebsAward.getFinancialAward().getAwardJustifications(), result.getAwardJustifications());
    assertEquals(ebsAward.getFinancialAward().getOtherDetails(), result.getOtherDetails());

    // afterMapping
    assertEquals(AWARD_TYPE_FINANCIAL, result.getRecovery().getAwardType());
    assertEquals(AWARD_TYPE_FINANCIAL_DESCRIPTION, result.getRecovery().getDescription());

    // afterMapping (baseAward)
    result
        .getLiableParties()
        .forEach(liableParty -> assertEquals(AWARD_TYPE_FINANCIAL, liableParty.getAwardType()));
  }

  @Test
  void testToLandAward() {
    Award ebsAward = buildLandAward();

    LandAwardDetail result = applicationMapper.toLandAward(ebsAward);

    assertNotNull(result);
    assertEquals(ebsAward.getAwardType(), result.getAwardCode());
    assertEquals(ebsAward.getAwardId(), result.getEbsId());
    assertEquals(toDate(ebsAward.getLandAward().getOrderDate()), result.getDateOfOrder());
    assertEquals(AWARD_TYPE_LAND, result.getAwardType());
    assertEquals(ebsAward.getLandAward().getValuation().getAmount(), result.getValuationAmount());
    assertEquals(
        ebsAward.getLandAward().getValuation().getCriteria(), result.getValuationCriteria());
    assertEquals(
        toDate(ebsAward.getLandAward().getValuation().getDate()), result.getValuationDate());
    assertNotNull(result.getTimeRecovery());
    assertNotNull(result.getLiableParties());
    assertEquals(
        ebsAward.getLandAward().getOtherProprietors().size(), result.getLiableParties().size());

    // Like for like mappings
    assertEquals(ebsAward.getDeleteAllowed(), result.getDeleteAllowed());
    assertEquals(ebsAward.getUpdateAllowed(), result.getUpdateAllowed());
    assertEquals(ebsAward.getLandAward().getDescription(), result.getDescription());
    assertEquals(ebsAward.getLandAward().getTitleNo(), result.getTitleNumber());
    assertEquals(
        ebsAward.getLandAward().getPropertyAddress().getAddressLine1(), result.getAddressLine1());
    assertEquals(
        ebsAward.getLandAward().getPropertyAddress().getAddressLine2(), result.getAddressLine2());
    assertEquals(
        ebsAward.getLandAward().getPropertyAddress().getAddressLine3(), result.getAddressLine3());
    assertEquals(ebsAward.getLandAward().getDisputedPercentage(), result.getDisputedPercentage());
    assertEquals(ebsAward.getLandAward().getAwardedPercentage(), result.getAwardedPercentage());
    assertEquals(ebsAward.getLandAward().getMortgageAmountDue(), result.getMortgageAmountDue());
    assertEquals(ebsAward.getLandAward().getAwardedBy(), result.getAwardedBy());
    assertEquals(ebsAward.getLandAward().getRecovery(), result.getRecovery());
    assertEquals(ebsAward.getLandAward().getNoRecoveryDetails(), result.getNoRecoveryDetails());
    assertEquals(
        ebsAward.getLandAward().getStatChargeExemptReason(),
        result.getStatutoryChargeExemptReason());
    assertEquals(
        ebsAward.getLandAward().getLandChargeRegistration(), result.getLandChargeRegistration());
    assertEquals(ebsAward.getLandAward().getRegistrationRef(), result.getRegistrationReference());

    // afterMapping
    assertTrue(result.getRecoveryOfAwardTimeRelated());
    assertEquals(AWARD_TYPE_LAND, result.getTimeRecovery().getAwardType());
    assertEquals(
        result.getValuationAmount().subtract(result.getMortgageAmountDue()), result.getEquity());

    // afterMapping (baseAward)
    result
        .getLiableParties()
        .forEach(liableParty -> assertEquals(AWARD_TYPE_LAND, liableParty.getAwardType()));
  }

  @Test
  void testToOtherAssetAward() {
    Award ebsAward = buildOtherAssetAward();

    OtherAssetAwardDetail result = applicationMapper.toOtherAssetAward(ebsAward);

    assertNotNull(result);
    assertEquals(ebsAward.getAwardId(), result.getEbsId());
    assertEquals(toDate(ebsAward.getOtherAsset().getOrderDate()), result.getDateOfOrder());
    assertEquals(AWARD_TYPE_OTHER_ASSET, result.getAwardType());
    assertEquals(ebsAward.getAwardType(), result.getAwardCode());
    assertEquals(ebsAward.getOtherAsset().getValuation().getAmount(), result.getValuationAmount());
    assertEquals(
        ebsAward.getOtherAsset().getValuation().getCriteria(), result.getValuationCriteria());
    assertEquals(
        toDate(ebsAward.getOtherAsset().getValuation().getDate()), result.getValuationDate());
    assertNotNull(result.getTimeRecovery());
    assertNotNull(result.getLiableParties());
    assertEquals(ebsAward.getOtherAsset().getHeldBy().size(), result.getLiableParties().size());

    // Like for like mappings
    assertEquals(ebsAward.getDeleteAllowed(), result.getDeleteAllowed());
    assertEquals(ebsAward.getUpdateAllowed(), result.getUpdateAllowed());
    assertEquals(ebsAward.getOtherAsset().getDescription(), result.getDescription());
    assertEquals(ebsAward.getOtherAsset().getAwardedBy(), result.getAwardedBy());
    assertEquals(ebsAward.getOtherAsset().getAwardedAmount(), result.getAwardedAmount());
    assertEquals(ebsAward.getOtherAsset().getAwardedPercentage(), result.getAwardedPercentage());
    assertEquals(ebsAward.getOtherAsset().getRecoveredAmount(), result.getRecoveredAmount());
    assertEquals(
        ebsAward.getOtherAsset().getRecoveredPercentage(), result.getRecoveredPercentage());
    assertEquals(ebsAward.getOtherAsset().getDisputedAmount(), result.getDisputedAmount());
    assertEquals(ebsAward.getOtherAsset().getDisputedPercentage(), result.getDisputedPercentage());
    assertEquals(ebsAward.getOtherAsset().getRecovery(), result.getRecovery());
    assertEquals(ebsAward.getOtherAsset().getNoRecoveryDetails(), result.getNoRecoveryDetails());
    assertEquals(
        ebsAward.getOtherAsset().getStatChargeExemptReason(),
        result.getStatutoryChargeExemptReason());

    // afterMapping
    assertTrue(result.getRecoveryOfAwardTimeRelated());
    assertEquals(AWARD_TYPE_OTHER_ASSET, result.getTimeRecovery().getAwardType());

    // afterMapping (baseAward)
    result
        .getLiableParties()
        .forEach(liableParty -> assertEquals(AWARD_TYPE_OTHER_ASSET, liableParty.getAwardType()));
  }

  @Test
  public void testToApplicationDetails() {
    List<BaseApplicationDetail> baseApplicationList =
        List.of(buildBaseApplication(1), buildBaseApplication(2));

    ApplicationDetails result =
        applicationMapper.toApplicationDetails(new PageImpl<>(baseApplicationList));

    assertNotNull(result);
    assertEquals(2, result.getSize());
    assertNotNull(result.getContent());
    assertEquals(baseApplicationList, result.getContent());
  }

  @Test
  public void testToBaseApplication() {
    CaseSummary ebsCaseSummary = buildCaseSummary();

    BaseApplicationDetail result = applicationMapper.toBaseApplication(ebsCaseSummary);

    assertNotNull(result);
    assertEquals(ebsCaseSummary.getCaseReferenceNumber(), result.getCaseReferenceNumber());
    assertEquals(ebsCaseSummary.getCaseStatusDisplay(), result.getStatus().getDisplayValue());
    assertEquals(ebsCaseSummary.getCategoryOfLaw(), result.getCategoryOfLaw().getDisplayValue());
    assertEquals(
        ebsCaseSummary.getClient().getClientReferenceNumber(), result.getClient().getReference());
    assertEquals(ebsCaseSummary.getClient().getFirstName(), result.getClient().getFirstName());
    assertEquals(ebsCaseSummary.getClient().getSurname(), result.getClient().getSurname());
    assertNotNull(result.getProviderDetails());
    assertEquals(
        ebsCaseSummary.getFeeEarnerName(),
        result.getProviderDetails().getFeeEarner().getDisplayValue());
    assertEquals(
        ebsCaseSummary.getProviderCaseReferenceNumber(),
        result.getProviderDetails().getProviderCaseReference());
  }

  private EbsApplicationMappingContext buildApplicationMappingContext(
      CaseDetail ebsCaseDetail, Boolean devolvedPowers, LocalDate devolvedPowersDate) {
    return EbsApplicationMappingContext.builder()
        .ebsCaseDetail(ebsCaseDetail)
        .applicationType(
            new CommonLookupValueDetail().code("apptypecode").description("apptypedesc"))
        .amendmentProceedingsInEbs(
            Collections.singletonList(
                buildProceedingMappingContext(
                    ebsCaseDetail.getApplicationDetails().getProceedings().getFirst())))
        .caseOutcome(buildCaseOutcomeMappingContext(ebsCaseDetail))
        .caseWithOnlyDraftProceedings(Boolean.TRUE)
        .certificate(
            new CommonLookupValueDetail().code("certcode").description("certificate descr"))
        .currentProviderBilledAmount(BigDecimal.ONE)
        .devolvedPowers(Pair.of(devolvedPowers, devolvedPowersDate))
        .feeEarnerContact(new ContactDetail().id(100).name("feeEarnerName"))
        .supervisorContact(new ContactDetail().id(101).name("supName"))
        .meansAssessment(ebsCaseDetail.getApplicationDetails().getMeansAssessments().getFirst())
        .meritsAssessment(ebsCaseDetail.getApplicationDetails().getMeritsAssessments().getFirst())
        .priorAuthorities(
            Collections.singletonList(
                buildPriorAuthorityMappingContext(ebsCaseDetail.getPriorAuthorities().getFirst())))
        .proceedings(
            Collections.singletonList(
                buildProceedingMappingContext(
                    ebsCaseDetail.getApplicationDetails().getProceedings().getFirst())))
        .providerDetail(new ProviderDetail().id(1).name("provname"))
        .providerOffice(new OfficeDetail().id(1000).name("offName"))
        .build();
  }

  private EbsProceedingMappingContext buildProceedingMappingContext(Proceeding ebsProceeding) {
    return EbsProceedingMappingContext.builder()
        .ebsProceeding(ebsProceeding)
        .clientInvolvement(
            new CommonLookupValueDetail().code("clientInv").description("clientDesc"))
        .proceedingCostLimitation(BigDecimal.TEN)
        .proceedingStatusLookup(
            new CommonLookupValueDetail().code("procStatCode").description("procStatDesc"))
        .levelOfService(new CommonLookupValueDetail().code("losCode").description("losDescr"))
        .proceedingLookup(
            new uk.gov.laa.ccms.data.model.ProceedingDetail()
                .code("procCode")
                .name("procName")
                .larScope("procLarScope"))
        .scopeLimitations(
            Collections.singletonList(
                Pair.of(
                    buildScopeLimitation(""),
                    new CommonLookupValueDetail()
                        .code("scopeLimitCode")
                        .description("scopeLimitDescr"))))
        .outcomeResultLookup(
            new OutcomeResultLookupValueDetail()
                .outcomeResult("or")
                .outcomeResultDescription("orDesc"))
        .courtLookup(new CommonLookupValueDetail().code("crt").description("crtDescr"))
        .stageEndLookup(new StageEndLookupValueDetail().stageEnd("se").description("seDescr"))
        .matterType(new CommonLookupValueDetail().code("mat").description("matDescr"))
        .build();
  }

  private EbsCaseOutcomeMappingContext buildCaseOutcomeMappingContext(CaseDetail ebsCase) {
    return EbsCaseOutcomeMappingContext.builder()
        .ebsCase(ebsCase)
        .costAwards(Collections.singletonList(ebsCase.getAwards().getFirst()))
        .financialAwards(Collections.singletonList(ebsCase.getAwards().get(1)))
        .landAwards(Collections.singletonList(ebsCase.getAwards().get(2)))
        .otherAssetAwards(Collections.singletonList(ebsCase.getAwards().get(3)))
        .proceedingOutcomes(
            Collections.singletonList(
                buildProceedingMappingContext(
                    ebsCase.getApplicationDetails().getProceedings().getFirst())))
        .build();
  }

  private EbsPriorAuthorityMappingContext buildPriorAuthorityMappingContext(
      PriorAuthority ebsPriorAuthority) {
    return EbsPriorAuthorityMappingContext.builder()
        .ebsPriorAuthority(ebsPriorAuthority)
        .priorAuthorityTypeLookup(buildPriorAuthorityTypeDetail("dataType"))
        .items(
            Collections.singletonList(
                Pair.of(
                    buildPriorAuthorityDetail("dataType"),
                    new CommonLookupValueDetail()
                        .code("priorAuthCode")
                        .description("priorAuthDesc"))))
        .build();
  }

  @Test
  @DisplayName("Test toCaseDetail with valid CaseMappingContext")
  void testToEbsCaseDetail_Valid() {
    final ApplicationDetail applicationDetail = buildApplicationDetail(1, false, new Date());
    final LinkedCaseDetail linkedCaseDetail =
        new LinkedCaseDetail().lscCaseReference("LC123").relationToCase("Related");
    final PriorAuthorityDetail priorAuthorityDetail =
        new PriorAuthorityDetail().summary("Test Prior Authority");
    final BaseEvidenceDocumentDetail caseDocDetail =
        new BaseEvidenceDocumentDetail().registeredDocumentId("DOC123");
    applicationDetail.setLinkedCases(Collections.singletonList(linkedCaseDetail));
    applicationDetail.setPriorAuthorities(Collections.singletonList(priorAuthorityDetail));

    final CaseMappingContext context =
        CaseMappingContext.builder()
            .tdsApplication(applicationDetail)
            .caseDocs(Collections.singletonList(caseDocDetail))
            .build();

    final CaseDetail result = applicationMapper.toEbsCaseDetail(context);

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
  void testToEbsCaseDetail_Null() {
    assertNull(applicationMapper.toEbsCaseDetail(null));
  }

  @Test
  @DisplayName("Test toEbsCaseDoc with valid BaseEvidenceDocumentDetail")
  void testToEbsCaseDoc_Valid() {
    final BaseEvidenceDocumentDetail evidenceDocumentDetail =
        new BaseEvidenceDocumentDetail()
            .documentType(new StringDisplayValue().displayValue("Test Document"))
            .registeredDocumentId("12345");

    final CaseDoc result = applicationMapper.toEbsCaseDoc(evidenceDocumentDetail);

    assertNotNull(result);
    assertEquals("12345", result.getCcmsDocumentId());
    assertEquals("Test Document", result.getDocumentSubject());
  }

  @Test
  @DisplayName("Test toEbsCaseDoc with null BaseEvidenceDocumentDetail")
  void testToEbsCaseDoc_Null() {
    assertNull(applicationMapper.toEbsCaseDoc(null));
  }

  @Test
  @DisplayName("Test toEbsPriorAuthority with valid PriorAuthorityDetail")
  void testToEbsPriorAuthority_Valid() {
    final PriorAuthorityDetail priorAuthorityDetail =
        new PriorAuthorityDetail()
            .summary("Test Summary")
            .justification("Test Justification")
            .amountRequested(new BigDecimal("1000.00"))
            .status("Approved")
            .items(
                Collections.singletonList(
                    new ReferenceDataItemDetail()
                        .code(
                            new StringDisplayValue().id("PA123").displayValue("Test Attribute"))));

    final PriorAuthority result = applicationMapper.toEbsPriorAuthority(priorAuthorityDetail);

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
  @DisplayName("Test toEbsPriorAuthority with null PriorAuthorityDetail")
  void testToEbsPriorAuthority_Null() {
    assertNull(applicationMapper.toEbsPriorAuthority(null));
  }

  @Test
  @DisplayName("Test toEbsPriorAuthorityAttribute with valid ReferenceDataItemDetail")
  void testToEbsPriorAuthorityAttribute_Valid() {
    final ReferenceDataItemDetail referenceDataItemDetail =
        new ReferenceDataItemDetail()
            .code(new StringDisplayValue().id("AttributeName"))
            .value(new StringDisplayValue().id("AttributeValue"));

    final PriorAuthorityAttribute result =
        applicationMapper.toEbsPriorAuthorityAttribute(referenceDataItemDetail);

    assertNotNull(result);
    assertEquals("AttributeName", result.getName());
    assertEquals("AttributeValue", result.getValue());
  }

  @Test
  @DisplayName("Test toEbsPriorAuthorityAttribute with null ReferenceDataItemDetail")
  void testToEbsPriorAuthorityAttribute_Null() {
    assertNull(applicationMapper.toEbsPriorAuthorityAttribute(null));
  }

  @Test
  @DisplayName("Test toEbsLinkedCase with valid LinkedCaseDetail")
  void testToEbsLinkedCase_Valid() {

    final LinkedCaseDetail linkedCaseDetail =
        new LinkedCaseDetail().lscCaseReference("LSC123").relationToCase("Related Case");

    final LinkedCase result = applicationMapper.toEbsLinkedCase(linkedCaseDetail);

    assertNotNull(result);
    assertEquals("LSC123", result.getCaseReferenceNumber());
    assertEquals("Related Case", result.getLinkType());
  }

  @Test
  @DisplayName("Test toEbsLinkedCase with null LinkedCaseDetail")
  void testToEbsLinkedCase_Null() {
    assertNull(applicationMapper.toEbsLinkedCase(null));
  }

  @Test
  @DisplayName("Test toSubmittedApplicationDetails with valid CaseMappingContext")
  void testToSubmittedApplicationDetails_Valid() {
    final ApplicationDetail applicationDetail = new ApplicationDetail();
    applicationDetail.setApplicationType(new ApplicationType().id("type123"));
    applicationDetail.setCorrespondenceAddress(new AddressDetail().preferredAddress("preferred"));
    applicationDetail.setClient(new ClientDetail().reference("client123"));
    applicationDetail.setProviderDetails(
        new ApplicationProviderDetails().providerCaseReference("CASE123"));
    applicationDetail.setCategoryOfLaw(new StringDisplayValue().id("category123"));
    applicationDetail.setCosts(
        new CostStructureDetail().requestedCostLimitation(new BigDecimal("1000.00")));

    final CaseMappingContext context =
        CaseMappingContext.builder()
            .tdsApplication(applicationDetail)
            .meansAssessment(new AssessmentDetail())
            .meritsAssessment(new AssessmentDetail())
            .caseDocs(Collections.singletonList(new BaseEvidenceDocumentDetail()))
            .build();

    final SubmittedApplicationDetails result =
        applicationMapper.toSubmittedApplicationDetails(context);

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
  @DisplayName("Test toEbsProviderDetail with valid ApplicationProviderDetails")
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

    final ProviderDetails result = applicationMapper.toEbsProviderDetail(providerDetails);

    assertNotNull(result);
    assertEquals("user123", result.getContactUserId().getLoginId());
    assertEquals("CASE123", result.getProviderCaseReferenceNumber());
    assertEquals("100", result.getProviderFirmId());
    assertEquals("200", result.getProviderOfficeId());
    assertEquals("300", result.getSupervisorContactId());
    assertEquals("400", result.getFeeEarnerContactId());
  }

  @Test
  @DisplayName("Test toEbsProviderDetail with null ApplicationProviderDetails")
  void testToEbsProviderDetail_Null() {
    assertNull(applicationMapper.toEbsProviderDetail(null));
  }

  @ParameterizedTest
  @CsvSource({
    "'LAW123', 'Test Law Description', '1000', '500', '1000'", // Case with requested amount
    "'LAW124', 'Another Law Description', '', '500', '500'", // Case with default amount (requested
    // amount is null)
    "'LAW125', 'Third Law Description', '', '', ''" // Case with no costs (null amounts)
  })
  @DisplayName("Parameterized Test toEbsCategoryOfLaw with different ApplicationDetail inputs")
  void testToEbsCategoryOfLaw_Parameterized(
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
    costStructureDetail.setRequestedCostLimitation(
        requestedCostStr.isEmpty() ? null : new BigDecimal(requestedCostStr));
    costStructureDetail.setDefaultCostLimitation(
        defaultCostStr.isEmpty() ? null : new BigDecimal(defaultCostStr));

    final ApplicationDetail applicationDetail = new ApplicationDetail();
    applicationDetail.setCategoryOfLaw(categoryOfLaw);
    applicationDetail.setCosts(costStructureDetail);

    // Call the method under test
    final CategoryOfLaw result = applicationMapper.toEbsCategoryOfLaw(applicationDetail);

    // Assertions
    if (expectedCostStr.isEmpty()) {
      assertNull(
          result.getRequestedAmount()); // No costs provided, so requested amount should be null
    } else {
      assertNotNull(result);
      assertEquals(lawCode, result.getCategoryOfLawCode());
      assertEquals(lawDescription, result.getCategoryOfLawDescription());
      assertEquals(new BigDecimal(expectedCostStr), result.getRequestedAmount());
    }
  }

  @Test
  @DisplayName("Test toEbsCategoryOfLaw with null ApplicationDetail")
  void testToEbsCategoryOfLaw_Null() {
    assertNull(applicationMapper.toEbsCategoryOfLaw(null));
  }

  @Test
  @DisplayName("Test toEbsAddressDetail with valid AddressDetail")
  void testToEbsAddressDetail_Valid() {
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

    final uk.gov.laa.ccms.data.model.AddressDetail result =
        applicationMapper.toEbsAddressDetail(addressDetail);

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
  @DisplayName("Test toEbsAddressDetail with null AddressDetail")
  void testToEbsAddressDetail_Null() {
    assertNull(applicationMapper.toEbsAddressDetail(null));
  }

  @Test
  @DisplayName("Test toEbsProceedingDetail with valid ProceedingDetail")
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

    final Proceeding result = applicationMapper.toEbsProceedingDetail(proceedingDetail);

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
  @DisplayName("Test toEbsProceedingDetail with null ProceedingDetail")
  void testToEbsProceedingDetail_Null() {
    assertNull(applicationMapper.toEbsProceedingDetail(null));
  }

  @Test
  @DisplayName("Test toEbsScopeLimitation with valid ScopeLimitationDetail")
  void testToEbsScopeLimitation_Valid() {
    final ScopeLimitationDetail scopeLimitationDetail = new ScopeLimitationDetail();
    scopeLimitationDetail.setScopeLimitation(
        new StringDisplayValue().id("scopeId").displayValue("Scope Description"));
    scopeLimitationDetail.setScopeLimitationWording("Scope Limitation Wording");
    scopeLimitationDetail.setDelegatedFuncApplyInd(new BooleanDisplayValue().flag(true));

    final ScopeLimitation result = applicationMapper.toEbsScopeLimitation(scopeLimitationDetail);

    assertNotNull(result);
    assertEquals("scopeId", result.getScopeLimitation());
    assertEquals("Scope Limitation Wording", result.getScopeLimitationWording());
    assertTrue(result.getDelegatedFunctionsApply());
  }

  @Test
  @DisplayName("Test toEbsScopeLimitation with null ScopeLimitationDetail")
  void testToEbsScopeLimitation_Null() {
    assertNull(applicationMapper.toEbsScopeLimitation(null));
  }

  @Test
  @DisplayName("Test toEbsOtherParty with individual type OpponentDetail")
  void testToEbsOtherParty_Individual() {
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
  @DisplayName("Test toEbsOtherParty with organisation type OpponentDetail")
  void testToEbsOtherParty_Organisation() {
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
  @DisplayName("Test toEbsOtherParty with null or unknown type OpponentDetail")
  void testToEbsOtherParty_NullOrUnknownType() {
    final OpponentDetail opponentDetail = new OpponentDetail();
    opponentDetail.setType(null);

    final OtherParty result = applicationMapper.toEbsOtherParty(opponentDetail);

    assertNotNull(result);
    assertNull(result.getPerson());
    assertNull(result.getOrganisation());
  }

  @Test
  @DisplayName("Test toEbsOtherParty with null OpponentDetail")
  void testToEbsOtherParty_Null() {
    assertNull(applicationMapper.toEbsOtherParty(null));
  }

  @ParameterizedTest
  @DisplayName("Test toEbsPerson with valid OpponentDetail inputs")
  @CsvSource({
    "'TestClientRel', 'TestCaseRel', '123456', 'true', 'true', 'John Doe', 'Test Employer', 'Employer Address', '1000', '500', '1990-01-01', 'Employed', 'Cert123', 'Monthly', '2023-10-12', 'OtherInfo'",
    "'ClientRel2', 'CaseRel2', '987654', 'false', 'false', 'Jane Smith', 'Another Employer', 'Another Address', '2000', '1000', '1985-05-05', 'Unemployed', 'Cert456', 'Weekly', '2023-08-20', 'Info2'"
  })
  void testToEbsPerson_Valid(
      final String relationToClient,
      final String relationToCase,
      final String niNumber,
      final boolean legalAided,
      final boolean courtOrderedMeansAssesment,
      final String contactName,
      final String employerName,
      final String employerAddress,
      final String assessedIncome,
      final String assessedAssets,
      final String dateOfBirth,
      final String employmentStatus,
      final String certificateNumber,
      final String assessedIncomeFrequency,
      final String assessmentDate,
      final String otherInformation)
      throws ParseException {

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
  @DisplayName("Test toEbsPerson with null OpponentDetail")
  void testToEbsPerson_NullOpponentDetail() {
    assertNull(applicationMapper.toEbsPerson(null));
  }

  @ParameterizedTest
  @DisplayName("Test toEbsOrganisation with valid OpponentDetail inputs")
  @CsvSource({
    "'ClientRelation', 'CaseRelation', 'OrgName', 'OrgType', 'ContactName', 'true', 'OtherInfo'",
    "'ClientRelation2', 'CaseRelation2', 'OrgName2', 'OrgType2', 'ContactName2', 'false', 'OtherInfo2'"
  })
  void testToEbsOrganisation_Valid(
      final String relationToClient,
      final String relationToCase,
      final String organisationName,
      final String organisationType,
      final String contactName,
      final boolean currentlyTrading,
      final String otherInformation) {
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
  @DisplayName("Test toEbsOrganisation with null OpponentDetail")
  void testToEbsOrganisation_NullOpponentDetail() {
    assertNull(applicationMapper.toEbsOrganisation(null));
  }

  @ParameterizedTest
  @DisplayName(
      "Test toOpaAttribute with different AssessmentAttributeDetail inputs, with different user defined indicator values")
  @CsvSource({
    "'TestName', 'TestType', 'TestValue', 'nonIntermediate', true",
    "'SA_TestName', 'TestType', 'TestValue', 'intermediate', false",
    "'TestName', 'TestType', 'TestValue', 'intermediate', false",
    "'SA_TestName', 'TestType', 'TestValue', 'nonIntermediate', false",
    "'TestName', 'TestType', 'TestValue', 'nonIntermediate', true"
  })
  void testToOpaAttribute_Valid_userDefinedIndicator(
      final String name,
      final String type,
      final String value,
      final String inferencingType,
      final boolean expectedUserDefinedInd) {
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
  @DisplayName("Test toEbsRecordHistory with valid CaseMappingContext")
  void testToEbsRecordHistory_ValidContext() {
    final Date testDate = new Date();
    final ApplicationDetail application = buildApplicationDetail(1, false, testDate);
    application.getAuditTrail().created(testDate);
    application.getAuditTrail().lastSaved(testDate);

    final UserDetail user = buildUserDetail();

    final CaseMappingContext caseMappingContext =
        CaseMappingContext.builder().tdsApplication(application).user(user).build();

    final RecordHistory result = applicationMapper.toEbsRecordHistory(caseMappingContext);

    assertNotNull(result);
    assertEquals("testLoginId", result.getLastUpdatedBy().getLoginId());
    assertEquals(toLocalDateTime(testDate), result.getDateLastUpdated());
    assertEquals(toLocalDateTime(testDate), result.getDateCreated());
  }

  @Test
  @DisplayName("Test toEbsRecordHistory with null context")
  void testToEbsRecordHistory_NullContext() {
    assertNull(applicationMapper.toEbsRecordHistory(null));
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
