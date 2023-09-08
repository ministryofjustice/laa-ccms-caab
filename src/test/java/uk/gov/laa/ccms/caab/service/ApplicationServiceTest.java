package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteria;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetails;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailRecordHistory;
import uk.gov.laa.ccms.soa.gateway.model.ClientNameDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;

@ExtendWith(MockitoExtension.class)
public class ApplicationServiceTest {
  @Mock
  private CaabApiClient caabApiClient;

  @Mock
  private SoaApiClient soaApiClient;

  @Mock
  private EbsApiClient ebsApiClient;

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
  void createApplication_success() {
    ApplicationDetails applicationDetails = buildApplicationDetails();
    ClientDetail clientInformation = buildClientInformation();
    UserDetail user = buildUser();

    // Mocking dependencies
    CaseReferenceSummary caseReferenceSummary =
        new CaseReferenceSummary().caseReferenceNumber("REF123");
    CommonLookupDetail categoryOfLawLookupDetail = new CommonLookupDetail();
    categoryOfLawLookupDetail.addContentItem(new CommonLookupValueDetail().code("CAT1").description("DESC1"));
    ContractDetails contractDetails = new ContractDetails();

    AmendmentTypeLookupValueDetail amendmentType = new AmendmentTypeLookupValueDetail()
        .applicationTypeCode("TEST")
        .applicationTypeDescription("TEST")
        .defaultLarScopeFlag("Y");

    AmendmentTypeLookupDetail amendmentTypes =
        new AmendmentTypeLookupDetail().addContentItem(amendmentType);

    when(soaApiClient.getCaseReference(user.getLoginId(), user.getUserType())).thenReturn(
        Mono.just(caseReferenceSummary));
    when(ebsApiClient.getCommonValues(anyString())).thenReturn(
        Mono.just(categoryOfLawLookupDetail));
    when(soaApiClient.getContractDetails(anyInt(), anyInt(), anyString(),
        anyString())).thenReturn(Mono.just(contractDetails));
    when(ebsApiClient.getAmendmentTypes(any())).thenReturn(Mono.just(amendmentTypes));
    when(caabApiClient.createApplication(anyString(), any())).thenReturn(Mono.empty());

    Mono<Void> voidMono = applicationService.createApplication(
        applicationDetails, clientInformation, user);

    StepVerifier.create(voidMono)
        .verifyComplete();

    verify(soaApiClient).getCaseReference(user.getLoginId(), user.getUserType());
    verify(ebsApiClient).getCommonValues(anyString());
    verify(soaApiClient).getContractDetails(anyInt(), anyInt(), anyString(), anyString());
    verify(ebsApiClient).getAmendmentTypes(any());
    verify(caabApiClient).createApplication(anyString(), any());

  }

  private UserDetail buildUser() {
    return new UserDetail()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId")
        .provider(buildProvider());
  }

  private ProviderDetail buildProvider() {
    return new ProviderDetail()
        .id(123)
        .addOfficesItem(
            new OfficeDetail()
                .id(1)
                .name("Office 1"));
  }

  private ApplicationDetails buildApplicationDetails() {
    ApplicationDetails applicationDetails = new ApplicationDetails();
    applicationDetails.setOfficeId(1);
    applicationDetails.setCategoryOfLawId("COL");
    applicationDetails.setExceptionalFunding(false);
    applicationDetails.setApplicationTypeCategory(APP_TYPE_SUBSTANTIVE);
    applicationDetails.setDelegatedFunctions(true);
    applicationDetails.setDelegatedFunctionUsedDay("01");
    applicationDetails.setDelegatedFunctionUsedMonth("01");
    applicationDetails.setDelegatedFunctionUsedYear("2022");
    return applicationDetails;
  }

  public ClientDetail buildClientInformation() {
    String clientReferenceNumber = "12345";
    return new ClientDetail()
        .clientReferenceNumber(clientReferenceNumber)
        .details(new ClientDetailDetails()
            .name(new ClientNameDetail()))
        .recordHistory(new ClientDetailRecordHistory());
  }

}