package uk.gov.laa.ccms.caab.client;


import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

/**
 * Provides error-handling capabilities for the API client interactions.
 */
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
  public <T> Mono<T> handleApiCreateError(
      final Throwable e, final String resourceType) {
    final String message = String.format(
        "Failed to create %s",
        resourceType);
    log.error(message, e);
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
    final String message = String.format(
        "Failed to delete %s with %s: %s",
        resourceType,
        resourceIdType,
        resourceId);
    log.error(message, e);
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

    final StringBuilder messageBuilder = new StringBuilder(
        String.format("Failed to delete %s",
            resourceType));

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
    final String message = String.format(
        "Failed to update %s with %s: %s",
        resourceType,
        resourceIdType,
        resourceId);
    log.error(message, e);
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
  public  <T> Mono<T> handleNotFoundError(
      final ClientResponse clientResponse,
      final String resourceType,
      final String resourceIdType,
      final String resourceId) {
    final String message = String.format(
        "Not found: %s with %s: %s",
        resourceType,
        resourceIdType,
        resourceId);
    log.error(message);
    return Mono.error(createException(message,
        HttpStatus.resolve(clientResponse.statusCode().value())));
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
  public  <T> Mono<T> handleApiRetrieveError(
      final Throwable e,
      final String resourceType,
      final String resourceIdType,
      final String resourceId) {
    if (e instanceof ApiClientException) {
      return Mono.error(e);
    }
    final String message = String.format(
        "Failed to retrieve %s with %s: %s",
        resourceType,
        resourceIdType,
        resourceId);
    log.error(message, e);
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

    final StringBuilder messageBuilder = new StringBuilder(
        String.format("Failed to retrieve %s", resourceType));

    return parameterizedError(e, queryParams, messageBuilder);
  }

  private <T> Mono<T> parameterizedError(
      final Throwable e,
      final MultiValueMap<String, String> queryParams,
      final StringBuilder messageBuilder) {

    if (queryParams != null && !queryParams.isEmpty()) {
      final String paramsString = queryParams.entrySet().stream()
          .flatMap(entry -> entry.getValue().stream().map(value -> entry.getKey() + "=" + value))
          .collect(Collectors.joining(", "));
      messageBuilder.append(" with parameters: ").append(paramsString);
    }

    final String message = messageBuilder.toString();
    log.error(message, e);

    return Mono.error(createException(message, e));
  }

}
