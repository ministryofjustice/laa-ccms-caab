package uk.gov.laa.ccms.caab.model;

import java.util.List;
import lombok.Data;

/**
 * Represents the display details for client address results.
 */
@Data
public class ClientAddressResultsDisplay {

  private List<ClientAddressResultRowDisplay> content;
}
