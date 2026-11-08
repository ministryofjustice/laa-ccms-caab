package uk.gov.laa.ccms.caab.bean;

import java.util.Objects;

/** Server-resolved journey context and address purpose; not an authorization credential. */
public record AddressFlowOwner(String context, String purpose) {

  public AddressFlowOwner {
    Objects.requireNonNull(context, "Owner context is required");
    Objects.requireNonNull(purpose, "Address purpose is required");
    if (context.isBlank() || purpose.isBlank()) {
      throw new IllegalArgumentException("Owner context and address purpose must not be blank");
    }
  }
}
