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
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildPriorAuthorityTypeDetails;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildProviderDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildAwardTypeLookupDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildCaseDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildPriorAuthority;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildProceedingDetail;

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
import uk.gov.laa.ccms.caab.mapper.context.SoaApplicationMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.SoaCaseOutcomeMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.SoaPriorAuthorityMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.SoaProceedingMappingContext;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetails;
import uk.gov.laa.ccms.data.model.StageEndLookupDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;
import uk.gov.laa.ccms.soa.gateway.model.PriorAuthority;
import uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail;

@ExtendWith(MockitoExtension.class)
@DisplayName("Soa Application Mapping Context Builder Test")
class SoaApplicationMappingContextBuilderTest {

  @Mock
  private ProviderService providerService;
  @Mock
  private LookupService lookupService;
  @Mock
  private EbsApiClient ebsApiClient;
  
  private SoaApplicationMappingContextBuilder applicationService;

  @BeforeEach
  void beforeEach() {
    applicationService = new SoaApplicationMappingContextBuilder(providerService, lookupService, ebsApiClient);
  }

  @Test
  void buildSoaCaseOutcomeMappingContext() {
    final CaseDetail soaCase = buildCaseDetail("anytype");
    final List<SoaProceedingMappingContext> SoaProceedingMappingContexts = Collections.singletonList(
        SoaProceedingMappingContext.builder().build());

    final AwardTypeLookupDetail awardTypes = buildAwardTypeLookupDetail(soaCase);

    when(lookupService.getAwardTypes()).thenReturn(Mono.just(awardTypes));

    final SoaCaseOutcomeMappingContext result = applicationService.buildCaseOutcomeMappingContext(
        soaCase,
        SoaProceedingMappingContexts);

    assertNotNull(result);
    assertEquals(soaCase, result.getSoaCase());
    assertNotNull(result.getCostAwards());
    assertNotNull(result.getFinancialAwards());
    assertNotNull(result.getLandAwards());
    assertNotNull(result.getOtherAssetAwards());
    assertEquals(1, result.getCostAwards().size());
    assertEquals(soaCase.getAwards().getFirst(), result.getCostAwards().getFirst());
    assertEquals(1, result.getFinancialAwards().size());
    assertEquals(soaCase.getAwards().get(1), result.getFinancialAwards().getFirst());
    assertEquals(1, result.getLandAwards().size());
    assertEquals(soaCase.getAwards().get(2), result.getLandAwards().getFirst());
    assertEquals(1, result.getOtherAssetAwards().size());
    assertEquals(soaCase.getAwards().get(3), result.getOtherAssetAwards().getFirst());
    assertEquals(SoaProceedingMappingContexts, result.getProceedingOutcomes());
  }

  @Test
  void buildSoaCaseOutcomeMappingContextNoAwardTypes() {
    CaseDetail soaCase = buildCaseDetail("anytype");
    List<SoaProceedingMappingContext> SoaProceedingMappingContexts = Collections.singletonList(
        SoaProceedingMappingContext.builder().build());

    AwardTypeLookupDetail awardTypes = new AwardTypeLookupDetail();

    when(lookupService.getAwardTypes()).thenReturn(Mono.just(awardTypes));

    assertThrows(CaabApplicationException.class, () ->
        applicationService.buildCaseOutcomeMappingContext(soaCase, SoaProceedingMappingContexts));
  }

  @Test
  void buildSoaCaseOutcomeMappingContextUnknownAwardType() {
    CaseDetail soaCase = buildCaseDetail("anytype");
    List<SoaProceedingMappingContext> SoaProceedingMappingContexts = Collections.singletonList(
        SoaProceedingMappingContext.builder().build());

    AwardTypeLookupDetail awardTypes = buildAwardTypeLookupDetail(soaCase);
    // Drop out one of the award types
    awardTypes.getContent().remove(awardTypes.getContent().size() - 1);

    when(lookupService.getAwardTypes()).thenReturn(Mono.just(awardTypes));

    Exception e = assertThrows(CaabApplicationException.class, () ->
        applicationService.buildCaseOutcomeMappingContext(soaCase, SoaProceedingMappingContexts));

    assertEquals("Failed to find AwardType with code: %s".formatted(
        soaCase.getAwards().get(3).getAwardType()), e.getMessage());
  }

