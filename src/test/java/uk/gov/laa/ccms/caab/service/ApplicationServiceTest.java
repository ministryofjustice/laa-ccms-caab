package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_LAND;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_OTHER_ASSET;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.REFERENCE_DATA_ITEM_TYPE_LOV;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;

import java.math.BigDecimal;
import java.sql.Date;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteria;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.ApplicationFormDataMapper;
import uk.gov.laa.ccms.caab.mapper.context.ApplicationMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.CaseOutcomeMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.PriorAuthorityMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.ProceedingMappingContext;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationSummaryDisplay;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.AuditDetail;
import uk.gov.laa.ccms.caab.model.Client;
import uk.gov.laa.ccms.caab.model.CostStructure;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.BaseOffice;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetails;
import uk.gov.laa.ccms.data.model.StageEndLookupDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.AddressDetail;
import uk.gov.laa.ccms.soa.gateway.model.ApplicationDetails;
import uk.gov.laa.ccms.soa.gateway.model.AssessmentScreen;
import uk.gov.laa.ccms.soa.gateway.model.Award;
import uk.gov.laa.ccms.soa.gateway.model.BaseClient;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetails;
import uk.gov.laa.ccms.soa.gateway.model.CaseDoc;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.CaseStatus;
import uk.gov.laa.ccms.soa.gateway.model.CategoryOfLaw;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailRecordHistory;
import uk.gov.laa.ccms.soa.gateway.model.ContactDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.CostLimitation;
import uk.gov.laa.ccms.soa.gateway.model.Discharge;
import uk.gov.laa.ccms.soa.gateway.model.ExternalResource;
import uk.gov.laa.ccms.soa.gateway.model.LandAward;
import uk.gov.laa.ccms.soa.gateway.model.LarDetails;
import uk.gov.laa.ccms.soa.gateway.model.NameDetail;
import uk.gov.laa.ccms.soa.gateway.model.OfferedAmount;
import uk.gov.laa.ccms.soa.gateway.model.OpaAttribute;
import uk.gov.laa.ccms.soa.gateway.model.OpaEntity;
import uk.gov.laa.ccms.soa.gateway.model.OpaGoal;
import uk.gov.laa.ccms.soa.gateway.model.OpaInstance;
import uk.gov.laa.ccms.soa.gateway.model.OtherAsset;
import uk.gov.laa.ccms.soa.gateway.model.OtherParty;
import uk.gov.laa.ccms.soa.gateway.model.OtherPartyOrganisation;
import uk.gov.laa.ccms.soa.gateway.model.OtherPartyPerson;
import uk.gov.laa.ccms.soa.gateway.model.OutcomeDetail;
import uk.gov.laa.ccms.soa.gateway.model.PriorAuthority;
import uk.gov.laa.ccms.soa.gateway.model.PriorAuthorityAttribute;
import uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail;
import uk.gov.laa.ccms.soa.gateway.model.ProviderDetail;
import uk.gov.laa.ccms.soa.gateway.model.RecordHistory;
import uk.gov.laa.ccms.soa.gateway.model.RecoveredAmount;
import uk.gov.laa.ccms.soa.gateway.model.Recovery;
import uk.gov.laa.ccms.soa.gateway.model.RecoveryAmount;
import uk.gov.laa.ccms.soa.gateway.model.ScopeLimitation;
import uk.gov.laa.ccms.soa.gateway.model.ServiceAddress;
import uk.gov.laa.ccms.soa.gateway.model.TimeRelatedAward;
import uk.gov.laa.ccms.soa.gateway.model.Valuation;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {
  @Mock
  private CaabApiClient caabApiClient;

  @Mock
  private SoaApiClient soaApiClient;

  @Mock
  private EbsApiClient ebsApiClient;

  @Mock
  private LookupService lookupService;

  @Mock
  private ProviderService providerService;

  @Mock
  private ApplicationFormDataMapper applicationFormDataMapper;

  @InjectMocks
  private ApplicationService applicationService;

  @Test
  void getCaseReference_returnsCaseReferenceSummary_Successful() {
    String loginId = "user1";
    String userType = "userType";

    CaseReferenceSummary mockCaseReferenceSummary = new CaseReferenceSummary();

    when(soaApiClient.getCaseReference(loginId, userType)).thenReturn(
        Mono.just(mockCaseReferenceSummary));

    Mono<CaseReferenceSummary> caseReferenceSummaryMono =
        applicationService.getCaseReference(loginId, userType);

    StepVerifier.create(caseReferenceSummaryMono)
        .expectNextMatches(summary -> summary == mockCaseReferenceSummary)
        .verifyComplete();
  }

  @Test
  void getCases_ReturnsCaseDetails_Successful() {
    CopyCaseSearchCriteria copyCaseSearchCriteria = new CopyCaseSearchCriteria();
    copyCaseSearchCriteria.setCaseReference("123");
    copyCaseSearchCriteria.setProviderCaseReference("456");
    copyCaseSearchCriteria.setActualStatus("appl");
    copyCaseSearchCriteria.setFeeEarnerId(789);
    copyCaseSearchCriteria.setOfficeId(999);
    copyCaseSearchCriteria.setClientSurname("asurname");
    String loginId = "user1";
    String userType = "userType";
    int page = 0;
    int size = 10;

    CaseDetails mockCaseDetails = new CaseDetails();

    when(soaApiClient.getCases(copyCaseSearchCriteria, loginId, userType, page, size))
        .thenReturn(Mono.just(mockCaseDetails));

    Mono<CaseDetails> caseDetailsMono =
        applicationService.getCases(copyCaseSearchCriteria, loginId, userType, page, size);

    StepVerifier.create(caseDetailsMono)
        .expectNextMatches(clientDetails -> clientDetails == mockCaseDetails)
        .verifyComplete();
  }

  @Test
  void getCaseStatusValuesCopyAllowed_returnsData() {
    CaseStatusLookupDetail caseStatusLookupDetail = new CaseStatusLookupDetail();

    when(ebsApiClient.getCaseStatusValues(Boolean.TRUE)).thenReturn(
        Mono.just(caseStatusLookupDetail));

    Mono<CaseStatusLookupDetail> lookupDetailMono =
        applicationService.getCaseStatusValues(true);

    StepVerifier.create(lookupDetailMono)
        .expectNext(caseStatusLookupDetail)
        .verifyComplete();
  }

  @Test
  void getCopyCaseStatus_returnsData() {
    CaseStatusLookupDetail caseStatusLookupDetail = new CaseStatusLookupDetail();
    caseStatusLookupDetail.addContentItem(new CaseStatusLookupValueDetail());

    when(ebsApiClient.getCaseStatusValues(Boolean.TRUE)).thenReturn(
        Mono.just(caseStatusLookupDetail));

    CaseStatusLookupValueDetail lookupValue = applicationService.getCopyCaseStatus();

    assertNotNull(lookupValue);
    assertEquals(caseStatusLookupDetail.getContent().get(0), lookupValue);
  }

  @Test
  void getCopyCaseStatus_handlesNullResponse() {

    when(ebsApiClient.getCaseStatusValues(Boolean.TRUE)).thenReturn(Mono.empty());

    CaseStatusLookupValueDetail lookupValue = applicationService.getCopyCaseStatus();

    assertNull(lookupValue);
  }

  @Test
  void createApplication_success() throws ParseException {
    ApplicationFormData applicationFormData = buildApplicationDetails();
    ClientDetail clientInformation = buildClientInformation();
    UserDetail user = buildUser();

    // Mocking dependencies
    CaseReferenceSummary caseReferenceSummary =
        new CaseReferenceSummary().caseReferenceNumber("REF123");
    CategoryOfLawLookupValueDetail categoryOfLawValue = new CategoryOfLawLookupValueDetail()
        .code(applicationFormData.getCategoryOfLawId()).matterTypeDescription("DESC1");
    ContractDetails contractDetails = new ContractDetails();

    AmendmentTypeLookupValueDetail amendmentType = new AmendmentTypeLookupValueDetail()
        .applicationTypeCode("TEST")
        .applicationTypeDescription("TEST")
        .defaultLarScopeFlag("Y");

    AmendmentTypeLookupDetail amendmentTypes =
        new AmendmentTypeLookupDetail().addContentItem(amendmentType);

    when(soaApiClient.getCaseReference(user.getLoginId(), user.getUserType())).thenReturn(
        Mono.just(caseReferenceSummary));
    when(soaApiClient.getContractDetails(anyInt(), anyInt(), anyString(),
        anyString())).thenReturn(Mono.just(contractDetails));
    when(lookupService.getCategoryOfLaw(applicationFormData.getCategoryOfLawId())).thenReturn(
        Mono.just(categoryOfLawValue));
    when(ebsApiClient.getAmendmentTypes(any())).thenReturn(Mono.just(amendmentTypes));
    when(caabApiClient.createApplication(anyString(), any())).thenReturn(Mono.empty());

    Mono<String> applicationMono = applicationService.createApplication(
        applicationFormData, clientInformation, user);

    StepVerifier.create(applicationMono)
        .verifyComplete();

    verify(soaApiClient).getCaseReference(user.getLoginId(), user.getUserType());
    verify(lookupService).getCategoryOfLaw(applicationFormData.getCategoryOfLawId());
    verify(soaApiClient).getContractDetails(anyInt(), anyInt(), anyString(), anyString());
    verify(ebsApiClient).getAmendmentTypes(any());
    verify(caabApiClient).createApplication(anyString(), any());

  }

  @Test
  void getApplicationSummary_returnsApplicationSummary_Successful() {
    String applicationId = "12345";

    // Create mock data for successful Mono results
    RelationshipToCaseLookupDetail orgRelationshipsDetail = new RelationshipToCaseLookupDetail();
    orgRelationshipsDetail.addContentItem(new RelationshipToCaseLookupValueDetail());

    RelationshipToCaseLookupDetail personRelationshipsDetail = new RelationshipToCaseLookupDetail();
    personRelationshipsDetail.addContentItem(new RelationshipToCaseLookupValueDetail());

    AuditDetail auditDetail = new AuditDetail();
    auditDetail.setLastSaved(Date.from(Instant.now()));
    auditDetail.setLastSavedBy("TestUser");

    CostStructure costStructure = new CostStructure();
    costStructure.setAuditTrail(auditDetail);

    Client client = new Client();
    client.setFirstName("bob");
    client.setSurname("ross");

    ApplicationType applicationType = new ApplicationType();
    applicationType.id("test 123");
    applicationType.setDisplayValue("testing123");

    ApplicationDetail mockApplicationDetail = new ApplicationDetail(null, null, null, null);
    mockApplicationDetail.setAuditTrail(auditDetail);
    mockApplicationDetail.setClient(client);
    mockApplicationDetail.setApplicationType(applicationType);
    mockApplicationDetail.setProceedings(new ArrayList<>());
    mockApplicationDetail.setPriorAuthorities(new ArrayList<>());
    mockApplicationDetail.setOpponents(new ArrayList<>());
    mockApplicationDetail.setCosts(costStructure);

    // Mock the behavior of your dependencies
    when(ebsApiClient.getOrganisationRelationshipsToCaseValues()).thenReturn(
        Mono.just(orgRelationshipsDetail));

    when(ebsApiClient.getPersonRelationshipsToCaseValues()).thenReturn(
        Mono.just(personRelationshipsDetail));

    when(caabApiClient.getApplication(applicationId)).thenReturn(
        Mono.just(mockApplicationDetail));

    Mono<ApplicationSummaryDisplay> summaryMono =
        applicationService.getApplicationSummary(applicationId);

    // Verify the result
    StepVerifier.create(summaryMono)
        .expectNextMatches(summary -> {
          // Add assertions to check the content of the summary
          assertNotNull(summary); // Check that summary is not null
          assertEquals("bob ross", summary.getClientFullName());
          assertEquals("testing123", summary.getApplicationType().getStatus());
          assertEquals("Started", summary.getProviderDetails().getStatus());
          assertEquals("Complete", summary.getClientDetails().getStatus());
          assertEquals("Started", summary.getGeneralDetails().getStatus());
          assertEquals("Not started", summary.getProceedingsAndCosts().getStatus());
          assertEquals("Not started", summary.getOpponentsAndOtherParties().getStatus());
          // Add more assertions as needed
          return true; // Return true to indicate the match is successful
        })
        .verifyComplete();
  }

  @Test
  void getApplicationSummary_returnsApplicationSummary() {
    String applicationId = "12345";

    // Create mock data for successful Mono results
    RelationshipToCaseLookupDetail orgRelationshipsDetail = new RelationshipToCaseLookupDetail();
    orgRelationshipsDetail.addContentItem(new RelationshipToCaseLookupValueDetail());

    RelationshipToCaseLookupDetail personRelationshipsDetail = new RelationshipToCaseLookupDetail();
    personRelationshipsDetail.addContentItem(new RelationshipToCaseLookupValueDetail());

    AuditDetail auditDetail = new AuditDetail();
    auditDetail.setLastSaved(Date.from(Instant.now()));
    auditDetail.setLastSavedBy("TestUser");

    CostStructure costStructure = new CostStructure();
    costStructure.setAuditTrail(auditDetail);

    Client client = new Client();
    client.setFirstName("bob");
    client.setSurname("ross");

    ApplicationType applicationType = new ApplicationType();
    applicationType.id("test 123");
    applicationType.setDisplayValue("testing123");

    ApplicationDetail mockApplicationDetail = new ApplicationDetail(null, null, null, null);
    mockApplicationDetail.setAuditTrail(auditDetail);
    mockApplicationDetail.setClient(client);
    mockApplicationDetail.setApplicationType(applicationType);
    mockApplicationDetail.setProceedings(new ArrayList<>());
    mockApplicationDetail.setPriorAuthorities(new ArrayList<>());
    mockApplicationDetail.setOpponents(new ArrayList<>());
    mockApplicationDetail.setCosts(costStructure);

    // Mock the behavior of your dependencies
    when(ebsApiClient.getOrganisationRelationshipsToCaseValues()).thenReturn(
        Mono.just(orgRelationshipsDetail));

    when(ebsApiClient.getPersonRelationshipsToCaseValues()).thenReturn(
        Mono.just(personRelationshipsDetail));

    when(caabApiClient.getApplication(applicationId)).thenReturn(
        Mono.just(mockApplicationDetail));

    Mono<ApplicationSummaryDisplay> summaryMono =
        applicationService.getApplicationSummary(applicationId);

    // Verify the result
    StepVerifier.create(summaryMono)
        .expectNextMatches(summary -> {
          // Add assertions to check the content of the summary
          assertNotNull(summary); // Check that summary is not null
          assertEquals("bob ross", summary.getClientFullName());
          assertEquals("testing123", summary.getApplicationType().getStatus());
          assertEquals("Started", summary.getProviderDetails().getStatus());
          assertEquals("Complete", summary.getClientDetails().getStatus());
          assertEquals("Started", summary.getGeneralDetails().getStatus());
          assertEquals("Not started", summary.getProceedingsAndCosts().getStatus());
          assertEquals("Not started", summary.getOpponentsAndOtherParties().getStatus());
          // Add more assertions as needed
          return true; // Return true to indicate the match is successful
        })
        .verifyComplete();
  }

  @Test
  void testGetApplicationTypeFormData() {
    String id = "12345";
    ApplicationFormData mockApplicationFormData = new ApplicationFormData();
    ApplicationType applicationType = new ApplicationType();
    // Set up any necessary mocks for caabApiClient.getApplicationType

    when(caabApiClient.getApplicationType(id))
        .thenReturn(Mono.just(applicationType));
    when(applicationFormDataMapper.toApplicationTypeFormData(applicationType))
        .thenReturn(mockApplicationFormData);

    ApplicationFormData result = applicationService.getApplicationTypeFormData(id);

    assertEquals(mockApplicationFormData, result);
  }

  @Test
  void testGetProviderDetailsFormData() {
    String id = "12345";
    ApplicationFormData mockApplicationFormData = new ApplicationFormData();
    ApplicationProviderDetails providerDetails = new ApplicationProviderDetails();

    when(caabApiClient.getProviderDetails(id))
        .thenReturn(Mono.just(providerDetails));
    when(applicationFormDataMapper.toApplicationProviderDetailsFormData(providerDetails))
        .thenReturn(mockApplicationFormData);

    ApplicationFormData result = applicationService.getProviderDetailsFormData(id);

    assertEquals(mockApplicationFormData, result);
  }

  @Test
  void testPatchApplicationType() throws ParseException {
    String id = "12345";
    ApplicationFormData applicationFormData = new ApplicationFormData();
    UserDetail user = new UserDetail().loginId("TEST123");

    ApplicationType mockApplicationType = new ApplicationType();

    when(caabApiClient.patchApplication(eq(id), eq(user.getLoginId()), any(), eq("application-type")))
        .thenReturn(Mono.empty());

    applicationService.patchApplicationType(id, applicationFormData, user);

    verify(caabApiClient).patchApplication(eq(id), eq(user.getLoginId()), any(), eq("application-type"));

  }

  @Test
  void testBuildCaseOutcomeMappingContext() {
    CaseDetail soaCase = buildCaseDetail("anytype");
    List<ProceedingMappingContext> proceedingMappingContexts = Collections.singletonList(
        ProceedingMappingContext.builder().build());

    AwardTypeLookupDetail awardTypes = new AwardTypeLookupDetail()
        .addContentItem(new AwardTypeLookupValueDetail()
            .awardType(AWARD_TYPE_COST)
            .code(soaCase.getAwards().get(0).getAwardType()))
        .addContentItem(new AwardTypeLookupValueDetail()
            .awardType(AWARD_TYPE_FINANCIAL)
            .code(soaCase.getAwards().get(1).getAwardType()))
        .addContentItem(new AwardTypeLookupValueDetail()
            .awardType(AWARD_TYPE_LAND)
            .code(soaCase.getAwards().get(2).getAwardType()))
        .addContentItem(new AwardTypeLookupValueDetail()
            .awardType(AWARD_TYPE_OTHER_ASSET)
            .code(soaCase.getAwards().get(3).getAwardType()));

    when(lookupService.getAwardTypes()).thenReturn(Mono.just(awardTypes));

    CaseOutcomeMappingContext result = applicationService.buildCaseOutcomeMappingContext(
        soaCase,
        proceedingMappingContexts);

    assertNotNull(result);
    assertEquals(soaCase, result.getSoaCase());
    assertNotNull(result.getCostAwards());
    assertNotNull(result.getFinancialAwards());
    assertNotNull(result.getLandAwards());
    assertNotNull(result.getOtherAssetAwards());
    assertEquals(1, result.getCostAwards().size());
    assertEquals(soaCase.getAwards().get(0), result.getCostAwards().get(0));
    assertEquals(1, result.getFinancialAwards().size());
    assertEquals(soaCase.getAwards().get(1), result.getFinancialAwards().get(0));
    assertEquals(1, result.getLandAwards().size());
    assertEquals(soaCase.getAwards().get(2), result.getLandAwards().get(0));
    assertEquals(1, result.getOtherAssetAwards().size());
    assertEquals(soaCase.getAwards().get(3), result.getOtherAssetAwards().get(0));
    assertEquals(proceedingMappingContexts, result.getProceedingOutcomes());
  }

  @Test
  void testBuildCaseOutcomeMappingContext_NoAwardTypes() {
    CaseDetail soaCase = buildCaseDetail("anytype");
    List<ProceedingMappingContext> proceedingMappingContexts = Collections.singletonList(
        ProceedingMappingContext.builder().build());

    AwardTypeLookupDetail awardTypes = new AwardTypeLookupDetail();

    when(lookupService.getAwardTypes()).thenReturn(Mono.just(awardTypes));

    assertThrows(CaabApplicationException.class, () ->
      applicationService.buildCaseOutcomeMappingContext(soaCase, proceedingMappingContexts));
  }

  @Test
  void testBuildCaseOutcomeMappingContext_UnknownAwardType() {
    CaseDetail soaCase = buildCaseDetail("anytype");
    List<ProceedingMappingContext> proceedingMappingContexts = Collections.singletonList(
        ProceedingMappingContext.builder().build());

    AwardTypeLookupDetail awardTypes = new AwardTypeLookupDetail()
        .addContentItem(new AwardTypeLookupValueDetail()
            .awardType(AWARD_TYPE_COST)
            .code(soaCase.getAwards().get(0).getAwardType()))
        .addContentItem(new AwardTypeLookupValueDetail()
            .awardType(AWARD_TYPE_FINANCIAL)
            .code(soaCase.getAwards().get(1).getAwardType()))
        .addContentItem(new AwardTypeLookupValueDetail()
            .awardType(AWARD_TYPE_LAND)
            .code(soaCase.getAwards().get(2).getAwardType()));

    when(lookupService.getAwardTypes()).thenReturn(Mono.just(awardTypes));

    Exception e = assertThrows(CaabApplicationException.class, () ->
        applicationService.buildCaseOutcomeMappingContext(soaCase, proceedingMappingContexts));

    assertEquals(String.format("Failed to find AwardType with code: %s",
        soaCase.getAwards().get(3).getAwardType()), e.getMessage());
  }

  @Test
  void testBuildPriorAuthorityMappingContext_LovLookup() {
    PriorAuthority soaPriorAuthority = buildPriorAuthority();

    PriorAuthorityDetail priorAuthoritiesItem = new PriorAuthorityDetail()
        .code(soaPriorAuthority.getDetails().get(0).getName())
        .description("priorAuthItemDesc")
        .dataType(REFERENCE_DATA_ITEM_TYPE_LOV)
        .lovCode("lov1");

    PriorAuthorityTypeDetails priorAuthorityTypeDetails = new PriorAuthorityTypeDetails()
        .addContentItem(new PriorAuthorityTypeDetail()
            .code(soaPriorAuthority.getPriorAuthorityType())
            .description("priorAuthDesc")
            .addPriorAuthoritiesItem(priorAuthoritiesItem));

    CommonLookupValueDetail lookup = new CommonLookupValueDetail()
        .code("thecode")
        .description("thedescription");

    when(lookupService.getCommonValue(priorAuthoritiesItem.getLovCode(),
        soaPriorAuthority.getDetails().get(0).getValue()))
        .thenReturn(Mono.just(lookup));

    PriorAuthorityMappingContext result = applicationService.buildPriorAuthorityMappingContext(
        soaPriorAuthority,
        priorAuthorityTypeDetails);

    verify(lookupService).getCommonValue(priorAuthoritiesItem.getLovCode(),
        soaPriorAuthority.getDetails().get(0).getValue());

    assertNotNull(result);
    assertEquals(soaPriorAuthority, result.getSoaPriorAuthority());
    assertNotNull(result.getPriorAuthorityTypeLookup());
    assertEquals(priorAuthorityTypeDetails.getContent().get(0),
        result.getPriorAuthorityTypeLookup());

    assertNotNull(result.getItems());
    assertEquals(1, result.getItems().size());
    assertEquals(priorAuthoritiesItem, result.getItems().get(0).getKey());
    assertEquals(soaPriorAuthority.getDetails().get(0).getValue(),
        result.getItems().get(0).getValue().getCode());
    assertEquals(lookup.getDescription(),
        result.getItems().get(0).getValue().getDescription());
  }

  @Test
  void testBuildPriorAuthorityMappingContext_NoLovLookup() {
    PriorAuthority soaPriorAuthority = buildPriorAuthority();

    PriorAuthorityDetail priorAuthoritiesItem = new PriorAuthorityDetail()
        .code(soaPriorAuthority.getDetails().get(0).getName())
        .description("priorAuthItemDesc")
        .dataType("other");

    PriorAuthorityTypeDetails priorAuthorityTypeDetails = new PriorAuthorityTypeDetails()
        .addContentItem(new PriorAuthorityTypeDetail()
            .code(soaPriorAuthority.getPriorAuthorityType())
            .description("priorAuthDesc")
            .addPriorAuthoritiesItem(priorAuthoritiesItem));

    PriorAuthorityMappingContext result = applicationService.buildPriorAuthorityMappingContext(
        soaPriorAuthority,
        priorAuthorityTypeDetails);

    verifyNoInteractions(lookupService);

    assertNotNull(result);
    assertEquals(soaPriorAuthority, result.getSoaPriorAuthority());
    assertNotNull(result.getPriorAuthorityTypeLookup());
    assertEquals(priorAuthorityTypeDetails.getContent().get(0),
        result.getPriorAuthorityTypeLookup());

    assertNotNull(result.getItems());
    assertEquals(1, result.getItems().size());
    assertEquals(priorAuthoritiesItem, result.getItems().get(0).getKey());
    assertEquals(soaPriorAuthority.getDetails().get(0).getValue(),
        result.getItems().get(0).getValue().getCode());
    assertEquals(soaPriorAuthority.getDetails().get(0).getValue(),
        result.getItems().get(0).getValue().getDescription());
  }

  @Test
  void testBuildPriorAuthorityMappingContext_UnknownPriorAuthType() {
    PriorAuthority soaPriorAuthority = buildPriorAuthority();

    PriorAuthorityDetail priorAuthoritiesItem = new PriorAuthorityDetail()
        .code(soaPriorAuthority.getDetails().get(0).getName())
        .description("priorAuthItemDesc")
        .dataType("other");

    PriorAuthorityTypeDetails priorAuthorityTypeDetails = new PriorAuthorityTypeDetails()
        .addContentItem(new PriorAuthorityTypeDetail()
            .code("adifferentcode")
            .description("priorAuthDesc")
            .addPriorAuthoritiesItem(priorAuthoritiesItem));

    Exception e = assertThrows(CaabApplicationException.class,
        () -> applicationService.buildPriorAuthorityMappingContext(
        soaPriorAuthority,
        priorAuthorityTypeDetails));

    assertEquals(String.format("Failed to find PriorAuthorityType with code: %s",
        soaPriorAuthority.getPriorAuthorityType()), e.getMessage());
  }

  @Test
  void testBuildPriorAuthorityMappingContext_UnknownLookup() {
    PriorAuthority soaPriorAuthority = buildPriorAuthority();

    PriorAuthorityDetail priorAuthoritiesItem = new PriorAuthorityDetail()
        .code(soaPriorAuthority.getDetails().get(0).getName())
        .description("priorAuthItemDesc")
        .dataType(REFERENCE_DATA_ITEM_TYPE_LOV);

    PriorAuthorityTypeDetails priorAuthorityTypeDetails = new PriorAuthorityTypeDetails()
        .addContentItem(new PriorAuthorityTypeDetail()
            .code(soaPriorAuthority.getPriorAuthorityType())
            .description("priorAuthDesc")
            .addPriorAuthoritiesItem(priorAuthoritiesItem));

    when(lookupService.getCommonValue(priorAuthoritiesItem.getLovCode(),
        soaPriorAuthority.getDetails().get(0).getValue()))
        .thenReturn(Mono.empty());

    Exception e = assertThrows(CaabApplicationException.class,
        () -> applicationService.buildPriorAuthorityMappingContext(
        soaPriorAuthority,
        priorAuthorityTypeDetails));

    assertEquals(String.format("Failed to find common value with code: %s",
        soaPriorAuthority.getDetails().get(0).getValue()), e.getMessage());
  }

  @Test
  void testBuildProceedingMappingContext_Emergency() {
    ProceedingDetail soaProceeding = buildProceedingDetail(STATUS_DRAFT);
    CaseDetail soaCase = buildCaseDetail(APP_TYPE_EMERGENCY);

    // Mock out the remaining lookups
    uk.gov.laa.ccms.data.model.ProceedingDetail proceedingLookup =
        new uk.gov.laa.ccms.data.model.ProceedingDetail();
    when(ebsApiClient.getProceeding(soaProceeding.getProceedingType()))
        .thenReturn(Mono.just(proceedingLookup));

    CommonLookupValueDetail proceedingStatusLookup =
        new CommonLookupValueDetail();
    when(lookupService.getProceedingStatus(soaProceeding.getStatus()))
        .thenReturn(Mono.just(proceedingStatusLookup));

    CommonLookupValueDetail matterTypeLookup = new CommonLookupValueDetail()
        .code(soaProceeding.getMatterType())
        .description("the matter type");
    CommonLookupValueDetail levelOfServiceLookup = new CommonLookupValueDetail()
        .code(soaProceeding.getLevelOfService())
        .description("the los");
    CommonLookupValueDetail clientInvLookup = new CommonLookupValueDetail()
        .code(soaProceeding.getClientInvolvementType())
        .description("the involvement");
    CommonLookupValueDetail scopeLimitationOneLookup = new CommonLookupValueDetail()
        .code(soaProceeding.getScopeLimitations().get(0).getScopeLimitation())
        .description("the limitation 1");
    CommonLookupValueDetail scopeLimitationTwoLookup = new CommonLookupValueDetail()
        .code(soaProceeding.getScopeLimitations().get(1).getScopeLimitation())
        .description("the limitation 2");
    CommonLookupValueDetail scopeLimitationThreeLookup = new CommonLookupValueDetail()
        .code(soaProceeding.getScopeLimitations().get(2).getScopeLimitation())
        .description("the limitation 3");

    Map<String, CommonLookupValueDetail> matterTypeLookups = Collections.singletonMap(
        soaProceeding.getMatterType(),
        matterTypeLookup);

    Map<String, CommonLookupValueDetail> levelOfServiceLookups = Collections.singletonMap(
        soaProceeding.getLevelOfService(),
        levelOfServiceLookup);

    Map<String, CommonLookupValueDetail> clientInvolvementLookups = Collections.singletonMap(
        soaProceeding.getClientInvolvementType(),
        clientInvLookup);

    Map<String, CommonLookupValueDetail> scopeLimitationLookups = Stream.of(
        scopeLimitationOneLookup,
        scopeLimitationTwoLookup,
        scopeLimitationThreeLookup)
        .collect(Collectors.toMap(CommonLookupValueDetail::getCode, Function.identity()));

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

    when(lookupService.getScopeLimitationDetails(any(ScopeLimitationDetail.class)))
        .thenReturn(Mono.just(scopeLimitationOne))
        .thenReturn(Mono.just(scopeLimitationTwo))
        .thenReturn(Mono.just(scopeLimitationThree));

    BigDecimal expectedCostLimitation = BigDecimal.ONE;

    // Mock objects for Proceeding Outcome
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
    Mono<ProceedingMappingContext> proceedingMappingContextMono =
        applicationService.buildProceedingMappingContext(
          soaProceeding,
          soaCase,
          matterTypeLookups,
          levelOfServiceLookups,
          clientInvolvementLookups,
          scopeLimitationLookups);

    StepVerifier.create(proceedingMappingContextMono)
        .expectNextMatches(result -> {
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
          assertEquals(soaProceeding.getScopeLimitations().get(0),
              result.getScopeLimitations().get(0).getKey());
          assertEquals(scopeLimitationOneLookup, result.getScopeLimitations().get(0).getValue());
          assertEquals(soaProceeding.getScopeLimitations().get(1),
              result.getScopeLimitations().get(1).getKey());
          assertEquals(scopeLimitationTwoLookup, result.getScopeLimitations().get(1).getValue());
          assertEquals(soaProceeding.getScopeLimitations().get(2),
              result.getScopeLimitations().get(2).getKey());
          assertEquals(scopeLimitationThreeLookup, result.getScopeLimitations().get(2).getValue());

          // Check the proceeding outcome data
          assertEquals(outcomeResults.getContent().get(0), result.getOutcomeResultLookup());
          assertEquals(stageEnds.getContent().get(0), result.getStageEndLookup());
          assertEquals(courts.getContent().get(0), result.getCourtLookup());
          return true; // Return true to indicate the match is successful
        })
        .verifyComplete();
  }

  @Test
  void testCalculateProceedingCostLimitation_NonEmergency() {
    ProceedingDetail soaProceeding = buildProceedingDetail(STATUS_DRAFT);
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

    when(lookupService.getScopeLimitationDetails(any(ScopeLimitationDetail.class)))
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
  void testAddProceedingOutcomeContext_NullOutcome() {
    ProceedingDetail soaProceeding = buildProceedingDetail(STATUS_DRAFT);
    soaProceeding.setOutcome(null);

    applicationService.addProceedingOutcomeContext(
        ProceedingMappingContext.builder(), soaProceeding);

    verifyNoInteractions(lookupService);
  }

  @Test
  void testBuildApplicationMappingContext_DevolvedPowersAllDraftProceedings() {
    CaseDetail soaCase = buildCaseDetail(APP_TYPE_EMERGENCY_DEVOLVED_POWERS);
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

    uk.gov.laa.ccms.data.model.ProviderDetail providerDetail =
        new uk.gov.laa.ccms.data.model.ProviderDetail()
            .addOfficesItem(new OfficeDetail()
                .id(0)
                .name("wrong office"))
            .addOfficesItem(new OfficeDetail()
                .id(Integer.parseInt(
                    soaCase.getApplicationDetails().getProviderDetails().getProviderOfficeId()))
                .name("right office")
                .addFeeEarnersItem(
                    new uk.gov.laa.ccms.data.model.ContactDetail()
                        .id(Integer.parseInt(
                            soaCase.getApplicationDetails().getProviderDetails()
                                .getFeeEarnerContactId()))
                        .name("fee earner"))
                .addFeeEarnersItem(
                    new uk.gov.laa.ccms.data.model.ContactDetail()
                        .id(Integer.parseInt(
                            soaCase.getApplicationDetails().getProviderDetails()
                                .getSupervisorContactId()))
                        .name("supervisor")));

    CommonLookupDetail matterTypeLookups = new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail().code("mat1").description("mat 1"));

    CommonLookupDetail levelOfServiceLookups = new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail().code("los1").description("los 1"));

    CommonLookupDetail clientInvolvementLookups = new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail().code("ci1").description("ci 1"));

    CommonLookupDetail scopeLimitations = new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail().code("sl1").description("sl 1"));

    PriorAuthorityTypeDetails priorAuthorityTypes = new PriorAuthorityTypeDetails()
        .addContentItem(new PriorAuthorityTypeDetail()
            .code("picode")
            .description("pi desc")
            .addPriorAuthoritiesItem(new PriorAuthorityDetail()));

    when(lookupService.getApplicationType(soaCase.getCertificateType()))
        .thenReturn(Mono.just(applicationTypeLookup));

    when(providerService.getProvider(Integer.parseInt(
        soaCase.getApplicationDetails().getProviderDetails().getProviderFirmId())))
        .thenReturn(Mono.just(providerDetail));

    when(lookupService.getMatterTypes()).thenReturn(Mono.just(matterTypeLookups));

    when(lookupService.getLevelsOfService()).thenReturn(Mono.just(levelOfServiceLookups));

    when(lookupService.getClientInvolvementTypes()).thenReturn(Mono.just(clientInvolvementLookups));

    when(lookupService.getScopeLimitations()).thenReturn(Mono.just(scopeLimitations));

    when(lookupService.getPriorAuthorityTypes()).thenReturn(Mono.just(priorAuthorityTypes));

    // Also need to mock calls for the 'sub' mapping contexts, but we aren't testing their
    // content here.
    when(ebsApiClient.getProceeding(any(String.class)))
        .thenReturn(Mono.just(new uk.gov.laa.ccms.data.model.ProceedingDetail()));
    when(lookupService.getProceedingStatus(any(String.class)))
        .thenReturn(Mono.just(new CommonLookupValueDetail()));
    when(lookupService.getAwardTypes()).thenReturn(Mono.just(
        new AwardTypeLookupDetail()
            .addContentItem(new AwardTypeLookupValueDetail())));

    Mono<ApplicationMappingContext> resultMono =
        applicationService.buildApplicationMappingContext(soaCase);

    StepVerifier.create(resultMono)
        .expectNextMatches(result -> {
          assertNotNull(result);
          assertEquals(soaCase, result.getSoaCaseDetail());
          assertEquals(applicationTypeLookup, result.getApplicationType());
          assertEquals(providerDetail, result.getProviderDetail());
          assertEquals(providerDetail.getOffices().get(1), result.getProviderOffice());
          assertEquals(providerDetail.getOffices().get(1).getFeeEarners().get(0),
              result.getFeeEarnerContact());
          assertEquals(providerDetail.getOffices().get(1).getFeeEarners().get(1),
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
          return true;
        })
        .verifyComplete();
  }

  @Test
  void testBuildApplicationMappingContext_NonDevolvedPowersMixedProceedings() {
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

    uk.gov.laa.ccms.data.model.ProviderDetail providerDetail =
        new uk.gov.laa.ccms.data.model.ProviderDetail()
            .addOfficesItem(new OfficeDetail()
                .id(0)
                .name("wrong office"))
            .addOfficesItem(new OfficeDetail()
                .id(Integer.parseInt(
                    soaCase.getApplicationDetails().getProviderDetails().getProviderOfficeId()))
                .name("right office")
                .addFeeEarnersItem(
                    new uk.gov.laa.ccms.data.model.ContactDetail()
                        .id(Integer.parseInt(
                            soaCase.getApplicationDetails().getProviderDetails()
                                .getFeeEarnerContactId()))
                        .name("fee earner"))
                .addFeeEarnersItem(
                    new uk.gov.laa.ccms.data.model.ContactDetail()
                        .id(Integer.parseInt(
                            soaCase.getApplicationDetails().getProviderDetails()
                                .getSupervisorContactId()))
                        .name("supervisor")));

    CommonLookupDetail matterTypeLookups = new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail().code("mat1").description("mat 1"));

    CommonLookupDetail levelOfServiceLookups = new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail().code("los1").description("los 1"));

    CommonLookupDetail clientInvolvementLookups = new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail().code("ci1").description("ci 1"));

    CommonLookupDetail scopeLimitations = new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail().code("sl1").description("sl 1"));

    PriorAuthorityTypeDetails priorAuthorityTypes = new PriorAuthorityTypeDetails()
        .addContentItem(new PriorAuthorityTypeDetail()
            .code("picode")
            .description("pi desc")
            .addPriorAuthoritiesItem(new PriorAuthorityDetail()));

    when(lookupService.getApplicationType(soaCase.getCertificateType()))
        .thenReturn(Mono.just(applicationTypeLookup));

    when(providerService.getProvider(Integer.parseInt(
        soaCase.getApplicationDetails().getProviderDetails().getProviderFirmId())))
        .thenReturn(Mono.just(providerDetail));

    when(lookupService.getMatterTypes()).thenReturn(Mono.just(matterTypeLookups));

    when(lookupService.getLevelsOfService()).thenReturn(Mono.just(levelOfServiceLookups));

    when(lookupService.getClientInvolvementTypes()).thenReturn(Mono.just(clientInvolvementLookups));

    when(lookupService.getScopeLimitations()).thenReturn(Mono.just(scopeLimitations));

    when(lookupService.getPriorAuthorityTypes()).thenReturn(Mono.just(priorAuthorityTypes));

    // Also need to mock calls for the 'sub' mapping contexts, but we aren't testing their
    // content here.
    when(ebsApiClient.getProceeding(any(String.class)))
        .thenReturn(Mono.just(new uk.gov.laa.ccms.data.model.ProceedingDetail()));
    when(lookupService.getProceedingStatus(any(String.class)))
        .thenReturn(Mono.just(new CommonLookupValueDetail()));
    when(lookupService.getAwardTypes()).thenReturn(Mono.just(
        new AwardTypeLookupDetail()
            .addContentItem(new AwardTypeLookupValueDetail())));

    Mono<ApplicationMappingContext> resultMono =
        applicationService.buildApplicationMappingContext(soaCase);

    StepVerifier.create(resultMono)
        .expectNextMatches(result -> {
          assertNotNull(result);

          assertFalse(result.getCaseWithOnlyDraftProceedings());
          assertFalse(result.getDevolvedPowers().getKey());
          assertNull(result.getDevolvedPowers().getValue());

          // Proceedings should be split between the two lists
          assertEquals(1, result.getAmendmentProceedingsInEbs().size());
          assertEquals(1, result.getProceedings().size());
          return true;
        })
        .verifyComplete();
  }

  private UserDetail buildUser() {
    return new UserDetail()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId")
        .provider(buildBaseProvider());
  }

  private BaseProvider buildBaseProvider() {
    return new BaseProvider()
        .id(123)
        .addOfficesItem(
            new BaseOffice()
                .id(1)
                .name("Office 1"));
  }

  private ApplicationFormData buildApplicationDetails() {
    ApplicationFormData applicationFormData = new ApplicationFormData();
    applicationFormData.setOfficeId(1);
    applicationFormData.setCategoryOfLawId("COL");
    applicationFormData.setExceptionalFunding(false);
    applicationFormData.setApplicationTypeCategory(APP_TYPE_SUBSTANTIVE);
    applicationFormData.setDelegatedFunctions(true);
    applicationFormData.setDelegatedFunctionUsedDay("01");
    applicationFormData.setDelegatedFunctionUsedMonth("01");
    applicationFormData.setDelegatedFunctionUsedYear("2022");
    return applicationFormData;
  }

  public ClientDetail buildClientInformation() {
    String clientReferenceNumber = "12345";
    return new ClientDetail()
        .clientReferenceNumber(clientReferenceNumber)
        .details(new ClientDetailDetails()
            .name(new NameDetail()))
        .recordHistory(new ClientDetailRecordHistory());
  }

  private BaseClient buildBaseClient() {
    return new BaseClient()
        .clientReferenceNumber("clientref")
        .firstName("firstname")
        .surname("surn");
  }

  private uk.gov.laa.ccms.soa.gateway.model.PriorAuthority buildPriorAuthority() {
    return new uk.gov.laa.ccms.soa.gateway.model.PriorAuthority()
        .assessedAmount(BigDecimal.TEN)
        .decisionStatus("dstat")
        .description("descr")
        .addDetailsItem(
            new PriorAuthorityAttribute()
                .name("attname")
                .value("attval"))
        .priorAuthorityType("prauthtype")
        .reasonForRequest("requestreason")
        .requestAmount(BigDecimal.ONE);
  }

  private CaseDetail buildCaseDetail(String appType) {
    return new CaseDetail()
        .applicationDetails(
            new ApplicationDetails()
                .applicationAmendmentType(appType)
                .categoryOfLaw(
                    new CategoryOfLaw()
                        .categoryOfLawCode("cat1")
                        .categoryOfLawDescription("cat 1")
                        .addCostLimitationsItem(buildCostLimitation("cat1"))
                        .addCostLimitationsItem(buildCostLimitation("cat2"))
                        .grantedAmount(BigDecimal.ZERO)
                        .requestedAmount(BigDecimal.ONE)
                        .totalPaidToDate(BigDecimal.TEN))
                .certificateType("certtype")
                .client(buildBaseClient())
                .correspondenceAddress(buildAddressDetail("corr"))
                .dateOfFirstAttendance(new java.util.Date())
                .dateOfHearing(new java.util.Date())
                .devolvedPowersDate(new java.util.Date())
                .addExternalResourcesItem(
                    new ExternalResource()
                        .costCeiling(Arrays.asList(
                            buildCostLimitation("ext1"),
                            buildCostLimitation("ext2"))))
                .fixedHearingDateInd(Boolean.TRUE)
                .highProfileCaseInd(Boolean.TRUE)
                .larDetails(
                    new LarDetails()
                        .larScopeFlag(Boolean.TRUE)
                        .legalHelpUfn("ufn")
                        .legalHelpOfficeCode("off1"))
                .addMeansAssesmentsItem(buildAssessmentResult("means"))
                .addMeritsAssesmentsItem(buildAssessmentResult("merits"))
                .otherParties(Arrays.asList(buildOtherPartyPerson(), buildOtherPartyOrganisation()))
                .preferredAddress("prefadd")
                .addProceedingsItem(buildProceedingDetail(STATUS_DRAFT))
                .addProceedingsItem(buildProceedingDetail("otherstatus"))
                .providerDetails(
                    new ProviderDetail()
                        .contactDetails(buildContactDetail("prov"))
                        .providerOfficeId("11111")
                        .contactUserId(buildUserDetail("contact"))
                        .providerFirmId("12345") // Defined as String, but data is numeric in db!
                        .providerCaseReferenceNumber("provcaseref123")
                        .feeEarnerContactId("22222")
                        .supervisorContactId("33333"))
                .purposeOfApplication("purposeA")
                .purposeOfHearing("purposeH"))
        .availableFunctions(Arrays.asList("func1", "func2"))
        .addAwardsItem(buildCostAward())
        .addAwardsItem(buildFinancialAward())
        .addAwardsItem(buildLandAward())
        .addAwardsItem(buildOtherAssetAward())
        .addCaseDocsItem(
            new CaseDoc()
                .ccmsDocumentId("docId")
                .documentSubject("thesub"))
        .caseReferenceNumber("caseref")
        .caseStatus(new CaseStatus()
            .actualCaseStatus("actualstat")
            .displayCaseStatus("displaystat")
            .statusUpdateInd(Boolean.TRUE))
        .certificateDate(new java.util.Date())
        .certificateType("certtype")
        .dischargeStatus(
            new Discharge()
                .clientContinuePvtInd(Boolean.TRUE)
                .otherDetails("dotherdets")
                .reason("dreason"))
        .legalHelpCosts(BigDecimal.TEN)
        .addLinkedCasesItem(buildLinkedCase())
        .preCertificateCosts(BigDecimal.ONE)
        .addPriorAuthoritiesItem(buildPriorAuthority())
        .recordHistory(
            new RecordHistory()
                .createdBy(buildUserDetail("creator"))
                .dateCreated(new java.util.Date())
                .dateLastUpdated(new java.util.Date())
                .lastUpdatedBy(buildUserDetail("lastUpd")))
        .undertakingAmount(BigDecimal.TEN);

  }

  private ProceedingDetail buildProceedingDetail(String status) {
    return new ProceedingDetail()
        .availableFunctions(Arrays.asList("pavfuncs1", "pavfuncs2"))
        .clientInvolvementType("citype")
        .dateApplied(new java.util.Date())
        .dateCostsValid(new java.util.Date())
        .dateDevolvedPowersUsed(new java.util.Date())
        .dateGranted(new java.util.Date())
        .devolvedPowersInd(Boolean.TRUE)
        .leadProceedingIndicator(Boolean.TRUE)
        .levelOfService("levofsvc")
        .matterType("pmattype")
        .orderType("ordtype")
        .outcome(buildOutcomeDetail())
        .outcomeCourtCaseNumber("occn")
        .proceedingCaseId("pcid")
        .proceedingDescription("procdescr")
        .proceedingType("proctype")
        .scopeLimitationApplied("scopelimapp")
        .addScopeLimitationsItem(buildScopeLimitation("1"))
        .addScopeLimitationsItem(buildScopeLimitation("2"))
        .addScopeLimitationsItem(buildScopeLimitation("3"))
        .stage("stg")
        .status(status);
  }

  private ScopeLimitation buildScopeLimitation(String prefix) {
    return new ScopeLimitation()
        .delegatedFunctionsApply(Boolean.TRUE)
        .scopeLimitation(prefix + "scopelim")
        .scopeLimitationId(prefix +"scopelimid")
        .scopeLimitationWording(prefix + "slwording");
  }

  private OutcomeDetail buildOutcomeDetail() {
    return new OutcomeDetail()
        .additionalResultInfo("addresinfo")
        .altAcceptanceReason("altaccreason")
        .altDisputeResolution("altdispres")
        .courtCode("courtcode")
        .finalWorkDate(new java.util.Date())
        .issueDate(new java.util.Date())
        .outcomeCourtCaseNumber("outccn")
        .resolutionMethod("resmeth")
        .result("res")
        .stageEnd("se")
        .widerBenefits("widerbens");
  }

  private OtherParty buildOtherPartyPerson() {
    return new OtherParty()
        .otherPartyId("opid")
        .person(
            new OtherPartyPerson()
                .address(buildAddressDetail("opp"))
                .assessedAssets(BigDecimal.TEN)
                .assessedIncome(BigDecimal.ONE)
                .assessedIncomeFrequency("often")
                .assessmentDate(new java.util.Date())
                .certificateNumber("certnum")
                .contactDetails(buildContactDetail("opp"))
                .contactName("conname")
                .courtOrderedMeansAssesment(Boolean.TRUE)
                .dateOfBirth(new java.util.Date())
                .employersName("employer")
                .employmentStatus("empstat")
                .name(buildNameDetail())
                .niNumber("ni123456")
                .organizationAddress("orgaddr")
                .organizationName("orgname")
                .otherInformation("otherinf")
                .partyLegalAidedInd(Boolean.TRUE)
                .publicFundingAppliedInd(Boolean.TRUE)
                .relationToCase("reltocase")
                .relationToClient("reltoclient"))
        .sharedInd(Boolean.TRUE);
  }

  private OtherParty buildOtherPartyOrganisation() {
    return new OtherParty()
        .otherPartyId("opid")
        .organisation(
            new OtherPartyOrganisation()
                .address(buildAddressDetail("opo"))
                .contactDetails(buildContactDetail("op"))
                .contactName("name")
                .currentlyTrading("curtrad")
                .relationToCase("reltocase")
                .organizationName("orgname")
                .organizationType("orgtype")
                .otherInformation("otherinf")
                .relationToClient("relclient"))
        .sharedInd(Boolean.TRUE);
  }

  private NameDetail buildNameDetail() {
    return new NameDetail()
        .firstName("firstname")
        .surname("thesurname")
        .fullName("thefullname")
        .middleName("mid")
        .surnameAtBirth("surbirth")
        .title("mr");
  }

  private uk.gov.laa.ccms.soa.gateway.model.LinkedCase buildLinkedCase() {
    return new uk.gov.laa.ccms.soa.gateway.model.LinkedCase()
        .caseReferenceNumber("lcaseref")
        .caseStatus("lcasestat")
        .categoryOfLawCode("lcat1")
        .categoryOfLawDesc("linked cat 1")
        .client(
            new BaseClient()
                .firstName("lfirstname")
                .surname("lsurname")
                .clientReferenceNumber("lclientref"))
        .feeEarnerId("lfeeearner")
        .feeEarnerName("lname")
        .linkType("ltype")
        .providerReferenceNumber("lprovrefnum")
        .publicFundingAppliedInd(Boolean.TRUE);
  }

  private AddressDetail buildAddressDetail(String prefix) {
    return new AddressDetail()
        .addressId(prefix + "address1")
        .addressLine1(prefix + "addline1")
        .addressLine2(prefix + "addline2")
        .addressLine3(prefix + "addline3")
        .addressLine4(prefix + "addline4")
        .careOfName(prefix + "cofname")
        .city(prefix + "thecity")
        .country(prefix + "thecountry")
        .county(prefix + "thecounty")
        .house(prefix + "thehouse")
        .postalCode(prefix + "pc")
        .province(prefix + "prov")
        .state(prefix + "st");
  }

  private ContactDetail buildContactDetail(String prefix) {
    return new ContactDetail()
        .correspondenceLanguage(prefix + "lang")
        .emailAddress(prefix + "email")
        .fax(prefix + "123765")
        .correspondenceMethod(prefix + "method")
        .password(prefix + "pass")
        .mobileNumber(prefix + "mobil123")
        .passwordReminder(prefix + "remember")
        .telephoneHome(prefix + "tel123")
        .telephoneWork(prefix + "telwork123");
  }

  private CostLimitation buildCostLimitation(String prefix) {
    return new CostLimitation()
        .costLimitId(prefix + "costid")
        .costCategory(prefix + "costcat1")
        .amount(BigDecimal.TEN)
        .paidToDate(BigDecimal.ONE)
        .billingProviderId(prefix + "billprovid")
        .billingProviderName(prefix + "billprovname");
  }

  private uk.gov.laa.ccms.soa.gateway.model.AssessmentResult buildAssessmentResult(
      String prefix) {
    return new uk.gov.laa.ccms.soa.gateway.model.AssessmentResult()
        .defaultInd(Boolean.TRUE)
        .date(new java.util.Date())
        .assessmentId(prefix + "assessid")
        .addResultsItem(
            new OpaGoal()
                .attributeValue(prefix + "val")
                .attribute(prefix + "att"))
        .addAssessmentDetailsItem(
            new AssessmentScreen()
                .caption(prefix + "cap")
                .screenName(prefix + "name")
                .addEntityItem(
                    new OpaEntity()
                        .sequenceNumber(1)
                        .entityName(prefix + "thentity")
                        .caption(prefix + "capt")
                        .addInstancesItem(
                            new OpaInstance()
                                .caption(prefix + "capti")
                                .instanceLabel(prefix + "label")
                                .addAttributesItem(
                                    new OpaAttribute()
                                        .userDefinedInd(Boolean.TRUE)
                                        .attribute(prefix + "attt")
                                        .responseText(prefix + "response")
                                        .responseType(prefix + "restype")
                                        .responseValue(prefix + "resval")
                                        .caption(prefix + "caption")))));
  }

  private Award buildCostAward() {
    return new Award()
        .deleteAllowed(Boolean.TRUE)
        .updateAllowed(Boolean.TRUE)
        .awardType(AWARD_TYPE_COST)
        .awardId("costAwardId")
        .awardCategory("costCat")
        .costAward(
            new uk.gov.laa.ccms.soa.gateway.model.CostAward()
                .serviceAddress(
                    new ServiceAddress()
                        .addressLine1("add1")
                        .addressLine2("add2")
                        .addressLine3("add3"))
                .recovery(buildRecovery())
                .awardedBy("me")
                .liableParties(Arrays.asList("a", "b", "c"))
                .orderDate(new java.util.Date())
                .orderDateServed(new java.util.Date())
                .otherDetails("otherDets")
                .certificateCostRateLsc(BigDecimal.ONE)
                .certificateCostRateMarket(BigDecimal.ZERO)
                .preCertificateAwardLsc(BigDecimal.TEN)
                .preCertificateAwardOth(BigDecimal.ONE)
                .courtAssessmentStatus("assessstate")
                .interestAwardedRate(BigDecimal.TEN)
                .interestAwardedStartDate(new java.util.Date()));
  }

  private Award buildFinancialAward() {
    return new Award()
        .deleteAllowed(Boolean.TRUE)
        .updateAllowed(Boolean.TRUE)
        .awardType(AWARD_TYPE_FINANCIAL)
        .awardId("finAwardId")
        .awardCategory("finCat")
        .financialAward(
            new uk.gov.laa.ccms.soa.gateway.model.FinancialAward()
                .serviceAddress(
                    new ServiceAddress()
                        .addressLine1("add1")
                        .addressLine2("add2")
                        .addressLine3("add3"))
                .recovery(buildRecovery())
                .awardedBy("me")
                .interimAward(BigDecimal.TEN)
                .amount(BigDecimal.ONE)
                .awardJustifications("justified")
                .liableParties(Arrays.asList("a", "b", "c"))
                .orderDate(new java.util.Date())
                .orderDateServed(new java.util.Date())
                .otherDetails("otherDets")
                .statutoryChangeReason("statreason"));
  }

  private Award buildLandAward() {
    return new Award()
        .deleteAllowed(Boolean.TRUE)
        .updateAllowed(Boolean.TRUE)
        .awardType(AWARD_TYPE_LAND)
        .awardId("landAwardId")
        .awardCategory("landCat")
        .landAward(
            new LandAward()
                .orderDate(new java.util.Date())
                .description("descr")
                .titleNo("title")
                .propertyAddress(
                    new ServiceAddress()
                        .addressLine1("add1")
                        .addressLine2("add2")
                        .addressLine3("add3"))
                .valuation(
                    new Valuation()
                        .amount(BigDecimal.TEN)
                        .criteria("crit")
                        .date(new java.util.Date()))
                .disputedPercentage(BigDecimal.ZERO)
                .awardedPercentage(BigDecimal.TEN)
                .mortgageAmountDue(BigDecimal.ONE)
                .equity("shouldn't be used")
                .awardedBy("me")
                .recovery("recov")
                .noRecoveryDetails("none")
                .statChargeExemptReason("exempt")
                .landChargeRegistration("landChargeReg")
                .registrationRef("regRef")
                .otherProprietors(Arrays.asList("a", "b", "c"))
                .timeRelatedAward(buildTimeRelatedAward()));
  }

  private Award buildOtherAssetAward() {
    return new Award()
        .deleteAllowed(Boolean.TRUE)
        .updateAllowed(Boolean.TRUE)
        .awardType(AWARD_TYPE_OTHER_ASSET)
        .awardId("otherAwardId")
        .awardCategory("otherCat")
        .otherAsset(
            new OtherAsset()
                .awardedBy("me")
                .description("descr")
                .awardedAmount(BigDecimal.ONE)
                .recoveredAmount(BigDecimal.ZERO)
                .disputedAmount(BigDecimal.TEN)
                .heldBy(Arrays.asList("A", "B", "C"))
                .awardedPercentage(BigDecimal.TEN)
                .recoveredPercentage(BigDecimal.ONE)
                .disputedPercentage(BigDecimal.ZERO)
                .noRecoveryDetails("none")
                .orderDate(new java.util.Date())
                .recovery("recov")
                .statChargeExemptReason("exempt")
                .timeRelatedAward(buildTimeRelatedAward())
                .valuation(
                    new Valuation()
                        .amount(BigDecimal.TEN)
                        .criteria("crit")
                        .date(new java.util.Date())));
  }

  private TimeRelatedAward buildTimeRelatedAward() {
    return new TimeRelatedAward()
        .awardDate(new java.util.Date())
        .amount(BigDecimal.TEN)
        .awardType("type")
        .description("desc1")
        .awardTriggeringEvent("theevent")
        .otherDetails("otherdets");
  }

  private Recovery buildRecovery() {
    return new Recovery()
        .awardValue(BigDecimal.ONE)
        .leaveOfCourtReqdInd(Boolean.TRUE)
        .offeredAmount(
            new OfferedAmount()
                .amount(BigDecimal.TEN)
                .conditionsOfOffer("cond1"))
        .recoveredAmount(
            new RecoveredAmount()
                .client(
                    new RecoveryAmount()
                        .amount(BigDecimal.ONE)
                        .dateReceived(new java.util.Date())
                        .paidToLsc(BigDecimal.ZERO))
                .court(
                    new RecoveryAmount()
                        .amount(BigDecimal.TEN)
                        .dateReceived(new java.util.Date())
                        .paidToLsc(BigDecimal.ONE))
                .solicitor(
                    new RecoveryAmount()
                        .amount(BigDecimal.ZERO)
                        .dateReceived(new java.util.Date())
                        .paidToLsc(BigDecimal.TEN)))
        .unRecoveredAmount(BigDecimal.ZERO);
  }

  private uk.gov.laa.ccms.soa.gateway.model.UserDetail buildUserDetail(String prefix) {
    return new uk.gov.laa.ccms.soa.gateway.model.UserDetail()
        .userType(prefix + "type1")
        .userLoginId(prefix + "login1")
        .userName(prefix + "username");
  }


}
