package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
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
import uk.gov.laa.ccms.caab.model.os.OrdinanceSurveyHeader;
import uk.gov.laa.ccms.caab.model.os.OrdinanceSurveyResponse;
import uk.gov.laa.ccms.caab.model.os.OrdinanceSurveyResult;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class OrdinanceSurveyApiClientTest {

  @Mock private WebClient webClientMock;
  @Mock private WebClient.RequestHeadersSpec requestHeadersMock;
  @Mock private WebClient.RequestHeadersUriSpec requestHeadersUriMock;
  @Mock private WebClient.ResponseSpec responseMock;

  private OrdinanceSurveyApiClient ordinanceSurveyApiClient;

  private String testKey = "test";

  ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

  @BeforeEach
  private void setup() {
    this.ordinanceSurveyApiClient = new OrdinanceSurveyApiClient(webClientMock, testKey);
  }

  @Test
  void getAddresses_returnData() {
    String postcode = "AB12CD";
    OrdinanceSurveyResponse ordinanceSurveyResponse = response(0, List.of());

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(OrdinanceSurveyResponse.class))
        .thenReturn(Mono.just(ordinanceSurveyResponse));

    Mono<OrdinanceSurveyResponse> ordinanceSurveyResponseMono =
        ordinanceSurveyApiClient.getAddresses(postcode);

    StepVerifier.create(ordinanceSurveyResponseMono)
        .expectNext(ordinanceSurveyResponse)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    assertEquals(
        "postcode=%s&key=%s&maxresults=100&offset=0".formatted(postcode, testKey),
        actualUri.getQuery());
  }

  @Test
  void getAddresses_fetchesEveryPage() {
    final List<OrdinanceSurveyResult> firstPage = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      firstPage.add(new OrdinanceSurveyResult());
    }
    final OrdinanceSurveyResponse firstResponse = response(101, firstPage);
    final OrdinanceSurveyResponse secondResponse =
        response(101, List.of(new OrdinanceSurveyResult()));

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(OrdinanceSurveyResponse.class))
        .thenReturn(Mono.just(firstResponse), Mono.just(secondResponse));

    StepVerifier.create(ordinanceSurveyApiClient.getAddresses("AB12CD"))
        .assertNext(response -> assertEquals(101, response.getResults().size()))
        .verifyComplete();

    final List<Function<UriBuilder, URI>> requestedUris = uriCaptor.getAllValues();
    assertEquals(2, requestedUris.size());
    assertEquals(
        "postcode=AB12CD&key=test&maxresults=100&offset=0",
        requestedUris.get(0).apply(UriComponentsBuilder.newInstance()).getQuery());
    assertEquals(
        "postcode=AB12CD&key=test&maxresults=100&offset=100",
        requestedUris.get(1).apply(UriComponentsBuilder.newInstance()).getQuery());
  }

  @Test
  void getAddressesRejectsAnIncompletePage() {
    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(OrdinanceSurveyResponse.class))
        .thenReturn(Mono.just(response(101, List.of(new OrdinanceSurveyResult()))));

    StepVerifier.create(ordinanceSurveyApiClient.getAddresses("AB12CD"))
        .expectError(OrdinanceSurveyResponseException.class)
        .verify();
  }

  private OrdinanceSurveyResponse response(
      final int totalResults, final List<OrdinanceSurveyResult> results) {
    final OrdinanceSurveyHeader header = new OrdinanceSurveyHeader();
    header.setTotalResults(totalResults);
    final OrdinanceSurveyResponse response = new OrdinanceSurveyResponse();
    response.setHeader(header);
    response.setResults(results);
    return response;
  }
}
