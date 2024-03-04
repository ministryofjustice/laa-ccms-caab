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
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_INDIVIDUAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_ORGANISATION;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.REFERENCE_DATA_ITEM_TYPE_LOV;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildApplicationDetail;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildOpponent;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.OpponentFormData;
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
import uk.gov.laa.ccms.caab.mapper.OpponentMapper;
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
import uk.gov.laa.ccms.caab.model.Opponent;
import uk.gov.laa.ccms.caab.model.OpponentRowDisplay;
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
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.ProviderDetail;
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
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.PriorAuthority;
import uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail;

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
  private OpponentMapper opponentMapper;

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
        eq(applicationToCopy),
        eq(caseReferenceSummary.getCaseReferenceNumber()),
        any(StringDisplayValue.class),
        eq(clientDetail),
        any(BigDecimal.class),
        any(BigDecimal.class)))
        .thenReturn(applicationToCopy);

    when(caabApiClient.createApplication(anyString(), any())).thenReturn(Mono.empty());

    // Call the method under test
    Mono<String> applicationMono = applicationService.createApplication(
        applicationFormData, clientDetail, user);

    StepVerifier.create(applicationMono)
        .verifyComplete();

    verify(copyApplicationMapper).copyApplication(
        eq(applicationToCopy),
        eq(caseReferenceSummary.getCaseReferenceNumber()),
        any(StringDisplayValue.class),
        eq(clientDetail),
        any(BigDecimal.class),
        any(BigDecimal.class));
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

    StringDisplayValue initialStatus = new StringDisplayValue()
        .id(STATUS_UNSUBMITTED_ACTUAL_VALUE)
        .displayValue(STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY);

    when(copyApplicationMapper.copyApplication(
        applicationToCopy,
        caseReferenceSummary.getCaseReferenceNumber(),
        initialStatus,
        clientDetail,
        expectedRequestedCostLimit,
        expectedDefaultCostLimit))
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
        applicationToCopy,
        caseReferenceSummary.getCaseReferenceNumber(),
        initialStatus,
        clientDetail,
        expectedRequestedCostLimit,
        expectedDefaultCostLimit);
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
    when(lookupService.getOrganisationToCaseRelationships()).thenReturn(
        Mono.just(orgRelationshipsDetail));

    when(lookupService.getPersonToCaseRelationships()).thenReturn(
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
    when(lookupService.getOrganisationToCaseRelationships()).thenReturn(
        Mono.just(orgRelationshipsDetail));

    when(lookupService.getPersonToCaseRelationships()).thenReturn(
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
    final String linkedCaseId = "67890";
    final UserDetail user = new UserDetail().loginId("userLoginId");

    when(caabApiClient.removeLinkedCase(linkedCaseId, user.getLoginId()))
        .thenReturn(Mono.empty());

    applicationService.removeLinkedCase(linkedCaseId, user);

    verify(caabApiClient).removeLinkedCase(linkedCaseId, user.getLoginId());
  }

  @Test
  void updateLinkedCase_success() {
    final String linkedCaseId = "linkedCaseId";
    final LinkedCaseResultRowDisplay data = new LinkedCaseResultRowDisplay();
    final UserDetail user = new UserDetail().loginId("userLoginId");
    final LinkedCase linkedCase = new LinkedCase();

    when(resultDisplayMapper.toLinkedCase(data)).thenReturn(linkedCase);
    when(caabApiClient.updateLinkedCase(linkedCaseId, linkedCase, user.getLoginId())).thenReturn(Mono.empty());

    applicationService.updateLinkedCase(linkedCaseId, data, user);

    verify(resultDisplayMapper).toLinkedCase(data);
    verify(caabApiClient).updateLinkedCase(linkedCaseId, linkedCase, user.getLoginId());
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

  @Test
  void testToIndividualOpponentPartyName_buildsFullName() {
    Opponent opponent = buildOpponent(new Date());

    CommonLookupDetail contactTitles = new CommonLookupDetail();
    CommonLookupValueDetail title = new CommonLookupValueDetail()
        .code(opponent.getTitle())
        .description("test");
    contactTitles.addContentItem(title);

    String fullName =
        applicationService.toIndividualOpponentPartyName(opponent, contactTitles);

    assertNotNull(fullName);
    String expectedResult = title.getDescription() + " " + opponent.getFirstName() + " " + opponent.getSurname();
    assertEquals(expectedResult, fullName);
  }

  @Test
  void testToIndividualOpponentPartyName_noTitleMatchReturnsCode() {
    Opponent opponent = buildOpponent(new Date());

    CommonLookupDetail contactTitles = new CommonLookupDetail();

    String fullName =
        applicationService.toIndividualOpponentPartyName(opponent, contactTitles);

    assertNotNull(fullName);
    String expectedResult = opponent.getTitle() + " " + opponent.getFirstName() + " " + opponent.getSurname();
    assertEquals(expectedResult, fullName);
  }

  @Test
  void testToIndividualOpponentPartyName_noNameElementsReturnsUndefined() {
    Opponent opponent = new Opponent();

    CommonLookupDetail contactTitles = new CommonLookupDetail();
    CommonLookupValueDetail title = new CommonLookupValueDetail()
        .code(opponent.getTitle())
        .description("test");
    contactTitles.addContentItem(title);

    String fullName =
        applicationService.toIndividualOpponentPartyName(opponent, contactTitles);

    assertNotNull(fullName);
    String expectedResult = "undefined";
    assertEquals(expectedResult, fullName);
  }

  @Test
  void testToIndividualOpponentPartyName_noFirstnameReturnsCorrectly() {
    Opponent opponent = buildOpponent(new Date());
    opponent.setFirstName(null);

    CommonLookupDetail contactTitles = new CommonLookupDetail();

    String fullName =
        applicationService.toIndividualOpponentPartyName(opponent, contactTitles);

    assertNotNull(fullName);
    String expectedResult = opponent.getTitle() + " " + opponent.getSurname();
    assertEquals(expectedResult, fullName);
  }

  @Test
  void testToIndividualOpponentPartyName_noSurnameReturnsCorrectly() {
    Opponent opponent = buildOpponent(new Date());
    opponent.setSurname(null);

    CommonLookupDetail contactTitles = new CommonLookupDetail();

    String fullName =
        applicationService.toIndividualOpponentPartyName(opponent, contactTitles);

    assertNotNull(fullName);
    String expectedResult = opponent.getTitle() + " " + opponent.getFirstName();
    assertEquals(expectedResult, fullName);
  }

  @Test
  void testToIndividualOpponentPartyName_noFirstnameSurnameReturnsTitleonly() {
    Opponent opponent = buildOpponent(new Date());
    opponent.setFirstName(null);
    opponent.setSurname(null);

    CommonLookupDetail contactTitles = new CommonLookupDetail();

    String fullName =
        applicationService.toIndividualOpponentPartyName(opponent, contactTitles);

    assertNotNull(fullName);
    String expectedResult = opponent.getTitle();
    assertEquals(expectedResult, fullName);
  }

  @Test
  void testBuildIndividualOpponentRowDisplay_noLookupMatchReturnsCodes() {
    Opponent opponent = buildOpponent(new Date());
    opponent.setType(OPPONENT_TYPE_INDIVIDUAL);

    OpponentRowDisplay result = applicationService.buildOpponentRowDisplay(
        opponent,
        new CommonLookupDetail(),
        new RelationshipToCaseLookupDetail(),
        new RelationshipToCaseLookupDetail(),
        new CommonLookupDetail());

    String expectedPartyName =
        opponent.getTitle() + " " + opponent.getFirstName() + " " + opponent.getSurname();

    assertNotNull(result);
    assertEquals(opponent.getId(), result.getId());
    assertEquals(expectedPartyName, result.getPartyName());
    assertEquals(opponent.getType(), result.getPartyType());
    assertEquals(opponent.getRelationshipToCase(), result.getRelationshipToCase());
    assertEquals(opponent.getRelationshipToClient(), result.getRelationshipToClient());
  }

  @Test
  void testBuildOrgOpponentRowDisplay_ReturnsOrganisationName() {
    Opponent opponent = buildOpponent(new Date());
    opponent.setType(OPPONENT_TYPE_ORGANISATION);

    OpponentRowDisplay result = applicationService.buildOpponentRowDisplay(
        opponent,
        new CommonLookupDetail(),
        new RelationshipToCaseLookupDetail(),
        new RelationshipToCaseLookupDetail(),
        new CommonLookupDetail());

    String expectedPartyName =
        opponent.getOrganisationName();

    assertNotNull(result);
    assertEquals(opponent.getId(), result.getId());
    assertEquals(expectedPartyName, result.getPartyName());
    assertEquals(opponent.getType(), result.getPartyType());
    assertEquals(opponent.getRelationshipToCase(), result.getRelationshipToCase());
    assertEquals(opponent.getRelationshipToClient(), result.getRelationshipToClient());
  }

  @Test
  void testBuildOrgOpponentRowDisplay_lookupMatchesReturnDisplayValues() {
    Opponent opponent = buildOpponent(new Date());
    opponent.setType(OPPONENT_TYPE_ORGANISATION);

    RelationshipToCaseLookupValueDetail orgRelationshipToCase =
        new RelationshipToCaseLookupValueDetail()
            .code(opponent.getRelationshipToCase())
            .description("org rel");
    RelationshipToCaseLookupDetail orgRelationshipsToCase =
        new RelationshipToCaseLookupDetail()
            .addContentItem(orgRelationshipToCase);

    CommonLookupValueDetail relationshipToClient = new CommonLookupValueDetail()
        .code(opponent.getRelationshipToClient())
        .description("rel 2 client");
    CommonLookupDetail relationshipsToClient = new CommonLookupDetail()
        .addContentItem(relationshipToClient);

    OpponentRowDisplay result = applicationService.buildOpponentRowDisplay(
        opponent,
        new CommonLookupDetail(),
        new RelationshipToCaseLookupDetail(),
        orgRelationshipsToCase,
        relationshipsToClient);

    String expectedPartyName =
        opponent.getOrganisationName();

    assertNotNull(result);
    assertEquals(opponent.getId(), result.getId());
    assertEquals(expectedPartyName, result.getPartyName());
    assertEquals(opponent.getType(), result.getPartyType());
    assertEquals(orgRelationshipToCase.getDescription(), result.getRelationshipToCase());
    assertEquals(relationshipToClient.getDescription(), result.getRelationshipToClient());
  }

  @Test
  void testBuildIndividualOpponentRowDisplay_lookupMatchesReturnDisplayValues() {
    Opponent opponent = buildOpponent(new Date());
    opponent.setType(OPPONENT_TYPE_INDIVIDUAL);

    RelationshipToCaseLookupValueDetail personRelationshipToCase =
        new RelationshipToCaseLookupValueDetail()
            .code(opponent.getRelationshipToCase())
            .description("ind rel");
    RelationshipToCaseLookupDetail personRelationshipsToCase =
        new RelationshipToCaseLookupDetail()
            .addContentItem(personRelationshipToCase);

    CommonLookupValueDetail relationshipToClient = new CommonLookupValueDetail()
        .code(opponent.getRelationshipToClient())
        .description("rel 2 client");
    CommonLookupDetail relationshipsToClient = new CommonLookupDetail()
        .addContentItem(relationshipToClient);

    OpponentRowDisplay result = applicationService.buildOpponentRowDisplay(
        opponent,
        new CommonLookupDetail(),
        personRelationshipsToCase,
        new RelationshipToCaseLookupDetail(),
        relationshipsToClient);

    String expectedPartyName =
        opponent.getTitle() + " " + opponent.getFirstName() + " " + opponent.getSurname();

    assertNotNull(result);
    assertEquals(opponent.getId(), result.getId());
    assertEquals(expectedPartyName, result.getPartyName());
    assertEquals(opponent.getType(), result.getPartyType());
    assertEquals(personRelationshipToCase.getDescription(), result.getRelationshipToCase());
    assertEquals(relationshipToClient.getDescription(), result.getRelationshipToClient());
  }

  @Test
  void testGetOpponents_queriesLookupData() {
    final String applicationId = "123";
    Opponent opponent = buildOpponent(new Date());

    when(lookupService.getContactTitles()).thenReturn(Mono.just(new CommonLookupDetail()));
    when(lookupService.getPersonToCaseRelationships()).thenReturn(Mono.just(new RelationshipToCaseLookupDetail()));
    when(lookupService.getOrganisationToCaseRelationships()).thenReturn(Mono.just(new RelationshipToCaseLookupDetail()));
    when(lookupService.getRelationshipsToClient()).thenReturn(Mono.just(new CommonLookupDetail()));

    when(caabApiClient.getOpponents(applicationId)).thenReturn(Mono.just(List.of(opponent)));
    ResultsDisplay<OpponentRowDisplay> result =
        applicationService.getOpponents(applicationId);

    assertNotNull(result);
    assertEquals(1, result.getContent().size());

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
  @Test
  void getApplication_returnsApplicationDetail_Successful() {
    final String applicationId = "12345";
    final ApplicationDetail mockApplicationDetail = new ApplicationDetail();

    when(caabApiClient.getApplication(applicationId)).thenReturn(Mono.just(mockApplicationDetail));

    final Mono<ApplicationDetail> applicationDetailMono = applicationService.getApplication(applicationId);

    StepVerifier.create(applicationDetailMono)
        .expectNextMatches(applicationDetail -> applicationDetail == mockApplicationDetail)
        .verifyComplete();
  }

  @Test
  void getDefaultScopeLimitation_withEmergencyApplicationType_returnsEmergencyScopeLimitations() {
    String categoryOfLaw = "Family";
    String matterType = "FAM";
    String proceedingCode = "PC001";
    String levelOfService = "3";
    String applicationType = APP_TYPE_EMERGENCY; // Assume this is one of the emergency application type codes

    ScopeLimitationDetail criteria = new ScopeLimitationDetail()
        .categoryOfLaw(categoryOfLaw)
        .matterType(matterType)
        .proceedingCode(proceedingCode)
        .levelOfService(levelOfService)
        .emergency(true)
        .emergencyScopeDefault(true);

    ScopeLimitationDetails mockScopeLimitationDetails = new ScopeLimitationDetails();

    when(lookupService.getScopeLimitationDetails(criteria)).thenReturn(Mono.just(mockScopeLimitationDetails));

    Mono<ScopeLimitationDetails> result = applicationService.getDefaultScopeLimitation(
        categoryOfLaw, matterType, proceedingCode, levelOfService, applicationType);

    StepVerifier.create(result)
        .expectNextMatches(scopeLimitationDetails -> scopeLimitationDetails == mockScopeLimitationDetails)
        .verifyComplete();
  }

  @Test
  void getDefaultScopeLimitation_withSubstantiveDevolvedPowersApplicationType_returnsEmergencyScopeLimitations() {
    String categoryOfLaw = "Criminal";
    String matterType = "CRM";
    String proceedingCode = "PC002";
    String levelOfService = "2";
    String applicationType = APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS;

    ScopeLimitationDetail criteria = new ScopeLimitationDetail()
        .categoryOfLaw(categoryOfLaw)
        .matterType(matterType)
        .proceedingCode(proceedingCode)
        .levelOfService(levelOfService)
        .emergency(true)
        .emergencyScopeDefault(true);

    ScopeLimitationDetails mockScopeLimitationDetails = new ScopeLimitationDetails();

    when(lookupService.getScopeLimitationDetails(criteria)).thenReturn(Mono.just(mockScopeLimitationDetails));

    Mono<ScopeLimitationDetails> result = applicationService.getDefaultScopeLimitation(
        categoryOfLaw, matterType, proceedingCode, levelOfService, applicationType);

    StepVerifier.create(result)
        .expectNextMatches(scopeLimitationDetails -> scopeLimitationDetails == mockScopeLimitationDetails)
        .verifyComplete();
  }

  @Test
  void getDefaultScopeLimitation_withNonEmergencyApplicationType_returnsDefaultScopeLimitations() {
    String categoryOfLaw = "Housing";
    String matterType = "HOU";
    String proceedingCode = "PC003";
    String levelOfService = "1";
    String applicationType = APP_TYPE_SUBSTANTIVE; // Assume this is a non-emergency application type

    ScopeLimitationDetail criteria = new ScopeLimitationDetail()
        .categoryOfLaw(categoryOfLaw)
        .matterType(matterType)
        .proceedingCode(proceedingCode)
        .levelOfService(levelOfService)
        .scopeDefault(true);

    ScopeLimitationDetails mockScopeLimitationDetails = new ScopeLimitationDetails();

    when(lookupService.getScopeLimitationDetails(criteria)).thenReturn(Mono.just(mockScopeLimitationDetails));

    Mono<ScopeLimitationDetails> result = applicationService.getDefaultScopeLimitation(
        categoryOfLaw, matterType, proceedingCode, levelOfService, applicationType);

    StepVerifier.create(result)
        .expectNextMatches(scopeLimitationDetails -> scopeLimitationDetails == mockScopeLimitationDetails)
        .verifyComplete();
  }

  @Test
  void getProceedingCostLimitation_withEmergencyApplicationType_returnsMaxCostLimitation() {
    String categoryOfLaw = "Family";
    String matterType = "FAM";
    String proceedingCode = "PC001";
    String levelOfService = "3";
    String applicationType = APP_TYPE_EMERGENCY; // Assume this is one of the emergency application type codes
    List<uk.gov.laa.ccms.caab.model.ScopeLimitation> scopeLimitations = List.of(
        new uk.gov.laa.ccms.caab.model.ScopeLimitation().scopeLimitation(new StringDisplayValue().id("SL1")),
        new uk.gov.laa.ccms.caab.model.ScopeLimitation().scopeLimitation(new StringDisplayValue().id("SL2"))
    );

    ScopeLimitationDetails mockScopeLimitationDetails1 = new ScopeLimitationDetails()
        .addContentItem(new ScopeLimitationDetail().costLimitation(new BigDecimal(500)));
    ScopeLimitationDetails mockScopeLimitationDetails2 = new ScopeLimitationDetails()
        .addContentItem(new ScopeLimitationDetail().costLimitation(new BigDecimal(1000)));

    when(lookupService.getScopeLimitationDetails(any(ScopeLimitationDetail.class)))
        .thenReturn(Mono.just(mockScopeLimitationDetails1), Mono.just(mockScopeLimitationDetails2));

    BigDecimal result = applicationService.getProceedingCostLimitation(
        categoryOfLaw, matterType, proceedingCode, levelOfService, applicationType, scopeLimitations);

    assertEquals(new BigDecimal(1000).setScale(2), result.setScale(2));
  }

  @Test
  void getProceedingCostLimitation_withNonEmergencyApplicationType_returnsMaxCostLimitation() {
    String categoryOfLaw = "Housing";
    String matterType = "HOU";
    String proceedingCode = "PC003";
    String levelOfService = "1";
    String applicationType = APP_TYPE_SUBSTANTIVE;
    List<uk.gov.laa.ccms.caab.model.ScopeLimitation> scopeLimitations = List.of(
        new uk.gov.laa.ccms.caab.model.ScopeLimitation().scopeLimitation(new StringDisplayValue().id("SL1")),
        new uk.gov.laa.ccms.caab.model.ScopeLimitation().scopeLimitation(new StringDisplayValue().id("SL2"))
    );

    ScopeLimitationDetails mockScopeLimitationDetails1 = new ScopeLimitationDetails()
        .addContentItem(new ScopeLimitationDetail().costLimitation(new BigDecimal(300)));
    ScopeLimitationDetails mockScopeLimitationDetails2 = new ScopeLimitationDetails()
        .addContentItem(new ScopeLimitationDetail().costLimitation(new BigDecimal(800)));

    when(lookupService.getScopeLimitationDetails(any(ScopeLimitationDetail.class)))
        .thenReturn(Mono.just(mockScopeLimitationDetails1), Mono.just(mockScopeLimitationDetails2));

    BigDecimal result = applicationService.getProceedingCostLimitation(
        categoryOfLaw, matterType, proceedingCode, levelOfService, applicationType, scopeLimitations);

    assertEquals(new BigDecimal(800).setScale(2), result.setScale(2));
  }

  @Test
  void getProceedingCostLimitation_withEmptyScopeLimitations_returnsZero() {
    String categoryOfLaw = "Immigration";
    String matterType = "IMG";
    String proceedingCode = "PC004";
    String levelOfService = "2";
    String applicationType = APP_TYPE_SUBSTANTIVE; // Any non-emergency application type
    List<uk.gov.laa.ccms.caab.model.ScopeLimitation> scopeLimitations = Collections.emptyList();

    BigDecimal result = applicationService.getProceedingCostLimitation(
        categoryOfLaw, matterType, proceedingCode, levelOfService, applicationType, scopeLimitations);

    assertEquals(BigDecimal.ZERO.setScale(2), result.setScale(2));
  }

  @Test
  void getProceedingStage_singleScopeLimitation_returnsMinStage() {
    String categoryOfLaw = "Family";
    String matterType = "FAM";
    String proceedingCode = "PC001";
    String levelOfService = "3";
    boolean isAmendment = false;
    List<uk.gov.laa.ccms.caab.model.ScopeLimitation> scopeLimitations = List.of(
        new uk.gov.laa.ccms.caab.model.ScopeLimitation().scopeLimitation(
            new StringDisplayValue().id("SL1"))
    );

    ScopeLimitationDetails mockScopeLimitationDetails = new ScopeLimitationDetails()
        .addContentItem(new ScopeLimitationDetail().stage(1))
        .addContentItem(new ScopeLimitationDetail().stage(2));

    when(lookupService.getScopeLimitationDetails(any(ScopeLimitationDetail.class)))
        .thenReturn(Mono.just(mockScopeLimitationDetails));

    Integer result = applicationService.getProceedingStage(
        categoryOfLaw, matterType, proceedingCode, levelOfService, scopeLimitations, isAmendment);

    assertEquals(1, result);
  }

  @Test
  void getProceedingStage_multipleScopeLimitationsWithCommonStages_returnsMinCommonStage() {
    String categoryOfLaw = "Criminal";
    String matterType = "CRM";
    String proceedingCode = "PC002";
    String levelOfService = "2";
    boolean isAmendment = false;
    List<uk.gov.laa.ccms.caab.model.ScopeLimitation> scopeLimitations = List.of(
        new uk.gov.laa.ccms.caab.model.ScopeLimitation().scopeLimitation(new StringDisplayValue().id("SL1")),
        new uk.gov.laa.ccms.caab.model.ScopeLimitation().scopeLimitation(new StringDisplayValue().id("SL2"))
    );

    ScopeLimitationDetails mockScopeLimitationDetails1 = new ScopeLimitationDetails()
        .addContentItem(new ScopeLimitationDetail().stage(1))
        .addContentItem(new ScopeLimitationDetail().stage(2));
    ScopeLimitationDetails mockScopeLimitationDetails2 = new ScopeLimitationDetails()
        .addContentItem(new ScopeLimitationDetail().stage(2))
        .addContentItem(new ScopeLimitationDetail().stage(3));

    when(lookupService.getScopeLimitationDetails(any(ScopeLimitationDetail.class)))
        .thenReturn(Mono.just(mockScopeLimitationDetails1), Mono.just(mockScopeLimitationDetails2));

    Integer result = applicationService.getProceedingStage(
        categoryOfLaw, matterType, proceedingCode, levelOfService, scopeLimitations, isAmendment);

    assertEquals(1, result);
  }


  @Test
  void getProceedingStage_multipleScopeLimitationsWithoutCommonStages_returnsMinOfMinStages() {
    String categoryOfLaw = "Housing";
    String matterType = "HOU";
    String proceedingCode = "PC003";
    String levelOfService = "1";
    boolean isAmendment = false;
    List<uk.gov.laa.ccms.caab.model.ScopeLimitation> scopeLimitations = List.of(
        new uk.gov.laa.ccms.caab.model.ScopeLimitation().scopeLimitation(new StringDisplayValue().id("SL1")),
        new uk.gov.laa.ccms.caab.model.ScopeLimitation().scopeLimitation(new StringDisplayValue().id("SL2"))
    );

    ScopeLimitationDetails mockScopeLimitationDetails1 = new ScopeLimitationDetails()
        .addContentItem(new ScopeLimitationDetail().stage(1))
        .addContentItem(new ScopeLimitationDetail().stage(3));
    ScopeLimitationDetails mockScopeLimitationDetails2 = new ScopeLimitationDetails()
        .addContentItem(new ScopeLimitationDetail().stage(4))
        .addContentItem(new ScopeLimitationDetail().stage(5));

    when(lookupService.getScopeLimitationDetails(any(ScopeLimitationDetail.class)))
        .thenReturn(Mono.just(mockScopeLimitationDetails1), Mono.just(mockScopeLimitationDetails2));

    Integer result = applicationService.getProceedingStage(
        categoryOfLaw, matterType, proceedingCode, levelOfService, scopeLimitations, isAmendment);

    assertEquals(1, result);
  }

  @Test
  void getProceedingStage_isAmendmentIgnored_returnsStageBasedOnScopeLimitations() {
    String categoryOfLaw = "Immigration";
    String matterType = "IMG";
    String proceedingCode = "PC004";
    String levelOfService = "4";
    boolean isAmendment = true;
    List<uk.gov.laa.ccms.caab.model.ScopeLimitation> scopeLimitations = List.of(
        new uk.gov.laa.ccms.caab.model.ScopeLimitation().scopeLimitation(new StringDisplayValue().id("SL1"))
    );

    ScopeLimitationDetails mockScopeLimitationDetails = new ScopeLimitationDetails()
        .addContentItem(new ScopeLimitationDetail().stage(2));

    when(lookupService.getScopeLimitationDetails(any(ScopeLimitationDetail.class)))
        .thenReturn(Mono.just(mockScopeLimitationDetails));

    Integer result = applicationService.getProceedingStage(
        categoryOfLaw, matterType, proceedingCode, levelOfService, scopeLimitations, isAmendment);

    assertEquals(2, result);
  }

  @Test
  void updateProviderDetails_updatesProviderDetailsSuccessfully() {
    String id = "12345";
    UserDetail user = buildUserDetail();
    ApplicationFormData applicationFormData = new ApplicationFormData();
    applicationFormData.setOfficeId(2);
    applicationFormData.setOfficeName("Office Name");
    applicationFormData.setFeeEarnerId(201);
    applicationFormData.setSupervisorId(301);
    applicationFormData.setContactNameId("401");
    applicationFormData.setProviderCaseReference("CaseRef123");

    ProviderDetail providerDetail = new ProviderDetail()
        .id(1)
        .name("Provider Name")
        .contactNames(List.of(new ContactDetail().id(401).name("Contact Name")));
    ContactDetail feeEarner = new ContactDetail().id(201).name("Fee Earner Name");
    ContactDetail supervisor = new ContactDetail().id(301).name("Supervisor Name");

    when(providerService.getProvider(user.getProvider().getId())).thenReturn(Mono.just(providerDetail));
    when(providerService.getFeeEarnerByOfficeAndId(providerDetail, applicationFormData.getOfficeId(), applicationFormData.getFeeEarnerId()))
        .thenReturn(feeEarner);
    when(providerService.getFeeEarnerByOfficeAndId(providerDetail, applicationFormData.getOfficeId(), applicationFormData.getSupervisorId()))
        .thenReturn(supervisor);

    when(caabApiClient.putApplication(eq(id), eq(user.getLoginId()), any(), eq("provider-details")))
        .thenReturn(Mono.empty());

    applicationService.updateProviderDetails(id, applicationFormData, user);

    ArgumentCaptor<ApplicationProviderDetails> providerDetailsCaptor = ArgumentCaptor.forClass(ApplicationProviderDetails.class);

    verify(caabApiClient).putApplication(eq(id), eq(user.getLoginId()), providerDetailsCaptor.capture(), eq("provider-details"));

    ApplicationProviderDetails capturedProviderDetails = providerDetailsCaptor.getValue();

    assertNotNull(capturedProviderDetails);
    assertEquals(providerDetail.getId(), capturedProviderDetails.getProvider().getId());
    assertEquals(applicationFormData.getOfficeId(), capturedProviderDetails.getOffice().getId());
    assertEquals(feeEarner.getId().toString(), capturedProviderDetails.getFeeEarner().getId());
    assertEquals(supervisor.getId().toString(), capturedProviderDetails.getSupervisor().getId());
    assertEquals(applicationFormData.getProviderCaseReference(), capturedProviderDetails.getProviderCaseReference());
  }

  @Test
  void prepareProceedingSummary_updatesCostsCorrectly() {
    String id = "12345";
    UserDetail user = new UserDetail().loginId("userLoginId");

    ApplicationDetail application = getApplicationDetail();

    when(caabApiClient.updateCostStructure(eq(id), any(CostStructure.class), eq(user.getLoginId()))).thenReturn(Mono.empty());

    applicationService.prepareProceedingSummary(id, application, user);
    
    ArgumentCaptor<CostStructure> costsCaptor = ArgumentCaptor.forClass(CostStructure.class);

    verify(caabApiClient).updateCostStructure(eq(id), costsCaptor.capture(), eq(user.getLoginId()));

    CostStructure capturedCosts = costsCaptor.getValue();
    assertNotNull(capturedCosts.getRequestedCostLimitation());
    assertEquals(0, capturedCosts.getRequestedCostLimitation().compareTo(new BigDecimal("1500.00")));
  }

  @Test
  void testAddOpponent() {
    String appplicationId = "12345";
    UserDetail user = new UserDetail().loginId("userLoginId");
    ApplicationDetail application = getApplicationDetail();

    OpponentFormData opponentFormData = new OpponentFormData();
    Opponent opponent = new Opponent();

    when(opponentMapper.toOpponent(opponentFormData)).thenReturn(opponent);
    when(caabApiClient.getApplication(appplicationId)).thenReturn(Mono.just(application));
    when(caabApiClient.addOpponent(appplicationId, opponent, user.getLoginId())).thenReturn(Mono.empty());

    applicationService.addOpponent(appplicationId, opponentFormData, user);

    verify(caabApiClient).addOpponent(appplicationId, opponent, user.getLoginId());

    assertEquals(application.getAppMode(), opponent.getAppMode());
    assertEquals(application.getAmendment(), opponent.getAmendment());
  }

  private static ApplicationDetail getApplicationDetail() {
    ApplicationDetail application = new ApplicationDetail();
    application.setAmendment(false);
    CostStructure costs = new CostStructure();
    costs.setDefaultCostLimitation(new BigDecimal("1000.00")); // Assume this gets set within getDefaultCostLimitation
    application.setCosts(costs);

    Proceeding proceeding = new Proceeding();
    proceeding.setCostLimitation(new BigDecimal("1500.00")); // This should trigger an update to default cost limitation
    application.setProceedings(List.of(proceeding));
    return application;
  }


}
