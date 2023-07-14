package uk.gov.laa.ccms.caab.service;

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
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CATEGORY_OF_LAW;


@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
public class DataServiceTest {

    @Mock
    private WebClient webClientMock;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersMock;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriMock;
    @Mock
    private WebClient.ResponseSpec responseMock;

    @InjectMocks
    private DataService dataService;

    @Mock
    private DataServiceErrorHandler dataServiceErrorHandler;

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

        Mono<UserDetail> userDetailsMono = dataService.getUser(loginId);

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
        when(responseMock.bodyToMono(UserDetail.class)).thenReturn(Mono.error(new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

        when(dataServiceErrorHandler.handleUserError(eq(loginId), any(WebClientResponseException.class))).thenReturn(Mono.empty());

        Mono<UserDetail> userDetailsMono = dataService.getUser(loginId);

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

        Mono<CommonLookupDetail> commonValuesMono = dataService.getCommonValues(type, code, sort);

        StepVerifier.create(commonValuesMono)
                .expectNext(commonValues)
                .verifyComplete();

        Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
        URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

        // Assert the URI
        assertEquals("/common-lookup-values?type=type1&code=code1&sort=sort1", actualUri.toString());
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
        when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(Mono.error(new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)));

        when(dataServiceErrorHandler.handleCommonValuesError(eq(type), eq(code), eq(sort), any(WebClientResponseException.class))).thenReturn(Mono.empty());

        Mono<CommonLookupDetail> commonValuesMono = dataService.getCommonValues(type, code, sort);

        StepVerifier.create(commonValuesMono)
                .verifyComplete();

        Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
        URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

        // Assert the URI
        assertEquals("/common-lookup-values?type=type1&code=code1&sort=sort1", actualUri.toString());
    }

    @ParameterizedTest
    @CsvSource({"DP, 0",
                "ECF, 0",
                "SUBDP, 0",
                "test1, 1",
                "test2, 1"})
    void getApplicationTypes_checkType(String code, int expectedSize) {
        CommonLookupDetail commonValues = new CommonLookupDetail();
        List<CommonLookupValueDetail> content = new ArrayList<>();
        CommonLookupValueDetail commonValueDetails = new CommonLookupValueDetail();
        commonValueDetails.setCode(code);
        content.add(commonValueDetails);
        commonValues.setContent(content);

        ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

        when(webClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(Mono.just(commonValues));

        List<CommonLookupValueDetail> applicationTypes = dataService.getApplicationTypes();
        assertEquals(expectedSize, applicationTypes.size());

        Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
        URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());
        assertEquals("/common-lookup-values?type=XXCCMS_APP_AMEND_TYPES", actualUri.toString());
    }

    @Test
    void getCategoriesOfLaw_returnsData() {
        String type = COMMON_VALUE_CATEGORY_OF_LAW;
        CommonLookupDetail commonValues = new CommonLookupDetail();
        commonValues.addContentItem(new CommonLookupValueDetail().code("CAT1").type(type));
        commonValues.addContentItem(new CommonLookupValueDetail().code("CAT2").type(type));

        ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

        when(webClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(Mono.just(commonValues));

        final List<String> catCodes = new ArrayList<>();
        catCodes.add("CAT1");

        List<CommonLookupValueDetail> response = dataService.getCategoriesOfLaw(catCodes);

        Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
        URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

        // Assert the URI
        assertEquals(String.format("/common-lookup-values?type=%s", type), actualUri.toString());
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(commonValues.getContent().get(0), response.get(0));
    }

    @Test
    void getAllCategoriesOfLaw_returnsData() {
        String type = COMMON_VALUE_CATEGORY_OF_LAW;
        CommonLookupDetail commonValues = new CommonLookupDetail();
        commonValues.addContentItem(new CommonLookupValueDetail().code("CAT1").type(type));
        commonValues.addContentItem(new CommonLookupValueDetail().code("CAT2").type(type));

        ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);

        when(webClientMock.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(CommonLookupDetail.class)).thenReturn(Mono.just(commonValues));

        List<CommonLookupValueDetail> response = dataService.getAllCategoriesOfLaw();

        Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
        URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

        // Assert the URI
        assertEquals(String.format("/common-lookup-values?type=%s", type), actualUri.toString());
        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals(commonValues.getContent().get(0), response.get(0));
        assertEquals(commonValues.getContent().get(1), response.get(1));
    }

}
