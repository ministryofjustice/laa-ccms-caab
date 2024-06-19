package uk.gov.laa.ccms.caab.client;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
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
  private static final String RESOURCE_TYPE_ASSESSMENT_CHECKPOINT = "assessment checkpoint";

  /**
   * Get assessments from the assessment API.
   *
   * @param assessmentNames the list of assessment names to filter
   * @param providerId the provider id
   * @param caseReferenceNumber the case reference number
   * @return the assessment details
   */
  public Mono<AssessmentDetails> getAssessments(
      final List<String> assessmentNames,
      final String providerId,
      final String caseReferenceNumber) {

    final MultiValueMap<String, String> queryParams =
        retrieveAssessmentsQueryParams(assessmentNames, providerId, caseReferenceNumber, null);

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
   * Delete assessments from the assessment API.
   *
   * @param assessmentNames the list of assessment names to filter
   * @param providerId the provider id
   * @param caseReferenceNumber the case reference number
   * @param status the status of the assessment
   * @param userLoginId the login ID of the user performing the delete
   * @return a Mono of Void
   */
  public Mono<Void> deleteAssessments(
      final List<String> assessmentNames,
      final String providerId,
      final String caseReferenceNumber,
      final String status,
      final String userLoginId) {

    final MultiValueMap<String, String> queryParams =
        retrieveAssessmentsQueryParams(assessmentNames, providerId, caseReferenceNumber, status);

    return assessmentApiWebClient
        .delete()
        .uri(uriBuilder -> uriBuilder
            .path("/assessments")
            .queryParams(queryParams)
            .build())
        .header("Caab-User-Login-Id", userLoginId)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> assessmentApiClientErrorHandler
            .handleApiDeleteError(e, RESOURCE_TYPE_ASSESSMENT, queryParams));
  }

  /**
   * Creates a new assessment.
   *
   * @param assessment the assessment details
   * @param userLoginId the login ID of the user
   * @return a Mono that completes when the creation is finished
   */
  public Mono<Void> createAssessment(
      final AssessmentDetail assessment,
      final String userLoginId) {

    return assessmentApiWebClient
        .post()
        .uri("/assessments")
        .header("Caab-User-Login-Id", userLoginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(assessment)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> assessmentApiClientErrorHandler
            .handleApiCreateError(e, RESOURCE_TYPE_ASSESSMENT));
  }

  /**
   * Updates an existing assessment.
   *
   * @param assessmentId the ID of the assessment to update
   * @param assessment the updated assessment details
   * @param userLoginId the login ID of the user
   * @return a Mono that completes when the update is finished
   */
  public Mono<Void> updateAssessment(
      final Long assessmentId,
      final AssessmentDetail assessment,
      final String userLoginId) {

    return assessmentApiWebClient
        .put()
        .uri("/assessments/{assessment-id}", assessmentId)
        .header("Caab-User-Login-Id", userLoginId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(assessment)
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> assessmentApiClientErrorHandler
            .handleApiUpdateError(e,
                RESOURCE_TYPE_ASSESSMENT,
                "id",
                String.valueOf(assessmentId)));
  }

  /**
   * Retrieve the query parameters for the assessments API assessments endpoints.
   *
   * @param assessmentNames the list of assessment names to filter
   * @param providerId the provider id
   * @param caseReferenceNumber the case reference number
   * @param status the status of the assessment
   * @return the query parameters
   */
  private MultiValueMap<String, String> retrieveAssessmentsQueryParams(
      final List<String> assessmentNames, final String providerId, final String caseReferenceNumber,
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
    return queryParams;
  }

  /**
   * Update an assessment.
   *
   * @param assessmentId The ID of the assessment to update.
   * @param userLoginId The login ID of the user performing the update.
   * @param patch The updated assessment details.
   * @return a Mono of AssessmentDetails containing the updated assessment.
   */
  public Mono<Void> patchAssessment(
      final Long assessmentId,
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
                String.valueOf(assessmentId)));
  }



}
