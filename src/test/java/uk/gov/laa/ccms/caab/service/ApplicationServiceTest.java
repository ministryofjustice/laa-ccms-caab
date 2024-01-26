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
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_INDIVIDUAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.REFERENCE_DATA_ITEM_TYPE_LOV;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildApplicationDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildCategoryOfLawLookupValueDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildPriorAuthorityTypeDetails;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildProviderDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildRelationshipToCaseLookupDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildAwardTypeLookupDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildCaseDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildCaseReferenceSummary;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildClientDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildPriorAuthority;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildProceedingDetail;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.constants.SearchConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.mapper.AddressFormDataMapper;
import uk.gov.laa.ccms.caab.mapper.ApplicationFormDataMapper;
import uk.gov.laa.ccms.caab.mapper.ApplicationMapper;
import uk.gov.laa.ccms.caab.mapper.CopyApplicationMapper;
import uk.gov.laa.ccms.caab.mapper.ResultDisplayMapper;
import uk.gov.laa.ccms.caab.mapper.context.ApplicationMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.CaseOutcomeMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.PriorAuthorityMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.ProceedingMappingContext;
import uk.gov.laa.ccms.caab.model.Address;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationSummaryDisplay;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.AuditDetail;
import uk.gov.laa.ccms.caab.model.BaseApplication;
import uk.gov.laa.ccms.caab.model.Client;
import uk.gov.laa.ccms.caab.model.CostStructure;
import uk.gov.laa.ccms.caab.model.LinkedCase;
import uk.gov.laa.ccms.caab.model.LinkedCaseResultRowDisplay;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
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
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetails;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.CaseSummary;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.NameDetail;
import uk.gov.laa.ccms.soa.gateway.model.PriorAuthority;
import uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail;
import uk.gov.laa.ccms.soa.gateway.model.RecordHistory;

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

  @Mock
  private ApplicationMapper applicationMapper;

  @Mock
  private AddressFormDataMapper addressFormDataMapper;

  @Mock
  private ResultDisplayMapper resultDisplayMapper;

  @Mock
  private CopyApplicationMapper copyApplicationMapper;

  @Mock
  private SearchConstants searchConstants;

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
  void getCases_UnSubmittedStatusDoesNotQuerySOA() {
    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    caseSearchCriteria.setCaseReference("123");
    caseSearchCriteria.setProviderCaseReference("456");
    caseSearchCriteria.setFeeEarnerId(789);
    caseSearchCriteria.setOfficeId(999);
    caseSearchCriteria.setClientSurname("asurname");

    caseSearchCriteria.setStatus(STATUS_UNSUBMITTED_ACTUAL_VALUE);

    String loginId = "user1";
    String userType = "userType";
    int page = 0;
    int size = 10;

    ApplicationDetails mockApplicationDetails = new ApplicationDetails()
        .addContentItem(new BaseApplication());

    when(caabApiClient.getApplications(caseSearchCriteria, page, size))
        .thenReturn(Mono.just(mockApplicationDetails));
    when(searchConstants.getMaxSearchResultsCases()).thenReturn(size);

    List<BaseApplication> results =
        applicationService.getCases(caseSearchCriteria, loginId, userType);

    verifyNoInteractions(soaApiClient);
    verify(caabApiClient).getApplications(caseSearchCriteria, page, size);

    assertNotNull(results);
    assertEquals(mockApplicationDetails.getContent(), results);
  }

  @Test
  void getCases_DraftStatusQueriesSOAAndTDS() {
    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    caseSearchCriteria.setCaseReference("123");
    caseSearchCriteria.setProviderCaseReference("456");
    caseSearchCriteria.setFeeEarnerId(789);
    caseSearchCriteria.setOfficeId(999);
    caseSearchCriteria.setClientSurname("asurname");

    caseSearchCriteria.setStatus(STATUS_DRAFT);

    String loginId = "user1";
    String userType = "userType";
    int page = 0;
    int size = 10;

    CaseDetails mockCaseDetails = new CaseDetails()
        .totalElements(1)
        .size(1)
        .addContentItem(new CaseSummary().caseReferenceNumber("2"));

    BaseApplication mockSoaApplication = new BaseApplication()
            .caseReferenceNumber("2");

    ApplicationDetails mockTdsApplicationDetails = new ApplicationDetails()
        .totalElements(1)
        .size(1)
        .addContentItem(new BaseApplication()
            .caseReferenceNumber("1"));

    // expected result, sorted by case reference
    List<BaseApplication> expectedResult = List.of(mockTdsApplicationDetails.getContent().get(0),
        mockSoaApplication);

    when(soaApiClient.getCases(caseSearchCriteria, loginId, userType, page, size))
        .thenReturn(Mono.just(mockCaseDetails));
    when(applicationMapper.toBaseApplication(mockCaseDetails.getContent().get(0)))
        .thenReturn(mockSoaApplication);
    when(caabApiClient.getApplications(caseSearchCriteria, page, size))
        .thenReturn(Mono.just(mockTdsApplicationDetails));
    when(searchConstants.getMaxSearchResultsCases()).thenReturn(size);

    List<BaseApplication> result =
        applicationService.getCases(caseSearchCriteria, loginId, userType);

    verify(soaApiClient).getCases(caseSearchCriteria, loginId, userType, page, size);
    verify(caabApiClient).getApplications(caseSearchCriteria, page, size);

    assertNotNull(result);
    assertEquals(expectedResult, result);
  }

  @Test
  void getCases_RemovesDuplicates_RetainingSoaCase() {
    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    caseSearchCriteria.setCaseReference("123");
    caseSearchCriteria.setProviderCaseReference("456");
    caseSearchCriteria.setFeeEarnerId(789);
    caseSearchCriteria.setOfficeId(999);
    caseSearchCriteria.setClientSurname("asurname");

    caseSearchCriteria.setStatus(STATUS_DRAFT);

    String loginId = "user1";
    String userType = "userType";
    int page = 0;
    int size = 10;

    CaseSummary soaCaseSummary = new CaseSummary()
        .caseReferenceNumber("1")
        .caseStatusDisplay("the soa one");

    BaseApplication mockSoaApplication = new BaseApplication()
        .caseReferenceNumber(soaCaseSummary.getCaseReferenceNumber())
        .status(new StringDisplayValue().displayValue(soaCaseSummary.getCaseStatusDisplay()));

    BaseApplication mockTdsApplication = new BaseApplication()
        .caseReferenceNumber("1")
        .status(new StringDisplayValue().displayValue("the tds one"));

    CaseDetails mockCaseDetails = new CaseDetails()
        .totalElements(1)
        .size(1)
        .addContentItem(soaCaseSummary);

    ApplicationDetails mockTdsApplicationDetails = new ApplicationDetails()
        .totalElements(1)
        .size(1)
        .addContentItem(mockTdsApplication);

    // expected result, only the soa case retained
    List<BaseApplication> expectedResult = List.of(mockSoaApplication);

    when(soaApiClient.getCases(caseSearchCriteria, loginId, userType, page, size))
        .thenReturn(Mono.just(mockCaseDetails));
    when(applicationMapper.toBaseApplication(mockCaseDetails.getContent().get(0)))
        .thenReturn(mockSoaApplication);
    when(caabApiClient.getApplications(caseSearchCriteria, page, size))
        .thenReturn(Mono.just(mockTdsApplicationDetails));
    when(searchConstants.getMaxSearchResultsCases()).thenReturn(size);

    List<BaseApplication> result =
        applicationService.getCases(caseSearchCriteria, loginId, userType);

    verify(soaApiClient).getCases(caseSearchCriteria, loginId, userType, page, size);
    verify(caabApiClient).getApplications(caseSearchCriteria, page, size);

    assertNotNull(result);
    assertEquals(expectedResult, result);
  }

  @Test
  void getCases_TooManySoaResults_ThrowsException() {
    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    caseSearchCriteria.setCaseReference("123");
    caseSearchCriteria.setProviderCaseReference("456");
    caseSearchCriteria.setFeeEarnerId(789);
    caseSearchCriteria.setOfficeId(999);
    caseSearchCriteria.setClientSurname("asurname");
    String loginId = "user1";
    String userType = "userType";
    int page = 0;
    int size = 1;

    caseSearchCriteria.setStatus(STATUS_DRAFT);

    CaseDetails mockCaseDetails = new CaseDetails()
        .totalElements(2)
        .size(2)
        .addContentItem(new CaseSummary())
        .addContentItem(new CaseSummary());

    when(soaApiClient.getCases(caseSearchCriteria, loginId, userType, page, size))
        .thenReturn(Mono.just(mockCaseDetails));
    when(searchConstants.getMaxSearchResultsCases()).thenReturn(size);

    assertThrows(TooManyResultsException.class, () ->
        applicationService.getCases(caseSearchCriteria, loginId, userType));
  }

  @Test
  void getCases_TooManyOverallResults_ThrowsException() {
    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    caseSearchCriteria.setCaseReference("123");
    caseSearchCriteria.setProviderCaseReference("456");
    caseSearchCriteria.setFeeEarnerId(789);
    caseSearchCriteria.setOfficeId(999);
    caseSearchCriteria.setClientSurname("asurname");
    String loginId = "user1";
    String userType = "userType";
    int page = 0;
    int size = 2;

    caseSearchCriteria.setStatus(STATUS_DRAFT);

    CaseDetails mockCaseDetails = new CaseDetails()
        .totalElements(2)
        .size(2)
        .addContentItem(new CaseSummary().caseReferenceNumber("1"))
        .addContentItem(new CaseSummary().caseReferenceNumber("2"));

    ApplicationDetails mockTdsApplicationDetails = new ApplicationDetails()
        .totalElements(1)
        .size(1)
        .addContentItem(new BaseApplication().caseReferenceNumber("3"));

    when(soaApiClient.getCases(caseSearchCriteria, loginId, userType, page, size))
        .thenReturn(Mono.just(mockCaseDetails));
    when(applicationMapper.toBaseApplication(mockCaseDetails.getContent().get(0)))
        .thenReturn(new BaseApplication()
            .caseReferenceNumber(mockCaseDetails.getContent().get(0).getCaseReferenceNumber()));
    when(applicationMapper.toBaseApplication(mockCaseDetails.getContent().get(1)))
        .thenReturn(new BaseApplication()
            .caseReferenceNumber(mockCaseDetails.getContent().get(1).getCaseReferenceNumber()));
    when(caabApiClient.getApplications(caseSearchCriteria, page, size))
        .thenReturn(Mono.just(mockTdsApplicationDetails));
    when(searchConstants.getMaxSearchResultsCases()).thenReturn(size);

    assertThrows(TooManyResultsException.class, () ->
        applicationService.getCases(caseSearchCriteria, loginId, userType));
  }

  @Test
  void getCopyCaseStatus_returnsData() {
    CaseStatusLookupDetail caseStatusLookupDetail = new CaseStatusLookupDetail();
    caseStatusLookupDetail.addContentItem(new CaseStatusLookupValueDetail());

    when(lookupService.getCaseStatusValues(Boolean.TRUE)).thenReturn(
        Mono.just(caseStatusLookupDetail));

    CaseStatusLookupValueDetail lookupValue = applicationService.getCopyCaseStatus();

    assertNotNull(lookupValue);
    assertEquals(caseStatusLookupDetail.getContent().get(0), lookupValue);
  }

  @Test
  void getCopyCaseStatus_handlesNullResponse() {

    when(lookupService.getCaseStatusValues(Boolean.TRUE)).thenReturn(Mono.empty());

    CaseStatusLookupValueDetail lookupValue = applicationService.getCopyCaseStatus();

    assertNull(lookupValue);
  }

  @Test
  void createApplication_success() throws ParseException {
    ApplicationFormData applicationFormData = buildApplicationFormData();
    ClientDetail clientInformation = buildClientDetail();
    UserDetail user = buildUserDetail();

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
  void createApplication_Copy_success() throws ParseException {
    String copyCaseReference = "1111";

    ApplicationFormData applicationFormData = buildApplicationFormData();
    applicationFormData.setCopyCaseReferenceNumber(copyCaseReference);
    ClientDetail clientDetail = buildClientDetail();
    CaseReferenceSummary caseReferenceSummary = buildCaseReferenceSummary();
    UserDetail user = buildUserDetail();

    // Mock everything that is needed to look up the copy Case and map it
    // to an ApplicationDetail.
    CaseDetail soaCase = buildCaseDetail(APP_TYPE_EMERGENCY);
    soaCase.setCaseReferenceNumber(copyCaseReference);
    // Reduce down to a single Proceeding for this test
    soaCase.getApplicationDetails().getProceedings().remove(
        soaCase.getApplicationDetails().getProceedings().size() - 1);

    when(soaApiClient.getCase(copyCaseReference, user.getLoginId(), user.getUserType()))
        .thenReturn(Mono.just(soaCase));

    when(soaApiClient.getCaseReference(user.getLoginId(), user.getUserType()))
        .thenReturn(Mono.just(caseReferenceSummary));

    /* START ApplicationMappingContext */
    CommonLookupValueDetail applicationTypeLookup = new CommonLookupValueDetail();

    uk.gov.laa.ccms.data.model.ProviderDetail providerDetail = buildProviderDetail(
        soaCase.getApplicationDetails().getProviderDetails().getProviderOfficeId(),
        soaCase.getApplicationDetails().getProviderDetails().getFeeEarnerContactId(),
        soaCase.getApplicationDetails().getProviderDetails().getSupervisorContactId());

    CommonLookupDetail matterTypeLookups = new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail().code("mat1").description("mat 1"));

    CommonLookupDetail levelOfServiceLookups = new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail().code("los1").description("los 1"));

    CommonLookupDetail clientInvolvementLookups = new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail().code("ci1").description("ci 1"));

    CommonLookupDetail scopeLimitations = new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail().code("sl1").description("sl 1"));

    PriorAuthorityTypeDetails priorAuthorityTypes =
        buildPriorAuthorityTypeDetails("dataType");

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


    /* START ProceedingMappingContext */
    ProceedingDetail soaProceeding = soaCase.getApplicationDetails().getProceedings().get(0);
    uk.gov.laa.ccms.data.model.ProceedingDetail proceedingLookup =
        new uk.gov.laa.ccms.data.model.ProceedingDetail();
    when(ebsApiClient.getProceeding(soaProceeding.getProceedingType()))
        .thenReturn(Mono.just(proceedingLookup));

    CommonLookupValueDetail proceedingStatusLookup =
        new CommonLookupValueDetail();
    when(lookupService.getProceedingStatus(soaProceeding.getStatus()))
        .thenReturn(Mono.just(proceedingStatusLookup));

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

    /* START CaseOutcomeMappingContext */
    AwardTypeLookupDetail awardTypes = buildAwardTypeLookupDetail(soaCase);

    when(lookupService.getAwardTypes()).thenReturn(Mono.just(awardTypes));

    /* END of mapping contexts */

    ApplicationDetail applicationToCopy = buildApplicationDetail(1, true, new Date());
    when(applicationMapper.toApplicationDetail(any(ApplicationMappingContext.class)))
        .thenReturn(applicationToCopy);

    // Mock out the additional calls for copying the application
    when(lookupService.getCategoryOfLaw(applicationToCopy.getCategoryOfLaw().getId()))
        .thenReturn(Mono.just(new CategoryOfLawLookupValueDetail()));
    when(lookupService.getPersonToCaseRelationships()).thenReturn(
        Mono.just(new RelationshipToCaseLookupDetail()));
    when(copyApplicationMapper.copyApplication(
        eq(applicationToCopy), eq(caseReferenceSummary),
        eq(clientDetail), any(BigDecimal.class), any(BigDecimal.class)))
        .thenReturn(applicationToCopy);

    when(caabApiClient.createApplication(anyString(), any())).thenReturn(Mono.empty());

    // Call the method under test
    Mono<String> applicationMono = applicationService.createApplication(
        applicationFormData, clientDetail, user);

    StepVerifier.create(applicationMono)
        .verifyComplete();

    verify(copyApplicationMapper).copyApplication(
        eq(applicationToCopy), eq(caseReferenceSummary),
        eq(clientDetail), any(BigDecimal.class), any(BigDecimal.class));
  }


  @ParameterizedTest
  @CsvSource({"true, 1, 10, other, false, false, false",
      "true, 10, 1, other, false, false, false",
      "false, 10, 1, other, false, false, false",
      "false, 10, 1, other, false, true, false",
      "false, 10, 1, " + OPPONENT_TYPE_INDIVIDUAL + ", true, true, false",
      "false, 10, 1, " + OPPONENT_TYPE_INDIVIDUAL + ", false, true, true"})
  void testCopyApplication(
      Boolean copyCostLimit,
      BigDecimal costLimit1,
      BigDecimal costLimit2,
      String opponentType,
      Boolean opponentShared,
      Boolean opponentRelCopyParty,
      Boolean opponentEbsIdCleared) {
    // Now add mocks for the actual copying.

    // Build the application to copy from
    ApplicationDetail applicationToCopy = buildApplicationDetail(2, Boolean.FALSE, new Date());
    UserDetail userDetail = buildUserDetail();

    ClientDetail clientDetail = buildClientDetail();
    CaseReferenceSummary caseReferenceSummary = buildCaseReferenceSummary();

    // Update the cost limitations for this test
    applicationToCopy.getProceedings().get(0).setCostLimitation(costLimit1);
    applicationToCopy.getProceedings().get(1).setCostLimitation(costLimit2);

    // Update the opponent type for this test
    applicationToCopy.getOpponents().get(0).setType(opponentType);
    applicationToCopy.getOpponents().get(0).setSharedInd(opponentShared);

    when(soaApiClient.getCaseReference(userDetail.getLoginId(), userDetail.getUserType()))
        .thenReturn(Mono.just(caseReferenceSummary));

    CategoryOfLawLookupValueDetail categoryOfLawLookupValueDetail =
        buildCategoryOfLawLookupValueDetail(copyCostLimit);
    // Update the category of law lookup for this test
    categoryOfLawLookupValueDetail.setCopyCostLimit(copyCostLimit);

    when(lookupService.getCategoryOfLaw(applicationToCopy.getCategoryOfLaw().getId()))
        .thenReturn(Mono.just(categoryOfLawLookupValueDetail));

    RelationshipToCaseLookupDetail relationshipToCaseLookupDetail =
        buildRelationshipToCaseLookupDetail();
    // Update the relationshipToCase lookup for this test
    relationshipToCaseLookupDetail.getContent().get(0).setCopyParty(opponentRelCopyParty);

    when(lookupService.getPersonToCaseRelationships()).thenReturn(
        Mono.just(relationshipToCaseLookupDetail));

    // If the category of law has copyCostLimit set to TRUE the requested cost
    // limit from the applicationToCopy's costs should be used.
    BigDecimal expectedRequestedCostLimit =
        copyCostLimit ? applicationToCopy.getCosts().getRequestedCostLimitation() : BigDecimal.ZERO;

    // Get the max cost limitation
    BigDecimal expectedDefaultCostLimit = costLimit1.max(costLimit2);

    when(copyApplicationMapper.copyApplication(
        applicationToCopy, caseReferenceSummary, clientDetail,
        expectedRequestedCostLimit, expectedDefaultCostLimit))
        .thenReturn(applicationToCopy);


    /* Call the method under test */
    Mono<ApplicationDetail> result =
        applicationService.copyApplication(applicationToCopy, clientDetail, userDetail);

    // If the opponent is an INDIVIDUAL, it is not shared, and the opponents relationship
    // to the case is set to 'Copy Party' then the ebsId should be null.
    StepVerifier.create(result)
        .expectNextMatches(applicationDetail ->
            opponentEbsIdCleared == (applicationDetail.getOpponents().get(0).getEbsId() == null))
        .verifyComplete();

    verify(copyApplicationMapper).copyApplication(
        applicationToCopy, caseReferenceSummary, clientDetail,
        expectedRequestedCostLimit, expectedDefaultCostLimit);
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

    ApplicationDetail mockApplicationDetail = new ApplicationDetail();
    mockApplicationDetail.setProviderDetails(new ApplicationProviderDetails());
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

    ApplicationDetail mockApplicationDetail = new ApplicationDetail();
    mockApplicationDetail.setProviderDetails(new ApplicationProviderDetails());
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
    final String id = "12345";
    final ApplicationFormData mockApplicationFormData = new ApplicationFormData();
    final ApplicationType applicationType = new ApplicationType();
    // Set up any necessary mocks for caabApiClient.getApplicationType

    when(caabApiClient.getApplicationType(id))
        .thenReturn(Mono.just(applicationType));
    when(applicationFormDataMapper.toApplicationTypeFormData(applicationType))
        .thenReturn(mockApplicationFormData);

    final ApplicationFormData result = applicationService.getApplicationTypeFormData(id);

    assertEquals(mockApplicationFormData, result);
  }

  @Test
  void testGetProviderDetailsFormData() {
    final String id = "12345";
    final ApplicationFormData mockApplicationFormData = new ApplicationFormData();
    final ApplicationProviderDetails providerDetails = new ApplicationProviderDetails();

    when(caabApiClient.getProviderDetails(id))
        .thenReturn(Mono.just(providerDetails));
    when(applicationFormDataMapper.toApplicationProviderDetailsFormData(providerDetails))
        .thenReturn(mockApplicationFormData);

    final ApplicationFormData result = applicationService.getProviderDetailsFormData(id);

    assertEquals(mockApplicationFormData, result);
  }

  @Test
  void testGetCorrespondenceAddressFormData() {
    final String id = "12345";
    final Address mockAddress = new Address();
    final AddressFormData expectedAddressFormData = new AddressFormData();

    when(caabApiClient.getCorrespondenceAddress(id)).thenReturn(Mono.just(mockAddress));

    when(addressFormDataMapper.toAddressFormData(mockAddress)).thenReturn(expectedAddressFormData);

    final AddressFormData result = applicationService.getCorrespondenceAddressFormData(id);

    assertNotNull(result);
    assertEquals(expectedAddressFormData, result);

    verify(caabApiClient).getCorrespondenceAddress(id);
    verify(addressFormDataMapper).toAddressFormData(mockAddress);
  }

  @Test
  void testGetLinkedCases() {
    final String id = "12345";
    final List<LinkedCase> mockLinkedCases = Arrays.asList(new LinkedCase(), new LinkedCase());
    final List<LinkedCaseResultRowDisplay> expectedLinkedCaseDisplays = Arrays.asList(
        new LinkedCaseResultRowDisplay(), new LinkedCaseResultRowDisplay());

    when(caabApiClient.getLinkedCases(id)).thenReturn(Mono.just(mockLinkedCases));

    IntStream.range(0, mockLinkedCases.size())
        .forEach(i -> when(resultDisplayMapper.toLinkedCaseResultRowDisplay(mockLinkedCases.get(i)))
            .thenReturn(expectedLinkedCaseDisplays.get(i)));

    final ResultsDisplay<LinkedCaseResultRowDisplay> result = applicationService.getLinkedCases(id);

    assertNotNull(result);
    assertEquals(expectedLinkedCaseDisplays, result.getContent());

    verify(caabApiClient).getLinkedCases(id);
  }

  @Test
  void removeLinkedCase_success() {
    final String primaryCaseId = "12345";
    final String linkedCaseId = "67890";
    final UserDetail user = new UserDetail().loginId("userLoginId");

    when(caabApiClient.removeLinkedCase(primaryCaseId, linkedCaseId, user.getLoginId()))
        .thenReturn(Mono.empty());

    applicationService.removeLinkedCase(primaryCaseId, linkedCaseId, user);

    verify(caabApiClient).removeLinkedCase(primaryCaseId, linkedCaseId, user.getLoginId());
  }

  @Test
  void updateLinkedCase_success() {
    final String id = "primaryCaseId";
    final String linkedCaseId = "linkedCaseId";
    final LinkedCaseResultRowDisplay data = new LinkedCaseResultRowDisplay();
    final UserDetail user = new UserDetail().loginId("userLoginId");
    final LinkedCase linkedCase = new LinkedCase();

    when(resultDisplayMapper.toLinkedCase(data)).thenReturn(linkedCase);
    when(caabApiClient.updateLinkedCase(id, linkedCaseId, linkedCase, user.getLoginId())).thenReturn(Mono.empty());

    applicationService.updateLinkedCase(id, linkedCaseId, data, user);

    verify(resultDisplayMapper).toLinkedCase(data);
    verify(caabApiClient).updateLinkedCase(id, linkedCaseId, linkedCase, user.getLoginId());
  }

  @Test
  void updateCorrespondenceAddress_success() {
    final String id = "applicationId";
    final AddressFormData addressFormData = new AddressFormData();
    final UserDetail user = new UserDetail().loginId("userLoginId");
    final Address correspondenceAddress = new Address();

    when(addressFormDataMapper.toAddress(addressFormData)).thenReturn(correspondenceAddress);
    when(caabApiClient.putApplication(
        id,
        user.getLoginId(),
        correspondenceAddress,
        "correspondence-address")).thenReturn(Mono.empty());

    applicationService.updateCorrespondenceAddress(id, addressFormData, user);

    verify(addressFormDataMapper).toAddress(addressFormData);
    verify(caabApiClient).putApplication(
        id,
        user.getLoginId(),
        correspondenceAddress,
        "correspondence-address");
  }

  @Test
  void testPatchApplicationType() throws ParseException {
    final String id = "12345";
    final ApplicationFormData applicationFormData = new ApplicationFormData();
    final UserDetail user = new UserDetail().loginId("TEST123");

    when(caabApiClient.putApplication(eq(id), eq(user.getLoginId()), any(), eq("application-type")))
        .thenReturn(Mono.empty());

    applicationService.updateApplicationType(id, applicationFormData, user);

    verify(caabApiClient).putApplication(eq(id), eq(user.getLoginId()), any(), eq("application-type"));

  }

  @Test
  void testBuildCaseOutcomeMappingContext() {
    final CaseDetail soaCase = buildCaseDetail("anytype");
    final List<ProceedingMappingContext> proceedingMappingContexts = Collections.singletonList(
        ProceedingMappingContext.builder().build());

    final AwardTypeLookupDetail awardTypes = buildAwardTypeLookupDetail(soaCase);

    when(lookupService.getAwardTypes()).thenReturn(Mono.just(awardTypes));

    final CaseOutcomeMappingContext result = applicationService.buildCaseOutcomeMappingContext(
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

    AwardTypeLookupDetail awardTypes = buildAwardTypeLookupDetail(soaCase);
    // Drop out one of the award types
    awardTypes.getContent().remove(awardTypes.getContent().size() - 1);

    when(lookupService.getAwardTypes()).thenReturn(Mono.just(awardTypes));

    Exception e = assertThrows(CaabApplicationException.class, () ->
        applicationService.buildCaseOutcomeMappingContext(soaCase, proceedingMappingContexts));

    assertEquals(String.format("Failed to find AwardType with code: %s",
        soaCase.getAwards().get(3).getAwardType()), e.getMessage());
  }

  @Test
  void testBuildPriorAuthorityMappingContext_LovLookup() {
    PriorAuthority soaPriorAuthority = buildPriorAuthority();

    PriorAuthorityTypeDetails priorAuthorityTypeDetails =
        buildPriorAuthorityTypeDetails(REFERENCE_DATA_ITEM_TYPE_LOV);
    PriorAuthorityDetail priorAuthoritiesItem = priorAuthorityTypeDetails.getContent().get(0)
        .getPriorAuthorities().get(0);

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

    PriorAuthorityTypeDetails priorAuthorityTypeDetails =
        buildPriorAuthorityTypeDetails("otherDataType");

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
    assertEquals(priorAuthorityTypeDetails.getContent().get(0).getPriorAuthorities().get(0),
        result.getItems().get(0).getKey());
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
   ProceedingMappingContext result =
        applicationService.buildProceedingMappingContext(
          soaProceeding,
          soaCase,
          matterTypeLookups,
          levelOfServiceLookups,
          clientInvolvementLookups,
          scopeLimitationLookups);

//    StepVerifier.create(proceedingMappingContextMono)
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
//          return true; // Return true to indicate the match is successful
//        })
//        .verifyComplete();
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
    final ProceedingDetail soaProceeding = buildProceedingDetail(STATUS_DRAFT);
    soaProceeding.setOutcome(null);

    applicationService.addProceedingOutcomeContext(
        ProceedingMappingContext.builder(), soaProceeding);

    verifyNoInteractions(lookupService);
  }

  @Test
  void testBuildApplicationMappingContext_DevolvedPowersAllDraftProceedings() {
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

    uk.gov.laa.ccms.data.model.ProviderDetail providerDetail = buildProviderDetail(
        soaCase.getApplicationDetails().getProviderDetails().getProviderOfficeId(),
        soaCase.getApplicationDetails().getProviderDetails().getFeeEarnerContactId(),
        soaCase.getApplicationDetails().getProviderDetails().getSupervisorContactId());

    CommonLookupDetail matterTypeLookups = new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail().code("mat1").description("mat 1"));

    CommonLookupDetail levelOfServiceLookups = new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail().code("los1").description("los 1"));

    CommonLookupDetail clientInvolvementLookups = new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail().code("ci1").description("ci 1"));

    CommonLookupDetail scopeLimitations = new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail().code("sl1").description("sl 1"));

    PriorAuthorityTypeDetails priorAuthorityTypes =
        buildPriorAuthorityTypeDetails("dataType");

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

    ApplicationMappingContext result =
        applicationService.buildApplicationMappingContext(soaCase);

    assertNotNull(result);
    assertEquals(soaCase, result.getSoaCaseDetail());
    assertEquals(applicationTypeLookup, result.getApplicationType());
    assertEquals(providerDetail, result.getProviderDetail());
    assertEquals(providerDetail.getOffices().get(2), result.getProviderOffice());
    assertEquals(providerDetail.getOffices().get(2).getFeeEarners().get(0),
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

    uk.gov.laa.ccms.data.model.ProviderDetail providerDetail = buildProviderDetail(
        soaCase.getApplicationDetails().getProviderDetails().getProviderOfficeId(),
        soaCase.getApplicationDetails().getProviderDetails().getFeeEarnerContactId(),
        soaCase.getApplicationDetails().getProviderDetails().getSupervisorContactId());

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

    final ApplicationMappingContext result =
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
  void getProceedings_ReturnsListOfProceedings_Successful() {
    final String applicationId = "app123";
    final List<Proceeding> mockProceedings = Arrays.asList(new Proceeding(), new Proceeding());

    when(caabApiClient.getProceedings(applicationId)).thenReturn(Mono.just(mockProceedings));

    final Mono<ResultsDisplay<Proceeding>> resultMono = applicationService.getProceedings(applicationId);

    StepVerifier.create(resultMono)
        .assertNext(resultsDisplay -> {
          assertNotNull(resultsDisplay.getContent());
          assertEquals(mockProceedings.size(), resultsDisplay.getContent().size());
          assertEquals(mockProceedings, resultsDisplay.getContent());
        })
        .verifyComplete();
  }

  @Test
  void getCosts_ReturnsCostStructure_Successful() {
    final String applicationId = "app123";
    final CostStructure mockCostStructure = new CostStructure();

    when(caabApiClient.getCosts(applicationId)).thenReturn(Mono.just(mockCostStructure));

    final Mono<CostStructure> resultMono = applicationService.getCosts(applicationId);

    StepVerifier.create(resultMono)
        .assertNext(costStructure -> assertEquals(mockCostStructure, costStructure))
        .verifyComplete();
  }

  @Test
  void getPriorAuthorities_ReturnsListOfPriorAuthorities_Successful() {
    final String applicationId = "app123";
    final List<uk.gov.laa.ccms.caab.model.PriorAuthority> mockPriorAuthorities =
        Arrays.asList(new uk.gov.laa.ccms.caab.model.PriorAuthority(), new uk.gov.laa.ccms.caab.model.PriorAuthority());

    when(caabApiClient.getPriorAuthorities(applicationId)).thenReturn(Mono.just(mockPriorAuthorities));

    final Mono<ResultsDisplay<uk.gov.laa.ccms.caab.model.PriorAuthority>> resultMono =
        applicationService.getPriorAuthorities(applicationId);

    StepVerifier.create(resultMono)
        .assertNext(resultsDisplay -> {
          assertNotNull(resultsDisplay.getContent());
          assertEquals(mockPriorAuthorities.size(), resultsDisplay.getContent().size());
          assertEquals(mockPriorAuthorities, resultsDisplay.getContent());
        })
        .verifyComplete();
  }

  @Test
  void makeLeadProceeding_UpdatesLeadProceeding_Successful() {
    final String applicationId = "app123";
    final Integer newLeadProceedingId = 2;
    final UserDetail user = new UserDetail().loginId("user1");

    final List<Proceeding> mockProceedings = Arrays.asList(
        new Proceeding().id(1).leadProceedingInd(true),
        new Proceeding().id(2).leadProceedingInd(false)
    );

    when(caabApiClient.getProceedings(applicationId)).thenReturn(Mono.just(mockProceedings));
    when(caabApiClient.updateProceeding(anyInt(), any(Proceeding.class), anyString()))
        .thenReturn(Mono.empty());

    applicationService.makeLeadProceeding(applicationId, newLeadProceedingId, user);

    verify(caabApiClient).updateProceeding(eq(1), any(Proceeding.class), eq(user.getLoginId()));
    verify(caabApiClient).updateProceeding(eq(2), any(Proceeding.class), eq(user.getLoginId()));
  }


  private ApplicationFormData buildApplicationFormData() {
    final ApplicationFormData applicationFormData = new ApplicationFormData();
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
    final String clientReferenceNumber = "12345";
    return new ClientDetail()
        .clientReferenceNumber(clientReferenceNumber)
        .details(new ClientDetailDetails()
            .name(new NameDetail()))
        .recordHistory(new RecordHistory());
  }

}
