package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;


@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
public class EbsApiClientTest {

  @Mock
  private WebClient webClientMock;
  @Mock
  private WebClient.RequestHeadersSpec requestHeadersMock;
  @Mock
  private WebClient.RequestHeadersUriSpec requestHeadersUriMock;
  @Mock
  private WebClient.ResponseSpec responseMock;

  @Mock
  private EbsApiClientErrorHandler ebsApiClientErrorHandler;

  @InjectMocks
  private EbsApiClient ebsApiClient;



  ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

  @Test
  void getUser_returnData() {

    String loginId = "user1";
    String expectedUri = "/users/{loginId}";

    UserDetail mockUser = new UserDetail();
    mockUser.setLoginId(loginId);

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, loginId)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(UserDetail.class)).thenReturn(Mono.just(mockUser));

    Mono<UserDetail> userDetailsMono = ebsApiClient.getUser(loginId);

    StepVerifier.create(userDetailsMono)
        .expectNextMatches(user -> user.getLoginId().equals(loginId))
        .verifyComplete();
  }

  @Test
  void getUser_notFound() {
    String loginId = "user1";
    String expectedUri = "/users/{loginId}";

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, loginId)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(UserDetail.class)).thenReturn(Mono.error(
        new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

    when(ebsApiClientErrorHandler.handleUserError(eq(loginId),
        any(WebClientResponseException.class))).thenReturn(Mono.empty());

    Mono<UserDetail> userDetailsMono = ebsApiClient.getUser(loginId);

    StepVerifier.create(userDetailsMono)
        .verifyComplete();
  }

  @Test
  void getCommonValues_returnsData() {
    String type = "type1";
    String code = "code1";
    String sort = "sort1";
    CommonLookupDetail commonValues = new CommonLookupDetail();

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(Mono.just(commonValues));

    Mono<CommonLookupDetail> commonValuesMono = ebsApiClient.getCommonValues(type, code, sort);

    StepVerifier.create(commonValuesMono)
        .expectNext(commonValues)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals("/lookup/common?type=type1&code=code1&sort=sort1", actualUri.toString());
  }

  @Test
  void getCommonValues_notFound() {
    String type = "type1";
    String code = "code1";
    String sort = "sort1";

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(Mono.error(
        new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

    when(ebsApiClientErrorHandler.handleCommonValuesError(eq(type), eq(code), eq(sort),
        any(WebClientResponseException.class))).thenReturn(Mono.empty());

    Mono<CommonLookupDetail> commonValuesMono = ebsApiClient.getCommonValues(type, code, sort);

    StepVerifier.create(commonValuesMono)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals("/lookup/common?type=type1&code=code1&sort=sort1", actualUri.toString());
  }

  @Test
  void getCaseStatusValuesCopyAllowed_returnsData() {
    CaseStatusLookupDetail caseStatusLookupDetail = new CaseStatusLookupDetail();

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CaseStatusLookupDetail.class)).thenReturn(
        Mono.just(caseStatusLookupDetail));

    Mono<CaseStatusLookupDetail> lookupDetailMono = ebsApiClient.getCaseStatusValues(true);

    StepVerifier.create(lookupDetailMono)
        .expectNext(caseStatusLookupDetail)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals("/lookup/case-status?copy-allowed=true", actualUri.toString());
  }

  @Test
  void getProvider_returnsData() {
    Integer providerId = 123;
    ProviderDetail providerDetail = new ProviderDetail();

    String expectedUri = "/providers/{providerId}";

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, String.valueOf(providerId))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ProviderDetail.class)).thenReturn(Mono.just(providerDetail));

    Mono<ProviderDetail> providerDetailMono = ebsApiClient.getProvider(providerId);

    StepVerifier.create(providerDetailMono)
        .expectNext(providerDetail)
        .verifyComplete();
  }

  @Test
  void getPersonRelationshipsToCaseValues_returnsData() {
    RelationshipToCaseLookupDetail relationshipToCaseLookupDetail
        = new RelationshipToCaseLookupDetail();

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(RelationshipToCaseLookupDetail.class))
        .thenReturn(Mono.just(relationshipToCaseLookupDetail));

    Mono<RelationshipToCaseLookupDetail> relationshipToCaseLookupDetailMono =
        ebsApiClient.getPersonRelationshipsToCaseValues();

    StepVerifier.create(relationshipToCaseLookupDetailMono)
        .expectNext(relationshipToCaseLookupDetail)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals("/lookup/person-to-case-relationships", actualUri.toString());
  }

  @Test
  void getOrganisationRelationshipsToCaseValues_returnsData() {
    RelationshipToCaseLookupDetail relationshipToCaseLookupDetail
        = new RelationshipToCaseLookupDetail();

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(RelationshipToCaseLookupDetail.class))
        .thenReturn(Mono.just(relationshipToCaseLookupDetail));

    Mono<RelationshipToCaseLookupDetail> relationshipToCaseLookupDetailMono =
        ebsApiClient.getOrganisationRelationshipsToCaseValues();

    StepVerifier.create(relationshipToCaseLookupDetailMono)
        .expectNext(relationshipToCaseLookupDetail)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals("/lookup/organisation-to-case-relationships", actualUri.toString());
  }

  @Test
  void getAmendmentTypes_returnsData() {
    String applicationType = "appType1";
    AmendmentTypeLookupDetail amendmentTypeLookupDetail = new AmendmentTypeLookupDetail();

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(AmendmentTypeLookupDetail.class)).thenReturn(
        Mono.just(amendmentTypeLookupDetail));

    Mono<AmendmentTypeLookupDetail> amendmentTypesMono =
        ebsApiClient.getAmendmentTypes(applicationType);

    StepVerifier.create(amendmentTypesMono)
        .expectNext(amendmentTypeLookupDetail)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals("/lookup/amendment-types?application-type=appType1", actualUri.toString());
  }

  @Test
  void getAmendmentTypes_notFound() {
    String applicationType = "appType1";

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(AmendmentTypeLookupDetail.class)).thenReturn(Mono.error(
        new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

    when(ebsApiClientErrorHandler.handleAmendmentTypeLookupError(eq(applicationType),
        any(WebClientResponseException.class))).thenReturn(Mono.empty());

    Mono<AmendmentTypeLookupDetail> amendmentTypesMono =
        ebsApiClient.getAmendmentTypes(applicationType);

    StepVerifier.create(amendmentTypesMono)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals("/lookup/amendment-types?application-type=appType1", actualUri.toString());
  }

  @Test
  void getCountries_returnsData() {
    CommonLookupDetail commonValues = new CommonLookupDetail();
    // Set up your mock commonValues

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(Mono.just(commonValues));

    Mono<CommonLookupDetail> countriesMono = ebsApiClient.getCountries();

    StepVerifier.create(countriesMono)
        .expectNext(commonValues)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals("/lookup/countries?size=1000", actualUri.toString());
  }

  @Test
  void getCountries_notFound() {
    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(Mono.error(
        new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

    when(ebsApiClientErrorHandler.handleCountryLookupError(any(WebClientResponseException.class)))
        .thenReturn(Mono.empty());

    Mono<CommonLookupDetail> countriesMono = ebsApiClient.getCountries();

    StepVerifier.create(countriesMono)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals("/lookup/countries?size=1000", actualUri.toString());
  }


}