  @Test
  void buildSoaPriorAuthorityMappingContextLovLookup() {
    PriorAuthority soaPriorAuthority = buildPriorAuthority();

    PriorAuthorityTypeDetails priorAuthorityTypeDetails =
        buildPriorAuthorityTypeDetails(REFERENCE_DATA_ITEM_TYPE_LOV);
    PriorAuthorityTypeDetail priorAuthorityTypeDetail = priorAuthorityTypeDetails.getContent().getFirst();
    PriorAuthorityDetail priorAuthoritiesItem = priorAuthorityTypeDetail.getPriorAuthorities().getFirst();

    when(lookupService.getPriorAuthorityType(soaPriorAuthority.getPriorAuthorityType()))
        .thenReturn(Mono.just(Optional.of(priorAuthorityTypeDetail)));

    CommonLookupValueDetail lookup = new CommonLookupValueDetail()
        .code("thecode")
        .description("thedescription");

    when(lookupService.getCommonValue(priorAuthoritiesItem.getLovCode(),
        soaPriorAuthority.getDetails().getFirst().getValue()))
        .thenReturn(Mono.just(Optional.of(lookup)));

    SoaPriorAuthorityMappingContext result = applicationService.buildPriorAuthorityMappingContext(
        soaPriorAuthority);

    verify(lookupService).getCommonValue(priorAuthoritiesItem.getLovCode(),
        soaPriorAuthority.getDetails().getFirst().getValue());

    assertNotNull(result);
    assertEquals(soaPriorAuthority, result.getSoaPriorAuthority());
    assertNotNull(result.getPriorAuthorityTypeLookup());
    assertEquals(priorAuthorityTypeDetails.getContent().getFirst(),
        result.getPriorAuthorityTypeLookup());

    assertNotNull(result.getItems());
    assertEquals(1, result.getItems().size());
    assertEquals(priorAuthoritiesItem, result.getItems().getFirst().getKey());
    assertEquals(soaPriorAuthority.getDetails().getFirst().getValue(),
        result.getItems().getFirst().getValue().getCode());
    assertEquals(lookup.getDescription(),
        result.getItems().getFirst().getValue().getDescription());
  }

  @Test
  void buildSoaPriorAuthorityMappingContextNoLovLookup() {
    PriorAuthority soaPriorAuthority = buildPriorAuthority();

    PriorAuthorityTypeDetails priorAuthorityTypeDetails =
        buildPriorAuthorityTypeDetails("otherDataType");
    PriorAuthorityTypeDetail priorAuthorityTypeDetail = priorAuthorityTypeDetails.getContent().getFirst();

    when(lookupService.getPriorAuthorityType(soaPriorAuthority.getPriorAuthorityType()))
        .thenReturn(Mono.just(Optional.of(priorAuthorityTypeDetail)));

    SoaPriorAuthorityMappingContext result = applicationService.buildPriorAuthorityMappingContext(
        soaPriorAuthority);

    assertNotNull(result);
    assertEquals(soaPriorAuthority, result.getSoaPriorAuthority());
    assertNotNull(result.getPriorAuthorityTypeLookup());
    assertEquals(priorAuthorityTypeDetails.getContent().getFirst(),
        result.getPriorAuthorityTypeLookup());

    assertNotNull(result.getItems());
    assertEquals(1, result.getItems().size());
    assertEquals(priorAuthorityTypeDetails.getContent().getFirst().getPriorAuthorities().getFirst(),
        result.getItems().getFirst().getKey());
    assertEquals(soaPriorAuthority.getDetails().getFirst().getValue(),
        result.getItems().getFirst().getValue().getCode());
    assertEquals(soaPriorAuthority.getDetails().getFirst().getValue(),
        result.getItems().getFirst().getValue().getDescription());
  }

