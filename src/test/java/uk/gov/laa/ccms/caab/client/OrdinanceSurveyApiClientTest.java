package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.model.os.OrdinanceSurveyResponse;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class OrdinanceSurveyApiClientTest {

  @Mock
  private WebClient webClientMock;
  @Mock
  private WebClient.RequestHeadersSpec requestHeadersMock;
  @Mock
  private WebClient.RequestHeadersUriSpec requestHeadersUriMock;
  @Mock
  private WebClient.ResponseSpec responseMock;

  private OrdinanceSurveyApiClient ordinanceSurveyApiClient;

  private String testKey = "test";

  ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

  @BeforeEach
  void setup() {
    this.ordinanceSurveyApiClient = new OrdinanceSurveyApiClient(webClientMock, testKey);
  }

  @Test
  void getAddresses_returnData() {
    String postcode = "AB12CD";
    String expectedUri = "/search/places/v1/postcode";
    OrdinanceSurveyResponse ordinanceSurveyResponse = new OrdinanceSurveyResponse();

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(OrdinanceSurveyResponse.class)).thenReturn(Mono.just(ordinanceSurveyResponse));

    Mono<OrdinanceSurveyResponse> ordinanceSurveyResponseMono = ordinanceSurveyApiClient.getAddresses(postcode);

    StepVerifier.create(ordinanceSurveyResponseMono)
        .expectNext(ordinanceSurveyResponse)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals(
        "%s?postcode=%s&key=%s".formatted(expectedUri, postcode, testKey),
        actualUri.toString());
  }

}
