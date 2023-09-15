package uk.gov.laa.ccms.caab.bean;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Represents the details of a client address search.
 */
@Data
@RequiredArgsConstructor
public class ClientAddressSearchDetails {

  private String uprn;
}
