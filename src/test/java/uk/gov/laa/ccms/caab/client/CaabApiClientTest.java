package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.BaseClientDetail;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetails;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetails;
import uk.gov.laa.ccms.caab.model.LinkedCaseDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetails;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
public class CaabApiClientTest {
    
  @Mock
  private WebClient caabApiWebClient;

  @Mock
  private WebClient.RequestBodyUriSpec requestBodyUriMock;

  @Mock
  private WebClient.RequestBodySpec requestBodyMock;

  @Mock
  private WebClient.RequestHeadersSpec requestHeadersMock;

  @Mock
  private WebClient.RequestHeadersUriSpec requestHeadersUriMock;

  @Mock
  private WebClient.ResponseSpec responseMock;

  @Mock
  private CaabApiClientErrorHandler apiClientErrorHandler;

  @InjectMocks
  private CaabApiClient caabApiClient;

  @Test
  void createApplication_success() {
    final String loginId = "user1";
    final ApplicationDetail application =
        new ApplicationDetail(); // Populate as needed
    final String expectedUri = "/applications";
    final String locationId = "123"; // Replace with your expected location header

    when(caabApiWebClient.post()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(any(MediaType.class))).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(ApplicationDetail.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.exchangeToMono(any(Function.class))).thenReturn(Mono.just(locationId));

    final Mono<String> result = caabApiClient.createApplication(loginId, application);

    StepVerifier.create(result)
        .expectNext(locationId) // Expect the location header value
        .verifyComplete();

    verify(requestHeadersMock, times(1)).exchangeToMono(any(Function.class));
  }

  @Test
  void deleteApplication_success() {
    final String applicationId = "123";
    final String loginId = "user123";
    final String expectedUri = "/applications/{id}";

    when(caabApiWebClient.delete()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, applicationId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.deleteApplication(applicationId, loginId);

    StepVerifier.create(result).verifyComplete();
  }


  @Test
  void getApplication_success() {

    final String id = "123";
    final String expectedUri = "/applications/{id}";

    final ApplicationDetail mockApplication = new ApplicationDetail();

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ApplicationDetail.class)).thenReturn(Mono.just(mockApplication));

    final Mono<ApplicationDetail> applicationDetailMono
        = caabApiClient.getApplication(id);

