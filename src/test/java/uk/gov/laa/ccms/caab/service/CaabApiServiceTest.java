package uk.gov.laa.ccms.caab.service;

import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
public class CaabApiServiceTest {

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
    private CaabApiServiceErrorHandler caabApiServiceErrorHandler;

    @InjectMocks
    private CaabApiService caabApiService;

    @Test
    void createApplication_success() {
        String loginId = "user1";
        ApplicationDetail application = new ApplicationDetail(); // Populate as needed
        String expectedUri = "/applications";

        when(caabApiWebClient.post()).thenReturn(requestBodyUriMock);
        when(requestBodyUriMock.uri(expectedUri)).thenReturn(requestBodyMock);
        when(requestBodyMock.header("Caab-User-Login-Id", loginId)).thenReturn(requestBodyMock);
        when(requestBodyMock.contentType(any(MediaType.class))).thenReturn(requestBodyMock);
        when(requestBodyMock.bodyValue(any(ApplicationDetail.class))).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.fromRunnable(() -> {}));

        Mono<Void> result = caabApiService.createApplication(loginId, application);

        StepVerifier.create(result)
                .verifyComplete();
    }

}
