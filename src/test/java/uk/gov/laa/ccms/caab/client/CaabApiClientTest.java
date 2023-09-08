package uk.gov.laa.ccms.caab.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;

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

    when(caabApiWebClient.post()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri(expectedUri)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(any(MediaType.class))).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(ApplicationDetail.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.fromRunnable(() -> {
    }));

    Mono<Void> result = caabApiClient.createApplication(loginId, application);

    StepVerifier.create(result)
        .verifyComplete();
  }

}
