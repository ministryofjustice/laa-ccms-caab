package uk.gov.laa.ccms.caab.builders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.REFERENCE_DATA_ITEM_TYPE_LOV;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CLIENT_INVOLVEMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_LEVEL_OF_SERVICE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MATTER_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_PROCEEDING_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_SCOPE_LIMITATIONS;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildAwardTypeLookupDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildCaseDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildPriorAuthority;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildPriorAuthorityTypeDetails;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildProceedingDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildProviderDetail;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.context.EbsApplicationMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.EbsCaseOutcomeMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.EbsPriorAuthorityMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.EbsProceedingMappingContext;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.CaseDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.PriorAuthority;
import uk.gov.laa.ccms.data.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.Proceeding;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetails;
import uk.gov.laa.ccms.data.model.StageEndLookupDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;

@ExtendWith(MockitoExtension.class)
@DisplayName("Ebs Application Mapping Context Builder Test")
class EbsApplicationMappingContextBuilderTest {

  @Mock
  private ProviderService providerService;
  @Mock
  private LookupService lookupService;
  @Mock
  private EbsApiClient ebsApiClient;
  
  private EbsApplicationMappingContextBuilder applicationService;

  @BeforeEach
  void beforeEach(){
    applicationService = new EbsApplicationMappingContextBuilder(providerService, lookupService, ebsApiClient);
  }

  @Test
  void testBuildEbsCaseOutcomeMappingContext() {
    final CaseDetail ebsCase = buildCaseDetail("anytype");
    final List<EbsProceedingMappingContext> EbsProceedingMappingContexts = Collections.singletonList(
        EbsProceedingMappingContext.builder().build());

    final AwardTypeLookupDetail awardTypes = buildAwardTypeLookupDetail(ebsCase);

    when(lookupService.getAwardTypes()).thenReturn(Mono.just(awardTypes));

    final EbsCaseOutcomeMappingContext result = applicationService.buildCaseOutcomeMappingContext(
        ebsCase,
        EbsProceedingMappingContexts);

    assertNotNull(result);
    assertEquals(ebsCase, result.getEbsCase());
    assertNotNull(result.getCostAwards());
    assertNotNull(result.getFinancialAwards());
    assertNotNull(result.getLandAwards());
    assertNotNull(result.getOtherAssetAwards());
    assertEquals(1, result.getCostAwards().size());
    assertEquals(ebsCase.getAwards().get(0), result.getCostAwards().get(0));
    assertEquals(1, result.getFinancialAwards().size());
    assertEquals(ebsCase.getAwards().get(1), result.getFinancialAwards().get(0));
    assertEquals(1, result.getLandAwards().size());
    assertEquals(ebsCase.getAwards().get(2), result.getLandAwards().get(0));
    assertEquals(1, result.getOtherAssetAwards().size());
    assertEquals(ebsCase.getAwards().get(3), result.getOtherAssetAwards().get(0));
    assertEquals(EbsProceedingMappingContexts, result.getProceedingOutcomes());
  }

  @Test
  void testBuildEbsCaseOutcomeMappingContext_NoAwardTypes() {
    CaseDetail ebsCase = buildCaseDetail("anytype");
    List<EbsProceedingMappingContext> EbsProceedingMappingContexts = Collections.singletonList(
        EbsProceedingMappingContext.builder().build());

    AwardTypeLookupDetail awardTypes = new AwardTypeLookupDetail();

    when(lookupService.getAwardTypes()).thenReturn(Mono.just(awardTypes));

    assertThrows(CaabApplicationException.class, () ->
        applicationService.buildCaseOutcomeMappingContext(ebsCase, EbsProceedingMappingContexts));
  }

  @Test
  void testBuildEbsCaseOutcomeMappingContext_UnknownAwardType() {
    CaseDetail ebsCase = buildCaseDetail("anytype");
    List<EbsProceedingMappingContext> EbsProceedingMappingContexts = Collections.singletonList(
        EbsProceedingMappingContext.builder().build());

    AwardTypeLookupDetail awardTypes = buildAwardTypeLookupDetail(ebsCase);
    // Drop out one of the award types
    awardTypes.getContent().remove(awardTypes.getContent().size() - 1);

    when(lookupService.getAwardTypes()).thenReturn(Mono.just(awardTypes));

    Exception e = assertThrows(CaabApplicationException.class, () ->
        applicationService.buildCaseOutcomeMappingContext(ebsCase, EbsProceedingMappingContexts));

    assertEquals(String.format("Failed to find AwardType with code: %s",
        ebsCase.getAwards().get(3).getAwardType()), e.getMessage());
  }

