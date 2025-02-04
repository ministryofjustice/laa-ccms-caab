package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URI;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.BaseUser;
import uk.gov.laa.ccms.data.model.CaseReferenceSummary;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupDetail;
import uk.gov.laa.ccms.data.model.ClientInvolvementTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.DeclarationLookupDetail;
import uk.gov.laa.ccms.data.model.EvidenceDocumentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.LevelOfServiceLookupDetail;
import uk.gov.laa.ccms.data.model.MatterTypeLookupDetail;
import uk.gov.laa.ccms.data.model.NotificationSummary;
import uk.gov.laa.ccms.data.model.Notifications;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.ProceedingDetail;
import uk.gov.laa.ccms.data.model.ProceedingDetails;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.ProviderRequestTypeLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetails;
import uk.gov.laa.ccms.data.model.StageEndLookupDetail;
import uk.gov.laa.ccms.data.model.TransactionStatus;
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
  private WebClient.RequestBodyUriSpec requestBodyUriMock;
  @Mock
  private WebClient.RequestBodySpec requestBodySpec;
  @Mock
  private WebClient.ResponseSpec responseMock;

  @Mock
  private EbsApiClientErrorHandler apiClientErrorHandler;

  @InjectMocks
  private EbsApiClient ebsApiClient;

  ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

  @Nested
  @DisplayName("getUser() Tests")
  class GetUserTests {

    @Test
    @DisplayName("Should return data")
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
    @DisplayName("Should return not found")
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

  }

  @Nested
  @DisplayName("getUserNotificationSummary() Tests")
  class GetUserNotificationSummaryTests {

    @Test
    @DisplayName("Should return not found")
    void getUserNotificationSummary_returnsData() {
      final String loginId = "user1";
      final String expectedUri = "/users/{loginId}/notifications/summary";

      final NotificationSummary mockNotificationSummary = new NotificationSummary();
      mockNotificationSummary.setNotifications(1);
      mockNotificationSummary.setStandardActions(3);
      mockNotificationSummary.setOverdueActions(2);

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(expectedUri, loginId)).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(NotificationSummary.class)).thenReturn(
          Mono.just(mockNotificationSummary));

      final Mono<NotificationSummary> notificationSummary = ebsApiClient.getUserNotificationSummary(
          loginId);

      StepVerifier.create(notificationSummary)
          .expectNextMatches(summary ->
              summary.getNotifications().equals(1) &&
                  summary.getStandardActions().equals(3) &&
                  summary.getOverdueActions().equals(2)
          )
          .verifyComplete();
    }

    @Test
    @DisplayName("Should return data")
    void getUserNotificationSummary_NotFound() {
      final String loginId = "user1";
      final String expectedUri = "/users/{loginId}/notifications/summary";

      final NotificationSummary mockNotificationSummary = new NotificationSummary();
      mockNotificationSummary.setNotifications(1);
      mockNotificationSummary.setStandardActions(3);
      mockNotificationSummary.setOverdueActions(2);

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(expectedUri, loginId)).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(NotificationSummary.class)).thenReturn(
          Mono.just(mockNotificationSummary));

      final Mono<NotificationSummary> notificationSummary = ebsApiClient.getUserNotificationSummary(
          loginId);

      StepVerifier.create(notificationSummary)
          .expectNextMatches(summary ->
              summary.getNotifications().equals(1) &&
                  summary.getStandardActions().equals(3) &&
                  summary.getOverdueActions().equals(2)
          )
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("getNotifications() Tests")
  class GetNotificationsTests {

    @Test
    @DisplayName("Should return successfully")
    void getNotifications_successful() {
      NotificationSearchCriteria criteria = new NotificationSearchCriteria();
      criteria.setAssignedToUserId("testUserId");

      criteria.setLoginId("testUserId");
      criteria.setUserType("testUserType");
      int page = 10, size = 10;
      String expectedUri = String.format(
          "/notifications?provider-id=1&assigned-to-user-id=%s&include-closed=%s&page=%s&" +
              "size=%s",
          criteria.getAssignedToUserId(),
          criteria.isIncludeClosed(),
          page,
          size);

      ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);

      Notifications notificationsObj = new Notifications();

      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(Notifications.class)).thenReturn(
          Mono.just(notificationsObj));
      Mono<uk.gov.laa.ccms.data.model.Notifications> notificationsMono =
          ebsApiClient.getNotifications(
              criteria, 1, page,
              size);

      StepVerifier.create(notificationsMono)
          .expectNextMatches(notifications -> notifications == notificationsObj)
          .verifyComplete();
      Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
      URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());
      assertEquals(expectedUri, actualUri.toString());
    }

    @Test
    @DisplayName("Should handle error")
    void getNotifications_handlesError() {

      NotificationSearchCriteria criteria = new NotificationSearchCriteria();
      criteria.setAssignedToUserId("testUserId");
      criteria.setLoginId("testUserId");
      criteria.setUserType("testUserType");
      int page = 10, size = 10;
      String expectedUri = String.format(
          "/notifications?provider-id=1&assigned-to-user-id=%s&include-closed=%s&page=%s&" +
              "size=%s",
          criteria.getAssignedToUserId(),
          criteria.isIncludeClosed(),
          page,
          size);

      ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(Notifications.class)).thenReturn(Mono.error(
          new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

      when(
          apiClientErrorHandler.handleApiRetrieveError(any(), eq("Notifications"),
              any())).thenReturn(
          Mono.empty());

      Mono<Notifications> notificationsMono =
          ebsApiClient.getNotifications(criteria, 1, page, size);

      StepVerifier.create(notificationsMono)
          .verifyComplete();
      Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
      URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());
      assertEquals(expectedUri, actualUri.toString());
      assertEquals(expectedUri, actualUri.toString());
    }

  }

  @Nested
  @DisplayName("getCommonValues() Tests")
  class GetCommonValuesTests {

    @Nested
    @DisplayName("getCommonValues(type,code,descr,sort)")
    class TypeCodeDescrSort {

      @Test
      @DisplayName("Should return data")
      void getCommonValues_returnsData() {
        final String type = "type1";
        final String code = "code1";
        final String descr = "desc1";
        final String sort = "sort1";
        final CommonLookupDetail commonValues = new CommonLookupDetail();

        final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(
            Function.class);

        when(webClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(Mono.just(commonValues));

        final Mono<CommonLookupDetail> commonValuesMono = ebsApiClient.getCommonValues(type, code,
            descr, sort);

        StepVerifier.create(commonValuesMono)
            .expectNext(commonValues)
            .verifyComplete();

        final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
        final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

        // Assert the URI
        assertEquals(
            String.format("/lookup/common?size=1000&type=%s&code=%s&description=%s&sort=%s",
                type, code, descr, sort), actualUri.toString());
      }

      @Test
      @DisplayName("Should return not found")
      void getCommonValues_notFound() {
        final String type = "type1";
        final String code = "code1";
        final String descr = "desc1";
        final String sort = "sort1";

        final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(
            Function.class);

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
        assertEquals(
            String.format("/lookup/common?size=1000&type=%s&code=%s&description=%s&sort=%s",
                type, code, descr, sort), actualUri.toString());
      }
    }

    @Nested
    @DisplayName("getCommonValues(type,code,descr)")
    class TypeCodeDescr {

      @Test
      @DisplayName("Should return success")
      void getCommonValues_withTypeCodeDescription_success() {
        final String type = "type1";
        final String code = "code1";
        final String description = "desc1";
        final CommonLookupDetail mockDetail = new CommonLookupDetail(); // Assume this is populated

        when(webClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(Mono.just(mockDetail));

        final Mono<CommonLookupDetail> result = ebsApiClient.getCommonValues(type, code,
            description);

        StepVerifier.create(result)
            .expectNext(mockDetail)
            .verifyComplete();

        verify(responseMock).bodyToMono(CommonLookupDetail.class);
      }

      @Test
      @DisplayName("Should handle error")
      void getCommonValues_withTypeCodeDescription_errorHandling() {
        final String type = "type1";
        final String code = "code1";
        final String description = "desc1";

        when(webClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(
            Mono.error(new RuntimeException("Error")));

        final Mono<CommonLookupDetail> result = ebsApiClient.getCommonValues(type, code,
            description);

        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();

        verify(responseMock).bodyToMono(CommonLookupDetail.class);
      }
    }

    @Nested
    @DisplayName("getCommonValues(type,code)")
    class TypeCode {
      @Test
      @DisplayName("Should return success")
      void getCommonValues_withTypeCode_success() {
        final String type = "type1";
        final String code = "code1";
        final CommonLookupDetail mockDetail = new CommonLookupDetail(); // Assume this is populated

        when(webClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(Mono.just(mockDetail));

        final Mono<CommonLookupDetail> result = ebsApiClient.getCommonValues(type, code);

        StepVerifier.create(result)
            .expectNext(mockDetail)
            .verifyComplete();

        verify(responseMock).bodyToMono(CommonLookupDetail.class);
      }
    }

    @Nested
    @DisplayName("getCommonValues(type)")
    class Type {

      @Test
      @DisplayName("Should return success")
      void getCommonValues_withType_success() {
        final String type = "type1";
        final CommonLookupDetail mockDetail = new CommonLookupDetail(); // Assume this is populated

        when(webClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(Mono.just(mockDetail));

        final Mono<CommonLookupDetail> result = ebsApiClient.getCommonValues(type);

        StepVerifier.create(result)
            .expectNext(mockDetail)
            .verifyComplete();

        verify(responseMock).bodyToMono(CommonLookupDetail.class);
      }
    }
  }

  @Nested
  @DisplayName("getCaseStatusValues() Tests")
  class GetCaseStatusValuesTests {

    @Test
    @DisplayName("Should return data")
    void getCaseStatusValuesCopyAllowed_returnsData() {
      final CaseStatusLookupDetail caseStatusLookupDetail = new CaseStatusLookupDetail();

      final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(
          Function.class);

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
  }

  @Nested
  @DisplayName("getProvider() Tests")
  class GetProviderTests {

    @Test
    @DisplayName("Should return data")
    void getProvider_returnsData() {
      final Integer providerId = 123;
      final ProviderDetail providerDetail = new ProviderDetail();

      final String expectedUri = "/providers/{providerId}";

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(expectedUri, String.valueOf(providerId))).thenReturn(
          requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(ProviderDetail.class)).thenReturn(Mono.just(providerDetail));

      final Mono<ProviderDetail> providerDetailMono = ebsApiClient.getProvider(providerId);

      StepVerifier.create(providerDetailMono)
          .expectNext(providerDetail)
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("getPersonRelationshipsToCaseValues() Tests")
  class GetPersonRelationshipsToCaseValuesTests {

    @Test
    @DisplayName("Should return data")
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

  }

  @Nested
  @DisplayName("getPersonRelationshipsToCaseValues() Tests")
  class GetOrganisationToCaseRelationshipValuesTests {

    @Test
    @DisplayName("Should return data")
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
  }

  @Nested
  @DisplayName("getAmendmentTypes() Tests")
  class GetAmendmentTypesTests {

    @Test
    @DisplayName("Should return data")
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
      assertEquals("/lookup/amendment-types?size=1000&application-type=appType1",
          actualUri.toString());
    }

    @Test
    @DisplayName("Should return not found")
    void getAmendmentTypes_notFound() {
      final String applicationType = "appType1";

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(AmendmentTypeLookupDetail.class)).thenReturn(Mono.error(
          new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

      when(apiClientErrorHandler.handleApiRetrieveError(any(), eq("Amendment types"),
          any())).thenReturn(Mono.empty());

      final Mono<AmendmentTypeLookupDetail> amendmentTypesMono =
          ebsApiClient.getAmendmentTypes(applicationType);

      StepVerifier.create(amendmentTypesMono)
          .verifyComplete();

      final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
      final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

      // Assert the URI
      assertEquals("/lookup/amendment-types?size=1000&application-type=appType1",
          actualUri.toString());
    }
  }

  @Nested
  @DisplayName("getCountries() Tests")
  class GetCountriesTests {

    @Test
    @DisplayName("Should return data")
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
    @DisplayName("Should return not found")
    void getCountries_notFound() {
      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(Mono.error(
          new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

      when(apiClientErrorHandler.handleApiRetrieveError(
          any(), eq("Countries"), any())).thenReturn(Mono.empty());

      final Mono<CommonLookupDetail> countriesMono = ebsApiClient.getCountries();

      StepVerifier.create(countriesMono)
          .verifyComplete();

      final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
      final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

      // Assert the URI
      assertEquals("/lookup/countries?size=1000", actualUri.toString());
    }
  }

  @Nested
  @DisplayName("getProceeding() Tests")
  class GetProceedingTests {

    @Test
    @DisplayName("Should return data")
    void getProceeding_returnsData() {
      final String proceedingCode = "PROC1";
      final ProceedingDetail proceedingDetail = new ProceedingDetail();

      final String expectedUri = "/proceedings/{proceeding-code}";

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(expectedUri, proceedingCode)).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(ProceedingDetail.class)).thenReturn(Mono.just(proceedingDetail));

      final Mono<ProceedingDetail> proceedingDetailMono = ebsApiClient.getProceeding(
          proceedingCode);

      StepVerifier.create(proceedingDetailMono)
          .expectNext(proceedingDetail)
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("getUsers() Tests")
  class GetUsersTests {

    @Test
    @DisplayName("Should return data")
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
    @DisplayName("Should return not found")
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

  }

  @Nested
  @DisplayName("getScopeLimitations() Tests")
  class GetScopeLimitationsTests {

    @Test
    @DisplayName("Should return data")
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
  }

  @Nested
  @DisplayName("getPriorAuthorityTypes() Tests")
  class GetPriorAuthorityTypesTests {

    @Test
    @DisplayName("Should return data")
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

  }

  @Nested
  @DisplayName("getOutcomeResults() Tests")
  class GetOutcomeResults {

    @Test
    @DisplayName("Should return data")
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
      assertEquals(
          String.format("/lookup/outcome-results?size=1000&proceeding-code=%s&outcome-result=%s",
              proceedingCode, outcomeResult), actualUri.toString());
    }
  }

  @Nested
  @DisplayName("getStageEnds() Tests")
  class GetStageEndsTests {

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
  }

  @Nested
  @DisplayName("getAwardTypes() Tests")
  class GetAwardTypesTests {

    @Test
    @DisplayName("Should return data")
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
      assertEquals("/lookup/award-types?size=1000&code=code1&award-type=type1",
          actualUri.toString());
    }
  }

  @Nested
  @DisplayName("getCategoriesOfLaw() Tests")
  class GetCategoriesOfLawTests {

    @Test
    @DisplayName("Should return success")
    void getCategoriesOfLaw_success() {
      final String code = "code";
      final String matterTypeDescription = "description";
      final Boolean copyCostLimit = true;
      final CategoryOfLawLookupDetail lookupDetail = new CategoryOfLawLookupDetail(); // Populate
      // this as needed

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(CategoryOfLawLookupDetail.class)).thenReturn(
          Mono.just(lookupDetail));

      final Mono<CategoryOfLawLookupDetail> result = ebsApiClient.getCategoriesOfLaw(code,
          matterTypeDescription, copyCostLimit);

      StepVerifier.create(result)
          .expectNext(lookupDetail)
          .verifyComplete();

      verify(responseMock).bodyToMono(CategoryOfLawLookupDetail.class);
    }

    @Test
    @DisplayName("Should handle error")
    void getCategoriesOfLaw_errorHandling() {
      final String code = "error_code";
      final String matterTypeDescription = "error_description";
      final Boolean copyCostLimit = false;

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(CategoryOfLawLookupDetail.class)).thenReturn(
          Mono.error(new RuntimeException("Error")));

      final Mono<CategoryOfLawLookupDetail> result = ebsApiClient.getCategoriesOfLaw(code,
          matterTypeDescription, copyCostLimit);

      StepVerifier.create(result)
          .expectError(RuntimeException.class)
          .verify();

      verify(responseMock).bodyToMono(CategoryOfLawLookupDetail.class);
    }

  }

  @Nested
  @DisplayName("getPersonToCaseRelationshipsTests() Tests")
  class GetPersonToCaseRelationshipsTests {

    @Test
    @DisplayName("Should return success")
    void getPersonToCaseRelationships_success() {
      final String code = "rel_code";
      final String description = "rel_description";
      final RelationshipToCaseLookupDetail lookupDetail = new RelationshipToCaseLookupDetail(); //
      // Populate this as needed

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(RelationshipToCaseLookupDetail.class)).thenReturn(
          Mono.just(lookupDetail));

      final Mono<RelationshipToCaseLookupDetail> result = ebsApiClient.getPersonToCaseRelationships(
          code, description);

      StepVerifier.create(result)
          .expectNext(lookupDetail)
          .verifyComplete();

      verify(responseMock).bodyToMono(RelationshipToCaseLookupDetail.class);
    }

    @Test
    @DisplayName("Should handle error")
    void getPersonToCaseRelationships_errorHandling() {
      final String code = "error_rel_code";
      final String description = "error_rel_description";

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(RelationshipToCaseLookupDetail.class)).thenReturn(
          Mono.error(new RuntimeException("Error")));

      final Mono<RelationshipToCaseLookupDetail> result = ebsApiClient.getPersonToCaseRelationships(
          code, description);

      StepVerifier.create(result)
          .expectError(RuntimeException.class)
          .verify();

      verify(responseMock).bodyToMono(RelationshipToCaseLookupDetail.class);
    }
  }

  @Nested
  @DisplayName("getProceedings() Tests")
  class GetProceedingsTests {

    @Test
    @DisplayName("Should return success")
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

      final Mono<ProceedingDetails> result = ebsApiClient.getProceedings(searchCriteria,
          larScopeFlag,
          applicationType, isLead);

      StepVerifier.create(result)
          .expectNextMatches(details -> details.equals(mockDetails))
          .verifyComplete();
    }

    @Test
    @DisplayName("Should return error")
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

      final Mono<ProceedingDetails> result = ebsApiClient.getProceedings(searchCriteria,
          larScopeFlag,
          applicationType, isLead);

      StepVerifier.create(result)
          .expectError(RuntimeException.class)
          .verify();
    }
  }

  @Nested
  @DisplayName("getClientInvolvementTypes() Tests")
  class GetClientInvolvementTypesTests {

    @Test
    @DisplayName("Should return success")
    void getClientInvolvementTypes_success() {
      final String proceedingCode = "PROC123";
      final ClientInvolvementTypeLookupDetail mockDetail =
          new ClientInvolvementTypeLookupDetail(); // Assume this is populated

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(ClientInvolvementTypeLookupDetail.class)).thenReturn(
          Mono.just(mockDetail));

      final Mono<ClientInvolvementTypeLookupDetail> result = ebsApiClient.getClientInvolvementTypes(
          proceedingCode);

      StepVerifier.create(result)
          .expectNextMatches(detail -> detail.equals(mockDetail))
          .verifyComplete();
    }

    @Test
    @DisplayName("Should return error")
    void getClientInvolvementTypes_error() {
      final String proceedingCode = "PROC123";

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(ClientInvolvementTypeLookupDetail.class))
          .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

      final Mono<ClientInvolvementTypeLookupDetail> result = ebsApiClient.getClientInvolvementTypes(
          proceedingCode);

      StepVerifier.create(result)
          .expectError(RuntimeException.class)
          .verify();
    }
  }

  @Nested
  @DisplayName("getLevelOfServiceTypes() Tests")
  class GetLevelOfServiceTypesTests {

    @Test
    @DisplayName("Should return success")
    void getLevelOfServiceTypes_success() {
      final String proceedingCode = "PROC123";
      final String categoryOfLaw = "FAM";
      final String matterType = "DOM";
      final LevelOfServiceLookupDetail mockDetail = new LevelOfServiceLookupDetail(); // Assume
      // this is populated

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(LevelOfServiceLookupDetail.class)).thenReturn(
          Mono.just(mockDetail));

      final Mono<LevelOfServiceLookupDetail> result = ebsApiClient.getLevelOfServiceTypes(
          proceedingCode, categoryOfLaw, matterType);

      StepVerifier.create(result)
          .expectNextMatches(detail -> detail.equals(mockDetail))
          .verifyComplete();
    }

    @Test
    @DisplayName("Should return error")
    void getLevelOfServiceTypes_error() {
      final String proceedingCode = "PROC404";
      final String categoryOfLaw = "UNK";
      final String matterType = "UNKNOWN";

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(LevelOfServiceLookupDetail.class))
          .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

      final Mono<LevelOfServiceLookupDetail> result = ebsApiClient.getLevelOfServiceTypes(
          proceedingCode, categoryOfLaw, matterType);

      StepVerifier.create(result)
          .expectError(RuntimeException.class)
          .verify();
    }
  }

  @Nested
  @DisplayName("getEvidenceDocumentTypes() Tests")
  class GetEvidenceDocumentTypesTests {

    @Test
    @DisplayName("Should return data")
    void getEvidenceDocumentTypes_returnsData() {
      final String type = "type1";
      final String code = "code1";
      final EvidenceDocumentTypeLookupDetail mockDetail = new EvidenceDocumentTypeLookupDetail();

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(EvidenceDocumentTypeLookupDetail.class)).thenReturn(
          Mono.just(mockDetail));

      final Mono<EvidenceDocumentTypeLookupDetail> result = ebsApiClient.getEvidenceDocumentTypes(
          type, code);

      StepVerifier.create(result)
          .expectNextMatches(detail -> detail.equals(mockDetail))
          .verifyComplete();

      final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
      final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

      assertEquals("/lookup/evidence-document-types?size=1000&type=type1&code=code1",
          actualUri.toString());
    }

    @Test
    @DisplayName("Should return not found")
    void getEvidenceDocumentTypes_notFound() {
      final String type = "type1";
      final String code = "code1";

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(EvidenceDocumentTypeLookupDetail.class)).thenReturn(Mono.error(
          new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

      when(apiClientErrorHandler.handleApiRetrieveError(any(), eq("Evidence document types"),
          any())).thenReturn(Mono.empty());

      final Mono<EvidenceDocumentTypeLookupDetail> result = ebsApiClient.getEvidenceDocumentTypes(
          type, code);

      StepVerifier.create(result)
          .verifyComplete();

      final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
      final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

      assertEquals("/lookup/evidence-document-types?size=1000&type=type1&code=code1",
          actualUri.toString());
    }
  }

  @Nested
  @DisplayName("getAssessmentSummaryAttributes() Tests")
  class GetAssessmentSummaryAttributesTests {

    @Test
    @DisplayName("Should return data")
    void getAssessmentSummaryAttributes_returnsData() {
      final String summaryType = "summary1";
      final AssessmentSummaryEntityLookupDetail mockDetail =
          new AssessmentSummaryEntityLookupDetail();

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(AssessmentSummaryEntityLookupDetail.class)).thenReturn(
          Mono.just(mockDetail));

      final Mono<AssessmentSummaryEntityLookupDetail> result =
          ebsApiClient.getAssessmentSummaryAttributes(
              summaryType);

      StepVerifier.create(result)
          .expectNextMatches(detail -> detail.equals(mockDetail))
          .verifyComplete();

      final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
      final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

      assertEquals("/lookup/assessment-summary-attributes?size=1000&summary-type=summary1",
          actualUri.toString());
    }

    @Test
    @DisplayName("Should return not found")
    void getAssessmentSummaryAttributes_notFound() {
      final String summaryType = "summary1";

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(AssessmentSummaryEntityLookupDetail.class)).thenReturn(
          Mono.error(
              new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

      when(apiClientErrorHandler.handleApiRetrieveError(any(), eq("Assessment summary attributes"),
          any())).thenReturn(Mono.empty());

      final Mono<AssessmentSummaryEntityLookupDetail> result =
          ebsApiClient.getAssessmentSummaryAttributes(
              summaryType);

      StepVerifier.create(result)
          .verifyComplete();

      final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
      final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

      assertEquals("/lookup/assessment-summary-attributes?size=1000&summary-type=summary1",
          actualUri.toString());
    }
  }

  @Nested
  @DisplayName("getMatterTypes(categoryOfLaw) Tests")
  class GetMatterTypesTests {

    @Test
    @DisplayName("Should handle success")
    void getMatterTypes_withCategoryOfLaw_success() {
      final String categoryOfLaw = "LAW1";
      final MatterTypeLookupDetail mockDetail = new MatterTypeLookupDetail(); // Assume this is
      // populated

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(MatterTypeLookupDetail.class)).thenReturn(Mono.just(mockDetail));

      final Mono<MatterTypeLookupDetail> result = ebsApiClient.getMatterTypes(categoryOfLaw);

      StepVerifier.create(result)
          .expectNext(mockDetail)
          .verifyComplete();

      verify(responseMock).bodyToMono(MatterTypeLookupDetail.class);
    }

    @Test
    @DisplayName("Should handle error")
    void getMatterTypes_withCategoryOfLaw_errorHandling() {
      final String categoryOfLaw = "LAW1";

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(MatterTypeLookupDetail.class)).thenReturn(
          Mono.error(new RuntimeException("Error")));

      final Mono<MatterTypeLookupDetail> result = ebsApiClient.getMatterTypes(categoryOfLaw);

      StepVerifier.create(result)
          .expectError(RuntimeException.class)
          .verify();

      verify(responseMock).bodyToMono(MatterTypeLookupDetail.class);
    }
  }

  @Nested
  @DisplayName("getDeclarations() Tests")
  class GetDeclarationsTests {

    @ParameterizedTest
    @CsvSource({
        "DECLARATION_TYPE, BILL_TYPE, /lookup/declarations?size=1000&type=DECLARATION_TYPE&billType"
            + "=BILL_TYPE",
        "DECLARATION_TYPE, , /lookup/declarations?size=1000&type=DECLARATION_TYPE",
        ", BILL_TYPE, /lookup/declarations?size=1000&billType=BILL_TYPE",
        ", , /lookup/declarations?size=1000"
    })
    @DisplayName("getDeclarations parameterized test")
    void testGetDeclarations(final String type, final String billType, final String expectedUri) {
      final DeclarationLookupDetail mockDeclarationDetail = new DeclarationLookupDetail();

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(DeclarationLookupDetail.class)).thenReturn(
          Mono.just(mockDeclarationDetail));

      final Mono<DeclarationLookupDetail> result = ebsApiClient.getDeclarations(type, billType);

      StepVerifier.create(result)
          .expectNext(mockDeclarationDetail)
          .verifyComplete();

      final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(
          Function.class);
      verify(requestHeadersUriMock).uri(uriCaptor.capture());

      final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
      final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

      assertEquals(expectedUri, actualUri.toString());
    }

    @ParameterizedTest
    @CsvSource({
        "DECLARATION_TYPE, BILL_TYPE, /lookup/declarations?size=1000&type=DECLARATION_TYPE&billType"
            + "=BILL_TYPE",
        "DECLARATION_TYPE, , /lookup/declarations?size=1000&type=DECLARATION_TYPE"
    })
    @DisplayName("getDeclarations handles errors in a parameterized test")
    void testGetDeclarations_handlesError(final String type, final String billType,
        final String expectedUri) {
      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(DeclarationLookupDetail.class)).thenReturn(
          Mono.error(new RuntimeException("Error")));

      when(apiClientErrorHandler.handleApiRetrieveError(any(), eq("Declarations"),
          any())).thenReturn(
          Mono.empty());

      final Mono<DeclarationLookupDetail> result = ebsApiClient.getDeclarations(type, billType);

      StepVerifier.create(result)
          .verifyComplete();

      final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(
          Function.class);
      verify(requestHeadersUriMock).uri(uriCaptor.capture());

      final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
      final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

      assertEquals(expectedUri, actualUri.toString());
    }
  }

  @Nested
  @DisplayName("getProviderRequestTypes() Tests")
  class GetProviderRequestTypesTests {

    @ParameterizedTest
    @CsvSource({
        "true, type1, /lookup/provider-request-types?size=1000&is-case-related=true&type=type1",
        "true, , /lookup/provider-request-types?size=1000&is-case-related=true",
        ", type1, /lookup/provider-request-types?size=1000&type=type1",
        ", , /lookup/provider-request-types?size=1000"
    })
    @DisplayName("Should match URI")
    void testGetProviderRequestTypes(final Boolean isCaseRelated, final String type,
        final String expectedUri) {
      final ProviderRequestTypeLookupDetail mockDetail = new ProviderRequestTypeLookupDetail();

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(ProviderRequestTypeLookupDetail.class)).thenReturn(
          Mono.just(mockDetail));

      final Mono<ProviderRequestTypeLookupDetail> result = ebsApiClient.getProviderRequestTypes(
          isCaseRelated, type);

      StepVerifier.create(result)
          .expectNext(mockDetail)
          .verifyComplete();

      final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(
          Function.class);
      verify(requestHeadersUriMock).uri(uriCaptor.capture());

      final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
      final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

      assertEquals(expectedUri, actualUri.toString());
    }

    @ParameterizedTest
    @CsvSource({
        "true, type1, /lookup/provider-request-types?size=1000&is-case-related=true&type=type1",
        "true, , /lookup/provider-request-types?size=1000&is-case-related=true"
    })
    @DisplayName("Should handle error")
    void testGetProviderRequestTypes_handlesError(final Boolean isCaseRelated, final String type,
        final String expectedUri) {
      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(any(Function.class))).thenReturn(requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.bodyToMono(ProviderRequestTypeLookupDetail.class)).thenReturn(
          Mono.error(new RuntimeException("Error")));

      when(apiClientErrorHandler.handleApiRetrieveError(any(), eq("Provider request types"),
          any())).thenReturn(Mono.empty());

      final Mono<ProviderRequestTypeLookupDetail> result = ebsApiClient.getProviderRequestTypes(
          isCaseRelated, type);

      StepVerifier.create(result)
          .verifyComplete();

      final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(
          Function.class);
      verify(requestHeadersUriMock).uri(uriCaptor.capture());

      final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
      final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

      assertEquals(expectedUri, actualUri.toString());
    }
  }

  @Nested
  @DisplayName("postAllocateNextCaseReference() Tests")
  class PostAllocateNextCaseReferenceTests {

    @Test
    @DisplayName("Should create case reference")
    void testPostAllocateNextCaseReference_createsCaseReference() {
      when(webClientMock.post()).thenReturn(requestBodyUriMock);
      when(requestBodyUriMock.uri("/case-reference")).thenReturn(requestBodySpec);
      when(requestBodySpec.retrieve()).thenReturn(responseMock);
      CaseReferenceSummary caseReferenceSummary = new CaseReferenceSummary().caseReferenceNumber(
          "123");
      when(responseMock.bodyToMono(CaseReferenceSummary.class)).thenReturn(
          Mono.just(caseReferenceSummary));

      final Mono<CaseReferenceSummary> result = ebsApiClient.postAllocateNextCaseReference();

      StepVerifier.create(result)
          .expectNext(caseReferenceSummary)
          .verifyComplete();

      verify(responseMock).bodyToMono(CaseReferenceSummary.class);
    }

    @Test
    @DisplayName("Should handle errors")
    void testPostAllocateNextCaseReference_handlesError() {
      when(webClientMock.post()).thenReturn(requestBodyUriMock);
      when(requestBodyUriMock.uri("/case-reference")).thenReturn(requestBodySpec);
      when(requestBodySpec.retrieve()).thenReturn(responseMock);
      CaseReferenceSummary caseReferenceSummary = new CaseReferenceSummary().caseReferenceNumber(
          "123");
      when(responseMock.bodyToMono(CaseReferenceSummary.class))
          .thenReturn(Mono.error(
              new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

      when(apiClientErrorHandler.handleApiRetrieveError(any(), eq("case reference"),
          any())).thenReturn(Mono.empty());

      final Mono<CaseReferenceSummary> result = ebsApiClient.postAllocateNextCaseReference();

      StepVerifier.create(result)
          .verifyComplete();

      verify(responseMock).bodyToMono(CaseReferenceSummary.class);
    }
  }

  @Nested
  @DisplayName("getClientStatus() Tests")
  class GetClientStatusTests {

    @Test
    @DisplayName("Should return successfully")
    void getClientStatus_Successful() {
      String transactionId = "123";
      String expectedUri = "/clients/status/{transactionId}";

      TransactionStatus mockTransactionStatus = new TransactionStatus();

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(expectedUri, transactionId)).thenReturn(
          requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.onStatus(any(), any())).thenReturn(responseMock);
      when(responseMock.bodyToMono(TransactionStatus.class)).thenReturn(
          Mono.just(mockTransactionStatus));

      Mono<TransactionStatus> transactionStatusMono = ebsApiClient.getClientStatus(transactionId);

      StepVerifier.create(transactionStatusMono)
          .expectNextMatches(transactionStatus -> transactionStatus == mockTransactionStatus)
          .verifyComplete();
    }

    @Test
    @DisplayName("Should handle error")
    void getClientStatus_Error() {
      String transactionId = "123";
      String expectedUri = "/clients/status/{transactionId}";

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(expectedUri, transactionId)).thenReturn(
          requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.onStatus(any(), any())).thenReturn(responseMock);
      when(responseMock.bodyToMono(TransactionStatus.class)).thenReturn(Mono.error(
          new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

      when(apiClientErrorHandler.handleApiRetrieveError(
          any(), eq("client transaction status"), eq("transaction id"), eq(transactionId)))
          .thenReturn(Mono.empty());

      Mono<TransactionStatus> transactionStatusMono = ebsApiClient.getClientStatus(transactionId);

      StepVerifier.create(transactionStatusMono)
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("getCaseStatus() Tests")
  class GetCaseStatusTests {

    @Test
    @DisplayName("Should return successfully")
    void getClientStatus_Successful() {
      String transactionId = "123";
      String expectedUri = "/cases/status/{transactionId}";

      TransactionStatus mockTransactionStatus = new TransactionStatus();

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(expectedUri, transactionId)).thenReturn(
          requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.onStatus(any(), any())).thenReturn(responseMock);
      when(responseMock.bodyToMono(TransactionStatus.class)).thenReturn(
          Mono.just(mockTransactionStatus));

      Mono<TransactionStatus> transactionStatusMono = ebsApiClient.getCaseStatus(transactionId);

      StepVerifier.create(transactionStatusMono)
          .expectNextMatches(transactionStatus -> transactionStatus == mockTransactionStatus)
          .verifyComplete();
    }

    @Test
    @DisplayName("Should handle error")
    void getClientStatus_Error() {
      String transactionId = "123";
      String expectedUri = "/cases/status/{transactionId}";

      when(webClientMock.get()).thenReturn(requestHeadersUriMock);
      when(requestHeadersUriMock.uri(expectedUri, transactionId)).thenReturn(
          requestHeadersMock);
      when(requestHeadersMock.retrieve()).thenReturn(responseMock);
      when(responseMock.onStatus(any(), any())).thenReturn(responseMock);
      when(responseMock.bodyToMono(TransactionStatus.class)).thenReturn(Mono.error(
          new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

      when(apiClientErrorHandler.handleApiRetrieveError(
          any(), eq("case transaction status"), eq("transaction id"), eq(transactionId)))
          .thenReturn(Mono.empty());

      Mono<TransactionStatus> transactionStatusMono = ebsApiClient.getCaseStatus(transactionId);

      StepVerifier.create(transactionStatusMono)
          .verifyComplete();
    }
  }
}
