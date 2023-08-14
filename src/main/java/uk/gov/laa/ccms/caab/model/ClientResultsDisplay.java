package uk.gov.laa.ccms.caab.model;

import java.util.List;
import lombok.Data;


/**
 * Represents the display details for client results.
 */
@Data
public class ClientResultsDisplay {

  private List<ClientResultRowDisplay> content;
  private Integer totalPages;
  private Integer totalElements;
  private Integer number;
  private Integer size;

}