  @Test
  void testBuildEbsPriorAuthorityMappingContext_LovLookup() {
    PriorAuthority ebsPriorAuthority = buildPriorAuthority();

    PriorAuthorityTypeDetails priorAuthorityTypeDetails =
        buildPriorAuthorityTypeDetails(REFERENCE_DATA_ITEM_TYPE_LOV);
    PriorAuthorityTypeDetail priorAuthorityTypeDetail = priorAuthorityTypeDetails.getContent().get(0);
    PriorAuthorityDetail priorAuthoritiesItem = priorAuthorityTypeDetail.getPriorAuthorities().get(0);

    when(lookupService.getPriorAuthorityType(ebsPriorAuthority.getPriorAuthorityType()))
        .thenReturn(Mono.just(Optional.of(priorAuthorityTypeDetail)));

    CommonLookupValueDetail lookup = new CommonLookupValueDetail()
        .code("thecode")
        .description("thedescription");

    when(lookupService.getCommonValue(priorAuthoritiesItem.getLovCode(),
        ebsPriorAuthority.getDetails().get(0).getValue()))
        .thenReturn(Mono.just(Optional.of(lookup)));

    EbsPriorAuthorityMappingContext result = applicationService.buildPriorAuthorityMappingContext(
        ebsPriorAuthority);

    verify(lookupService).getCommonValue(priorAuthoritiesItem.getLovCode(),
        ebsPriorAuthority.getDetails().get(0).getValue());

    assertNotNull(result);
    assertEquals(ebsPriorAuthority, result.getEbsPriorAuthority());
    assertNotNull(result.getPriorAuthorityTypeLookup());
    assertEquals(priorAuthorityTypeDetails.getContent().get(0),
        result.getPriorAuthorityTypeLookup());

    assertNotNull(result.getItems());
    assertEquals(1, result.getItems().size());
    assertEquals(priorAuthoritiesItem, result.getItems().get(0).getKey());
    assertEquals(ebsPriorAuthority.getDetails().get(0).getValue(),
        result.getItems().get(0).getValue().getCode());
    assertEquals(lookup.getDescription(),
        result.getItems().get(0).getValue().getDescription());
  }

  @Test
  void testBuildEbsPriorAuthorityMappingContext_NoLovLookup() {
    PriorAuthority ebsPriorAuthority = buildPriorAuthority();

    PriorAuthorityTypeDetails priorAuthorityTypeDetails =
        buildPriorAuthorityTypeDetails("otherDataType");
    PriorAuthorityTypeDetail priorAuthorityTypeDetail = priorAuthorityTypeDetails.getContent().get(0);

    when(lookupService.getPriorAuthorityType(ebsPriorAuthority.getPriorAuthorityType()))
        .thenReturn(Mono.just(Optional.of(priorAuthorityTypeDetail)));

    EbsPriorAuthorityMappingContext result = applicationService.buildPriorAuthorityMappingContext(
        ebsPriorAuthority);

    assertNotNull(result);
    assertEquals(ebsPriorAuthority, result.getEbsPriorAuthority());
    assertNotNull(result.getPriorAuthorityTypeLookup());
    assertEquals(priorAuthorityTypeDetails.getContent().get(0),
        result.getPriorAuthorityTypeLookup());

    assertNotNull(result.getItems());
    assertEquals(1, result.getItems().size());
    assertEquals(priorAuthorityTypeDetails.getContent().get(0).getPriorAuthorities().get(0),
        result.getItems().get(0).getKey());
    assertEquals(ebsPriorAuthority.getDetails().get(0).getValue(),
        result.getItems().get(0).getValue().getCode());
    assertEquals(ebsPriorAuthority.getDetails().get(0).getValue(),
        result.getItems().get(0).getValue().getDescription());
  }

