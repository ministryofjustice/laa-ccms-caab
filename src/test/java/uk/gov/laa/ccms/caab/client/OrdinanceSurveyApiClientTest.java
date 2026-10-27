package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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
        "postcode=%s&key=%s&maxresults=100".formatted(postcode, testKey), actualUri.getQuery());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 25, 100, 101})
  void getAddresses_acceptsAnyResultCountWithoutAHeader(final int count) {
    final List<OrdinanceSurveyResult> results = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      results.add(new OrdinanceSurveyResult());
    }
    final OrdinanceSurveyResponse response = new OrdinanceSurveyResponse();
    response.setResults(results);

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(OrdinanceSurveyResponse.class)).thenReturn(Mono.just(response));

    StepVerifier.create(ordinanceSurveyApiClient.getAddresses("AB12CD"))
        .assertNext(actual -> assertEquals(results, actual.getResults()))
        .verifyComplete();

    verify(webClientMock).get();
    assertEquals(
        "postcode=AB12CD&key=test&maxresults=100",
        uriCaptor.getValue().apply(UriComponentsBuilder.newInstance()).getQuery());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(ints = {-1, 0, 101})
  void getAddresses_ignoresMissingOrInconsistentTotalResults(final Integer totalResults) {
    final OrdinanceSurveyResponse response =
        response(totalResults, List.of(new OrdinanceSurveyResult()));

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(OrdinanceSurveyResponse.class)).thenReturn(Mono.just(response));

    StepVerifier.create(ordinanceSurveyApiClient.getAddresses("AB12CD"))
        .expectNext(response)
        .verifyComplete();

    verify(webClientMock).get();
  }

  @Test
  void getAddressesRejectsAMissingResultsCollection() {
    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(OrdinanceSurveyResponse.class))
        .thenReturn(Mono.just(response(0, null)));

    StepVerifier.create(ordinanceSurveyApiClient.getAddresses("AB12CD"))
        .expectError(OrdinanceSurveyResponseException.class)
        .verify();
  }

  private OrdinanceSurveyResponse response(
      final Integer totalResults, final List<OrdinanceSurveyResult> results) {
    final OrdinanceSurveyHeader header = new OrdinanceSurveyHeader();
    header.setTotalResults(totalResults);
    final OrdinanceSurveyResponse response = new OrdinanceSurveyResponse();
    response.setHeader(header);
    response.setResults(results);
    return response;
  }
}
