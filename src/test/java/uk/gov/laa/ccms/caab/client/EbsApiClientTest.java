package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.BaseUser;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.ProceedingDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetails;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.data.model.UserDetails;


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
    String descr = "desc1";
    String sort = "sort1";
    CommonLookupDetail commonValues = new CommonLookupDetail();

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(Mono.just(commonValues));

    Mono<CommonLookupDetail> commonValuesMono = ebsApiClient.getCommonValues(type, code, descr, sort);

    StepVerifier.create(commonValuesMono)
        .expectNext(commonValues)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals(String.format("/lookup/common?type=%s&code=%s&description=%s&sort=%s",
            type, code, descr, sort), actualUri.toString());
  }

  @Test
  void getCommonValues_notFound() {
    String type = "type1";
    String code = "code1";
    String descr = "desc1";
    String sort = "sort1";

    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(Mono.error(
        new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

    when(ebsApiClientErrorHandler.handleCommonValuesError(eq(type), eq(code), eq(sort),
        any(WebClientResponseException.class))).thenReturn(Mono.empty());

    Mono<CommonLookupDetail> commonValuesMono = ebsApiClient.getCommonValues(
        type, code, descr, sort);

    StepVerifier.create(commonValuesMono)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals(String.format("/lookup/common?type=%s&code=%s&description=%s&sort=%s",
        type, code, descr, sort), actualUri.toString());
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
        ebsApiClient.getOrganisationRelationshipsToCaseValues(null, null);

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

  @Test
  void getProceeding_returnsData() {
    String proceedingCode = "PROC1";
    ProceedingDetail proceedingDetail = new ProceedingDetail();

    String expectedUri = "/proceedings/{proceeding-code}";

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, proceedingCode)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ProceedingDetail.class)).thenReturn(Mono.just(proceedingDetail));

    Mono<ProceedingDetail> proceedingDetailMono = ebsApiClient.getProceeding(proceedingCode);

    StepVerifier.create(proceedingDetailMono)
        .expectNext(proceedingDetail)
        .verifyComplete();
  }
  @Test
  void getUsersForProvider_returnsData() {
    // Mock some objects
    String username = "user1";
    String expectedUri = "/users?provider-id=123";
    Integer providerId = 123;
    BaseUser mockUser = new BaseUser()
        .username(username)
        .userId(123);
    UserDetails mockDetails = new UserDetails()
        .addContentItem(mockUser);

    // Stubs
    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(UserDetails.class)).thenReturn(Mono.just(mockDetails));

    // Call the method
    Mono<UserDetails> userDetailsMono = ebsApiClient.getUsers(providerId);

    // Assertions
    StepVerifier.create(userDetailsMono)
        .expectNext(mockDetails)
        .verifyComplete();
    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());
    assertEquals(expectedUri, actualUri.toString());

  }

  @Test
  void getUsersForProvider_notFound() {
    Integer providerId = 123;

    String expectedUri = "/users?provider-id=123";

    // Stubs
    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(UserDetails.class)).thenReturn(Mono.error(
        new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));
    when(ebsApiClientErrorHandler.handleUsersError(eq(providerId.toString()),
        any(WebClientResponseException.class))).thenReturn(Mono.empty());

    Mono<UserDetails> userDetailsMono = ebsApiClient.getUsers(providerId);
    StepVerifier.create(userDetailsMono)
        .verifyComplete();
    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());
    assertEquals(expectedUri, actualUri.toString());
  }

  @Test
  void getScopeLimitations_returnsData() {
    uk.gov.laa.ccms.data.model.ScopeLimitationDetail scopeLimitationDetail =
        new uk.gov.laa.ccms.data.model.ScopeLimitationDetail()
        .scopeLimitations("scopeLimitations")
        .categoryOfLaw("categoryOfLaw")
        .matterType("matterType")
        .proceedingCode("proceedingCode")
        .levelOfService("levelOfService")
        .defaultWording("defaultWording")
        .stage(1)
        .costLimitation(BigDecimal.TEN)
        .emergencyCostLimitation(BigDecimal.TEN)
        .nonStandardWordingRequired(Boolean.TRUE)
        .emergencyScopeDefault(Boolean.TRUE)
        .emergency(Boolean.TRUE)
        .defaultCode(Boolean.TRUE)
        .scopeDefault(Boolean.TRUE);

    ScopeLimitationDetails expectedResponse = new ScopeLimitationDetails();

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ScopeLimitationDetails.class)).thenReturn(
        Mono.just(expectedResponse));

    Mono<ScopeLimitationDetails> resultsMono =
        ebsApiClient.getScopeLimitations(scopeLimitationDetail);

    StepVerifier.create(resultsMono)
        .expectNext(expectedResponse)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    String expectedUri = String.format(
        "/scope-limitations?scope-limitations=%s&category-of-law=%s&matter-type=%s"
            + "&proceeding-code=%s&level-of-service=%s&default-wording=%s"
            + "&stage=%s&cost-limitation=%s&emergency-cost-limitation=%s&non-standard-wording=%s"
            + "&emergency-scope-default=%s&emergency=%s&default-code=%s&scope-default=%s",
        scopeLimitationDetail.getScopeLimitations(),
        scopeLimitationDetail.getCategoryOfLaw(),
        scopeLimitationDetail.getMatterType(),
        scopeLimitationDetail.getProceedingCode(),
        scopeLimitationDetail.getLevelOfService(),
        scopeLimitationDetail.getDefaultWording(),
        scopeLimitationDetail.getStage(),
        scopeLimitationDetail.getCostLimitation(),
        scopeLimitationDetail.getEmergencyCostLimitation(),
        scopeLimitationDetail.getNonStandardWordingRequired(),
        scopeLimitationDetail.getEmergencyScopeDefault(),
        scopeLimitationDetail.getEmergency(),
        scopeLimitationDetail.getDefaultCode(),
        scopeLimitationDetail.getScopeDefault());

    // Assert the URI
    assertEquals(expectedUri, actualUri.toString());
  }

  @Test
  void getPriorAuthorityTypes_returnsData() {
    String code = "code1";
    Boolean valueRequired = Boolean.TRUE;

    PriorAuthorityTypeDetails priorAuthorityTypeDetails = new PriorAuthorityTypeDetails();

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(PriorAuthorityTypeDetails.class)).thenReturn(
        Mono.just(priorAuthorityTypeDetails));

    Mono<PriorAuthorityTypeDetails> priorAuthorityTypeDetailsMono =
        ebsApiClient.getPriorAuthorityTypes(code, valueRequired);

    StepVerifier.create(priorAuthorityTypeDetailsMono)
        .expectNext(priorAuthorityTypeDetails)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals(String.format("/prior-authority-types?code=%s&value-required=%s",
        code, valueRequired), actualUri.toString());
  }

  @Test
  void getOutcomeResults_returnsData() {
    String proceedingCode = "code1";
    String outcomeResult = "or1";

    OutcomeResultLookupDetail outcomeResultLookupDetail = new OutcomeResultLookupDetail();

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(OutcomeResultLookupDetail.class)).thenReturn(
        Mono.just(outcomeResultLookupDetail));

    Mono<OutcomeResultLookupDetail> outcomeResultLookupDetailMono =
        ebsApiClient.getOutcomeResults(proceedingCode, outcomeResult);

    StepVerifier.create(outcomeResultLookupDetailMono)
        .expectNext(outcomeResultLookupDetail)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals(String.format("/lookup/outcome-results?proceeding-code=%s&outcome-result=%s",
        proceedingCode, outcomeResult), actualUri.toString());
  }

  @Test
  void getStageEnds_returnsData() {
    String proceedingCode = "code1";
    String stageEnd = "stageEnd1";

    StageEndLookupDetail stageEndLookupDetail = new StageEndLookupDetail();

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(StageEndLookupDetail.class)).thenReturn(
        Mono.just(stageEndLookupDetail));

    Mono<StageEndLookupDetail> stageEndLookupDetailMono =
        ebsApiClient.getStageEnds(proceedingCode, stageEnd);

    StepVerifier.create(stageEndLookupDetailMono)
        .expectNext(stageEndLookupDetail)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals(String.format("/lookup/stage-ends?proceeding-code=%s&stage-end=%s",
        proceedingCode, stageEnd), actualUri.toString());
  }

  @Test
  void getAwardTypes_returnsData() {
    String code = "code1";
    String awardType = "type1";

    AwardTypeLookupDetail awardTypeLookupDetail = new AwardTypeLookupDetail();

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(AwardTypeLookupDetail.class)).thenReturn(
        Mono.just(awardTypeLookupDetail));

    Mono<AwardTypeLookupDetail> awardTypeLookupDetailMono =
        ebsApiClient.getAwardTypes(code, awardType);

    StepVerifier.create(awardTypeLookupDetailMono)
        .expectNext(awardTypeLookupDetail)
        .verifyComplete();

    Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals(String.format("/lookup/award-types?code=%s&award-type=%s",
        code, awardType), actualUri.toString());
  }
}
