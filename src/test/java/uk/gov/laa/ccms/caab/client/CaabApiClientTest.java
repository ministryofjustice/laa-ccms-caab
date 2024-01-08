package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.model.Address;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.LinkedCase;

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
  private CaabApiClientErrorHandler caabApiClientErrorHandler;

  @InjectMocks
  private CaabApiClient caabApiClient;

  @Test
  void createApplication_success() {
    String loginId = "user1";
    ApplicationDetail application =
        new ApplicationDetail(null, null, null, null); // Populate as needed
    String expectedUri = "/applications";
    String locationId = "123"; // Replace with your expected location header

    when(caabApiWebClient.post()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(any(MediaType.class))).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(ApplicationDetail.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.exchangeToMono(any(Function.class))).thenReturn(Mono.just(locationId));

    Mono<String> result = caabApiClient.createApplication(loginId, application);

    StepVerifier.create(result)
        .expectNext(locationId) // Expect the location header value
        .verifyComplete();

    verify(requestHeadersMock, times(1)).exchangeToMono(any(Function.class));
  }


  @Test
  void getApplication_success() {

    String id = "123";
    String expectedUri = "/applications/{id}";

    ApplicationDetail mockApplication = new ApplicationDetail(null, null,null, null);

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ApplicationDetail.class)).thenReturn(Mono.just(mockApplication));

    Mono<ApplicationDetail> applicationDetailMono
        = caabApiClient.getApplication(id);

    StepVerifier.create(applicationDetailMono)
        .expectNext(mockApplication)
        .verifyComplete();
  }

  @Test
  void getApplicationType_success() {

    String id = "123";
    String expectedUri = "/applications/{id}/application-type";

    ApplicationType mockApplication = new ApplicationType();

    when(caabApiWebClient.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, id)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ApplicationType.class)).thenReturn(Mono.just(mockApplication));

    Mono<ApplicationType> applicationTypeMono
        = caabApiClient.getApplicationType(id);

    StepVerifier.create(applicationTypeMono)
        .expectNext(mockApplication)
        .verifyComplete();
  }

  @Test
  void getCorrespondenceAddress_success() {

    String id = "123";
    String expectedUri = "/applications/{id}/correspondence-address";

    Address mockApplication = new Address();

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
    final String applicationId = "app123";
    final String linkedCaseId = "case456";
    final String loginId = "user789";
    final String expectedUri = "/applications/{applicationId}/linked-cases/{linkedCaseId}";

    when(caabApiWebClient.delete()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, applicationId, linkedCaseId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.removeLinkedCase(applicationId, linkedCaseId, loginId);

    StepVerifier.create(result)
        .verifyComplete();

    verify(requestBodyMock, times(1)).header("Caab-User-Login-Id", loginId);
    verify(responseMock, times(1)).bodyToMono(Void.class);
  }

  @Test
  void updateLinkedCase_success() {
    final String applicationId = "app123";
    final String linkedCaseId = "case456";
    final LinkedCase linkedCaseData = new LinkedCase(); // Populate this with test data as needed
    final String loginId = "user789";
    final String expectedUri = "/applications/{applicationId}/linked-cases/{linkedCaseId}";

    when(caabApiWebClient.patch()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri, applicationId, linkedCaseId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(linkedCaseData)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = caabApiClient.updateLinkedCase(applicationId, linkedCaseId, linkedCaseData, loginId);

    StepVerifier.create(result)
        .verifyComplete();

    verify(requestBodyMock, times(1)).header("Caab-User-Login-Id", loginId);
    verify(requestBodyMock, times(1)).contentType(MediaType.APPLICATION_JSON);
    verify(requestBodyMock, times(1)).bodyValue(linkedCaseData);
    verify(responseMock, times(1)).bodyToMono(Void.class);
  }





}
