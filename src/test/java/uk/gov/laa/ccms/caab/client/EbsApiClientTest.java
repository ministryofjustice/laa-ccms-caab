package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupDetail;
import uk.gov.laa.ccms.data.model.ClientInvolvementTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.LevelOfServiceLookupDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.ProceedingDetail;
import uk.gov.laa.ccms.data.model.ProceedingDetails;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetails;
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
  private EbsApiClientErrorHandler apiClientErrorHandler;

  @InjectMocks
  private EbsApiClient ebsApiClient;



  ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

  @Test
  void getUser_returnData() {

    final String loginId = "user1";
    final String expectedUri = "/users/{loginId}";

    final UserDetail mockUser = new UserDetail();
    mockUser.setLoginId(loginId);

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, loginId)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(UserDetail.class)).thenReturn(Mono.just(mockUser));

    final Mono<UserDetail> userDetailsMono = ebsApiClient.getUser(loginId);

    StepVerifier.create(userDetailsMono)
        .expectNextMatches(user -> user.getLoginId().equals(loginId))
        .verifyComplete();
  }

  @Test
  void getUser_notFound() {
    final String loginId = "user1";
    final String expectedUri = "/users/{loginId}";

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, loginId)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(UserDetail.class)).thenReturn(Mono.error(
        new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

    when(apiClientErrorHandler.handleApiRetrieveError(
        any(), eq("User"), eq("login id"), eq(loginId))).thenReturn(Mono.empty());

    final Mono<UserDetail> userDetailsMono = ebsApiClient.getUser(loginId);

    StepVerifier.create(userDetailsMono)
        .verifyComplete();
  }

  @Test
  void getCommonValues_returnsData() {
    final String type = "type1";
    final String code = "code1";
    final String descr = "desc1";
    final String sort = "sort1";
    final CommonLookupDetail commonValues = new CommonLookupDetail();

    final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(Mono.just(commonValues));

    final Mono<CommonLookupDetail> commonValuesMono = ebsApiClient.getCommonValues(type, code, descr, sort);

    StepVerifier.create(commonValuesMono)
        .expectNext(commonValues)
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals(String.format("/lookup/common?size=1000&type=%s&code=%s&description=%s&sort=%s",
            type, code, descr, sort), actualUri.toString());
  }

  @Test
  void getCommonValues_notFound() {
    final String type = "type1";
    final String code = "code1";
    final String descr = "desc1";
    final String sort = "sort1";

    final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(Mono.error(
        new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

    when(apiClientErrorHandler.handleApiRetrieveError(
        any(), eq("Common values"), any())).thenReturn(Mono.empty());

    final Mono<CommonLookupDetail> commonValuesMono = ebsApiClient.getCommonValues(
        type, code, descr, sort);

    StepVerifier.create(commonValuesMono)
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals(String.format("/lookup/common?size=1000&type=%s&code=%s&description=%s&sort=%s",
        type, code, descr, sort), actualUri.toString());
  }

  @Test
  void getCaseStatusValuesCopyAllowed_returnsData() {
    final CaseStatusLookupDetail caseStatusLookupDetail = new CaseStatusLookupDetail();

    final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CaseStatusLookupDetail.class)).thenReturn(
        Mono.just(caseStatusLookupDetail));

    final Mono<CaseStatusLookupDetail> lookupDetailMono = ebsApiClient.getCaseStatusValues(true);

    StepVerifier.create(lookupDetailMono)
        .expectNext(caseStatusLookupDetail)
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals("/lookup/case-status?size=1000&copy-allowed=true", actualUri.toString());
  }

  @Test
  void getProvider_returnsData() {
    final Integer providerId = 123;
    final ProviderDetail providerDetail = new ProviderDetail();

    final String expectedUri = "/providers/{providerId}";

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, String.valueOf(providerId))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ProviderDetail.class)).thenReturn(Mono.just(providerDetail));

    final Mono<ProviderDetail> providerDetailMono = ebsApiClient.getProvider(providerId);

    StepVerifier.create(providerDetailMono)
        .expectNext(providerDetail)
        .verifyComplete();
  }

  @Test
  void getPersonRelationshipsToCaseValues_returnsData() {
    final RelationshipToCaseLookupDetail relationshipToCaseLookupDetail
        = new RelationshipToCaseLookupDetail();

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(RelationshipToCaseLookupDetail.class))
        .thenReturn(Mono.just(relationshipToCaseLookupDetail));

    final Mono<RelationshipToCaseLookupDetail> relationshipToCaseLookupDetailMono =
        ebsApiClient.getPersonRelationshipsToCaseValues();

    StepVerifier.create(relationshipToCaseLookupDetailMono)
        .expectNext(relationshipToCaseLookupDetail)
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals("/lookup/person-to-case-relationships", actualUri.toString());
  }

  @Test
  void getOrganisationRelationshipsToCaseValues_returnsData() {
    final RelationshipToCaseLookupDetail relationshipToCaseLookupDetail
        = new RelationshipToCaseLookupDetail();

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(RelationshipToCaseLookupDetail.class))
        .thenReturn(Mono.just(relationshipToCaseLookupDetail));

    final Mono<RelationshipToCaseLookupDetail> relationshipToCaseLookupDetailMono =
        ebsApiClient.getOrganisationToCaseRelationshipValues(null, null);

    StepVerifier.create(relationshipToCaseLookupDetailMono)
        .expectNext(relationshipToCaseLookupDetail)
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals("/lookup/organisation-to-case-relationships?size=1000", actualUri.toString());
  }

  @Test
  void getAmendmentTypes_returnsData() {
    final String applicationType = "appType1";
    final AmendmentTypeLookupDetail amendmentTypeLookupDetail = new AmendmentTypeLookupDetail();

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(AmendmentTypeLookupDetail.class)).thenReturn(
        Mono.just(amendmentTypeLookupDetail));

    final Mono<AmendmentTypeLookupDetail> amendmentTypesMono =
        ebsApiClient.getAmendmentTypes(applicationType);

    StepVerifier.create(amendmentTypesMono)
        .expectNext(amendmentTypeLookupDetail)
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals("/lookup/amendment-types?size=1000&application-type=appType1", actualUri.toString());
  }

  @Test
  void getAmendmentTypes_notFound() {
    final String applicationType = "appType1";

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(AmendmentTypeLookupDetail.class)).thenReturn(Mono.error(
        new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

    when(apiClientErrorHandler.handleApiRetrieveError(any(), eq("Amendment types"), any())).thenReturn(Mono.empty());

    final Mono<AmendmentTypeLookupDetail> amendmentTypesMono =
        ebsApiClient.getAmendmentTypes(applicationType);

    StepVerifier.create(amendmentTypesMono)
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals("/lookup/amendment-types?size=1000&application-type=appType1", actualUri.toString());
  }

  @Test
  void getCountries_returnsData() {
    final CommonLookupDetail commonValues = new CommonLookupDetail();
    // Set up your mock commonValues

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(Mono.just(commonValues));

    final Mono<CommonLookupDetail> countriesMono = ebsApiClient.getCountries();

    StepVerifier.create(countriesMono)
        .expectNext(commonValues)
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

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

    when(apiClientErrorHandler.handleApiRetrieveError(
        any(),eq("Countries"), any())).thenReturn(Mono.empty());

    final Mono<CommonLookupDetail> countriesMono = ebsApiClient.getCountries();

    StepVerifier.create(countriesMono)
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals("/lookup/countries?size=1000", actualUri.toString());
  }

  @Test
  void getProceeding_returnsData() {
    final String proceedingCode = "PROC1";
    final ProceedingDetail proceedingDetail = new ProceedingDetail();

    final String expectedUri = "/proceedings/{proceeding-code}";

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(expectedUri, proceedingCode)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ProceedingDetail.class)).thenReturn(Mono.just(proceedingDetail));

    final Mono<ProceedingDetail> proceedingDetailMono = ebsApiClient.getProceeding(proceedingCode);

    StepVerifier.create(proceedingDetailMono)
        .expectNext(proceedingDetail)
        .verifyComplete();
  }
  @Test
  void getUsersForProvider_returnsData() {
    // Mock some objects
    final String username = "user1";
    final String expectedUri = "/users?size=1000&provider-id=123";
    final Integer providerId = 123;
    final BaseUser mockUser = new BaseUser()
        .username(username)
        .userId(123);
    final UserDetails mockDetails = new UserDetails()
        .addContentItem(mockUser);

    // Stubs
    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(UserDetails.class)).thenReturn(Mono.just(mockDetails));

    // Call the method
    final Mono<UserDetails> userDetailsMono = ebsApiClient.getUsers(providerId);

    // Assertions
    StepVerifier.create(userDetailsMono)
        .expectNext(mockDetails)
        .verifyComplete();
    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());
    assertEquals(expectedUri, actualUri.toString());

  }

  @Test
  void getUsersForProvider_notFound() {
    final Integer providerId = 123;

    final String expectedUri = "/users?size=1000&provider-id=123";

    // Stubs
    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(UserDetails.class)).thenReturn(Mono.error(
        new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

    when(apiClientErrorHandler.handleApiRetrieveError(any(), any(), any()))
        .thenReturn(Mono.empty());

    final Mono<UserDetails> userDetailsMono = ebsApiClient.getUsers(providerId);
    StepVerifier.create(userDetailsMono)
        .verifyComplete();
    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());
    assertEquals(expectedUri, actualUri.toString());
  }

  @Test
  void getScopeLimitations_returnsData() {
    final uk.gov.laa.ccms.data.model.ScopeLimitationDetail scopeLimitationDetail =
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

    final ScopeLimitationDetails expectedResponse = new ScopeLimitationDetails();

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ScopeLimitationDetails.class)).thenReturn(
        Mono.just(expectedResponse));

    final Mono<ScopeLimitationDetails> resultsMono =
        ebsApiClient.getScopeLimitations(scopeLimitationDetail);

    StepVerifier.create(resultsMono)
        .expectNext(expectedResponse)
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    final String expectedUri = String.format(
        "/scope-limitations?size=1000&scope-limitations=%s&category-of-law=%s&matter-type=%s"
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
    final String code = "code1";
    final Boolean valueRequired = Boolean.TRUE;

    final PriorAuthorityTypeDetails priorAuthorityTypeDetails = new PriorAuthorityTypeDetails();

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(PriorAuthorityTypeDetails.class)).thenReturn(
        Mono.just(priorAuthorityTypeDetails));

    final Mono<PriorAuthorityTypeDetails> priorAuthorityTypeDetailsMono =
        ebsApiClient.getPriorAuthorityTypes(code, valueRequired);

    StepVerifier.create(priorAuthorityTypeDetailsMono)
        .expectNext(priorAuthorityTypeDetails)
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals(String.format("/prior-authority-types?size=1000&code=%s&value-required=%s",
        code, valueRequired), actualUri.toString());
  }

  @Test
  void getOutcomeResults_returnsData() {
    final String proceedingCode = "code1";
    final String outcomeResult = "or1";

    final OutcomeResultLookupDetail outcomeResultLookupDetail = new OutcomeResultLookupDetail();

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(OutcomeResultLookupDetail.class)).thenReturn(
        Mono.just(outcomeResultLookupDetail));

    final Mono<OutcomeResultLookupDetail> outcomeResultLookupDetailMono =
        ebsApiClient.getOutcomeResults(proceedingCode, outcomeResult);

    StepVerifier.create(outcomeResultLookupDetailMono)
        .expectNext(outcomeResultLookupDetail)
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals(String.format("/lookup/outcome-results?size=1000&proceeding-code=%s&outcome-result=%s",
        proceedingCode, outcomeResult), actualUri.toString());
  }

  @Test
  void getStageEnds_returnsData() {
    final String proceedingCode = "code1";
    final String stageEnd = "stageEnd1";

    final StageEndLookupDetail stageEndLookupDetail = new StageEndLookupDetail();

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(StageEndLookupDetail.class)).thenReturn(
        Mono.just(stageEndLookupDetail));

    final Mono<StageEndLookupDetail> stageEndLookupDetailMono =
        ebsApiClient.getStageEnds(proceedingCode, stageEnd);

    StepVerifier.create(stageEndLookupDetailMono)
        .expectNext(stageEndLookupDetail)
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals(String.format("/lookup/stage-ends?size=1000&proceeding-code=%s&stage-end=%s",
        proceedingCode, stageEnd), actualUri.toString());
  }

  @Test
  void getAwardTypes_returnsData() {
    final String code = "code1";
    final String awardType = "type1";

    final AwardTypeLookupDetail awardTypeLookupDetail = new AwardTypeLookupDetail();

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(AwardTypeLookupDetail.class)).thenReturn(
        Mono.just(awardTypeLookupDetail));

    final Mono<AwardTypeLookupDetail> awardTypeLookupDetailMono =
        ebsApiClient.getAwardTypes(code, awardType);

    StepVerifier.create(awardTypeLookupDetailMono)
        .expectNext(awardTypeLookupDetail)
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    // Assert the URI
    assertEquals("/lookup/award-types?size=1000&code=code1&award-type=type1", actualUri.toString());
  }

  @Test
  void getCategoriesOfLaw_success() {
    final String code = "code";
    final String matterTypeDescription = "description";
    final Boolean copyCostLimit = true;
    final CategoryOfLawLookupDetail lookupDetail = new CategoryOfLawLookupDetail(); // Populate this as needed

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CategoryOfLawLookupDetail.class)).thenReturn(Mono.just(lookupDetail));

    final Mono<CategoryOfLawLookupDetail> result = ebsApiClient.getCategoriesOfLaw(code, matterTypeDescription, copyCostLimit);

    StepVerifier.create(result)
        .expectNext(lookupDetail)
        .verifyComplete();

    verify(responseMock).bodyToMono(CategoryOfLawLookupDetail.class);
  }

  @Test
  void getCategoriesOfLaw_errorHandling() {
    final String code = "error_code";
    final String matterTypeDescription = "error_description";
    final Boolean copyCostLimit = false;

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(CategoryOfLawLookupDetail.class)).thenReturn(Mono.error(new RuntimeException("Error")));

    final Mono<CategoryOfLawLookupDetail> result = ebsApiClient.getCategoriesOfLaw(code, matterTypeDescription, copyCostLimit);

    StepVerifier.create(result)
        .expectError(RuntimeException.class)
        .verify();

    verify(responseMock).bodyToMono(CategoryOfLawLookupDetail.class);
  }

  @Test
  void getPersonToCaseRelationships_success() {
    final String code = "rel_code";
    final String description = "rel_description";
    final RelationshipToCaseLookupDetail lookupDetail = new RelationshipToCaseLookupDetail(); // Populate this as needed

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(RelationshipToCaseLookupDetail.class)).thenReturn(Mono.just(lookupDetail));

    final Mono<RelationshipToCaseLookupDetail> result = ebsApiClient.getPersonToCaseRelationships(code, description);

    StepVerifier.create(result)
        .expectNext(lookupDetail)
        .verifyComplete();

    verify(responseMock).bodyToMono(RelationshipToCaseLookupDetail.class);
  }

  @Test
  void getPersonToCaseRelationships_errorHandling() {
    final String code = "error_rel_code";
    final String description = "error_rel_description";

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(RelationshipToCaseLookupDetail.class)).thenReturn(Mono.error(new RuntimeException("Error")));

    final Mono<RelationshipToCaseLookupDetail> result = ebsApiClient.getPersonToCaseRelationships(code, description);

    StepVerifier.create(result)
        .expectError(RuntimeException.class)
        .verify();

    verify(responseMock).bodyToMono(RelationshipToCaseLookupDetail.class);
  }

  @Test
  void getProceedings_success() {
    final ProceedingDetail searchCriteria = new ProceedingDetail();
    searchCriteria.setCategoryOfLawCode("FAM");
    searchCriteria.setMatterType("DOM");
    searchCriteria.setAmendmentOnly(true);
    final Boolean larScopeFlag = true;
    final String applicationType = "NEW";
    final Boolean isLead = true;

    final ProceedingDetails mockDetails = new ProceedingDetails();

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ProceedingDetails.class)).thenReturn(Mono.just(mockDetails));

    final Mono<ProceedingDetails> result = ebsApiClient.getProceedings(searchCriteria, larScopeFlag, applicationType, isLead);

    StepVerifier.create(result)
        .expectNextMatches(details -> details.equals(mockDetails))
        .verifyComplete();
  }

  @Test
  void getProceedings_error() {
    final ProceedingDetail searchCriteria = new ProceedingDetail();
    final Boolean larScopeFlag = false;
    final String applicationType = "MOD";
    final Boolean isLead = false;

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ProceedingDetails.class))
        .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

    final Mono<ProceedingDetails> result = ebsApiClient.getProceedings(searchCriteria, larScopeFlag, applicationType, isLead);

    StepVerifier.create(result)
        .expectError(RuntimeException.class)
        .verify();
  }


  @Test
  void getClientInvolvementTypes_success() {
    final String proceedingCode = "PROC123";
    final ClientInvolvementTypeLookupDetail mockDetail = new ClientInvolvementTypeLookupDetail(); // Assume this is populated

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ClientInvolvementTypeLookupDetail.class)).thenReturn(Mono.just(mockDetail));

    final Mono<ClientInvolvementTypeLookupDetail> result = ebsApiClient.getClientInvolvementTypes(proceedingCode);

    StepVerifier.create(result)
        .expectNextMatches(detail -> detail.equals(mockDetail))
        .verifyComplete();
  }

  @Test
  void getClientInvolvementTypes_error() {
    final String proceedingCode = "PROC123";

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(ClientInvolvementTypeLookupDetail.class))
        .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

    final Mono<ClientInvolvementTypeLookupDetail> result = ebsApiClient.getClientInvolvementTypes(proceedingCode);

    StepVerifier.create(result)
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void getLevelOfServiceTypes_success() {
    final String proceedingCode = "PROC123";
    final String categoryOfLaw = "FAM";
    final String matterType = "DOM";
    final LevelOfServiceLookupDetail mockDetail = new LevelOfServiceLookupDetail(); // Assume this is populated

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(LevelOfServiceLookupDetail.class)).thenReturn(Mono.just(mockDetail));

    final Mono<LevelOfServiceLookupDetail> result = ebsApiClient.getLevelOfServiceTypes(proceedingCode, categoryOfLaw, matterType);

    StepVerifier.create(result)
        .expectNextMatches(detail -> detail.equals(mockDetail))
        .verifyComplete();
  }

  @Test
  void getLevelOfServiceTypes_error() {
    final String proceedingCode = "PROC404";
    final String categoryOfLaw = "UNK";
    final String matterType = "UNKNOWN";

    when(webClientMock.get()).thenReturn(requestHeadersUriMock);
    when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(LevelOfServiceLookupDetail.class))
        .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

    final Mono<LevelOfServiceLookupDetail> result = ebsApiClient.getLevelOfServiceTypes(proceedingCode, categoryOfLaw, matterType);

    StepVerifier.create(result)
        .expectError(RuntimeException.class)
        .verify();
  }

}