    StepVerifier.create(applicationDetailMono)
        .expectNext(mockApplication)
        .verifyComplete();
  }

  @Test
  void getApplications_success() {

    final CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    caseSearchCriteria.setCaseReference("123");
    caseSearchCriteria.setClientReference("cliref");
    caseSearchCriteria.setProviderCaseReference("456");
    caseSearchCriteria.setFeeEarnerId(789);
    caseSearchCriteria.setOfficeId(999);
    caseSearchCriteria.setClientSurname("asurname");
    caseSearchCriteria.setStatus("thestatus");

    final Integer providerId = 9876;
    final int page = 0;
    final int size = 2;

    final String expectedUri = String.format(
        "/applications?case-reference-number=%s&provider-id=%s&provider-case-ref=%s&client-surname=%s&client-reference=%s&fee-earner=%s&office-id=%s&status=%s&page=%s&size=%s",
        caseSearchCriteria.getCaseReference(),
        providerId,
        caseSearchCriteria.getProviderCaseReference(),
        caseSearchCriteria.getClientSurname(),
        caseSearchCriteria.getClientReference(),
        caseSearchCriteria.getFeeEarnerId(),
        caseSearchCriteria.getOfficeId(),
        caseSearchCriteria.getStatus(),
        page,
        size);

    final ApplicationDetails mockApplicationDetails = new ApplicationDetails();

    final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ApplicationDetails.class))
        .thenReturn(Mono.just(mockApplicationDetails));

    final Mono<ApplicationDetails> applicationDetailsMono
        = caabApiClient.getApplications(caseSearchCriteria, providerId, page, size);

    StepVerifier.create(applicationDetailsMono)
        .expectNext(Objects.requireNonNull(applicationDetailsMono.block()))
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals(expectedUri, actualUri.toString());
  }

  @Test
  void getApplicationType_success() {

    final String id = "123";
    final String expectedUri = "/applications/{id}/application-type";

    final ApplicationType mockApplication = new ApplicationType();

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ApplicationType.class)).thenReturn(Mono.just(mockApplication));

    final Mono<ApplicationType> applicationTypeMono
        = caabApiClient.getApplicationType(id);

    StepVerifier.create(applicationTypeMono)
        .expectNext(mockApplication)
        .verifyComplete();
  }

  @Test
  void getCorrespondenceAddress_success() {

    final String id = "123";
    final String expectedUri = "/applications/{id}/correspondence-address";

    final AddressDetail mockApplication = new AddressDetail();

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(AddressDetail.class)).thenReturn(Mono.just(mockApplication));

    final Mono<AddressDetail> addressMono
        = caabApiClient.getCorrespondenceAddress(id);

    StepVerifier.create(addressMono)
        .expectNext(mockApplication)
        .verifyComplete();
  }

  @Test
  void patchApplication_applicationType_success() {
    final String loginId = "user1";
    final String id = "123";
    final String type = "application-type";
    final String expectedUri = String.format("/applications/{id}/%s", type);

    final ApplicationType applicationType = new ApplicationType();

    when(caabApiWebClient.put()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri, id)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(any(MediaType.class))).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(ApplicationType.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    assertDoesNotThrow(() -> {
      caabApiClient.putApplication(id, loginId, applicationType, type).block();
    });
  }

  @Test
  void patchApplication_correspondenceAddress_success() {
    final String loginId = "user1";
    final String id = "123";
    final String type = "correspondence-address";
    final String expectedUri = String.format("/applications/{id}/%s", type);

    final AddressDetail correspondenceAddress = new AddressDetail();

    when(caabApiWebClient.put()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri, id)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(any(MediaType.class))).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(AddressDetail.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    assertDoesNotThrow(() -> {
      caabApiClient.putApplication(id, loginId, correspondenceAddress, type).block();
    });
  }

  @Test
  void getProviderDetails_success() {

    final String id = "123";
    final String expectedUri = "/applications/{id}/provider-details";

    final ApplicationProviderDetails mockProvider = new ApplicationProviderDetails();

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ApplicationProviderDetails.class)).thenReturn(
        Mono.just(mockProvider));

    final Mono<ApplicationProviderDetails> applicationProviderDetailsMono
        = caabApiClient.getProviderDetails(id);

    StepVerifier.create(applicationProviderDetailsMono)
        .expectNext(mockProvider)
        .verifyComplete();
  }

  @Test
  void getOpponents_success() {
    final String id = "123";
    final String expectedUri = "/applications/{id}/opponents";

    final List<OpponentDetail> mockOpponents = new ArrayList<>();

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(new ParameterizedTypeReference<List<OpponentDetail>>() {})).thenReturn(
        Mono.just(mockOpponents));

    final Mono<List<OpponentDetail>> listMono
        = caabApiClient.getOpponents(id);

    StepVerifier.create(listMono)
        .expectNext(mockOpponents)
        .verifyComplete();
  }

  @Test
  void getLinkedCases_success() {
    final String id = "123";
    final String expectedUri = "/applications/{id}/linked-cases";

    final List<LinkedCaseDetail> mockLinkedCases = new ArrayList<>(); // Add mock data to the list as needed

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(new ParameterizedTypeReference<List<LinkedCaseDetail>>() {}))
        .thenReturn(Mono.just(mockLinkedCases));

    final Mono<List<LinkedCaseDetail>> linkedCasesMono = caabApiClient.getLinkedCases(id);

    StepVerifier.create(linkedCasesMono)
        .expectNext(mockLinkedCases)
        .verifyComplete();
  }

  @Test
  void removeLinkedCase_success() {
    final String linkedCaseId = "case456";
    final String loginId = "user789";
    final String expectedUri = "/linked-cases/{linkedCaseId}";

    when(caabApiWebClient.delete()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, linkedCaseId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.removeLinkedCase(linkedCaseId, loginId);

    StepVerifier.create(result)
        .verifyComplete();

    verify(requestBodyMock, times(1)).header("Caab-User-Login-Id", loginId);
    verify(responseMock, times(1)).bodyToMono(Void.class);
  }

  @Test
  void updateLinkedCase_success() {
    final String linkedCaseId = "case456";
    final LinkedCaseDetail linkedCaseData = new LinkedCaseDetail(); // Populate this with test data as needed
    final String loginId = "user789";
    final String expectedUri = "/linked-cases/{linkedCaseId}";

    when(caabApiWebClient.patch()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri, linkedCaseId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(linkedCaseData)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.updateLinkedCase(linkedCaseId, linkedCaseData, loginId);

    StepVerifier.create(result)
        .verifyComplete();

    verify(requestBodyMock, times(1)).header("Caab-User-Login-Id", loginId);
    verify(requestBodyMock, times(1)).contentType(MediaType.APPLICATION_JSON);
    verify(requestBodyMock, times(1)).bodyValue(linkedCaseData);
    verify(responseMock, times(1)).bodyToMono(Void.class);
  }

  @Test
  void addLinkedCase_success() {
    final String applicationId = "app123";
    final LinkedCaseDetail linkedCaseData = new LinkedCaseDetail(); // Populate this with test data as needed
    final String loginId = "user789";
    final String expectedUri = "applications/{applicationId}/linked-cases";

    when(caabApiWebClient.post()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri, applicationId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(linkedCaseData)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.addLinkedCase(applicationId, linkedCaseData, loginId);

    StepVerifier.create(result)
        .verifyComplete();

    verify(requestBodyMock, times(1)).header("Caab-User-Login-Id", loginId);
    verify(requestBodyMock, times(1)).contentType(MediaType.APPLICATION_JSON);
    verify(requestBodyMock, times(1)).bodyValue(linkedCaseData);
    verify(responseMock, times(1)).bodyToMono(Void.class);
  }

  @Test
  void getProceedings_success() {
    final String id = "123";
    final String expectedUri = "/applications/{id}/proceedings";

    final List<ProceedingDetail> mockProceedings = new ArrayList<>(); // Populate this as needed

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(new ParameterizedTypeReference<List<ProceedingDetail>>() {})).thenReturn(Mono.just(mockProceedings));

    final Mono<List<ProceedingDetail>> result = caabApiClient.getProceedings(id);

    StepVerifier.create(result)
        .expectNext(mockProceedings)
        .verifyComplete();
  }

  @Test
  void getCosts_success() {
    final String id = "123";
    final String expectedUri = "/applications/{id}/cost-structure";

    final CostStructureDetail mockCostStructure = new CostStructureDetail(); // Populate this as needed

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(new ParameterizedTypeReference<CostStructureDetail>() {})).thenReturn(Mono.just(mockCostStructure));

    final Mono<CostStructureDetail> result = caabApiClient.getCostStructure(id);

    StepVerifier.create(result)
        .expectNext(mockCostStructure)
        .verifyComplete();
  }

  @Test
  void updateCosts_success() {
    final String id = "123";
    final CostStructureDetail costs = new CostStructureDetail(); // Populate this as needed
    final String loginId = "user1";
    final String expectedUri = "/applications/{id}/cost-structure";

    when(caabApiWebClient.put()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri, id)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(costs)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.updateCostStructure(id, costs, loginId);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void getPriorAuthorities_success() {
    final String id = "123";
    final String expectedUri = "/applications/{id}/prior-authorities";

    final List<PriorAuthorityDetail> mockPriorAuthorities = new ArrayList<>(); // Populate this as needed

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(new ParameterizedTypeReference<List<PriorAuthorityDetail>>() {})).thenReturn(Mono.just(mockPriorAuthorities));

    final Mono<List<PriorAuthorityDetail>> result = caabApiClient.getPriorAuthorities(id);

    StepVerifier.create(result)
        .expectNext(mockPriorAuthorities)
        .verifyComplete();
  }

  @Test
  void updateProceeding_success() {
    final Integer proceedingId = 123;
    final ProceedingDetail data = new ProceedingDetail(); // Populate this as needed
    final String loginId = "user1";
    final String expectedUri = "/proceedings/{proceeding-id}";

    when(caabApiWebClient.patch()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri, proceedingId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(data)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.updateProceeding(proceedingId, data, loginId);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void addProceeding_success() {
    final String applicationId = "app123";
    final ProceedingDetail proceeding = new ProceedingDetail(); // Populate this as needed
    final String loginId = "user789";
    final String expectedUri = "/applications/{applicationId}/proceedings";

    when(caabApiWebClient.post()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri, applicationId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(proceeding)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.addProceeding(applicationId, proceeding, loginId);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void updateClient_success() {
    final String clientReferenceId = "client123";
    final String loginId = "user456";
    final BaseClientDetail data = new BaseClientDetail(); // Populate this as needed
    final String expectedUri = "/applications/clients/{clientReferenceId}";

    when(caabApiWebClient.patch()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri, clientReferenceId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(data)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.updateClient(clientReferenceId, loginId, data);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void addPriorAuthority_success() {
    final String applicationId = "app123";
    final PriorAuthorityDetail priorAuthority = new PriorAuthorityDetail(); // Populate this as needed
    final String loginId = "user789";
    final String expectedUri = "/applications/{applicationId}/prior-authorities";

    when(caabApiWebClient.post()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri, applicationId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(priorAuthority)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.addPriorAuthority(applicationId, priorAuthority, loginId);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void updatePriorAuthority_success() {
    final Integer priorAuthorityId = 123;
    final PriorAuthorityDetail data = new PriorAuthorityDetail(); // Populate this as needed
    final String loginId = "user456";
    final String expectedUri = "/prior-authorities/{prior-authority-id}";

    when(caabApiWebClient.patch()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri, priorAuthorityId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(data)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.updatePriorAuthority(priorAuthorityId, data, loginId);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void deletePriorAuthority_success() {
    final Integer priorAuthorityId = 456;
    final String loginId = "user123";
    final String expectedUri = "/prior-authorities/{prior-authority-id}";

    when(caabApiWebClient.delete()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, priorAuthorityId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.deletePriorAuthority(priorAuthorityId, loginId);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void addOpponent_success() {
    final String applicationId = "app123";
    final OpponentDetail opponent = new OpponentDetail(); // Populate this as needed
    final String loginId = "user789";
    final String expectedUri = "/applications/{applicationId}/opponents";

    when(caabApiWebClient.post()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri, applicationId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(opponent)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.addOpponent(applicationId, opponent, loginId);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void updateOpponent_success() {
    final Integer opponentId = 123;
    final OpponentDetail opponent = new OpponentDetail(); // Populate this as needed
    final String loginId = "user789";
    final String expectedUri = "/opponents/{opponent-id}";

    when(caabApiWebClient.patch()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri, opponentId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(opponent)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.updateOpponent(opponentId, opponent, loginId);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void deleteOpponent_success() {
    final Integer opponentId = 123;
    final String loginId = "user123";
    final String expectedUri = "/opponents/{opponent-id}";

    when(caabApiWebClient.delete()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, opponentId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.deleteOpponent(opponentId, loginId);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void createCaseOutcome_success() {
    final CaseOutcomeDetail caseOutcomeDetail = new CaseOutcomeDetail(); // Populate this as needed
    final String loginId = "user789";
    final String expectedUri = "/case-outcomes";
    final String locationId = "123";

    when(caabApiWebClient.post()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(CaseOutcomeDetail.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.exchangeToMono(any(Function.class))).thenReturn(Mono.just(locationId));

    final Mono<String> result = caabApiClient.createCaseOutcome(loginId, caseOutcomeDetail);

    StepVerifier.create(result)
        .expectNext(locationId)
        .verifyComplete();

    verify(requestHeadersMock, times(1)).exchangeToMono(any(Function.class));
  }

  @Test
  void deleteCaseOutcome_success() {
    final Integer caseOutcomeId = 123;
    final String loginId = "user123";
    final String expectedUri = "/case-outcomes/{case-outcome-id}";

    when(caabApiWebClient.delete()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, caseOutcomeId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.deleteCaseOutcome(caseOutcomeId, loginId);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void deleteCaseOutcomes_success() {
    final String caseReferenceNumber = "123";
    final String loginId = "user123";
    final Integer providerId = 456;

    final String expectedUri = String.format(
        "/case-outcomes?case-reference-number=%s&provider-id=%s",
        caseReferenceNumber,
        providerId);

    final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(caabApiWebClient.delete()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> resultMono
        = caabApiClient.deleteCaseOutcomes(caseReferenceNumber, providerId, loginId);

    StepVerifier.create(resultMono).verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals(expectedUri, actualUri.toString());

  }

  @Test
  void getCaseOutcomes_success() {
    final String caseReferenceNumber = "123";
    final Integer providerId = 456;

    final String expectedUri = String.format(
        "/case-outcomes?case-reference-number=%s&provider-id=%s",
        caseReferenceNumber,
        providerId);

    final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CaseOutcomeDetails.class))
        .thenReturn(Mono.just(new CaseOutcomeDetails()));

    final Mono<CaseOutcomeDetails> resultMono
        = caabApiClient.getCaseOutcomes(caseReferenceNumber, providerId);

    StepVerifier.create(resultMono)
        .expectNext(Objects.requireNonNull(resultMono.block()))
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals(expectedUri, actualUri.toString());

  }

  @Test
  void getCaseOutcome_success() {

    final String id = "123";
    final String expectedUri = "/case-outcomes/{case-outcome-id}";

    final CaseOutcomeDetail caseOutcomeDetail = new CaseOutcomeDetail();

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CaseOutcomeDetail.class)).thenReturn(Mono.just(caseOutcomeDetail));

    final Mono<CaseOutcomeDetail> result
        = caabApiClient.getCaseOutcome(id);

    StepVerifier.create(result)
        .expectNext(caseOutcomeDetail)
        .verifyComplete();
  }

  @Test
  void createEvidenceDocument_success() {
    final EvidenceDocumentDetail evidenceDocument = new EvidenceDocumentDetail(); // Populate this as needed
    final String loginId = "user789";
    final String expectedUri = "/evidence";
    final String locationId = "123";

    when(caabApiWebClient.post()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(EvidenceDocumentDetail.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.exchangeToMono(any(Function.class))).thenReturn(Mono.just(locationId));

    final Mono<String> result = caabApiClient.createEvidenceDocument(evidenceDocument, loginId);

    StepVerifier.create(result)
        .expectNext(locationId)
        .verifyComplete();

    verify(requestHeadersMock, times(1)).exchangeToMono(any(Function.class));
  }

  @Test
  void deleteEvidenceDocument_success() {
    final Integer evidenceDocumentId = 123;
    final String loginId = "user123";
    final String expectedUri = "/evidence/{evidence-document-id}";

    when(caabApiWebClient.delete()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, evidenceDocumentId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.deleteEvidenceDocument(evidenceDocumentId, loginId);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void deleteEvidenceDocuments_success() {
    final String applicationOrOutcomeId = "123";
    final String caseReferenceNumber = "456";
    final Integer providerId = 789;
    final String documentType = "docType";
    final String ccmsModule = "ccmsMod";
    final Boolean transferPending = true;
    final String loginId = "user123";
    final String expectedUri = String.format(
        "/evidence?application-or-outcome-id=%s&case-reference-number=%s&provider-id=%s&document-type=%s&ccms-module=%s&transfer-pending=%s",
        applicationOrOutcomeId,
        caseReferenceNumber,
        providerId,
        documentType,
        ccmsModule,
        transferPending);

    final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(caabApiWebClient.delete()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.deleteEvidenceDocuments(
        applicationOrOutcomeId,
        caseReferenceNumber,
        providerId,
        documentType,
        ccmsModule,
        transferPending,
        loginId);

    StepVerifier.create(result).verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    assertEquals(expectedUri, actualUri.toString());
  }

  @Test
  void getEvidenceDocuments_success() {
    final String applicationOrOutcomeId = "234";
    final String caseReferenceNumber = "123";
    final Integer providerId = 456;
    final String documentType = "docType";
    final String ccmsModule = "A";
    final Boolean transferPending = true;
    final String size = "1000";

    final String expectedUri = String.format(
        "/evidence?size=%s&application-or-outcome-id=%s&case-reference-number=%s&provider-id=%s&document-type=%s&ccms-module=%s&transfer-pending=%s",
        size,
        applicationOrOutcomeId,
        caseReferenceNumber,
        providerId,
        documentType,
        ccmsModule,
        transferPending);

    final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(EvidenceDocumentDetails.class))
        .thenReturn(Mono.just(new EvidenceDocumentDetails()));

    final Mono<EvidenceDocumentDetails> resultMono = caabApiClient.getEvidenceDocuments(
        applicationOrOutcomeId,
        caseReferenceNumber,
        providerId,
        documentType,
        ccmsModule,
        transferPending);

    StepVerifier.create(resultMono)
        .expectNext(Objects.requireNonNull(resultMono.block()))
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals(expectedUri, actualUri.toString());

  }

  @Test
  void getEvidenceDocument_success() {

    final Integer id = 123;
    final String expectedUri = "/evidence/{evidence-document-id}";

    final EvidenceDocumentDetail evidenceDocumentDetail = new EvidenceDocumentDetail();

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(EvidenceDocumentDetail.class)).thenReturn(
        Mono.just(evidenceDocumentDetail));

    final Mono<EvidenceDocumentDetail> result
        = caabApiClient.getEvidenceDocument(id);

    StepVerifier.create(result)
        .expectNext(evidenceDocumentDetail)
        .verifyComplete();
  }

  @Test
  void createNotificationAttachment_success() {
    final NotificationAttachmentDetail notificationAttachment =
        new NotificationAttachmentDetail(); // Populate this as needed
    final String loginId = "user1";
    final String expectedUri = "/notification-attachments";
    final String locationId = "123";

    when(caabApiWebClient.post()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(NotificationAttachmentDetail.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.exchangeToMono(any(Function.class))).thenReturn(Mono.just(locationId));

    final Mono<String> result = caabApiClient.createNotificationAttachment(notificationAttachment, loginId);

    StepVerifier.create(result)
        .expectNext(locationId)
        .verifyComplete();

    verify(requestHeadersMock, times(1)).exchangeToMono(any(Function.class));
  }

  @Test
  void updateNotificationAttachment_success() {
    final NotificationAttachmentDetail notificationAttachment =
        new NotificationAttachmentDetail(); // Populate this as needed
    notificationAttachment.setId(123);

    final Integer notificationAttachmentId = 123;
    final String loginId = "user1";
    final String expectedUri = "/notification-attachments/{notification-attachment-id}";

    when(caabApiWebClient.put()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri, notificationAttachmentId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(NotificationAttachmentDetail.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.updateNotificationAttachment(notificationAttachment, loginId);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void deleteNotificationAttachment_success() {
    final Integer notificationAttachmentId = 123;
    final String loginId = "user123";
    final String expectedUri = "/notification-attachments/{notification-attachment-id}";

    when(caabApiWebClient.delete()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, notificationAttachmentId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.deleteNotificationAttachment(notificationAttachmentId, loginId);

    StepVerifier.create(result).verifyComplete();
  }

  @Test
  void deleteNotificationAttachments_success() {
    final String notificationReference = "123";
    final Integer providerId = 456;
    final String documentType = "docType";
    final String sendBy = "sendBy";
    final String loginId = "user123";

    final String expectedUri = String.format(
        "/notification-attachments?notification-reference=%s"
            + "&provider-id=%s&document-type=%s&send-by=%s",
        notificationReference,
        providerId,
        documentType,
        sendBy);

    final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(caabApiWebClient.delete()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.deleteNotificationAttachments(notificationReference,
        providerId, documentType, sendBy, loginId);

    StepVerifier.create(result).verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    assertEquals(expectedUri, actualUri.toString());
  }

  @Test
  void getNotificationAttachments_success() {
    final String notificationReference = "123";
    final Integer providerId = 456;
    final String documentType = "docType";
    final String sendBy = "sendBy";
    final String size = "1000";

    final String expectedUri = String.format(
        "/notification-attachments?size=%s&notification-reference=%s"
            + "&provider-id=%s&document-type=%s&send-by=%s",
        size,
        notificationReference,
        providerId,
        documentType,
        sendBy);

    final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(NotificationAttachmentDetails.class))
        .thenReturn(Mono.just(new NotificationAttachmentDetails()));

    final Mono<NotificationAttachmentDetails> resultMono = caabApiClient.getNotificationAttachments(
        notificationReference,
        providerId,
        documentType,
        sendBy);

    StepVerifier.create(resultMono)
        .expectNext(Objects.requireNonNull(resultMono.block()))
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals(expectedUri, actualUri.toString());

  }

  @Test
  void getNotificationAttachment_success() {

    final Integer id = 123;
    final String expectedUri = "/notification-attachments/{notification-attachment-id}";

    final NotificationAttachmentDetail notificationAttachmentDetail = new NotificationAttachmentDetail();

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(NotificationAttachmentDetail.class)).thenReturn(
        Mono.just(notificationAttachmentDetail));

    final Mono<NotificationAttachmentDetail> result
        = caabApiClient.getNotificationAttachment(id);

    StepVerifier.create(result)
        .expectNext(notificationAttachmentDetail)
        .verifyComplete();
  }

  @Test
  @DisplayName("deleteProceeding succeeds when called with valid proceedingId and loginId")
  void deleteProceeding_success() {
    final Integer proceedingId = 123;
    final String loginId = "user456";

    when(caabApiWebClient.delete()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri("/proceedings/{proceeding-id}", proceedingId))
        .thenReturn(requestHeadersMock);
    when(requestHeadersMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.deleteProceeding(proceedingId, loginId);

    StepVerifier.create(result).verifyComplete();

    verify(requestHeadersUriMock).uri("/proceedings/{proceeding-id}", proceedingId);
    verify(requestHeadersMock).header("Caab-User-Login-Id", loginId);
    verify(requestHeadersMock).retrieve();
  }

  @Test
  @DisplayName("getScopeLimitations succeeds when called with valid proceedingId")
  void getScopeLimitations_success() {
    final Integer proceedingId = 123;
    final List<ScopeLimitationDetail> mockScopeLimitations = List.of(new ScopeLimitationDetail());

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri("/proceedings/{id}/scope-limitations", proceedingId))
        .thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(new ParameterizedTypeReference<List<ScopeLimitationDetail>>() {}))
        .thenReturn(Mono.just(mockScopeLimitations));

    final Mono<List<ScopeLimitationDetail>> result = caabApiClient.getScopeLimitations(proceedingId);

    StepVerifier.create(result)
        .expectNext(mockScopeLimitations)
        .verifyComplete();

    verify(requestHeadersUriMock).uri("/proceedings/{id}/scope-limitations", proceedingId);
    verify(requestHeadersMock).retrieve();
  }

  @Test
  @DisplayName("patchApplication succeeds when called with valid id, ApplicationDetail, and loginId")
  void patchApplication_success() {
    final String id = "app123";
    final String loginId = "user789";
    final ApplicationDetail patch = new ApplicationDetail(); // Populate this with test data as needed

    when(caabApiWebClient.patch()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri("/applications/{id}", id)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(patch)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.patchApplication(id, patch, loginId);

    StepVerifier.create(result).verifyComplete();

    verify(requestBodyUriMock).uri("/applications/{id}", id);
    verify(requestBodyMock).header("Caab-User-Login-Id", loginId);
    verify(requestBodyMock).contentType(MediaType.APPLICATION_JSON);
    verify(requestBodyMock).bodyValue(patch);
    verify(requestHeadersMock).retrieve();
  }

}