  @Test
  void testBuildEbsPriorAuthorityMappingContext_UnknownPriorAuthType() {
    PriorAuthority ebsPriorAuthority = buildPriorAuthority();

    when(lookupService.getPriorAuthorityType(ebsPriorAuthority.getPriorAuthorityType()))
        .thenReturn(Mono.empty());

    Exception e = assertThrows(CaabApplicationException.class,
        () -> applicationService.buildPriorAuthorityMappingContext(
            ebsPriorAuthority));

    assertEquals(String.format("Failed to find PriorAuthorityType with code: %s",
        ebsPriorAuthority.getPriorAuthorityType()), e.getMessage());
  }

  @Test
  void testBuildEbsPriorAuthorityMappingContext_UnknownLookup() {
    PriorAuthority ebsPriorAuthority = buildPriorAuthority();

    PriorAuthorityDetail priorAuthoritiesItem = new PriorAuthorityDetail()
        .code(ebsPriorAuthority.getDetails().get(0).getName())
        .description("priorAuthItemDesc")
        .dataType(REFERENCE_DATA_ITEM_TYPE_LOV);

    PriorAuthorityTypeDetail priorAuthorityTypeDetail = new PriorAuthorityTypeDetail()
        .code(ebsPriorAuthority.getPriorAuthorityType())
        .description("priorAuthDesc")
        .addPriorAuthoritiesItem(priorAuthoritiesItem);

    when(lookupService.getPriorAuthorityType(ebsPriorAuthority.getPriorAuthorityType()))
        .thenReturn(Mono.just(Optional.of(priorAuthorityTypeDetail)));

    when(lookupService.getCommonValue(priorAuthoritiesItem.getLovCode(),
        ebsPriorAuthority.getDetails().get(0).getValue()))
        .thenReturn(Mono.empty());

    Exception e = assertThrows(CaabApplicationException.class,
        () -> applicationService.buildPriorAuthorityMappingContext(
            ebsPriorAuthority));

    assertEquals(String.format("Failed to find common value with code: %s",
        ebsPriorAuthority.getDetails().get(0).getValue()), e.getMessage());
  }