  @Test
  void buildSoaPriorAuthorityMappingContextUnknownPriorAuthType() {
    PriorAuthority soaPriorAuthority = buildPriorAuthority();

    when(lookupService.getPriorAuthorityType(soaPriorAuthority.getPriorAuthorityType()))
        .thenReturn(Mono.empty());

    Exception e = assertThrows(CaabApplicationException.class,
        () -> applicationService.buildPriorAuthorityMappingContext(
            soaPriorAuthority));

    assertEquals("Failed to find PriorAuthorityType with code: %s".formatted(
        soaPriorAuthority.getPriorAuthorityType()), e.getMessage());
  }

  @Test
  void buildSoaPriorAuthorityMappingContextUnknownLookup() {
    PriorAuthority soaPriorAuthority = buildPriorAuthority();

    PriorAuthorityDetail priorAuthoritiesItem = new PriorAuthorityDetail()
        .code(soaPriorAuthority.getDetails().getFirst().getName())
        .description("priorAuthItemDesc")
        .dataType(REFERENCE_DATA_ITEM_TYPE_LOV);

    PriorAuthorityTypeDetail priorAuthorityTypeDetail = new PriorAuthorityTypeDetail()
        .code(soaPriorAuthority.getPriorAuthorityType())
        .description("priorAuthDesc")
        .addPriorAuthoritiesItem(priorAuthoritiesItem);

    when(lookupService.getPriorAuthorityType(soaPriorAuthority.getPriorAuthorityType()))
        .thenReturn(Mono.just(Optional.of(priorAuthorityTypeDetail)));

    when(lookupService.getCommonValue(priorAuthoritiesItem.getLovCode(),
        soaPriorAuthority.getDetails().getFirst().getValue()))
        .thenReturn(Mono.empty());

    Exception e = assertThrows(CaabApplicationException.class,
        () -> applicationService.buildPriorAuthorityMappingContext(
            soaPriorAuthority));

    assertEquals("Failed to find common value with code: %s".formatted(
        soaPriorAuthority.getDetails().getFirst().getValue()), e.getMessage());
  }

