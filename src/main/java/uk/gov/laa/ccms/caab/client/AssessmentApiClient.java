package uk.gov.laa.ccms.caab.client;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetails;
import uk.gov.laa.ccms.caab.assessment.model.PatchAssessmentDetail;

/**
 * Client responsible for interactions with the Assessment API.
 */
@Service
@RequiredArgsConstructor
public class AssessmentApiClient {
  private final WebClient assessmentApiWebClient;

  private final AssessmentApiClientErrorHandler assessmentApiClientErrorHandler;
  private static final String RESOURCE_TYPE_ASSESSMENT = "assessments";

  /**
   * Get assessments from the assessment API.
   *
   * @param assessmentNames the list of assessment names to filter
   * @param providerId the provider id
   * @param caseReferenceNumber the case reference number
   * @param status the status of the assessment
   * @return the assessment details
   */
  public Mono<AssessmentDetails> getAssessments(
      final List<String> assessmentNames,
      final String providerId,
      final String caseReferenceNumber,
      final String status) {

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    Optional.ofNullable(assessmentNames)
        .ifPresent(names -> queryParams.add("name", String.join(",", names)));
    Optional.ofNullable(providerId)
        .ifPresent(param -> queryParams.add("provider-id", param));
    Optional.ofNullable(caseReferenceNumber)
        .ifPresent(param -> queryParams.add("case-reference-number", param));
    Optional.ofNullable(status)
        .ifPresent(param -> queryParams.add("status", param));

    return assessmentApiWebClient
        .get()
        .uri(uriBuilder -> uriBuilder
            .path("/assessments")
            .queryParams(queryParams)
            .build())
        .retrieve()
        .bodyToMono(AssessmentDetails.class)
        .onErrorResume(e -> assessmentApiClientErrorHandler
          .handleApiRetrieveError(e, RESOURCE_TYPE_ASSESSMENT, queryParams));
  }

  /**
   * Update an assessment.
   *
   * @param assessmentId The ID of the assessment to update.
   * @param userLoginId The login ID of the user performing the update.
   * @param patch The updated assessment details.
   * @return a Mono of AssessmentDetails containing the updated assessment.
   */
  public Mono<Void> updateAssessment(
      final String assessmentId,
      final String userLoginId,
      final PatchAssessmentDetail patch) {

    return assessmentApiWebClient
        .patch()
        .uri("/assessments/{assessment-id}", assessmentId)
        .header("Caab-User-Login-Id", userLoginId)
        .bodyValue(patch)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> assessmentApiClientErrorHandler
            .handleApiUpdateError(
                e,
                RESOURCE_TYPE_ASSESSMENT,
                "assessment-id",
                assessmentId));
  }



}
