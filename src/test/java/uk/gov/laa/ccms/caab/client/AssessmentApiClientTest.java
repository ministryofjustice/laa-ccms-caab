package uk.gov.laa.ccms.caab.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.PatchAssessmentDetail;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class AssessmentApiClientTest {

  @Mock
  private WebClient assessmentApiWebClient;

  @Mock
  private WebClient.RequestBodyUriSpec requestBodyUriMock;

  @Mock
  private WebClient.RequestBodySpec requestBodyMock;

  @Mock
  private WebClient.RequestHeadersSpec requestHeadersMock;

  @Mock
  private WebClient.RequestHeadersUriSpec requestHeadersUriMock;

  @Mock
  private WebClient.ResponseSpec responseMock;

  @Mock
  private AssessmentApiClientErrorHandler apiClientErrorHandler;

  @InjectMocks
  private AssessmentApiClient assessmentApiClient;

  @Test
  void getAssessments_success() {
    final String assessmentName = "meansAssessment";
    final String providerId = "987";
    final String caseReferenceNumber = "case456";
    final AssessmentDetails mockAssessmentDetails = new AssessmentDetails();

    when(assessmentApiWebClient.get()).thenReturn(requestHeadersUriMock);
    final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(AssessmentDetails.class)).thenReturn(Mono.just(mockAssessmentDetails));

    final Mono<AssessmentDetails> result = assessmentApiClient.getAssessments(List.of(assessmentName), providerId, caseReferenceNumber);

    StepVerifier.create(result)
        .expectNext(mockAssessmentDetails)
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    assertEquals("/assessments?name=meansAssessment&provider-id=987&case-reference-number=case456", actualUri.toString());
  }

  @Test
  @DisplayName("createAssessment succeeds when called with valid assessment and loginId")
  void createAssessment_success() {
    final AssessmentDetail assessmentDetail = new AssessmentDetail(); // Populate this as needed
    final String userLoginId = "user123";

    when(assessmentApiWebClient.post()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri("/assessments")).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", userLoginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(assessmentDetail)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = assessmentApiClient.createAssessment(assessmentDetail, userLoginId);

    StepVerifier.create(result)
        .verifyComplete();

    verify(responseMock).bodyToMono(Void.class);
  }


  @Test
  @DisplayName("updateAssessment succeeds when called with valid assessmentId, assessment, and loginId")
  void updateAssessment_success() {
    final Long assessmentId = 123L;
    final AssessmentDetail assessmentDetail = new AssessmentDetail(); // Populate this as needed
    final String userLoginId = "user123";

    when(assessmentApiWebClient.put()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri("/assessments/{assessment-id}", assessmentId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", userLoginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(assessmentDetail)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = assessmentApiClient.updateAssessment(assessmentId, assessmentDetail, userLoginId);

    StepVerifier.create(result)
        .verifyComplete();

    verify(responseMock).bodyToMono(Void.class);
  }

  @Test
  void patchAssessment_success() {
    final Long assessmentId = 123L;
    final String userLoginId = "user456";
    final PatchAssessmentDetail patchDetails = new PatchAssessmentDetail(); // Populate this as needed for the test

    when(assessmentApiWebClient.patch()).thenReturn(requestBodyUriMock);
    when(requestBodyUriMock.uri("/assessments/{assessment-id}", assessmentId)).thenReturn(requestBodyMock);
    when(requestBodyMock.header("Caab-User-Login-Id", userLoginId)).thenReturn(requestBodyMock);
    when(requestBodyMock.bodyValue(patchDetails)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = assessmentApiClient.patchAssessment(assessmentId, userLoginId, patchDetails);

    StepVerifier.create(result)
        .verifyComplete();

    verify(responseMock).bodyToMono(Void.class);
  }

  @Test
  void deleteAssessments_success() {
    final List<String> assessmentNames = List.of("meansAssessment", "meritsAssessment");
    final String providerId = "987";
    final String caseReferenceNumber = "case456";
    final String status = "PENDING";
    final String userLoginId = "user789";

    when(assessmentApiWebClient.delete()).thenReturn(requestHeadersUriMock);
    final ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor = ArgumentCaptor.forClass(Function.class);
    when(requestHeadersUriMock.uri(uriCaptor.capture())).thenReturn(requestHeadersMock);
    when(requestHeadersMock.header("Caab-User-Login-Id", userLoginId)).thenReturn(requestHeadersMock);
    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
    when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());

    final Mono<Void> result = assessmentApiClient.deleteAssessments(
        assessmentNames, providerId, caseReferenceNumber, status, userLoginId
    );

    StepVerifier.create(result)
        .verifyComplete();

    final Function<UriBuilder, URI> uriFunction = uriCaptor.getValue();
    final URI actualUri = uriFunction.apply(UriComponentsBuilder.newInstance());

    assertEquals("/assessments?name=meansAssessment,meritsAssessment&provider-id=987&case-reference-number=case456&status=PENDING", actualUri.toString());
  }



  
}