  @Test
  void buildSoaProceedingMappingContextEmergency() {
    ProceedingDetail soaProceeding =
        buildProceedingDetail(STATUS_DRAFT);
    CaseDetail soaCase = buildCaseDetail(APP_TYPE_EMERGENCY);

    // Mock out the remaining lookups
    uk.gov.laa.ccms.data.model.ProceedingDetail proceedingLookup =
        new uk.gov.laa.ccms.data.model.ProceedingDetail();
    when(ebsApiClient.getProceeding(soaProceeding.getProceedingType()))
        .thenReturn(Mono.just(proceedingLookup));

    CommonLookupValueDetail proceedingStatusLookup =
        new CommonLookupValueDetail();
    when(lookupService.getCommonValue(COMMON_VALUE_PROCEEDING_STATUS, soaProceeding.getStatus()))
        .thenReturn(Mono.just(Optional.of(proceedingStatusLookup)));

    CommonLookupValueDetail matterTypeLookup = new CommonLookupValueDetail()
        .code(soaProceeding.getMatterType())
        .description("the matter type");
    when(lookupService.getCommonValue(COMMON_VALUE_MATTER_TYPES, soaProceeding.getMatterType()))
        .thenReturn(Mono.just(Optional.of(matterTypeLookup)));

    CommonLookupValueDetail levelOfServiceLookup = new CommonLookupValueDetail()
        .code(soaProceeding.getLevelOfService())
        .description("the los");
    when(lookupService.getCommonValue(COMMON_VALUE_LEVEL_OF_SERVICE, soaProceeding.getLevelOfService()))
        .thenReturn(Mono.just(Optional.of(levelOfServiceLookup)));

    CommonLookupValueDetail clientInvLookup = new CommonLookupValueDetail()
        .code(soaProceeding.getClientInvolvementType())
        .description("the involvement");
    when(lookupService.getCommonValue(COMMON_VALUE_CLIENT_INVOLVEMENT_TYPES, soaProceeding.getClientInvolvementType()))
        .thenReturn(Mono.just(Optional.of(clientInvLookup)));

    CommonLookupValueDetail scopeLimitationOneLookup = new CommonLookupValueDetail()
        .code(soaProceeding.getScopeLimitations().getFirst().getScopeLimitation())
        .description("the limitation 1");
    when(lookupService.getCommonValue(COMMON_VALUE_SCOPE_LIMITATIONS, soaProceeding.getScopeLimitations().getFirst().getScopeLimitation()))
        .thenReturn(Mono.just(Optional.of(scopeLimitationOneLookup)));

    CommonLookupValueDetail scopeLimitationTwoLookup = new CommonLookupValueDetail()
        .code(soaProceeding.getScopeLimitations().get(1).getScopeLimitation())
        .description("the limitation 2");
    when(lookupService.getCommonValue(COMMON_VALUE_SCOPE_LIMITATIONS, soaProceeding.getScopeLimitations().get(1).getScopeLimitation()))
        .thenReturn(Mono.just(Optional.of(scopeLimitationTwoLookup)));

    CommonLookupValueDetail scopeLimitationThreeLookup = new CommonLookupValueDetail()
        .code(soaProceeding.getScopeLimitations().get(2).getScopeLimitation())
        .description("the limitation 3");
    when(lookupService.getCommonValue(COMMON_VALUE_SCOPE_LIMITATIONS, soaProceeding.getScopeLimitations().get(2).getScopeLimitation()))
        .thenReturn(Mono.just(Optional.of(scopeLimitationThreeLookup)));

    // Mock the call for scopeLimitationDetails, used to calculate the max cost limitation.
    ScopeLimitationDetails scopeLimitationOne = new ScopeLimitationDetails()
        .totalElements(1)
        .addContentItem(
            new ScopeLimitationDetail()
                .costLimitation(BigDecimal.ZERO)
                .emergencyCostLimitation(BigDecimal.ZERO));

    ScopeLimitationDetails scopeLimitationTwo = new ScopeLimitationDetails()
        .totalElements(1)
        .addContentItem(
            new ScopeLimitationDetail()
                .costLimitation(BigDecimal.TEN)
                .emergencyCostLimitation(BigDecimal.ZERO));

    ScopeLimitationDetails scopeLimitationThree = new ScopeLimitationDetails()
        .totalElements(1)
        .addContentItem(
            new ScopeLimitationDetail()
                .costLimitation(BigDecimal.ZERO)
                .emergencyCostLimitation(BigDecimal.ONE));

    when(lookupService.getScopeLimitationDetails(any(
        ScopeLimitationDetail.class)))
        .thenReturn(Mono.just(scopeLimitationOne))
        .thenReturn(Mono.just(scopeLimitationTwo))
        .thenReturn(Mono.just(scopeLimitationThree));

    BigDecimal expectedCostLimitation = BigDecimal.ONE;

    // Mock objects for ProceedingDetail Outcome
    OutcomeResultLookupDetail outcomeResults = new OutcomeResultLookupDetail()
        .totalElements(1)
        .addContentItem(new OutcomeResultLookupValueDetail());
    when(lookupService.getOutcomeResults(soaProceeding.getProceedingType(),
        soaProceeding.getOutcome().getResult()))
        .thenReturn(Mono.just(outcomeResults));

    StageEndLookupDetail stageEnds = new StageEndLookupDetail()
        .totalElements(1)
        .addContentItem(new StageEndLookupValueDetail());
    when(lookupService.getStageEnds(soaProceeding.getProceedingType(),
        soaProceeding.getOutcome().getStageEnd()))
        .thenReturn(Mono.just(stageEnds));

    CommonLookupDetail courts = new CommonLookupDetail()
        .totalElements(1)
        .addContentItem(new CommonLookupValueDetail());
    when(lookupService.getCourts(soaProceeding.getOutcome().getCourtCode()))
        .thenReturn(Mono.just(courts));

    // Call the method under test
    SoaProceedingMappingContext result =
        applicationService.buildProceedingMappingContext(
            soaProceeding,
            soaCase);

//    StepVerifier.create(SoaProceedingMappingContextMono)
//        .expectNextMatches(result -> {
    assertNotNull(result);
    assertEquals(soaProceeding, result.getSoaProceeding());
    assertEquals(clientInvLookup, result.getClientInvolvement());
    assertEquals(levelOfServiceLookup, result.getLevelOfService());
    assertEquals(matterTypeLookup, result.getMatterType());
    assertEquals(expectedCostLimitation, result.getProceedingCostLimitation());
    assertEquals(proceedingLookup, result.getProceedingLookup());
    assertEquals(proceedingStatusLookup, result.getProceedingStatusLookup());
    assertNotNull(result.getScopeLimitations());
    assertEquals(3, result.getScopeLimitations().size());
    assertEquals(soaProceeding.getScopeLimitations().getFirst(),
        result.getScopeLimitations().getFirst().getKey());
    assertEquals(scopeLimitationOneLookup, result.getScopeLimitations().getFirst().getValue());
    assertEquals(soaProceeding.getScopeLimitations().get(1),
        result.getScopeLimitations().get(1).getKey());
    assertEquals(scopeLimitationTwoLookup, result.getScopeLimitations().get(1).getValue());
    assertEquals(soaProceeding.getScopeLimitations().get(2),
        result.getScopeLimitations().get(2).getKey());
    assertEquals(scopeLimitationThreeLookup, result.getScopeLimitations().get(2).getValue());

    // Check the proceeding outcome data
    assertEquals(outcomeResults.getContent().getFirst(), result.getOutcomeResultLookup());
    assertEquals(stageEnds.getContent().getFirst(), result.getStageEndLookup());
    assertEquals(courts.getContent().getFirst(), result.getCourtLookup());
//          return true; // Return true to indicate the match is successful
//        })
//        .verifyComplete();
  }

