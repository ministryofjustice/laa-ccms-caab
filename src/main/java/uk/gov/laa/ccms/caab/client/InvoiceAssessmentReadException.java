package uk.gov.laa.ccms.caab.client;

import org.springframework.http.HttpStatus;

/** Retains read failure details for classification without exposing them in error-page logs. */
public class InvoiceAssessmentReadException extends EbsApiClientException {

  public InvoiceAssessmentReadException(final String message, final HttpStatus status) {
    super(message, status);
  }

  public InvoiceAssessmentReadException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