  @Test
  void testBuildEbsProceedingMappingContext_Emergency() {
    Proceeding ebsProceeding =
        buildProceedingDetail(STATUS_DRAFT);
    CaseDetail ebsCase = buildCaseDetail(APP_TYPE_EMERGENCY);

    // Mock out the remaining lookups
    uk.gov.laa.ccms.data.model.ProceedingDetail proceedingLookup =
        new uk.gov.laa.ccms.data.model.ProceedingDetail();
    when(ebsApiClient.getProceeding(ebsProceeding.getProceedingType()))
        .thenReturn(Mono.just(proceedingLookup));

    CommonLookupValueDetail proceedingStatusLookup =
        new CommonLookupValueDetail();
    when(lookupService.getCommonValue(COMMON_VALUE_PROCEEDING_STATUS,  ebsProceeding.getStatus()))
        .thenReturn(Mono.just(Optional.of(proceedingStatusLookup)));

    CommonLookupValueDetail matterTypeLookup = new CommonLookupValueDetail()
        .code(ebsProceeding.getMatterType())
        .description("the matter type");
    when(lookupService.getCommonValue(COMMON_VALUE_MATTER_TYPES, ebsProceeding.getMatterType()))
        .thenReturn(Mono.just(Optional.of(matterTypeLookup)));

    CommonLookupValueDetail levelOfServiceLookup = new CommonLookupValueDetail()
        .code(ebsProceeding.getLevelOfService())
        .description("the los");
    when(lookupService.getCommonValue(COMMON_VALUE_LEVEL_OF_SERVICE,ebsProceeding.getLevelOfService()))
        .thenReturn(Mono.just(Optional.of(levelOfServiceLookup)));

    CommonLookupValueDetail clientInvLookup = new CommonLookupValueDetail()
        .code(ebsProceeding.getClientInvolvementType())
        .description("the involvement");
    when(lookupService.getCommonValue(COMMON_VALUE_CLIENT_INVOLVEMENT_TYPES, ebsProceeding.getClientInvolvementType()))
        .thenReturn(Mono.just(Optional.of(clientInvLookup)));

    CommonLookupValueDetail scopeLimitationOneLookup = new CommonLookupValueDetail()
        .code(ebsProceeding.getScopeLimitations().get(0).getScopeLimitation())
        .description("the limitation 1");
    when(lookupService.getCommonValue(COMMON_VALUE_SCOPE_LIMITATIONS, ebsProceeding.getScopeLimitations().get(0).getScopeLimitation()))
        .thenReturn(Mono.just(Optional.of(scopeLimitationOneLookup)));

    CommonLookupValueDetail scopeLimitationTwoLookup = new CommonLookupValueDetail()
        .code(ebsProceeding.getScopeLimitations().get(1).getScopeLimitation())
        .description("the limitation 2");
    when(lookupService.getCommonValue(COMMON_VALUE_SCOPE_LIMITATIONS, ebsProceeding.getScopeLimitations().get(1).getScopeLimitation()))
        .thenReturn(Mono.just(Optional.of(scopeLimitationTwoLookup)));

    CommonLookupValueDetail scopeLimitationThreeLookup = new CommonLookupValueDetail()
        .code(ebsProceeding.getScopeLimitations().get(2).getScopeLimitation())
        .description("the limitation 3");
    when(lookupService.getCommonValue(COMMON_VALUE_SCOPE_LIMITATIONS, ebsProceeding.getScopeLimitations().get(2).getScopeLimitation()))
        .thenReturn(Mono.just(Optional.of(scopeLimitationThreeLookup)));

    // Mock the call for scopeLimitationDetails, used to calculate the max cost limitation.
    ScopeLimitationDetails scopeLimitationOne = new ScopeLimitationDetails()
        .totalElements(1)
        .addContentItem(
            new uk.gov.laa.ccms.data.model.ScopeLimitationDetail()
                .costLimitation(BigDecimal.ZERO)
                .emergencyCostLimitation(BigDecimal.ZERO));

    ScopeLimitationDetails scopeLimitationTwo = new ScopeLimitationDetails()
        .totalElements(1)
        .addContentItem(
            new uk.gov.laa.ccms.data.model.ScopeLimitationDetail()
                .costLimitation(BigDecimal.TEN)
                .emergencyCostLimitation(BigDecimal.ZERO));

    ScopeLimitationDetails scopeLimitationThree = new ScopeLimitationDetails()
        .totalElements(1)
        .addContentItem(
            new uk.gov.laa.ccms.data.model.ScopeLimitationDetail()
                .costLimitation(BigDecimal.ZERO)
                .emergencyCostLimitation(BigDecimal.ONE));

    when(lookupService.getScopeLimitationDetails(any(
        uk.gov.laa.ccms.data.model.ScopeLimitationDetail.class)))
        .thenReturn(Mono.just(scopeLimitationOne))
        .thenReturn(Mono.just(scopeLimitationTwo))
        .thenReturn(Mono.just(scopeLimitationThree));

    BigDecimal expectedCostLimitation = BigDecimal.ONE;

    // Mock objects for ProceedingDetail Outcome
    OutcomeResultLookupDetail outcomeResults = new OutcomeResultLookupDetail()
        .totalElements(1)
        .addContentItem(new OutcomeResultLookupValueDetail());
    when(lookupService.getOutcomeResults(ebsProceeding.getProceedingType(),
        ebsProceeding.getOutcome().getResult()))
        .thenReturn(Mono.just(outcomeResults));

    StageEndLookupDetail stageEnds = new StageEndLookupDetail()
        .totalElements(1)
        .addContentItem(new StageEndLookupValueDetail());
    when(lookupService.getStageEnds(ebsProceeding.getProceedingType(),
        ebsProceeding.getOutcome().getStageEnd()))
        .thenReturn(Mono.just(stageEnds));

    CommonLookupDetail courts = new CommonLookupDetail()
        .totalElements(1)
        .addContentItem(new CommonLookupValueDetail());
    when(lookupService.getCourts(ebsProceeding.getOutcome().getCourtCode()))
        .thenReturn(Mono.just(courts));

    // Call the method under test
    EbsProceedingMappingContext result =
        applicationService.buildProceedingMappingContext(
            ebsProceeding,
            ebsCase);

//    StepVerifier.create(EbsProceedingMappingContextMono)
//        .expectNextMatches(result -> {
    assertNotNull(result);
    assertEquals(ebsProceeding, result.getEbsProceeding());
    assertEquals(clientInvLookup, result.getClientInvolvement());
    assertEquals(levelOfServiceLookup, result.getLevelOfService());
    assertEquals(matterTypeLookup, result.getMatterType());
    assertEquals(expectedCostLimitation, result.getProceedingCostLimitation());
    assertEquals(proceedingLookup, result.getProceedingLookup());
    assertEquals(proceedingStatusLookup, result.getProceedingStatusLookup());
    assertNotNull(result.getScopeLimitations());
    assertEquals(3, result.getScopeLimitations().size());
    assertEquals(ebsProceeding.getScopeLimitations().get(0),
        result.getScopeLimitations().get(0).getKey());
    assertEquals(scopeLimitationOneLookup, result.getScopeLimitations().get(0).getValue());
    assertEquals(ebsProceeding.getScopeLimitations().get(1),
        result.getScopeLimitations().get(1).getKey());
    assertEquals(scopeLimitationTwoLookup, result.getScopeLimitations().get(1).getValue());
    assertEquals(ebsProceeding.getScopeLimitations().get(2),
        result.getScopeLimitations().get(2).getKey());
    assertEquals(scopeLimitationThreeLookup, result.getScopeLimitations().get(2).getValue());

    // Check the proceeding outcome data
    assertEquals(outcomeResults.getContent().get(0), result.getOutcomeResultLookup());
    assertEquals(stageEnds.getContent().get(0), result.getStageEndLookup());
    assertEquals(courts.getContent().get(0), result.getCourtLookup());
//          return true; // Return true to indicate the match is successful
//        })
//        .verifyComplete();
  }

