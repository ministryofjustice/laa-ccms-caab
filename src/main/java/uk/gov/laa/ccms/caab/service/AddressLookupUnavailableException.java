package uk.gov.laa.ccms.caab.service;

/** Signals that the address provider could not return a usable lookup response. */
public class AddressLookupUnavailableException extends RuntimeException {

  public AddressLookupUnavailableException(final Throwable cause) {
    super("Address lookup is unavailable", cause);
  }
}
