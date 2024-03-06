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
import uk.gov.laa.ccms.caab.model.Address;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.BaseClient;
import uk.gov.laa.ccms.caab.model.CostStructure;
import uk.gov.laa.ccms.caab.model.LinkedCase;
import uk.gov.laa.ccms.caab.model.Opponent;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.PriorAuthority;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupDetail;

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
  private ApiClientErrorHandler apiClientErrorHandler;

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

    final int page = 0;
    final int size = 2;

    final String expectedUri = String.format(
        "/applications?case-reference-number=%s&provider-case-ref=%s&client-surname=%s&client-reference=%s&fee-earner=%s&office-id=%s&status=%s&page=%s&size=%s",
        caseSearchCriteria.getCaseReference(),
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
        = caabApiClient.getApplications(caseSearchCriteria, page, size);

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

    final Address mockApplication = new Address();

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Address.class)).thenReturn(Mono.just(mockApplication));

    final Mono<Address> addressMono
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

    final Address correspondenceAddress = new Address();

    when(caabApiWebClient.put()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri, id)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(any(MediaType.class))).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(Address.class))).thenReturn(requestHeadersMock);
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

    final List<Opponent> mockOpponents = new ArrayList<>();

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(new ParameterizedTypeReference<List<Opponent>>() {})).thenReturn(
        Mono.just(mockOpponents));

    final Mono<List<Opponent>> listMono
        = caabApiClient.getOpponents(id);

    StepVerifier.create(listMono)
        .expectNext(mockOpponents)
        .verifyComplete();
  }

  @Test
  void getLinkedCases_success() {
    final String id = "123";
    final String expectedUri = "/applications/{id}/linked-cases";

    final List<LinkedCase> mockLinkedCases = new ArrayList<>(); // Add mock data to the list as needed

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(new ParameterizedTypeReference<List<LinkedCase>>() {}))
        .thenReturn(Mono.just(mockLinkedCases));

    final Mono<List<LinkedCase>> linkedCasesMono = caabApiClient.getLinkedCases(id);

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
    final LinkedCase linkedCaseData = new LinkedCase(); // Populate this with test data as needed
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
    final LinkedCase linkedCaseData = new LinkedCase(); // Populate this with test data as needed
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

    final List<Proceeding> mockProceedings = new ArrayList<>(); // Populate this as needed

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(new ParameterizedTypeReference<List<Proceeding>>() {})).thenReturn(Mono.just(mockProceedings));

    final Mono<List<Proceeding>> result = caabApiClient.getProceedings(id);

    StepVerifier.create(result)
        .expectNext(mockProceedings)
        .verifyComplete();
  }

  @Test
  void getCosts_success() {
    final String id = "123";
    final String expectedUri = "/applications/{id}/cost-structure";

    final CostStructure mockCostStructure = new CostStructure(); // Populate this as needed

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(new ParameterizedTypeReference<CostStructure>() {})).thenReturn(Mono.just(mockCostStructure));

    final Mono<CostStructure> result = caabApiClient.getCostStructure(id);

    StepVerifier.create(result)
        .expectNext(mockCostStructure)
        .verifyComplete();
  }

  @Test
  void updateCosts_success() {
    final String id = "123";
    final CostStructure costs = new CostStructure(); // Populate this as needed
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

    final List<PriorAuthority> mockPriorAuthorities = new ArrayList<>(); // Populate this as needed

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(new ParameterizedTypeReference<List<PriorAuthority>>() {})).thenReturn(Mono.just(mockPriorAuthorities));

    final Mono<List<PriorAuthority>> result = caabApiClient.getPriorAuthorities(id);

    StepVerifier.create(result)
        .expectNext(mockPriorAuthorities)
        .verifyComplete();
  }

  @Test
  void updateProceeding_success() {
    final Integer proceedingId = 123;
    final Proceeding data = new Proceeding(); // Populate this as needed
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
    final Proceeding proceeding = new Proceeding(); // Populate this as needed
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
    final BaseClient data = new BaseClient(); // Populate this as needed
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
    final PriorAuthority priorAuthority = new PriorAuthority(); // Populate this as needed
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
    final PriorAuthority data = new PriorAuthority(); // Populate this as needed
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
    final Opponent opponent = new Opponent(); // Populate this as needed
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

}
