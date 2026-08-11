package uk.gov.laa.ccms.caab.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.opa.session.OpaSessionJson;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Connector API client tests")
class ConnectorApiClientTest {

  @Mock private WebClient connectorApiWebClient;
  @Mock private WebClient.RequestBodyUriSpec requestBodyUriMock;
  @Mock private WebClient.RequestBodySpec requestBodyMock;
  @Mock private WebClient.RequestHeadersSpec requestHeadersMock;
  @Mock private WebClient.ResponseSpec responseMock;

  private ConnectorApiClient client() {
    return new ConnectorApiClient(connectorApiWebClient);
  }

  private OpaSessionJson session() {
    final OpaSessionJson session = new OpaSessionJson();
    session.setAssessment("poaAssessment_PREPOP");
    session.setTargetId("300000319502");
    return session;
  }

  @Test
  @DisplayName("Posts the session as text/plain to puiAssessService and returns the assessed one")
  void assessesSession() {
    final OpaSessionJson assessed = new OpaSessionJson();
    assessed.setAssessment("poaAssessment_PREPOP");

    when(connectorApiWebClient.post()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri("/service-tds/puiAssessService")).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.TEXT_PLAIN)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(String.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(OpaSessionJson.class)).thenReturn(Mono.just(assessed));

    StepVerifier.create(client().assess(session())).expectNext(assessed).verifyComplete();

    // The connector declares the endpoint as text/plain even though the body is JSON.
    verify(requestBodyMock).contentType(MediaType.TEXT_PLAIN);

    final ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(requestBodyMock).bodyValue(bodyCaptor.capture());
    assertThat(bodyCaptor.getValue()).contains("\"targetID\":\"300000319502\"");
  }

  @Test
  @DisplayName("Surfaces a connector failure as an error rather than an empty session")
  void surfacesFailure() {
    when(connectorApiWebClient.post()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri("/service-tds/puiAssessService")).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.TEXT_PLAIN)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(any(String.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(OpaSessionJson.class))
        .thenReturn(Mono.error(new RuntimeException("connector down")));

    StepVerifier.create(client().assess(session())).expectError().verify();
  }
}