  @Test
  void testCalculateProceedingCostLimitation_NonEmergency() {
    Proceeding ebsProceeding =
        buildProceedingDetail(STATUS_DRAFT);
    CaseDetail ebsCase = buildCaseDetail(APP_TYPE_EXCEPTIONAL_CASE_FUNDING);

    // Mock the call for scopeLimitationDetails, used to calculate the max cost limitation.
    ScopeLimitationDetails scopeLimitationOne = new ScopeLimitationDetails()
        .totalElements(1)
        .addContentItem(
            new uk.gov.laa.ccms.data.model.ScopeLimitationDetail()
                .costLimitation(BigDecimal.ZERO)
                .emergencyCostLimitation(BigDecimal.ZERO));

    ScopeLimitationDetails scopeLimitationTwo = new ScopeLimitationDetails()
        .totalElements(1)
        .addContentItem(
            new uk.gov.laa.ccms.data.model.ScopeLimitationDetail()
                .costLimitation(BigDecimal.TEN)
                .emergencyCostLimitation(BigDecimal.ZERO));

    ScopeLimitationDetails scopeLimitationThree = new ScopeLimitationDetails()
        .totalElements(1)
        .addContentItem(
            new uk.gov.laa.ccms.data.model.ScopeLimitationDetail()
                .costLimitation(BigDecimal.ZERO)
                .emergencyCostLimitation(BigDecimal.ONE));

    when(lookupService.getScopeLimitationDetails(any(
        uk.gov.laa.ccms.data.model.ScopeLimitationDetail.class)))
        .thenReturn(Mono.just(scopeLimitationOne))
        .thenReturn(Mono.just(scopeLimitationTwo))
        .thenReturn(Mono.just(scopeLimitationThree));

    BigDecimal expectedCostLimitation = BigDecimal.TEN;

    // Call the method under test
    BigDecimal result =
        applicationService.calculateProceedingCostLimitation(ebsProceeding,
            ebsCase);

    verify(lookupService, times(3))
        .getScopeLimitationDetails(any(uk.gov.laa.ccms.data.model.ScopeLimitationDetail.class));

    assertEquals(expectedCostLimitation, result);
  }

  @Test
  void testAddProceedingOutcomeContext_NullOutcome() {
    final Proceeding ebsProceeding =
        buildProceedingDetail(STATUS_DRAFT);
    ebsProceeding.setOutcome(null);

    applicationService.addProceedingOutcomeContext(
        EbsProceedingMappingContext.builder(), ebsProceeding);

    verifyNoInteractions(lookupService);
  }

