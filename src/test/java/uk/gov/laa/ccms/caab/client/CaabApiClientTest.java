package uk.gov.laa.ccms.caab.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

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

}
