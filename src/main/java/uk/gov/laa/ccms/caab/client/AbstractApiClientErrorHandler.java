package uk.gov.laa.ccms.caab.client;

import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.logging.LogMasker;
import uk.gov.laa.ccms.caab.logging.UserIdentifier;

/** Provides error-handling capabilities for the API client interactions. */
@Slf4j
public abstract class AbstractApiClientErrorHandler {

  /**
   * Abstract method used to create an exception with a specified message and cause.
   *
   * @param message the detail message for the exception.
   * @param cause the cause of the exception.
   * @return a new instance of RuntimeException.
   */
  protected abstract RuntimeException createException(final String message, final Throwable cause);

  /**
   * Abstract method used to create an exception with a specified message and http status code.
   *
   * @param message the detail message for the exception.
   * @param httpStatus the http status of the response.
   * @return a new instance of RuntimeException.
   */
  protected abstract RuntimeException createException(final String message, HttpStatus httpStatus);

  /**
   * Handles errors occurring during API create operations.
   *
   * @param e the exception thrown during the API operation.
   * @param resourceType the type of resource involved in the create operation.
   * @param <T> the type of the response expected from the operation.
   * @return a Mono error signaling the exception.
   */
  public <T> Mono<T> handleApiCreateError(final Throwable e, final String resourceType) {
    final String message = "Failed to create %s".formatted(resourceType);
    log.error("{}{}", message, responseBody(e), e);
    if (e instanceof WebClientResponseException webClientResponseException) {
      final HttpStatus httpStatus =
          HttpStatus.resolve(webClientResponseException.getStatusCode().value());
      if (httpStatus != null) {
        return Mono.error(createException(message, httpStatus));
      }
    }
    return Mono.error(createException(message, e));
  }

  /**
   * Handles errors occurring during API delete operations.
   *
   * @param e the exception thrown during the API operation.
   * @param resourceType the type of resource involved in the delete operation.
   * @param resourceIdType the type of the resource ID.
   * @param resourceId the ID of the resource.
   * @param <T> the type of the response expected from the operation.
   * @return a Mono error signaling the exception.
   */
  public <T> Mono<T> handleApiDeleteError(
      final Throwable e,
      final String resourceType,
      final String resourceIdType,
      final String resourceId) {
    final String message =
        "Failed to delete %s with %s: %s"
            .formatted(resourceType, resourceIdType, identifierForLog(resourceIdType, resourceId));
    log.error("{}{}", message, responseBody(e), e);
    return Mono.error(createException(message, e));
  }

  /**
   * Handles errors occurring during API delete operations using query parameters.
   *
   * @param e the exception thrown during the API operation.
   * @param resourceType the type of resource involved in the delete operation.
   * @param queryParams the query parameters used in the delete operation.
   * @param <T> the type of the response expected from the operation.
   * @return a Mono error signaling the exception.
   */
  public <T> Mono<T> handleApiDeleteError(
      final Throwable e,
      final String resourceType,
      final MultiValueMap<String, String> queryParams) {

    final StringBuilder messageBuilder =
        new StringBuilder("Failed to delete %s".formatted(resourceType));

    return parameterizedError(e, queryParams, messageBuilder);
  }

  /**
   * Handles errors occurring during API update operations.
   *
   * @param e the exception thrown during the API operation.
   * @param resourceType the type of resource involved in the update operation.
   * @param resourceIdType the type of the resource ID.
   * @param resourceId the ID of the resource.
   * @param <T> the type of the response expected from the operation.
   * @return a Mono error signaling the exception.
   */
  public <T> Mono<T> handleApiUpdateError(
      final Throwable e,
      final String resourceType,
      final String resourceIdType,
      final String resourceId) {
    final String message =
        "Failed to update %s with %s: %s"
            .formatted(resourceType, resourceIdType, identifierForLog(resourceIdType, resourceId));
    log.error("{}{}", message, responseBody(e), e);
    return Mono.error(createException(message, e));
  }

