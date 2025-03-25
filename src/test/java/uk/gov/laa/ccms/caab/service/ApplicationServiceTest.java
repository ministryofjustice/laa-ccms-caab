package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CASE_ADDRESS_OPTION;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CLIENT_INVOLVEMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_LEVEL_OF_SERVICE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MATTER_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ORGANISATION_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_PROCEEDING_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_RELATIONSHIP_TO_CLIENT;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_SCOPE_LIMITATIONS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MEANS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MEANS_PREPOP;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MERITS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MERITS_PREPOP;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildApplicationDetail;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildApplicationProviderDetails;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildOpponent;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildCaseReferenceSummary;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildCategoryOfLawLookupValueDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildPriorAuthorityTypeDetails;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildProviderDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildRelationshipToCaseLookupDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildAwardTypeLookupDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildCaseDetail;
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
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
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
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.opponent.AbstractOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.constants.SearchConstants;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentStatus;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.mapper.AddressFormDataMapper;
import uk.gov.laa.ccms.caab.mapper.ApplicationFormDataMapper;
import uk.gov.laa.ccms.caab.mapper.ApplicationMapper;
import uk.gov.laa.ccms.caab.mapper.CopyApplicationMapperImpl;
import uk.gov.laa.ccms.caab.mapper.OpponentMapper;
import uk.gov.laa.ccms.caab.mapper.ResultDisplayMapper;
import uk.gov.laa.ccms.caab.mapper.context.ApplicationMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.CaseOutcomeMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.PriorAuthorityMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.ProceedingMappingContext;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.AuditDetail;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.ClientDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.LinkedCaseDetail;
import uk.gov.laa.ccms.caab.model.LinkedCaseResultRowDisplay;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.CaseDetails;
import uk.gov.laa.ccms.data.model.CaseReferenceSummary;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.CaseSummary;
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
import uk.gov.laa.ccms.data.model.ScopeLimitationDetails;
import uk.gov.laa.ccms.data.model.StageEndLookupDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;
import uk.gov.laa.ccms.data.model.TransactionStatus;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.PriorAuthority;


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
  private AssessmentService assessmentService;
  @Mock EvidenceService evidenceService;
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
  private CopyApplicationMapperImpl copyApplicationMapper;

  @Mock
  private OpponentMapper opponentMapper;

  @Mock
  private SearchConstants searchConstants;



  @InjectMocks
  private ApplicationService applicationService;

  @Test
  void getCaseReference_returnsCaseReferenceSummary_Successful() {

    final CaseReferenceSummary mockCaseReferenceSummary = new CaseReferenceSummary();

    when(ebsApiClient.postAllocateNextCaseReference()).thenReturn(
        Mono.just(mockCaseReferenceSummary));

    final Mono<CaseReferenceSummary> caseReferenceSummaryMono =
        applicationService.getCaseReference();

    StepVerifier.create(caseReferenceSummaryMono)
        .expectNextMatches(summary -> summary == mockCaseReferenceSummary)
        .verifyComplete();
  }

  @Test
  void getCases_UnSubmittedStatusDoesNotQuerySOA() {
    final CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    caseSearchCriteria.setCaseReference("123");
    caseSearchCriteria.setProviderCaseReference("456");
    caseSearchCriteria.setFeeEarnerId(789);
    caseSearchCriteria.setOfficeId(999);
    caseSearchCriteria.setClientSurname("asurname");

    caseSearchCriteria.setStatus(STATUS_UNSUBMITTED_ACTUAL_VALUE);

    final UserDetail userDetail = buildUserDetail();
    final int page = 0;
    final int size = 10;

    final ApplicationDetails mockApplicationDetails = new ApplicationDetails()
        .addContentItem(new BaseApplicationDetail());

    when(caabApiClient.getApplications(caseSearchCriteria, userDetail.getProvider().getId(),
        page, size)).thenReturn(Mono.just(mockApplicationDetails));
    when(searchConstants.getMaxSearchResultsCases()).thenReturn(size);

    final List<BaseApplicationDetail> results =
        applicationService.getCases(caseSearchCriteria, userDetail);

    verifyNoInteractions(soaApiClient);
    verify(caabApiClient).getApplications(caseSearchCriteria, userDetail.getProvider().getId(),
        page, size);

    assertNotNull(results);
    assertEquals(mockApplicationDetails.getContent(), results);
  }

  @Test
  void getCases_DraftStatusQueriesEBSAndTDS() {
    final CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    caseSearchCriteria.setCaseReference("123");
    caseSearchCriteria.setProviderCaseReference("456");
    caseSearchCriteria.setFeeEarnerId(789);
    caseSearchCriteria.setOfficeId(999);
    caseSearchCriteria.setClientSurname("asurname");

    caseSearchCriteria.setStatus(STATUS_DRAFT);

    final UserDetail userDetail = buildUserDetail();

    final int page = 0;
    final int size = 10;

    final CaseDetails mockCaseDetails = new CaseDetails()
        .totalElements(1)
        .size(1)
        .addContentItem(new CaseSummary().caseReferenceNumber("2"));

    final BaseApplicationDetail mockEbsApplication = new BaseApplicationDetail()
            .caseReferenceNumber("2");

    final ApplicationDetails mockTdsApplicationDetails = new ApplicationDetails()
        .totalElements(1)
        .size(1)
        .addContentItem(new BaseApplicationDetail()
            .caseReferenceNumber("1"));

    // expected result, sorted by case reference
    final List<BaseApplicationDetail> expectedResult = List.of(mockTdsApplicationDetails.getContent().get(0),
        mockEbsApplication);

    when(ebsApiClient.getCases(
        caseSearchCriteria, userDetail.getProvider().getId(), page, size))
        .thenReturn(Mono.just(mockCaseDetails));
    when(applicationMapper.toBaseApplication(mockCaseDetails.getContent().get(0)))
        .thenReturn(mockEbsApplication);
    when(caabApiClient.getApplications(caseSearchCriteria, userDetail.getProvider().getId(),
            page, size)).thenReturn(Mono.just(mockTdsApplicationDetails));
    when(searchConstants.getMaxSearchResultsCases()).thenReturn(size);

    final List<BaseApplicationDetail> result =
        applicationService.getCases(caseSearchCriteria, userDetail);

    verify(ebsApiClient).getCases(caseSearchCriteria, userDetail.getProvider().getId(), page, size);
    verify(caabApiClient).getApplications(caseSearchCriteria,
        userDetail.getProvider().getId(), page, size);

    assertNotNull(result);
    assertEquals(expectedResult, result);
  }

  @Test
  void getCases_RemovesDuplicates_RetainingEbsCase() {
    final CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    caseSearchCriteria.setCaseReference("123");
    caseSearchCriteria.setProviderCaseReference("456");
    caseSearchCriteria.setFeeEarnerId(789);
    caseSearchCriteria.setOfficeId(999);
    caseSearchCriteria.setClientSurname("asurname");

    caseSearchCriteria.setStatus(STATUS_DRAFT);

    final UserDetail userDetail = buildUserDetail();

    final int page = 0;
    final int size = 10;

    final CaseSummary soaCaseSummary = new CaseSummary()
        .caseReferenceNumber("1")
        .caseStatusDisplay("the soa one");

    final BaseApplicationDetail mockEbsApplication = new BaseApplicationDetail()
        .caseReferenceNumber(soaCaseSummary.getCaseReferenceNumber())
        .status(new StringDisplayValue().displayValue(soaCaseSummary.getCaseStatusDisplay()));

    final BaseApplicationDetail mockTdsApplication = new BaseApplicationDetail()
        .caseReferenceNumber("1")
        .status(new StringDisplayValue().displayValue("the tds one"));

    final CaseDetails mockCaseDetails = new CaseDetails()
        .totalElements(1)
        .size(1)
        .addContentItem(soaCaseSummary);

    final ApplicationDetails mockTdsApplicationDetails = new ApplicationDetails()
        .totalElements(1)
        .size(1)
        .addContentItem(mockTdsApplication);

    // expected result, only the soa case retained
    final List<BaseApplicationDetail> expectedResult = List.of(mockEbsApplication);

    when(ebsApiClient.getCases(caseSearchCriteria, userDetail.getProvider().getId(),
        page, size)).thenReturn(Mono.just(mockCaseDetails));
    when(applicationMapper.toBaseApplication(mockCaseDetails.getContent().get(0)))
        .thenReturn(mockEbsApplication);
    when(caabApiClient.getApplications(caseSearchCriteria,
        userDetail.getProvider().getId(), page, size))
        .thenReturn(Mono.just(mockTdsApplicationDetails));
    when(searchConstants.getMaxSearchResultsCases()).thenReturn(size);

    final List<BaseApplicationDetail> result =
        applicationService.getCases(caseSearchCriteria, userDetail);

    verify(ebsApiClient).getCases(caseSearchCriteria, userDetail.getProvider().getId(), page, size);
    verify(caabApiClient).getApplications(caseSearchCriteria,
        userDetail.getProvider().getId(), page, size);

    assertNotNull(result);
    assertEquals(expectedResult, result);
  }

  @Test
  void getCases_TooManySoaResults_ThrowsException() {
    final CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    caseSearchCriteria.setCaseReference("123");
    caseSearchCriteria.setProviderCaseReference("456");
    caseSearchCriteria.setFeeEarnerId(789);
    caseSearchCriteria.setOfficeId(999);
    caseSearchCriteria.setClientSurname("asurname");

    final UserDetail userDetail = buildUserDetail();

    final int page = 0;
    final int size = 1;

    caseSearchCriteria.setStatus(STATUS_DRAFT);

    final CaseDetails mockCaseDetails = new CaseDetails()
        .totalElements(2)
        .size(2)
        .addContentItem(new CaseSummary())
        .addContentItem(new CaseSummary());

    when(ebsApiClient.getCases(caseSearchCriteria, userDetail.getProvider().getId(),
        page, size)).thenReturn(Mono.just(mockCaseDetails));
    when(searchConstants.getMaxSearchResultsCases()).thenReturn(size);

    assertThrows(TooManyResultsException.class, () ->
        applicationService.getCases(caseSearchCriteria, userDetail));
  }

  @Test
  void getCases_TooManyOverallResults_ThrowsException() {
    final CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    caseSearchCriteria.setCaseReference("123");
    caseSearchCriteria.setProviderCaseReference("456");
    caseSearchCriteria.setFeeEarnerId(789);
    caseSearchCriteria.setOfficeId(999);
    caseSearchCriteria.setClientSurname("asurname");

    final UserDetail userDetail = buildUserDetail();

    final int page = 0;
    final int size = 2;

    caseSearchCriteria.setStatus(STATUS_DRAFT);

    final CaseDetails mockCaseDetails = new CaseDetails()
        .totalElements(2)
        .size(2)
        .addContentItem(new CaseSummary().caseReferenceNumber("1"))
        .addContentItem(new CaseSummary().caseReferenceNumber("2"));

    final ApplicationDetails mockTdsApplicationDetails = new ApplicationDetails()
        .totalElements(1)
        .size(1)
        .addContentItem(new BaseApplicationDetail().caseReferenceNumber("3"));

    when(ebsApiClient.getCases(caseSearchCriteria,
        userDetail.getProvider().getId(), page, size))
        .thenReturn(Mono.just(mockCaseDetails));
    when(applicationMapper.toBaseApplication(mockCaseDetails.getContent().get(0)))
        .thenReturn(new BaseApplicationDetail()
            .caseReferenceNumber(mockCaseDetails.getContent().get(0).getCaseReferenceNumber()));
    when(applicationMapper.toBaseApplication(mockCaseDetails.getContent().get(1)))
        .thenReturn(new BaseApplicationDetail()
            .caseReferenceNumber(mockCaseDetails.getContent().get(1).getCaseReferenceNumber()));
    when(caabApiClient.getApplications(caseSearchCriteria, userDetail.getProvider().getId(),
        page, size)).thenReturn(Mono.just(mockTdsApplicationDetails));
    when(searchConstants.getMaxSearchResultsCases()).thenReturn(size);

    assertThrows(TooManyResultsException.class, () ->
        applicationService.getCases(caseSearchCriteria, userDetail));
  }

  @Test
  void getCopyCaseStatus_returnsData() {
    final CaseStatusLookupDetail caseStatusLookupDetail = new CaseStatusLookupDetail();
    caseStatusLookupDetail.addContentItem(new CaseStatusLookupValueDetail());

    when(lookupService.getCaseStatusValues(Boolean.TRUE)).thenReturn(
        Mono.just(caseStatusLookupDetail));

    final CaseStatusLookupValueDetail lookupValue = applicationService.getCopyCaseStatus();

    assertNotNull(lookupValue);
    assertEquals(caseStatusLookupDetail.getContent().get(0), lookupValue);
  }

  @Test
  void getCopyCaseStatus_handlesNullResponse() {

    when(lookupService.getCaseStatusValues(Boolean.TRUE)).thenReturn(Mono.empty());

    final CaseStatusLookupValueDetail lookupValue = applicationService.getCopyCaseStatus();

    assertNull(lookupValue);
  }

  @Test
  void createApplication_success() throws ParseException {
    final ApplicationFormData applicationFormData = buildApplicationFormData();
    final uk.gov.laa.ccms.soa.gateway.model.ClientDetail clientInformation = buildClientDetail();
    final UserDetail user = buildUserDetail();

    // Mocking dependencies
    final CaseReferenceSummary caseReferenceSummary =
        new CaseReferenceSummary().caseReferenceNumber("REF123");
    final CategoryOfLawLookupValueDetail categoryOfLawValue = new CategoryOfLawLookupValueDetail()
        .code(applicationFormData.getCategoryOfLawId()).matterTypeDescription("DESC1");
    final ContractDetails contractDetails = new ContractDetails();

    final AmendmentTypeLookupValueDetail amendmentType = new AmendmentTypeLookupValueDetail()
        .applicationTypeCode("TEST")
        .applicationTypeDescription("TEST")
        .defaultLarScopeFlag("Y");

    final AmendmentTypeLookupDetail amendmentTypes =
        new AmendmentTypeLookupDetail().addContentItem(amendmentType);

    when(ebsApiClient.postAllocateNextCaseReference()).thenReturn(
        Mono.just(caseReferenceSummary));
    when(soaApiClient.getContractDetails(anyInt(), anyInt(), anyString(),
        anyString())).thenReturn(Mono.just(contractDetails));
    when(lookupService.getCategoryOfLaw(applicationFormData.getCategoryOfLawId())).thenReturn(
        Mono.just(Optional.of(categoryOfLawValue)));
    when(ebsApiClient.getAmendmentTypes(any())).thenReturn(Mono.just(amendmentTypes));
    when(caabApiClient.createApplication(anyString(), any())).thenReturn(Mono.empty());

    final Mono<String> applicationMono = applicationService.createApplication(
        applicationFormData, clientInformation, user);

    StepVerifier.create(applicationMono)
        .verifyComplete();

    verify(ebsApiClient).postAllocateNextCaseReference();
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
    uk.gov.laa.ccms.soa.gateway.model.ClientDetail clientDetail = buildClientDetail();
    CaseReferenceSummary caseReferenceSummary = buildCaseReferenceSummary();
    UserDetail user = buildUserDetail();

    // Mock everything that is needed to look up the copy Case and map it
    // to an ApplicationDetail.
    CaseDetail soaCase = buildCaseDetail(APP_TYPE_EMERGENCY);
    soaCase.setCaseReferenceNumber(copyCaseReference);
    // Reduce down to a single ProceedingDetail for this test
    soaCase.getApplicationDetails().getProceedings().remove(
        soaCase.getApplicationDetails().getProceedings().size() - 1);

    when(soaApiClient.getCase(copyCaseReference, user.getLoginId(), user.getUserType()))
        .thenReturn(Mono.just(soaCase));

    when(ebsApiClient.postAllocateNextCaseReference())
        .thenReturn(Mono.just(caseReferenceSummary));

    /* START ApplicationMappingContext */
    CommonLookupValueDetail applicationTypeLookup = new CommonLookupValueDetail();

    uk.gov.laa.ccms.data.model.ProviderDetail providerDetail = buildProviderDetail(
        soaCase.getApplicationDetails().getProviderDetails().getProviderOfficeId(),
        soaCase.getApplicationDetails().getProviderDetails().getFeeEarnerContactId(),
        soaCase.getApplicationDetails().getProviderDetails().getSupervisorContactId());

    PriorAuthorityTypeDetails priorAuthorityTypes =
        buildPriorAuthorityTypeDetails("dataType");

    when(lookupService.getCommonValue(COMMON_VALUE_APPLICATION_TYPE, soaCase.getCertificateType()))
        .thenReturn(Mono.just(Optional.of(applicationTypeLookup)));

    when(lookupService.getCommonValue(COMMON_VALUE_APPLICATION_TYPE,
        soaCase.getApplicationDetails().getApplicationAmendmentType()))
        .thenReturn(Mono.just(Optional.of(applicationTypeLookup)));

    when(providerService.getProvider(Integer.parseInt(
        soaCase.getApplicationDetails().getProviderDetails().getProviderFirmId())))
        .thenReturn(Mono.just(providerDetail));

    /* START ProceedingMappingContext */
    uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail soaProceeding =
        soaCase.getApplicationDetails().getProceedings().get(0);
    uk.gov.laa.ccms.data.model.ProceedingDetail proceedingLookup =
        new uk.gov.laa.ccms.data.model.ProceedingDetail();
    when(ebsApiClient.getProceeding(soaProceeding.getProceedingType()))
        .thenReturn(Mono.just(proceedingLookup));

    CommonLookupValueDetail matterTypeLookup =
        new CommonLookupValueDetail().code("mat1").description("mat 1");
    when(lookupService.getCommonValue(eq(COMMON_VALUE_MATTER_TYPES), anyString()))
        .thenReturn(Mono.just(Optional.of(matterTypeLookup)));

    CommonLookupValueDetail levelOfServiceLookup =
        new CommonLookupValueDetail().code("los1").description("los 1");
    when(lookupService.getCommonValue(eq(COMMON_VALUE_LEVEL_OF_SERVICE), anyString()))
        .thenReturn(Mono.just(Optional.of(levelOfServiceLookup)));

    CommonLookupValueDetail clientInvolvementLookup =
        new CommonLookupValueDetail().code("ci1").description("ci 1");
    when(lookupService.getCommonValue(eq(COMMON_VALUE_CLIENT_INVOLVEMENT_TYPES), anyString()))
        .thenReturn(Mono.just(Optional.of(clientInvolvementLookup)));

    CommonLookupValueDetail proceedingStatusLookup =
        new CommonLookupValueDetail();
    when(lookupService.getCommonValue(COMMON_VALUE_PROCEEDING_STATUS, soaProceeding.getStatus()))
        .thenReturn(Mono.just(Optional.of(proceedingStatusLookup)));

    when(lookupService.getCommonValue(eq(COMMON_VALUE_SCOPE_LIMITATIONS), anyString()))
        .thenReturn(Mono.just(Optional.of(new CommonLookupValueDetail())));

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

    /* START PriorAuthorityMappingContext */
    when(lookupService.getPriorAuthorityType(anyString())).thenReturn(
        Mono.just(Optional.of(priorAuthorityTypes.getContent().get(0))));

    /* END PriorAuthorityMappingContext */


    /* START CaseOutcomeMappingContext */
    AwardTypeLookupDetail awardTypes = buildAwardTypeLookupDetail(soaCase);

    when(lookupService.getAwardTypes()).thenReturn(Mono.just(awardTypes));

    /* END of mapping contexts */

    ApplicationDetail applicationToCopy = buildApplicationDetail(1, true, new Date());
    when(applicationMapper.toApplicationDetail(any(ApplicationMappingContext.class)))
        .thenReturn(applicationToCopy);

    // Mock out the additional calls for copying the application
    when(lookupService.getCategoryOfLaw(applicationToCopy.getCategoryOfLaw().getId()))
        .thenReturn(Mono.just(Optional.of(new CategoryOfLawLookupValueDetail())));
    when(lookupService.getPersonToCaseRelationships()).thenReturn(
        Mono.just(new RelationshipToCaseLookupDetail()));
    when(soaApiClient.getContractDetails(
        user.getProvider().getId(),
        applicationToCopy.getProviderDetails().getOffice().getId(),
        user.getLoginId(),
        user.getUserType())).thenReturn(Mono.just(new ContractDetails()));

    when(copyApplicationMapper.copyApplication(
        any(ApplicationDetail.class),
        eq(applicationToCopy)))
        .thenReturn(applicationToCopy);

    when(caabApiClient.createApplication(anyString(), any())).thenReturn(Mono.empty());

    // Call the method under test
    Mono<String> applicationMono = applicationService.createApplication(
        applicationFormData, clientDetail, user);

    StepVerifier.create(applicationMono)
        .verifyComplete();

    verify(copyApplicationMapper).copyApplication(
        any(ApplicationDetail.class),
        eq(applicationToCopy));
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

    uk.gov.laa.ccms.soa.gateway.model.ClientDetail clientDetail = buildClientDetail();
    CaseReferenceSummary caseReferenceSummary = buildCaseReferenceSummary();

    // Update the cost limitations for this test
    applicationToCopy.getProceedings().get(0).setCostLimitation(costLimit1);
    applicationToCopy.getProceedings().get(1).setCostLimitation(costLimit2);

    // Update the opponent type for this test
    applicationToCopy.getOpponents().get(0).setType(opponentType);
    applicationToCopy.getOpponents().get(0).setSharedInd(opponentShared);

    when(ebsApiClient.postAllocateNextCaseReference())
        .thenReturn(Mono.just(caseReferenceSummary));

    CategoryOfLawLookupValueDetail categoryOfLawLookupValueDetail =
        buildCategoryOfLawLookupValueDetail(copyCostLimit);
    // Update the category of law lookup for this test
    categoryOfLawLookupValueDetail.setCopyCostLimit(copyCostLimit);

    when(lookupService.getCategoryOfLaw(applicationToCopy.getCategoryOfLaw().getId()))
        .thenReturn(Mono.just(Optional.of(categoryOfLawLookupValueDetail)));

    RelationshipToCaseLookupDetail relationshipToCaseLookupDetail =
        buildRelationshipToCaseLookupDetail();
    // Update the relationshipToCase lookup for this test
    relationshipToCaseLookupDetail.getContent().get(0).setCopyParty(opponentRelCopyParty);

    when(lookupService.getPersonToCaseRelationships()).thenReturn(
        Mono.just(relationshipToCaseLookupDetail));

    when(soaApiClient.getContractDetails(
        userDetail.getProvider().getId(),
        applicationToCopy.getProviderDetails().getOffice().getId(),
        userDetail.getLoginId(),
        userDetail.getUserType())).thenReturn(Mono.just(new ContractDetails()));

    // If the category of law has copyCostLimit set to TRUE the requested cost
    // limit from the applicationToCopy's costs should be used.
    BigDecimal expectedRequestedCostLimit =
        copyCostLimit ? applicationToCopy.getCosts().getRequestedCostLimitation() : BigDecimal.ZERO;

    // Get the max cost limitation
    BigDecimal expectedDefaultCostLimit = costLimit1.max(costLimit2);

    ArgumentCaptor<ApplicationDetail> newApplicationCaptor = ArgumentCaptor.forClass(ApplicationDetail.class);

    when(copyApplicationMapper.copyApplication(
        newApplicationCaptor.capture(),
        eq(applicationToCopy)))
        .then(returnsSecondArg());

    /* Call the method under test */
    Mono<ApplicationDetail> resultMono =
        applicationService.copyApplication(applicationToCopy, clientDetail, userDetail);

    // If the opponent is an INDIVIDUAL, it is not shared, and the opponents relationship
    // to the case is set to 'Copy Party' then the ebsId should be null.
    StepVerifier.create(resultMono)
        .expectNextMatches(applicationDetail ->
            opponentEbsIdCleared == (applicationDetail.getOpponents().get(0).getEbsId() == null)
                && expectedDefaultCostLimit.equals(
                newApplicationCaptor.getValue().getCosts().getDefaultCostLimitation())
                && expectedRequestedCostLimit.equals(
                newApplicationCaptor.getValue().getCosts().getRequestedCostLimitation())
        ).verifyComplete();
  }

  @Test
  void getApplicationSummary_returnsApplicationSummary_Successful() {

    final UserDetail user = buildUserDetail();

    // Create mock data for successful Mono results
    final RelationshipToCaseLookupDetail orgRelationshipsDetail = new RelationshipToCaseLookupDetail();
    orgRelationshipsDetail.addContentItem(new RelationshipToCaseLookupValueDetail());

    final RelationshipToCaseLookupDetail personRelationshipsDetail = new RelationshipToCaseLookupDetail();
    personRelationshipsDetail.addContentItem(new RelationshipToCaseLookupValueDetail());

    final CommonLookupDetail relationshipToClientLookupDetail = new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail());

    final CommonLookupDetail contactTitleLookupDetail = new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail());

    final AuditDetail auditDetail = new AuditDetail();
    auditDetail.setLastSaved(Date.from(Instant.now()));
    auditDetail.setLastSavedBy("TestUser");

    final CostStructureDetail costStructure = new CostStructureDetail();
    costStructure.setAuditTrail(auditDetail);

    final ClientDetail client = new ClientDetail();
    client.setFirstName("bob");
    client.setSurname("ross");

    final ApplicationType applicationType = new ApplicationType();
    applicationType.id("test 123");
    applicationType.setDisplayValue("testing123");

    ApplicationProviderDetails providerDetails = buildApplicationProviderDetails(1);
    providerDetails.setProviderContact(null); // used to determine the provider status in the builder.

    AddressDetail address = new AddressDetail()
        .preferredAddress("prefAdd");

    final ApplicationDetail applicationDetail = new ApplicationDetail();
    applicationDetail.setProviderDetails(providerDetails);
    applicationDetail.setAuditTrail(auditDetail);
    applicationDetail.setClient(client);
    applicationDetail.setApplicationType(applicationType);
    applicationDetail.setProceedings(new ArrayList<>());
    applicationDetail.setPriorAuthorities(new ArrayList<>());
    applicationDetail.setOpponents(new ArrayList<>());
    applicationDetail.setCosts(costStructure);
    applicationDetail.setCorrespondenceAddress(address);

    final AssessmentDetails meansAssessmentDetails = new AssessmentDetails()
        .addContentItem(new AssessmentDetail()
            .status(AssessmentStatus.INCOMPLETE.getStatus()));

    final AssessmentDetails meritsAssessmentDetails = new AssessmentDetails()
        .addContentItem(new AssessmentDetail()
            .status(AssessmentStatus.COMPLETE.getStatus()));

    CommonLookupValueDetail correspondenceMethodLookup =
        new CommonLookupValueDetail()
            .description("correspondence method1");

    when(lookupService.getCommonValue(COMMON_VALUE_CASE_ADDRESS_OPTION,
        applicationDetail.getCorrespondenceAddress().getPreferredAddress()))
        .thenReturn(Mono.just(Optional.of(correspondenceMethodLookup)));
    when(lookupService.getOrganisationToCaseRelationships()).thenReturn(
        Mono.just(orgRelationshipsDetail));
    when(lookupService.getPersonToCaseRelationships()).thenReturn(
        Mono.just(personRelationshipsDetail));
    when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT)).thenReturn(
          Mono.just(relationshipToClientLookupDetail));
    when(lookupService.getCommonValues(COMMON_VALUE_CONTACT_TITLE)).thenReturn(
        Mono.just(contactTitleLookupDetail));

    when(assessmentService.getAssessments(List.of("meansAssessment"),
        user.getProvider().getId().toString(), applicationDetail.getCaseReferenceNumber()))
        .thenReturn(Mono.just(meansAssessmentDetails));
    when(assessmentService.getAssessments(List.of("meritsAssessment"),
        user.getProvider().getId().toString(), applicationDetail.getCaseReferenceNumber()))
        .thenReturn(Mono.just(meritsAssessmentDetails));
    when(evidenceService.isEvidenceRequired(
        any(AssessmentDetail.class),
        any(AssessmentDetail.class),
        eq(applicationType),
        anyList())).thenReturn(false);

    final ApplicationSectionDisplay summary =
        applicationService.getApplicationSections(applicationDetail, user);

    assertNotNull(summary);
    assertEquals("Complete", summary.getGeneralDetails().getStatus());
    assertEquals(correspondenceMethodLookup.getDescription(),
        summary.getGeneralDetails().getCorrespondenceMethod());
    assertEquals("bob ross", summary.getClient().getClientFullName());
    assertEquals(applicationType.getDisplayValue(), summary.getApplicationType().getDescription());
    assertEquals("Started", summary.getProvider().getStatus());
    assertEquals("Complete", summary.getClient().getStatus());
    assertEquals("Not started", summary.getProceedingsAndCosts().getStatus());
    assertEquals("Not started", summary.getOpponentsAndOtherParties().getStatus());
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
  @DisplayName("getMonoProviderDetailsFormData returns Mono of ApplicationFormData when successful")
  void testGetMonoProviderDetailsFormData_Successful() {
    final String id = "12345";
    final ApplicationProviderDetails mockProviderDetails = new ApplicationProviderDetails();
    final ApplicationFormData expectedApplicationFormData = new ApplicationFormData();

    when(caabApiClient.getProviderDetails(id)).thenReturn(Mono.just(mockProviderDetails));
    when(applicationFormDataMapper.toApplicationProviderDetailsFormData(mockProviderDetails))
        .thenReturn(expectedApplicationFormData);

    final Mono<ApplicationFormData> result = applicationService.getMonoProviderDetailsFormData(id);

    StepVerifier.create(result)
        .expectNextMatches(applicationFormData -> applicationFormData == expectedApplicationFormData)
        .verifyComplete();

    verify(caabApiClient).getProviderDetails(id);
    verify(applicationFormDataMapper).toApplicationProviderDetailsFormData(mockProviderDetails);
  }

  @Test
  void testGetCorrespondenceAddressFormData() {
    final String id = "12345";
    final AddressDetail mockAddress = new AddressDetail();
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
  @DisplayName("getMonoCorrespondenceAddressFormData returns Mono of AddressFormData when successful")
  void testGetMonoCorrespondenceAddressFormData_Successful() {
    final String id = "12345";
    final AddressDetail mockAddress = new AddressDetail();
    final AddressFormData expectedAddressFormData = new AddressFormData();

    when(caabApiClient.getCorrespondenceAddress(id)).thenReturn(Mono.just(mockAddress));
    when(addressFormDataMapper.toAddressFormData(mockAddress)).thenReturn(expectedAddressFormData);

    final Mono<AddressFormData> result = applicationService.getMonoCorrespondenceAddressFormData(id);

    StepVerifier.create(result)
        .expectNextMatches(addressFormData -> addressFormData == expectedAddressFormData)
        .verifyComplete();

    verify(caabApiClient).getCorrespondenceAddress(id);
    verify(addressFormDataMapper).toAddressFormData(mockAddress);
  }

  @Test
  void testGetLinkedCases() {
    final String id = "12345";
    final List<LinkedCaseDetail> mockLinkedCases = Arrays.asList(new LinkedCaseDetail(), new LinkedCaseDetail());
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
    final LinkedCaseDetail linkedCase = new LinkedCaseDetail();

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
    final AddressDetail correspondenceAddress = new AddressDetail();

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
    PriorAuthorityTypeDetail priorAuthorityTypeDetail = priorAuthorityTypeDetails.getContent().get(0);
    PriorAuthorityDetail priorAuthoritiesItem = priorAuthorityTypeDetail.getPriorAuthorities().get(0);

    when(lookupService.getPriorAuthorityType(soaPriorAuthority.getPriorAuthorityType()))
        .thenReturn(Mono.just(Optional.of(priorAuthorityTypeDetail)));

    CommonLookupValueDetail lookup = new CommonLookupValueDetail()
        .code("thecode")
        .description("thedescription");

    when(lookupService.getCommonValue(priorAuthoritiesItem.getLovCode(),
        soaPriorAuthority.getDetails().get(0).getValue()))
        .thenReturn(Mono.just(Optional.of(lookup)));

    PriorAuthorityMappingContext result = applicationService.buildPriorAuthorityMappingContext(
        soaPriorAuthority);

    verify(lookupService).getCommonValue(priorAuthoritiesItem.getLovCode(),
        soaPriorAuthority.getDetails().get(0).getValue());

    assertNotNull(result);
    assertEquals(soaPriorAuthority, result.getEbsPriorAuthority());
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
    PriorAuthorityTypeDetail priorAuthorityTypeDetail = priorAuthorityTypeDetails.getContent().get(0);

    when(lookupService.getPriorAuthorityType(soaPriorAuthority.getPriorAuthorityType()))
        .thenReturn(Mono.just(Optional.of(priorAuthorityTypeDetail)));

    PriorAuthorityMappingContext result = applicationService.buildPriorAuthorityMappingContext(
        soaPriorAuthority);

    assertNotNull(result);
    assertEquals(soaPriorAuthority, result.getEbsPriorAuthority());
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

    when(lookupService.getPriorAuthorityType(soaPriorAuthority.getPriorAuthorityType()))
        .thenReturn(Mono.empty());

    Exception e = assertThrows(CaabApplicationException.class,
        () -> applicationService.buildPriorAuthorityMappingContext(
        soaPriorAuthority));

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

    PriorAuthorityTypeDetail priorAuthorityTypeDetail = new PriorAuthorityTypeDetail()
        .code(soaPriorAuthority.getPriorAuthorityType())
        .description("priorAuthDesc")
        .addPriorAuthoritiesItem(priorAuthoritiesItem);

    when(lookupService.getPriorAuthorityType(soaPriorAuthority.getPriorAuthorityType()))
        .thenReturn(Mono.just(Optional.of(priorAuthorityTypeDetail)));

    when(lookupService.getCommonValue(priorAuthoritiesItem.getLovCode(),
        soaPriorAuthority.getDetails().get(0).getValue()))
        .thenReturn(Mono.empty());

    Exception e = assertThrows(CaabApplicationException.class,
        () -> applicationService.buildPriorAuthorityMappingContext(
        soaPriorAuthority));

    assertEquals(String.format("Failed to find common value with code: %s",
        soaPriorAuthority.getDetails().get(0).getValue()), e.getMessage());
  }

  @Test
  void testBuildProceedingMappingContext_Emergency() {
    uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail soaProceeding =
        buildProceedingDetail(STATUS_DRAFT);
    CaseDetail soaCase = buildCaseDetail(APP_TYPE_EMERGENCY);

    // Mock out the remaining lookups
    uk.gov.laa.ccms.data.model.ProceedingDetail proceedingLookup =
        new uk.gov.laa.ccms.data.model.ProceedingDetail();
    when(ebsApiClient.getProceeding(soaProceeding.getProceedingType()))
        .thenReturn(Mono.just(proceedingLookup));

    CommonLookupValueDetail proceedingStatusLookup =
        new CommonLookupValueDetail();
    when(lookupService.getCommonValue(COMMON_VALUE_PROCEEDING_STATUS,  soaProceeding.getStatus()))
        .thenReturn(Mono.just(Optional.of(proceedingStatusLookup)));

    CommonLookupValueDetail matterTypeLookup = new CommonLookupValueDetail()
        .code(soaProceeding.getMatterType())
        .description("the matter type");
    when(lookupService.getCommonValue(COMMON_VALUE_MATTER_TYPES, soaProceeding.getMatterType()))
        .thenReturn(Mono.just(Optional.of(matterTypeLookup)));

    CommonLookupValueDetail levelOfServiceLookup = new CommonLookupValueDetail()
        .code(soaProceeding.getLevelOfService())
        .description("the los");
    when(lookupService.getCommonValue(COMMON_VALUE_LEVEL_OF_SERVICE,soaProceeding.getLevelOfService()))
        .thenReturn(Mono.just(Optional.of(levelOfServiceLookup)));

    CommonLookupValueDetail clientInvLookup = new CommonLookupValueDetail()
        .code(soaProceeding.getClientInvolvementType())
        .description("the involvement");
    when(lookupService.getCommonValue(COMMON_VALUE_CLIENT_INVOLVEMENT_TYPES, soaProceeding.getClientInvolvementType()))
        .thenReturn(Mono.just(Optional.of(clientInvLookup)));

    CommonLookupValueDetail scopeLimitationOneLookup = new CommonLookupValueDetail()
        .code(soaProceeding.getScopeLimitations().get(0).getScopeLimitation())
        .description("the limitation 1");
    when(lookupService.getCommonValue(COMMON_VALUE_SCOPE_LIMITATIONS, soaProceeding.getScopeLimitations().get(0).getScopeLimitation()))
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
          soaCase);

//    StepVerifier.create(proceedingMappingContextMono)
//        .expectNextMatches(result -> {
          assertNotNull(result);
          assertEquals(soaProceeding, result.getEbsProceeding());
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
    uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail soaProceeding =
        buildProceedingDetail(STATUS_DRAFT);
    CaseDetail soaCase = buildCaseDetail(APP_TYPE_EXCEPTIONAL_CASE_FUNDING);

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
        applicationService.calculateProceedingCostLimitation(soaProceeding,
            soaCase);

    verify(lookupService, times(3))
        .getScopeLimitationDetails(any(uk.gov.laa.ccms.data.model.ScopeLimitationDetail.class));

    assertEquals(expectedCostLimitation, result);
  }

  @Test
  void testAddProceedingOutcomeContext_NullOutcome() {
    final uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail soaProceeding =
        buildProceedingDetail(STATUS_DRAFT);
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
    when(lookupService.getCommonValue(eq(COMMON_VALUE_PROCEEDING_STATUS),  any(String.class)))
        .thenReturn(Mono.just(Optional.of(new CommonLookupValueDetail())));
    when(lookupService.getAwardTypes()).thenReturn(Mono.just(
        new AwardTypeLookupDetail()
            .addContentItem(new AwardTypeLookupValueDetail())));

    ApplicationMappingContext result =
        applicationService.buildApplicationMappingContext(soaCase);

    assertNotNull(result);
    assertEquals(soaCase, result.getEbsCaseDetail());
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

    when(lookupService.getCommonValue(COMMON_VALUE_APPLICATION_TYPE,soaCase.getCertificateType()))
        .thenReturn(Mono.just(Optional.of(applicationTypeLookup)));

    when(lookupService.getCommonValue(COMMON_VALUE_APPLICATION_TYPE,
        soaCase.getApplicationDetails().getApplicationAmendmentType()))
        .thenReturn(Mono.just(Optional.of(applicationTypeLookup)));

    when(providerService.getProvider(Integer.parseInt(
        soaCase.getApplicationDetails().getProviderDetails().getProviderFirmId())))
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
  void testBuildApplicationMappingContext_NoAppTypeDefaultsToCertificate() {
    CaseDetail soaCase = buildCaseDetail(null);
    soaCase.getApplicationDetails().getProceedings().forEach(
        proceedingDetail -> {
          // Clear the outcome and scopelimitations from all proceedings - this is tested elsewhere
          proceedingDetail.setOutcome(null);
          proceedingDetail.getScopeLimitations().clear();
        });
    soaCase.getPriorAuthorities().clear(); // PriorAuthority mapping context tested elsewhere.
    soaCase.getAwards().clear(); // Awards tested separately.


    uk.gov.laa.ccms.data.model.ProviderDetail providerDetail = buildProviderDetail(
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

    final ApplicationMappingContext result =
        applicationService.buildApplicationMappingContext(soaCase);

    assertNotNull(result);

    // The soa case has no applicationAmendmentType, so the lookup should default to the
    // certificateType.
    assertEquals(certificateTypeLookup, result.getApplicationType());
  }

  @Test
  void makeLeadProceeding_UpdatesLeadProceeding_Successful() {
    final String applicationId = "app123";
    final Integer newLeadProceedingId = 2;
    final UserDetail user = new UserDetail().loginId("user1");

    final List<ProceedingDetail> mockProceedings = Arrays.asList(
        new ProceedingDetail().id(1).leadProceedingInd(true),
        new ProceedingDetail().id(2).leadProceedingInd(false)
    );

    when(caabApiClient.getProceedings(applicationId)).thenReturn(Mono.just(mockProceedings));
    when(caabApiClient.updateProceeding(anyInt(), any(ProceedingDetail.class), eq(user.getLoginId())))
        .thenReturn(Mono.empty());
    when(caabApiClient.patchApplication(
        eq(applicationId),
        any(ApplicationDetail.class),
        eq(user.getLoginId())))
        .thenReturn(Mono.empty());

    applicationService.makeLeadProceeding(applicationId, newLeadProceedingId, user);

    final ArgumentCaptor<ProceedingDetail> proceedingArgumentCaptor = ArgumentCaptor.forClass(ProceedingDetail.class);
    verify(caabApiClient, times(2)).updateProceeding(anyInt(), proceedingArgumentCaptor.capture(), eq(user.getLoginId()));

    final List<ProceedingDetail> capturedProceedings = proceedingArgumentCaptor.getAllValues();

    assertFalse(capturedProceedings.get(0).getLeadProceedingInd());
    assertTrue(capturedProceedings.get(1).getLeadProceedingInd());

    verify(caabApiClient).patchApplication(eq(applicationId), any(ApplicationDetail.class), eq(user.getLoginId()));
  }

  @Test
  void testBuildIndividualOpponentFormData_noLookupMatchReturnsCodes() {
    OpponentDetail opponent = buildOpponent(new Date());
    opponent.setType(OPPONENT_TYPE_INDIVIDUAL);

    when(lookupService.getCommonValue(COMMON_VALUE_CONTACT_TITLE, opponent.getTitle())).thenReturn(
        Mono.just(Optional.empty()));
    when(lookupService.getPersonToCaseRelationship(opponent.getRelationshipToCase())).thenReturn(
        Mono.just(Optional.empty()));
    when(lookupService.getCommonValue(COMMON_VALUE_RELATIONSHIP_TO_CLIENT,
        opponent.getRelationshipToClient())).thenReturn(
        Mono.just(Optional.empty()));

    String expectedPartyName =
        opponent.getTitle() + " " + opponent.getFirstName() + " " + opponent.getSurname();

    when(opponentMapper.toOpponentFormData(
        opponent,
        expectedPartyName,
        null,
        opponent.getRelationshipToCase(),
        opponent.getRelationshipToClient(),
        true)).thenReturn(new IndividualOpponentFormData());

    AbstractOpponentFormData result = applicationService.buildOpponentFormData(opponent);

    assertNotNull(result);
    verify(opponentMapper).toOpponentFormData(
        opponent,
        expectedPartyName,
        null,
        opponent.getRelationshipToCase(),
        opponent.getRelationshipToClient(),
        true);
  }

  @Test
  void testBuildOrgOpponentFormData_ReturnsOrganisationName() {
    OpponentDetail opponent = buildOpponent(new Date());
    opponent.setType(OPPONENT_TYPE_ORGANISATION);

    when(lookupService.getCommonValue(COMMON_VALUE_ORGANISATION_TYPES,
        opponent.getOrganisationType())).thenReturn(
        Mono.just(Optional.empty()));
    when(lookupService.getOrganisationToCaseRelationship(opponent.getRelationshipToCase())).thenReturn(
        Mono.just(Optional.empty()));
    when(lookupService.getCommonValue(COMMON_VALUE_RELATIONSHIP_TO_CLIENT,
        opponent.getRelationshipToClient())).thenReturn(
        Mono.just(Optional.empty()));
    when(lookupService.getCommonValue(COMMON_VALUE_CONTACT_TITLE,
        opponent.getTitle())).thenReturn(
        Mono.just(Optional.empty()));

    String expectedPartyName = opponent.getOrganisationName();

    when(opponentMapper.toOpponentFormData(
        opponent,
        expectedPartyName,
        opponent.getOrganisationType(),
        opponent.getRelationshipToCase(),
        opponent.getRelationshipToClient(),
        true)).thenReturn(new OrganisationOpponentFormData());

    AbstractOpponentFormData result = applicationService.buildOpponentFormData(opponent);

    assertNotNull(result);
    verify(opponentMapper).toOpponentFormData(
        opponent,
        expectedPartyName,
        opponent.getOrganisationType(),
        opponent.getRelationshipToCase(),
        opponent.getRelationshipToClient(),
        true);
  }

  @Test
  void testBuildOrgOpponentFormData_lookupMatchesReturnDisplayValues() {
    OpponentDetail opponent = buildOpponent(new Date());
    opponent.setType(OPPONENT_TYPE_ORGANISATION);

    CommonLookupValueDetail organisationTypeLookup = new CommonLookupValueDetail()
        .code(opponent.getOrganisationType())
        .description("org type");

    when(lookupService.getCommonValue(COMMON_VALUE_ORGANISATION_TYPES,
        opponent.getOrganisationType())).thenReturn(
        Mono.just(Optional.of(organisationTypeLookup)));

    RelationshipToCaseLookupValueDetail orgRelationshipToCase =
        new RelationshipToCaseLookupValueDetail()
            .code(opponent.getRelationshipToCase())
            .description("org rel");

    when(lookupService.getOrganisationToCaseRelationship(opponent.getRelationshipToCase())).thenReturn(
        Mono.just(Optional.of(orgRelationshipToCase)));

    CommonLookupValueDetail relationshipToClient = new CommonLookupValueDetail()
        .code(opponent.getRelationshipToClient())
        .description("rel 2 client");

    when(lookupService.getCommonValue(COMMON_VALUE_RELATIONSHIP_TO_CLIENT,
        opponent.getRelationshipToClient())).thenReturn(
        Mono.just(Optional.of(relationshipToClient)));

    CommonLookupValueDetail titleLookup = new CommonLookupValueDetail()
        .code(opponent.getTitle())
        .description("Mr");

    when(lookupService.getCommonValue(COMMON_VALUE_CONTACT_TITLE,
        opponent.getTitle())).thenReturn(
        Mono.just(Optional.of(titleLookup)));

    String expectedPartyName =
        opponent.getOrganisationName();

    when(opponentMapper.toOpponentFormData(
        opponent,
        expectedPartyName,
        organisationTypeLookup.getDescription(),
        orgRelationshipToCase.getDescription(),
        relationshipToClient.getDescription(),
        true)).thenReturn(new OrganisationOpponentFormData());

    AbstractOpponentFormData result = applicationService.buildOpponentFormData(opponent);

    assertNotNull(result);
    verify(opponentMapper).toOpponentFormData(
        opponent,
        expectedPartyName,
        organisationTypeLookup.getDescription(),
        orgRelationshipToCase.getDescription(),
        relationshipToClient.getDescription(),
        true);
  }

  @Test
  void testBuildIndividualOpponentFormData_lookupMatchesReturnDisplayValues() {
    OpponentDetail opponent = buildOpponent(new Date());
    opponent.setType(OPPONENT_TYPE_INDIVIDUAL);

    when(lookupService.getCommonValue(COMMON_VALUE_CONTACT_TITLE,
        opponent.getTitle())).thenReturn(
        Mono.just(Optional.empty()));

    RelationshipToCaseLookupValueDetail personRelationshipToCase =
        new RelationshipToCaseLookupValueDetail()
            .code(opponent.getRelationshipToCase())
            .description("ind rel");

    when(lookupService.getPersonToCaseRelationship(opponent.getRelationshipToCase())).thenReturn(
        Mono.just(Optional.of(personRelationshipToCase)));

    CommonLookupValueDetail relationshipToClient = new CommonLookupValueDetail()
        .code(opponent.getRelationshipToClient())
        .description("rel 2 client");

    when(lookupService.getCommonValue(COMMON_VALUE_RELATIONSHIP_TO_CLIENT,
        opponent.getRelationshipToClient())).thenReturn(
        Mono.just(Optional.of(relationshipToClient)));

    String expectedPartyName =
        opponent.getTitle() + " " + opponent.getFirstName() + " " + opponent.getSurname();

    when(opponentMapper.toOpponentFormData(
        opponent,
        expectedPartyName,
        null,
        personRelationshipToCase.getDescription(),
        relationshipToClient.getDescription(),
        true)).thenReturn(new IndividualOpponentFormData());

    AbstractOpponentFormData result = applicationService.buildOpponentFormData(opponent);

    assertNotNull(result);
    verify(opponentMapper).toOpponentFormData(opponent,
        expectedPartyName,
        null,
        personRelationshipToCase.getDescription(),
        relationshipToClient.getDescription(),
        true);
  }

  @Test
  void testGetOpponents_queriesLookupData() {
    final String applicationId = "123";
    OpponentDetail opponent = buildOpponent(new Date());
    opponent.setType(OPPONENT_TYPE_INDIVIDUAL);

    when(caabApiClient.getOpponents(applicationId)).thenReturn(Mono.just(List.of(opponent)));

    when(lookupService.getCommonValue(COMMON_VALUE_CONTACT_TITLE, opponent.getTitle()))
        .thenReturn(Mono.just(Optional.empty()));
    when(lookupService.getPersonToCaseRelationship(opponent.getRelationshipToCase()))
        .thenReturn(Mono.just(Optional.empty()));
    when(lookupService.getCommonValue(COMMON_VALUE_RELATIONSHIP_TO_CLIENT, opponent.getRelationshipToClient()))
        .thenReturn(Mono.just(Optional.empty()));

    when(opponentMapper.toOpponentFormData(eq(opponent), anyString(), isNull(), anyString(), anyString(), anyBoolean()))
        .thenReturn(new IndividualOpponentFormData());

    List<AbstractOpponentFormData> result =
        applicationService.getOpponents(applicationId);

    assertNotNull(result);
    assertEquals(1, result.size());

    verify(opponentMapper).toOpponentFormData(eq(opponent), anyString(), isNull(), anyString(), anyString(), anyBoolean());
  }

  private ApplicationFormData buildApplicationFormData() {
    final ApplicationFormData applicationFormData = new ApplicationFormData();
    applicationFormData.setOfficeId(1);
    applicationFormData.setCategoryOfLawId("COL");
    applicationFormData.setExceptionalFunding(false);
    applicationFormData.setApplicationTypeCategory(APP_TYPE_SUBSTANTIVE);
    applicationFormData.setDelegatedFunctions(true);
    applicationFormData.setDelegatedFunctionUsedDate("1/1/2022");
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
  void abandonApplication_success() {
    final ApplicationDetail application = buildApplicationDetail(1, true, new Date());
    List<String> expectedAssessmentNames = List.of(
        MEANS.getName(),
        MEANS_PREPOP.getName(),
        MERITS.getName(),
        MERITS_PREPOP.getName());
    final UserDetail user = new UserDetail().loginId("userLoginId");

    when(evidenceService.removeDocuments(application.getCaseReferenceNumber(), user.getLoginId())).thenReturn(Mono.empty());

    when(caabApiClient.deleteApplication(String.valueOf(application.getId()), user.getLoginId())).thenReturn(Mono.empty());

    when(assessmentService.deleteAssessments(user, expectedAssessmentNames, application.getCaseReferenceNumber(), null)).thenReturn(Mono.empty());

    applicationService.abandonApplication(application, user);

    verify(evidenceService).removeDocuments(application.getCaseReferenceNumber(), user.getLoginId());

    verify(caabApiClient).deleteApplication(String.valueOf(application.getId()), user.getLoginId());

    verify(assessmentService).deleteAssessments(user, expectedAssessmentNames, application.getCaseReferenceNumber(), null);

  }

  @Test
  void getDefaultScopeLimitation_withEmergencyApplicationType_returnsEmergencyScopeLimitations() {
    String categoryOfLaw = "Family";
    String matterType = "FAM";
    String proceedingCode = "PC001";
    String levelOfService = "3";
    String applicationType = APP_TYPE_EMERGENCY; // Assume this is one of the emergency application type codes

    uk.gov.laa.ccms.data.model.ScopeLimitationDetail criteria =
        new uk.gov.laa.ccms.data.model.ScopeLimitationDetail()
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

    uk.gov.laa.ccms.data.model.ScopeLimitationDetail criteria =
        new uk.gov.laa.ccms.data.model.ScopeLimitationDetail()
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

    uk.gov.laa.ccms.data.model.ScopeLimitationDetail criteria =
        new uk.gov.laa.ccms.data.model.ScopeLimitationDetail()
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
    List<ScopeLimitationDetail> scopeLimitations = List.of(
        new ScopeLimitationDetail().scopeLimitation(new StringDisplayValue().id("SL1")),
        new ScopeLimitationDetail().scopeLimitation(new StringDisplayValue().id("SL2"))
    );

    ScopeLimitationDetails mockScopeLimitationDetails1 = new ScopeLimitationDetails()
        .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail()
            .costLimitation(new BigDecimal(500)));
    ScopeLimitationDetails mockScopeLimitationDetails2 = new ScopeLimitationDetails()
        .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail()
            .costLimitation(new BigDecimal(1000)));

    when(lookupService.getScopeLimitationDetails(any(
        uk.gov.laa.ccms.data.model.ScopeLimitationDetail.class)))
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
    List<ScopeLimitationDetail> scopeLimitations = List.of(
        new ScopeLimitationDetail().scopeLimitation(new StringDisplayValue().id("SL1")),
        new ScopeLimitationDetail().scopeLimitation(new StringDisplayValue().id("SL2"))
    );

    ScopeLimitationDetails mockScopeLimitationDetails1 = new ScopeLimitationDetails()
        .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail()
            .costLimitation(new BigDecimal(300)));
    ScopeLimitationDetails mockScopeLimitationDetails2 = new ScopeLimitationDetails()
        .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail()
            .costLimitation(new BigDecimal(800)));

    when(lookupService.getScopeLimitationDetails(any(
        uk.gov.laa.ccms.data.model.ScopeLimitationDetail.class)))
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
    List<ScopeLimitationDetail> scopeLimitations = Collections.emptyList();

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
    List<ScopeLimitationDetail> scopeLimitations = List.of(
        new ScopeLimitationDetail().scopeLimitation(
            new StringDisplayValue().id("SL1"))
    );

    ScopeLimitationDetails mockScopeLimitationDetails = new ScopeLimitationDetails()
        .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().stage(1))
        .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().stage(2));

    when(lookupService.getScopeLimitationDetails(any(
        uk.gov.laa.ccms.data.model.ScopeLimitationDetail.class)))
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
    List<ScopeLimitationDetail> scopeLimitations = List.of(
        new ScopeLimitationDetail().scopeLimitation(new StringDisplayValue().id("SL1")),
        new ScopeLimitationDetail().scopeLimitation(new StringDisplayValue().id("SL2"))
    );

    ScopeLimitationDetails mockScopeLimitationDetails1 = new ScopeLimitationDetails()
        .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().stage(1))
        .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().stage(2));
    ScopeLimitationDetails mockScopeLimitationDetails2 = new ScopeLimitationDetails()
        .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().stage(2))
        .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().stage(3));

    when(lookupService.getScopeLimitationDetails(any(
        uk.gov.laa.ccms.data.model.ScopeLimitationDetail.class)))
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
    List<ScopeLimitationDetail> scopeLimitations = List.of(
        new ScopeLimitationDetail().scopeLimitation(new StringDisplayValue().id("SL1")),
        new ScopeLimitationDetail().scopeLimitation(new StringDisplayValue().id("SL2"))
    );

    ScopeLimitationDetails mockScopeLimitationDetails1 = new ScopeLimitationDetails()
        .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().stage(1))
        .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().stage(3));
    ScopeLimitationDetails mockScopeLimitationDetails2 = new ScopeLimitationDetails()
        .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().stage(4))
        .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().stage(5));

    when(lookupService.getScopeLimitationDetails(any(
        uk.gov.laa.ccms.data.model.ScopeLimitationDetail.class)))
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
    List<ScopeLimitationDetail> scopeLimitations = List.of(
        new ScopeLimitationDetail().scopeLimitation(new StringDisplayValue().id("SL1"))
    );

    ScopeLimitationDetails mockScopeLimitationDetails = new ScopeLimitationDetails()
        .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().stage(2));

    when(lookupService.getScopeLimitationDetails(any(
        uk.gov.laa.ccms.data.model.ScopeLimitationDetail.class)))
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

    when(caabApiClient.updateCostStructure(eq(id), any(CostStructureDetail.class), eq(user.getLoginId()))).thenReturn(Mono.empty());

    applicationService.prepareProceedingSummary(id, application, user);
    
    ArgumentCaptor<CostStructureDetail> costsCaptor = ArgumentCaptor.forClass(CostStructureDetail.class);

    verify(caabApiClient).updateCostStructure(eq(id), costsCaptor.capture(), eq(user.getLoginId()));

    CostStructureDetail capturedCosts = costsCaptor.getValue();
    assertNotNull(capturedCosts.getRequestedCostLimitation());
    assertEquals(0, capturedCosts.getRequestedCostLimitation().compareTo(new BigDecimal("1500.00")));
  }

  @Test
  void testAddOpponent() {
    String appplicationId = "12345";
    UserDetail user = new UserDetail().loginId("userLoginId");
    ApplicationDetail application = getApplicationDetail();

    AbstractOpponentFormData opponentFormData = new IndividualOpponentFormData();
    OpponentDetail opponent = new OpponentDetail();

    when(opponentMapper.toOpponent(opponentFormData)).thenReturn(opponent);
    when(caabApiClient.getApplication(appplicationId)).thenReturn(Mono.just(application));
    when(caabApiClient.addOpponent(appplicationId, opponent, user.getLoginId())).thenReturn(Mono.empty());

    applicationService.addOpponent(appplicationId, opponentFormData, user);

    verify(caabApiClient).addOpponent(appplicationId, opponent, user.getLoginId());

    assertEquals(application.getAppMode(), opponent.getAppMode());
    assertEquals(application.getAmendment(), opponent.getAmendment());
  }

  @Test
  void testGetCaseStatus(){
    // Given
    String transactionId = "12345";
    TransactionStatus expected = new TransactionStatus().submissionStatus("Success")
        .referenceNumber("123");
    when(ebsApiClient.getCaseStatus(transactionId)).thenReturn(Mono
        .just(expected));
    // When
    Mono<TransactionStatus> result = ebsApiClient.getCaseStatus(transactionId);
    // Then
    assertEquals(expected, result.block());
  }

  private static ApplicationDetail getApplicationDetail() {
    ApplicationDetail application = new ApplicationDetail();
    application.setAmendment(false);
    CostStructureDetail costs = new CostStructureDetail();
    costs.setDefaultCostLimitation(new BigDecimal("1000.00")); // Assume this gets set within getDefaultCostLimitation
    application.setCosts(costs);

    ProceedingDetail proceeding = new ProceedingDetail();
    proceeding.setCostLimitation(new BigDecimal("1500.00")); // This should trigger an update to default cost limitation
    application.setProceedings(List.of(proceeding));
    return application;
  }


}