  @Test
  void calculateProceedingCostLimitationNonEmergency() {
    ProceedingDetail soaProceeding =
        buildProceedingDetail(STATUS_DRAFT);
    CaseDetail soaCase = buildCaseDetail(APP_TYPE_EXCEPTIONAL_CASE_FUNDING);

    // Mock the call for scopeLimitationDetails, used to calculate the max cost limitation.
    ScopeLimitationDetails scopeLimitationOne = new ScopeLimitationDetails()
        .totalElements(1)
        .addContentItem(
            new ScopeLimitationDetail()
                .costLimitation(BigDecimal.ZERO)
                .emergencyCostLimitation(BigDecimal.ZERO));

    ScopeLimitationDetails scopeLimitationTwo = new ScopeLimitationDetails()
        .totalElements(1)
        .addContentItem(
            new ScopeLimitationDetail()
                .costLimitation(BigDecimal.TEN)
                .emergencyCostLimitation(BigDecimal.ZERO));

    ScopeLimitationDetails scopeLimitationThree = new ScopeLimitationDetails()
        .totalElements(1)
        .addContentItem(
            new ScopeLimitationDetail()
                .costLimitation(BigDecimal.ZERO)
                .emergencyCostLimitation(BigDecimal.ONE));

    when(lookupService.getScopeLimitationDetails(any(
        ScopeLimitationDetail.class)))
        .thenReturn(Mono.just(scopeLimitationOne))
        .thenReturn(Mono.just(scopeLimitationTwo))
        .thenReturn(Mono.just(scopeLimitationThree));

    BigDecimal expectedCostLimitation = BigDecimal.TEN;

    // Call the method under test
    BigDecimal result =
        applicationService.calculateProceedingCostLimitation(soaProceeding,
            soaCase);

    verify(lookupService, times(3))
        .getScopeLimitationDetails(any(ScopeLimitationDetail.class));

    assertEquals(expectedCostLimitation, result);
  }

  @Test
  void addProceedingOutcomeContextNullOutcome() {
    final ProceedingDetail soaProceeding =
        buildProceedingDetail(STATUS_DRAFT);
    soaProceeding.setOutcome(null);

    applicationService.addProceedingOutcomeContext(
        SoaProceedingMappingContext.builder(), soaProceeding);

    verifyNoInteractions(lookupService);
  }