  /**
   * Handles not found errors occurring during API retrieve operations for a specific resource.
   *
   * @param clientResponse the error response for the API operation.
   * @param resourceType the type of resource involved in the retrieve operation.
   * @param resourceIdType the type of the resource ID.
   * @param resourceId the ID of the resource.
   * @param <T> the type of the response expected from the operation.
   * @return a Mono error signaling the exception.
   */
  public <T> Mono<T> handleNotFoundError(
      final ClientResponse clientResponse,
      final String resourceType,
      final String resourceIdType,
      final String resourceId) {
    final String message =
        "Not found: %s with %s: %s"
            .formatted(resourceType, resourceIdType, identifierForLog(resourceIdType, resourceId));
    log.error(message);
    return Mono.error(
        createException(message, HttpStatus.resolve(clientResponse.statusCode().value())));
  }

  /**
   * Handles errors occurring during API retrieve operations for a specific resource.
   *
   * @param e the exception thrown during the API operation.
   * @param resourceType the type of resource involved in the retrieve operation.
   * @param resourceIdType the type of the resource ID.
   * @param resourceId the ID of the resource.
   * @param <T> the type of the response expected from the operation.
   * @return a Mono error signaling the exception.
   */
  public <T> Mono<T> handleApiRetrieveError(
      final Throwable e,
      final String resourceType,
      final String resourceIdType,
      final String resourceId) {
    if (e instanceof ApiClientException) {
      return Mono.error(e);
    }
    final String message =
        "Failed to retrieve %s with %s: %s"
            .formatted(resourceType, resourceIdType, identifierForLog(resourceIdType, resourceId));
    log.error("{}{}", message, responseBody(e), e);
    return Mono.error(createException(message, e));
  }

  /**
   * Handles errors occurring during API retrieve operations using query parameters.
   *
   * @param e the exception thrown during the API operation.
   * @param resourceType the type of resource involved in the retrieve operation.
   * @param queryParams the query parameters used in the retrieve operation.
   * @param <T> the type of the response expected from the operation.
   * @return a Mono error signaling the exception.
   */
  public <T> Mono<T> handleApiRetrieveError(
      final Throwable e,
      final String resourceType,
      final MultiValueMap<String, String> queryParams) {

    if (e instanceof ApiClientException) {
      return Mono.error(e);
    }

    final StringBuilder messageBuilder =
        new StringBuilder("Failed to retrieve %s".formatted(resourceType));

    return parameterizedError(e, queryParams, messageBuilder);
  }

  private <T> Mono<T> parameterizedError(
      final Throwable e,
      final MultiValueMap<String, String> queryParams,
      final StringBuilder messageBuilder) {

    if (queryParams != null && !queryParams.isEmpty()) {
      messageBuilder.append(" with parameters: ").append(String.join(", ", queryParams.keySet()));
    }

    final String message = messageBuilder.toString();
    log.error("{}{}", message, responseBody(e), e);

    return Mono.error(createException(message, e));
  }

  /**
   * Returns an identifier safe to put in a log line or exception message. A login id is an email
   * style identifier, so it is pseudonymised the way user events are; other identifiers are opaque
   * references that support staff need in order to find the record.
   */
  private static String identifierForLog(final String resourceIdType, final String resourceId) {
    return resourceIdType != null && resourceIdType.toLowerCase(Locale.ROOT).contains("login")
        ? UserIdentifier.pseudonymise(resourceId)
        : resourceId;
  }

  /**
   * The body the API sent back with its error, appended to the log message.
   *
   * <p>Without it a 4xx says only that the request was rejected, leaving the field at fault to be
   * guessed at - for a validation failure the body names it.
   *
   * @param e the exception thrown during the API operation.
   * @return the response body, prefixed for the log, or an empty string when there is none.
   */
  private String responseBody(final Throwable e) {
    if (e instanceof WebClientResponseException webClientResponseException) {
      final String body = webClientResponseException.getResponseBodyAsString();
      if (!body.isBlank()) {
        return " - the API responded: " + LogMasker.errorSummary(body);
      }
    }
    return "";
  }
}
