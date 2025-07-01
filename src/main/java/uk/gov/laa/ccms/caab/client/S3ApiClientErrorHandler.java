package uk.gov.laa.ccms.caab.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Provides error-handling capabilities for AWS S3 API interactions. */
@Component
@Slf4j
public class S3ApiClientErrorHandler {

  private static final String S3_FAILURE_MSG =
      "An error occurred when processing the request to " + "S3.";

  private static final String S3_FILE_NOT_FOUND_MSG = "The document specified was not found in S3.";

  /**
   * Handles an exception response from the S3 request, wrapping and throwing a {@link
   * S3ApiClientException}.
   *
   * @param e the exception thrown during the API operation.
   * @throws S3ApiClientException wrapping the original throwable.
   */
  public void handleS3ApiError(final Throwable e) throws S3ApiClientException {
    log.error(S3_FAILURE_MSG, e);
    throw new S3ApiClientException(S3_FAILURE_MSG, e);
  }

  /**
   * Handles a file not found related exception response from the S3 request, wrapping and throwing
   * a {@link S3ApiFileNotFoundException}.
   *
   * @param e the exception thrown during the API operation.
   * @throws S3ApiFileNotFoundException wrapping the original throwable.
   */
  public void handleFileNotFoundError(final Throwable e) throws S3ApiFileNotFoundException {
    log.error(S3_FILE_NOT_FOUND_MSG, e);
    throw new S3ApiFileNotFoundException(S3_FILE_NOT_FOUND_MSG, e);
  }
}