  @Test
  void buildSoaApplicationMappingContextDevolvedPowersAllDraftProceedings() {
    final CaseDetail soaCase = buildCaseDetail(APP_TYPE_EMERGENCY_DEVOLVED_POWERS);
    soaCase.getApplicationDetails().getProceedings().forEach(
        proceedingDetail -> {
          // Clear the outcome and scopelimitations from all proceedings - this is tested elsewhere
          proceedingDetail.setOutcome(null);
          proceedingDetail.getScopeLimitations().clear();

          // Set status for all proceedings to DRAFT
          proceedingDetail.setStatus(STATUS_DRAFT);
        });
    soaCase.getPriorAuthorities().clear(); // PriorAuthority mapping context tested elsewhere.
    soaCase.getAwards().clear(); // Awards tested separately.

    CommonLookupValueDetail applicationTypeLookup = new CommonLookupValueDetail();

    ProviderDetail providerDetail = buildProviderDetail(
        soaCase.getApplicationDetails().getProviderDetails().getProviderOfficeId(),
        soaCase.getApplicationDetails().getProviderDetails().getFeeEarnerContactId(),
        soaCase.getApplicationDetails().getProviderDetails().getSupervisorContactId());

    when(lookupService.getCommonValue(COMMON_VALUE_APPLICATION_TYPE, soaCase.getCertificateType()))
        .thenReturn(Mono.just(Optional.of(applicationTypeLookup)));

    when(lookupService.getCommonValue(COMMON_VALUE_APPLICATION_TYPE,
        soaCase.getApplicationDetails().getApplicationAmendmentType()))
        .thenReturn(Mono.just(Optional.of(applicationTypeLookup)));

    when(providerService.getProvider(Integer.parseInt(
        soaCase.getApplicationDetails().getProviderDetails().getProviderFirmId())))
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
    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROCEEDING_STATUS), any(String.class)))
        .thenReturn(Mono.just(Optional.of(new CommonLookupValueDetail())));
    when(lookupService.getAwardTypes()).thenReturn(Mono.just(
        new AwardTypeLookupDetail()
            .addContentItem(new AwardTypeLookupValueDetail())));

    SoaApplicationMappingContext result =
        applicationService.buildApplicationMappingContext(soaCase);

    assertNotNull(result);
    assertEquals(soaCase, result.getSoaCaseDetail());
    assertEquals(applicationTypeLookup, result.getApplicationType());
    assertEquals(providerDetail, result.getProviderDetail());
    assertEquals(providerDetail.getOffices().get(2), result.getProviderOffice());
    assertEquals(providerDetail.getOffices().get(2).getFeeEarners().getFirst(),
        result.getFeeEarnerContact());
    assertEquals(providerDetail.getOffices().get(2).getFeeEarners().get(1),
        result.getSupervisorContact());
    assertTrue(result.getCaseWithOnlyDraftProceedings());
    assertTrue(result.getDevolvedPowers().getKey());
    assertEquals(soaCase.getApplicationDetails().getDevolvedPowersDate(),
        result.getDevolvedPowers().getValue());

    // Category of law has an overall totalPaidToDate of 10.
    // Two cost limitations in the category of law, with a paidToDate of 1 for each.
    assertEquals(8, result.getCurrentProviderBilledAmount().intValue());

    // Case is draft-only, so amendmentProceedings should be empty.
    assertTrue(result.getAmendmentProceedingsInEbs().isEmpty());
    assertEquals(soaCase.getApplicationDetails().getProceedings().size(),
        result.getProceedings().size());

    assertNotNull(result.getCaseOutcome());
    assertEquals(soaCase.getApplicationDetails().getProceedings().size(),
        result.getCaseOutcome().getProceedingOutcomes().size());

    assertNotNull(result.getMeansAssessment());
    assertNotNull(result.getMeritsAssessment());

    assertTrue(result.getPriorAuthorities().isEmpty());
  }

  @Test
  void buildSoaApplicationMappingContextNonDevolvedPowersMixedProceedings() {
    CaseDetail soaCase = buildCaseDetail(APP_TYPE_EMERGENCY);
    soaCase.getApplicationDetails().getProceedings().forEach(
        proceedingDetail -> {
          // Clear the outcome and scopelimitations from all proceedings - this is tested elsewhere
          proceedingDetail.setOutcome(null);
          proceedingDetail.getScopeLimitations().clear();
        });
    soaCase.getPriorAuthorities().clear(); // PriorAuthority mapping context tested elsewhere.
    soaCase.getAwards().clear(); // Awards tested separately.

    CommonLookupValueDetail applicationTypeLookup = new CommonLookupValueDetail();

    ProviderDetail providerDetail = buildProviderDetail(
        soaCase.getApplicationDetails().getProviderDetails().getProviderOfficeId(),
        soaCase.getApplicationDetails().getProviderDetails().getFeeEarnerContactId(),
        soaCase.getApplicationDetails().getProviderDetails().getSupervisorContactId());

    when(lookupService.getCommonValue(COMMON_VALUE_APPLICATION_TYPE, soaCase.getCertificateType()))
        .thenReturn(Mono.just(Optional.of(applicationTypeLookup)));

    when(lookupService.getCommonValue(COMMON_VALUE_APPLICATION_TYPE,
        soaCase.getApplicationDetails().getApplicationAmendmentType()))
        .thenReturn(Mono.just(Optional.of(applicationTypeLookup)));

    when(providerService.getProvider(Integer.parseInt(
        soaCase.getApplicationDetails().getProviderDetails().getProviderFirmId())))
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
    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROCEEDING_STATUS), any(String.class)))
        .thenReturn(Mono.just(Optional.of(new CommonLookupValueDetail())));
    when(lookupService.getAwardTypes()).thenReturn(Mono.just(
        new AwardTypeLookupDetail()
            .addContentItem(new AwardTypeLookupValueDetail())));

    final SoaApplicationMappingContext result =
        applicationService.buildApplicationMappingContext(soaCase);

    assertNotNull(result);

    assertFalse(result.getCaseWithOnlyDraftProceedings());
    assertFalse(result.getDevolvedPowers().getKey());
    assertNull(result.getDevolvedPowers().getValue());

    // Proceedings should be split between the two lists
    assertEquals(1, result.getAmendmentProceedingsInEbs().size());
    assertEquals(1, result.getProceedings().size());

  }

  @Test
  void buildSoaApplicationMappingContextNoAppTypeDefaultsToCertificate() {
    CaseDetail soaCase = buildCaseDetail(null);
    soaCase.getApplicationDetails().getProceedings().forEach(
        proceedingDetail -> {
          // Clear the outcome and scopelimitations from all proceedings - this is tested elsewhere
          proceedingDetail.setOutcome(null);
          proceedingDetail.getScopeLimitations().clear();
        });
    soaCase.getPriorAuthorities().clear(); // PriorAuthority mapping context tested elsewhere.
    soaCase.getAwards().clear(); // Awards tested separately.


    ProviderDetail providerDetail = buildProviderDetail(
        soaCase.getApplicationDetails().getProviderDetails().getProviderOfficeId(),
        soaCase.getApplicationDetails().getProviderDetails().getFeeEarnerContactId(),
        soaCase.getApplicationDetails().getProviderDetails().getSupervisorContactId());

    CommonLookupValueDetail certificateTypeLookup = new CommonLookupValueDetail();
    when(lookupService.getCommonValue(COMMON_VALUE_APPLICATION_TYPE, soaCase.getCertificateType()))
        .thenReturn(Mono.just(Optional.of(certificateTypeLookup)));

    when(providerService.getProvider(Integer.parseInt(
        soaCase.getApplicationDetails().getProviderDetails().getProviderFirmId())))
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
    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROCEEDING_STATUS), any(String.class)))
        .thenReturn(Mono.just(Optional.of(new CommonLookupValueDetail())));
    when(lookupService.getAwardTypes()).thenReturn(Mono.just(
        new AwardTypeLookupDetail()
            .addContentItem(new AwardTypeLookupValueDetail())));

    final SoaApplicationMappingContext result =
        applicationService.buildApplicationMappingContext(soaCase);

    assertNotNull(result);

    // The soa case has no applicationAmendmentType, so the lookup should default to the
    // certificateType.
    assertEquals(certificateTypeLookup, result.getApplicationType());
  }


}