  @Test
  void testBuildEbsApplicationMappingContext_DevolvedPowersAllDraftProceedings() {
    final CaseDetail ebsCase = buildCaseDetail(APP_TYPE_EMERGENCY_DEVOLVED_POWERS);
    ebsCase.getApplicationDetails().getProceedings().forEach(
        proceedingDetail -> {
          // Clear the outcome and scopelimitations from all proceedings - this is tested elsewhere
          proceedingDetail.setOutcome(null);
          proceedingDetail.getScopeLimitations().clear();

          // Set status for all proceedings to DRAFT
          proceedingDetail.setStatus(STATUS_DRAFT);
        });
    ebsCase.getPriorAuthorities().clear(); // PriorAuthority mapping context tested elsewhere.
    ebsCase.getAwards().clear(); // Awards tested separately.

    CommonLookupValueDetail applicationTypeLookup = new CommonLookupValueDetail();

    uk.gov.laa.ccms.data.model.ProviderDetail providerDetail = buildProviderDetail(
        ebsCase.getApplicationDetails().getProviderDetails().getProviderOfficeId(),
        ebsCase.getApplicationDetails().getProviderDetails().getFeeEarnerContactId(),
        ebsCase.getApplicationDetails().getProviderDetails().getSupervisorContactId());

    when(lookupService.getCommonValue(COMMON_VALUE_APPLICATION_TYPE, ebsCase.getCertificateType()))
        .thenReturn(Mono.just(Optional.of(applicationTypeLookup)));

    when(lookupService.getCommonValue(COMMON_VALUE_APPLICATION_TYPE,
        ebsCase.getApplicationDetails().getApplicationAmendmentType()))
        .thenReturn(Mono.just(Optional.of(applicationTypeLookup)));

    when(providerService.getProvider(Integer.parseInt(
        ebsCase.getApplicationDetails().getProviderDetails().getProviderFirmId())))
        .thenReturn(Mono.just(providerDetail));

    CommonLookupValueDetail matterTypeLookup =
        new CommonLookupValueDetail().code("mat1").description("mat 1");
    when(lookupService.getCommonValue(eq(COMMON_VALUE_MATTER_TYPES), anyString())).thenReturn(Mono.just(Optional.of(matterTypeLookup)));

    CommonLookupValueDetail levelOfServiceLookup =
        new CommonLookupValueDetail().code("los1").description("los 1");
    when(lookupService.getCommonValue(eq(COMMON_VALUE_LEVEL_OF_SERVICE), anyString())).thenReturn(Mono.just(Optional.of(levelOfServiceLookup)));

    CommonLookupValueDetail clientInvolvementLookup =
        new CommonLookupValueDetail().code("ci1").description("ci 1");
    when(lookupService.getCommonValue(eq(COMMON_VALUE_CLIENT_INVOLVEMENT_TYPES), anyString())).thenReturn(Mono.just(Optional.of(clientInvolvementLookup)));

    // Also need to mock calls for the 'sub' mapping contexts, but we aren't testing their
    // content here.
    when(ebsApiClient.getProceeding(any(String.class)))
        .thenReturn(Mono.just(new uk.gov.laa.ccms.data.model.ProceedingDetail()));
    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROCEEDING_STATUS),  any(String.class)))
        .thenReturn(Mono.just(Optional.of(new CommonLookupValueDetail())));
    when(lookupService.getAwardTypes()).thenReturn(Mono.just(
        new AwardTypeLookupDetail()
            .addContentItem(new AwardTypeLookupValueDetail())));

    EbsApplicationMappingContext result =
        applicationService.buildApplicationMappingContext(ebsCase);

    assertNotNull(result);
    assertEquals(ebsCase, result.getEbsCaseDetail());
    assertEquals(applicationTypeLookup, result.getApplicationType());
    assertEquals(providerDetail, result.getProviderDetail());
    assertEquals(providerDetail.getOffices().get(2), result.getProviderOffice());
    assertEquals(providerDetail.getOffices().get(2).getFeeEarners().get(0),
        result.getFeeEarnerContact());
    assertEquals(providerDetail.getOffices().get(2).getFeeEarners().get(1),
        result.getSupervisorContact());
    assertTrue(result.getCaseWithOnlyDraftProceedings());
    assertTrue(result.getDevolvedPowers().getKey());
    assertEquals(ebsCase.getApplicationDetails().getDevolvedPowersDate(),
        result.getDevolvedPowers().getValue());

    // Category of law has an overall totalPaidToDate of 10.
    // Two cost limitations in the category of law, with a paidToDate of 1 for each.
    assertEquals(8, result.getCurrentProviderBilledAmount().intValue());

    // Case is draft-only, so amendmentProceedings should be empty.
    assertTrue(result.getAmendmentProceedingsInEbs().isEmpty());
    assertEquals(ebsCase.getApplicationDetails().getProceedings().size(),
        result.getProceedings().size());

    assertNotNull(result.getCaseOutcome());
    assertEquals(ebsCase.getApplicationDetails().getProceedings().size(),
        result.getCaseOutcome().getProceedingOutcomes().size());

    assertNotNull(result.getMeansAssessment());
    assertNotNull(result.getMeritsAssessment());

    assertTrue(result.getPriorAuthorities().isEmpty());
  }

  @Test
  void testBuildEbsApplicationMappingContext_NonDevolvedPowersMixedProceedings() {
    CaseDetail ebsCase = buildCaseDetail(APP_TYPE_EMERGENCY);
    ebsCase.getApplicationDetails().getProceedings().forEach(
        proceedingDetail -> {
          // Clear the outcome and scopelimitations from all proceedings - this is tested elsewhere
          proceedingDetail.setOutcome(null);
          proceedingDetail.getScopeLimitations().clear();
        });
    ebsCase.getPriorAuthorities().clear(); // PriorAuthority mapping context tested elsewhere.
    ebsCase.getAwards().clear(); // Awards tested separately.

    CommonLookupValueDetail applicationTypeLookup = new CommonLookupValueDetail();

    uk.gov.laa.ccms.data.model.ProviderDetail providerDetail = buildProviderDetail(
        ebsCase.getApplicationDetails().getProviderDetails().getProviderOfficeId(),
        ebsCase.getApplicationDetails().getProviderDetails().getFeeEarnerContactId(),
        ebsCase.getApplicationDetails().getProviderDetails().getSupervisorContactId());

    when(lookupService.getCommonValue(COMMON_VALUE_APPLICATION_TYPE,ebsCase.getCertificateType()))
        .thenReturn(Mono.just(Optional.of(applicationTypeLookup)));

    when(lookupService.getCommonValue(COMMON_VALUE_APPLICATION_TYPE,
        ebsCase.getApplicationDetails().getApplicationAmendmentType()))
        .thenReturn(Mono.just(Optional.of(applicationTypeLookup)));

    when(providerService.getProvider(Integer.parseInt(
        ebsCase.getApplicationDetails().getProviderDetails().getProviderFirmId())))
        .thenReturn(Mono.just(providerDetail));

    CommonLookupValueDetail matterTypeLookup =
        new CommonLookupValueDetail().code("mat1").description("mat 1");
    when(lookupService.getCommonValue(eq(COMMON_VALUE_MATTER_TYPES),anyString())).thenReturn(Mono.just(Optional.of(matterTypeLookup)));

    CommonLookupValueDetail levelOfServiceLookup =
        new CommonLookupValueDetail().code("los1").description("los 1");
    when(lookupService.getCommonValue(eq(COMMON_VALUE_LEVEL_OF_SERVICE),anyString())).thenReturn(Mono.just(Optional.of(levelOfServiceLookup)));

    CommonLookupValueDetail clientInvolvementLookup =
        new CommonLookupValueDetail().code("ci1").description("ci 1");
    when(lookupService.getCommonValue(eq(COMMON_VALUE_CLIENT_INVOLVEMENT_TYPES),anyString())).thenReturn(Mono.just(Optional.of(clientInvolvementLookup)));

    // Also need to mock calls for the 'sub' mapping contexts, but we aren't testing their
    // content here.
    when(ebsApiClient.getProceeding(any(String.class)))
        .thenReturn(Mono.just(new uk.gov.laa.ccms.data.model.ProceedingDetail()));
    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROCEEDING_STATUS),  any(String.class)))
        .thenReturn(Mono.just(Optional.of(new CommonLookupValueDetail())));
    when(lookupService.getAwardTypes()).thenReturn(Mono.just(
        new AwardTypeLookupDetail()
            .addContentItem(new AwardTypeLookupValueDetail())));

    final EbsApplicationMappingContext result =
        applicationService.buildApplicationMappingContext(ebsCase);

    assertNotNull(result);

    assertFalse(result.getCaseWithOnlyDraftProceedings());
    assertFalse(result.getDevolvedPowers().getKey());
    assertNull(result.getDevolvedPowers().getValue());

    // Proceedings should be split between the two lists
    assertEquals(1, result.getAmendmentProceedingsInEbs().size());
    assertEquals(1, result.getProceedings().size());

  }

  @Test
  void testBuildEbsApplicationMappingContext_NoAppTypeDefaultsToCertificate() {
    CaseDetail ebsCase = buildCaseDetail(null);
    ebsCase.getApplicationDetails().getProceedings().forEach(
        proceedingDetail -> {
          // Clear the outcome and scopelimitations from all proceedings - this is tested elsewhere
          proceedingDetail.setOutcome(null);
          proceedingDetail.getScopeLimitations().clear();
        });
    ebsCase.getPriorAuthorities().clear(); // PriorAuthority mapping context tested elsewhere.
    ebsCase.getAwards().clear(); // Awards tested separately.


    uk.gov.laa.ccms.data.model.ProviderDetail providerDetail = buildProviderDetail(
        ebsCase.getApplicationDetails().getProviderDetails().getProviderOfficeId(),
        ebsCase.getApplicationDetails().getProviderDetails().getFeeEarnerContactId(),
        ebsCase.getApplicationDetails().getProviderDetails().getSupervisorContactId());

    CommonLookupValueDetail certificateTypeLookup = new CommonLookupValueDetail();
    when(lookupService.getCommonValue(COMMON_VALUE_APPLICATION_TYPE, ebsCase.getCertificateType()))
        .thenReturn(Mono.just(Optional.of(certificateTypeLookup)));

    when(providerService.getProvider(Integer.parseInt(
        ebsCase.getApplicationDetails().getProviderDetails().getProviderFirmId())))
        .thenReturn(Mono.just(providerDetail));

    CommonLookupValueDetail matterTypeLookup =
        new CommonLookupValueDetail().code("mat1").description("mat 1");
    when(lookupService.getCommonValue(eq(COMMON_VALUE_MATTER_TYPES),anyString())).thenReturn(Mono.just(Optional.of(matterTypeLookup)));

    CommonLookupValueDetail levelOfServiceLookup =
        new CommonLookupValueDetail().code("los1").description("los 1");
    when(lookupService.getCommonValue(eq(COMMON_VALUE_LEVEL_OF_SERVICE),anyString())).thenReturn(Mono.just(Optional.of(levelOfServiceLookup)));

    CommonLookupValueDetail clientInvolvementLookup =
        new CommonLookupValueDetail().code("ci1").description("ci 1");
    when(lookupService.getCommonValue(eq(COMMON_VALUE_CLIENT_INVOLVEMENT_TYPES),anyString())).thenReturn(Mono.just(Optional.of(clientInvolvementLookup)));


    // Also need to mock calls for the 'sub' mapping contexts, but we aren't testing their
    // content here.
    when(ebsApiClient.getProceeding(any(String.class)))
        .thenReturn(Mono.just(new uk.gov.laa.ccms.data.model.ProceedingDetail()));
    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROCEEDING_STATUS),  any(String.class)))
        .thenReturn(Mono.just(Optional.of(new CommonLookupValueDetail())));
    when(lookupService.getAwardTypes()).thenReturn(Mono.just(
        new AwardTypeLookupDetail()
            .addContentItem(new AwardTypeLookupValueDetail())));

    final EbsApplicationMappingContext result =
        applicationService.buildApplicationMappingContext(ebsCase);

    assertNotNull(result);

    // The ebs case has no applicationAmendmentType, so the lookup should default to the
    // certificateType.
    assertEquals(certificateTypeLookup, result.getApplicationType());
  }


}
