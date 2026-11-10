package uk.gov.laa.ccms.caab.client;

/** Indicates that the OS API returned an incomplete or inconsistent response. */
public class OrdinanceSurveyResponseException extends RuntimeException {

  public OrdinanceSurveyResponseException(final String message) {
    super(message);
  }
}
